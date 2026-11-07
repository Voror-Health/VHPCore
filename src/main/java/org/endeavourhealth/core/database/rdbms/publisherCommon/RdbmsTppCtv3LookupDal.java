package org.endeavourhealth.core.database.rdbms.publisherCommon;

import org.apache.commons.io.FilenameUtils;
import org.endeavourhealth.common.utility.FileHelper;
import org.endeavourhealth.core.database.dal.publisherCommon.TppCtv3LookupDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.models.TppCtv3Lookup;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

public class RdbmsTppCtv3LookupDal implements TppCtv3LookupDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsTppCtv3LookupDal.class);

    @Override
    public TppCtv3Lookup getContentFromCtv3Code(String ctv3Code) throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT ctv3_code, ctv3_term"
                    + " FROM tpp_ctv3_lookup_2 "
                    + " WHERE ctv3_code = ?";

            ps = connection.prepareStatement(sql);
            ps.setString(1, ctv3Code);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int col = 1;
                String code = rs.getString(col++);
                String term = rs.getString(col++);

                TppCtv3Lookup ret = new TppCtv3Lookup();
                ret.setCtv3Code(code);
                ret.setCtv3Text(term);
                return ret;

            } else {
                return null;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    /**
     * new-style approach to load data into reference and stating tables using bulk operations
     * - copy file from S3 to local temp dir
     * - use MySQL bulk load command to get into temp table
     * - use SQL to update reference table
     * - drop temp table
     * - delete temp file
     *
     * NOTE: the RowIdentifier on the TPP file is inconsistent. Each copy of the file has completely
     * new identifiers, so this field cannot be used as a unique ID to handle new records and updated
     */
    @Override
    public void updateLookupTable(String filePath, Date dataDate) throws Exception {

        long msStart = System.currentTimeMillis();

        //copy the file from S3 to local disk
        File f = FileHelper.copyFileFromStorageToTempDirIfNecessary(filePath);
        filePath = f.getAbsolutePath();

        Connection connection = ConnectionManager.getPublisherCommonNonPooledConnection();
        //create a temporary table to load the data into
        String tempTableName = ConnectionManager.generateTempTableName(FilenameUtils.getBaseName(filePath));
        try {
            //turn on auto commit so we don't need to separately commit these large SQL operations
            connection.setAutoCommit(true);

            //LOG.debug("Loading " + f + " into " + tempTableName);
            String sql = "CREATE TABLE " + tempTableName + " ("
                    + "RowIdentifier int, "
                    + "IDOrganisationVisibleTo varchar(255), "
                    + "Ctv3Code varchar(5) binary, "
                    + "Ctv3Text varchar(255), "
                    + "RemovedData int, "
                    + "key_exists boolean DEFAULT FALSE, "
                    + "CONSTRAINT pk PRIMARY KEY (Ctv3Code), "
                    + "KEY ix_key_exists (key_exists))";
            Statement statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //bulk load temp table
            //LOG.debug("Starting bulk load into " + tempTableName);
            sql = "LOAD DATA LOCAL INFILE '" + filePath.replace("\\", "\\\\") + "'"
                    + " INTO TABLE " + tempTableName
                    + " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\\\"'"
                    + " LINES TERMINATED BY '\\r\\n'"
                    + " IGNORE 1 LINES";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //work out which records already exist in the target table
            //LOG.debug("Finding records that exist in tpp_ctv3_lookup_2");
            sql = "UPDATE " + tempTableName + " s"
                    + " INNER JOIN tpp_ctv3_lookup_2 t"
                    + " ON t.ctv3_code = s.Ctv3Code"
                    + " SET s.key_exists = true";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //insert records into the target table where the staging has new records
            //LOG.debug("Copying new records into target table tpp_ctv3_lookup_2");
            sql = "INSERT IGNORE INTO tpp_ctv3_lookup_2 (ctv3_code, ctv3_term, dt_last_updated)"
                    + " SELECT Ctv3Code, Ctv3Text, " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName
                    + " WHERE key_exists = false";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //update any records that previously existed, but may have a changed term and set the latest dt_last_updated (SD-781)
            //LOG.debug("Updating existing records in target table tpp_ctv3_lookup_2");
            sql = "UPDATE tpp_ctv3_lookup_2 t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.ctv3_code = s.Ctv3Code"
                    + " SET t.ctv3_term = s.Ctv3Text, t.dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true)
                    + " WHERE dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            long msEnd = System.currentTimeMillis();
            LOG.debug("Update of tpp_ctv3_lookup_2 Completed in " + ((msEnd-msStart)/1000) + "s");

        } finally {
            //MUST change this back to false
            connection.setAutoCommit(false);
            connection.close();

            ConnectionManager.dropTempTable(tempTableName, ConnectionManager.Db.PublisherCommon);

            //delete the temp file
            FileHelper.deleteFileFromTempDirIfNecessary(f);
        }
    }

    @Override
    public Date getMostRecentCtv3CodeDate() throws Exception {
        DeadlockHandler h = new DeadlockHandler();
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                return getMostRecentCtv3CodeDateImpl();

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }


    public Date getMostRecentCtv3CodeDateImpl() throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT MAX(dt_last_updated)"
                    + " FROM tpp_ctv3_lookup_2";

            ps = connection.prepareStatement(sql);
            //ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getTimestamp(1) != null) {
                return new java.util.Date(rs.getTimestamp(1).getTime());

            } else {
                return null;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }
}
