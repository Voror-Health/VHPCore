package org.endeavourhealth.core.database.rdbms.publisherCommon;

import org.apache.commons.io.FilenameUtils;
import org.endeavourhealth.common.utility.FileHelper;
import org.endeavourhealth.core.database.dal.publisherCommon.TppMultilexLookupDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.models.TppMultilexProductToCtv3Map;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

public class RdbmsTppMultilexLookupDal implements TppMultilexLookupDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsTppMultilexLookupDal.class);

    @Override
    public TppMultilexProductToCtv3Map getMultilexToCtv3MapForProductId(int multiLexProductId) throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT multilex_product_id, ctv3_code, ctv3_term"
                    + " FROM tpp_multilex_to_ctv3_map_2"
                    + " WHERE multilex_product_id = ?";
            ps = connection.prepareStatement(sql);

            ps.setInt(1, multiLexProductId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int col = 1;

                TppMultilexProductToCtv3Map ret = new TppMultilexProductToCtv3Map();
                ret.setMultiLexProductId(rs.getInt(col++));
                ret.setCtv3ReadCode(rs.getString(col++));
                ret.setCtv3ReadTerm(rs.getString(col++));
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
    public void updateProductIdToCtv3LookupTable(String filePath, Date dataDate) throws Exception {

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
                    + "IDMultiLexProduct int, "
                    + "DrugReadCode varchar(255) BINARY, "
                    + "DrugReadCodeDesc varchar(255), "
                    + "RemovedData int, "
                    + "key_exists boolean DEFAULT FALSE, "
                    + "CONSTRAINT pk PRIMARY KEY (IDMultiLexProduct), "
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
            //LOG.debug("Finding records that exist in tpp_multilex_to_ctv3_map_2");
            sql = "UPDATE " + tempTableName + " s"
                    + " INNER JOIN tpp_multilex_to_ctv3_map_2 t"
                    + " ON t.multilex_product_id = s.IDMultiLexProduct"
                    + " SET s.key_exists = true";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();


            //insert records into the target table where the staging has new records
            //LOG.debug("Copying new records into target table tpp_multilex_to_ctv3_map_2");
            sql = "INSERT IGNORE INTO tpp_multilex_to_ctv3_map_2 (multilex_product_id, ctv3_code, ctv3_term, dt_last_updated)"
                    + " SELECT IDMultiLexProduct, DrugReadCode, DrugReadCodeDesc, " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName
                    + " WHERE key_exists = false";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //update any records that previously existed, but have a changed term
            //LOG.debug("Updating existing records in target table tpp_multilex_to_ctv3_map_2");
            sql = "UPDATE tpp_multilex_to_ctv3_map_2 t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.multilex_product_id = s.IDMultiLexProduct"
                    + " SET t.ctv3_code = s.DrugReadCode,"
                    + " t.ctv3_term = s.DrugReadCodeDesc,"
                    + " t.dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true)
                    + " WHERE t.dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            long msEnd = System.currentTimeMillis();
            LOG.debug("Update of tpp_multilex_to_ctv3_map_2 Completed in " + ((msEnd-msStart)/1000) + "s");

        } finally {
            //MUST change this back to false
            connection.setAutoCommit(false);
            connection.close();

            ConnectionManager.dropTempTable(tempTableName, ConnectionManager.Db.PublisherCommon);

            //delete the temp file
            FileHelper.deleteFileFromTempDirIfNecessary(f);
        }
    }

    /**
     * the tpp_multilex_action_group_lookup table provides a lookup for the action group name for the given ID
     * NOTE: this lookup was populated using a one-off dump taken from SystmOne and is not updated as part of the
     * regular SystmOne extract process
     */
    @Override
    public String getMultilexActionGroupNameForId(int actionGroupId) throws Exception {

        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT action_group_name"
                    + " FROM tpp_multilex_action_group_lookup"
                    + " WHERE action_group_id = ?";
            ps = connection.prepareStatement(sql);

            ps.setInt(1, actionGroupId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString(1);
                return name;

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
