package org.endeavourhealth.core.database.rdbms.publisherCommon;

import org.apache.commons.io.FilenameUtils;
import org.endeavourhealth.common.utility.FileHelper;
import org.endeavourhealth.core.database.dal.publisherCommon.EmisUserInRoleDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.models.EmisUserInRole;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class RdbmsEmisUserInRoleDal implements EmisUserInRoleDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsEmisOrganisationDal.class);


    @Override
    public Set<String> retrieveAllIds() throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT user_in_role_guid FROM emis_user_in_role";
            ps = connection.prepareStatement(sql);

            Set<String> ret = new HashSet<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString(1));
            }

            return ret;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    @Override
    public Set<EmisUserInRole> retrieveRecordsForIds(Set<String> hsIds) throws Exception {
        
        List<String> ids = new ArrayList<>(hsIds);

        Connection connection = ConnectionManager.getPublisherCommonNonPooledConnection();
        PreparedStatement ps = null;
        try {

            String sql = "SELECT user_in_role_guid, organisation_guid, title, given_name,"
                    + " surname, job_category_code, job_category_name, contract_start_date, contract_end_date,"
                    + " published_file_id, published_file_record_number, dt_last_updated"
                    + " FROM emis_user_in_role"
                    + " WHERE user_in_role_guid IN (";
            for (int i=0; i<ids.size(); i++) {
                if (i>0) {
                    sql += ", ";
                }
                sql += "?";
            }
            sql += ")";

            ps = connection.prepareStatement(sql);

            int col = 1;
            for (int i=0; i<ids.size(); i++) {
                String id = ids.get(i);
                ps.setString(col++, id);
            }

            Set<EmisUserInRole> ret = new HashSet<>();

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                col = 1;

                EmisUserInRole u = new EmisUserInRole();

                u.setUserInRoleGuid(rs.getString(col++));
                u.setOrganisationGuid(rs.getString(col++));
                u.setTitle(rs.getString(col++));
                u.setGivenName(rs.getString(col++));
                u.setSurname(rs.getString(col++));
                u.setJobCategoryCode(rs.getString(col++));
                u.setJobCategoryName(rs.getString(col++));

                java.sql.Timestamp ts = rs.getTimestamp(col++);
                if (ts != null) {
                    u.setContractStartDate(new java.util.Date(ts.getTime()));
                }

                ts = rs.getTimestamp(col++);
                if (ts != null) {
                    u.setContractEndDate(new java.util.Date(ts.getTime()));
                }

                u.setPublishedFileId(rs.getInt(col++));
                u.setPublishedFileRecordNumber(rs.getInt(col++));

                ts = rs.getTimestamp(col++);

                ret.add(u);
            }
            
            return ret;
            
        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    @Override
    public void updateStagingTable(String filePath, Date dataDate, int publishedFileId) throws Exception {

        //if we get an exchange with a large update to this table, it can lock it for long enough for other apps
        //to timeout, so give them another go
        DeadlockHandler h = new DeadlockHandler();
        h.setDelayBackOff(true); //SD-779 - Gives the process more time to complete to prevent lock timeouts when 5+ larger extract are processing
        h.setMaxAttempts(10);    //As above. This was also done for TPP staff file (SD-408) so keeps the approaches consistent
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                tryUpdateStagingTable(filePath, dataDate, publishedFileId);
                break;

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    private void tryUpdateStagingTable(String filePath, Date dataDate, int publishedFileId) throws Exception {
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
                    + "UserInRoleGuid varchar(255), "
                    + "OrganisationGuid varchar(255), "
                    + "Title varchar(255), "
                    + "GivenName varchar(255), "
                    + "Surname varchar(255), "
                    + "JobCategoryCode varchar(255), "
                    + "JobCategoryName varchar(255), "
                    + "ContractStartDate varchar(255), "
                    + "ContractEndDate varchar(255), "
                    + "ProcessingId varchar(255), "
                    + "record_number int, " //note this is auto-generated by the bulk load
                    + "key_exists boolean DEFAULT FALSE, "
                    + "CONSTRAINT pk PRIMARY KEY (UserInRoleGuid), "
                    + "KEY ix_key_exists (key_exists))";;
            Statement statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //bulk load temp table, adding record number as we go
            //LOG.debug("Starting bulk load into " + tempTableName);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            sql = "set @row = 1";
            statement.executeUpdate(sql);
            sql = "LOAD DATA LOCAL INFILE '" + filePath.replace("\\", "\\\\") + "'"
                    + " INTO TABLE " + tempTableName
                    + " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\\\"' ESCAPED BY '\\b'" //escaping stops if going wrong if slashes are in the file
                    + " LINES TERMINATED BY '\\r\\n'"
                    + " IGNORE 1 LINES"
                    + " SET record_number = @row:=@row+1";
            statement.executeUpdate(sql);
            statement.close();

            //work out which records already exist in the target table
            //LOG.debug("Finding records that exist in emis_user_in_role");
            sql = "UPDATE " + tempTableName + " s"
                    + " INNER JOIN emis_user_in_role t"
                    + " ON t.user_in_role_guid = s.UserInRoleGuid"
                    + " SET s.key_exists = true";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //insert records into the target table where the staging has new records
            //LOG.debug("Copying new records into target table emis_user_in_role");
            sql = "INSERT IGNORE INTO emis_user_in_role (user_in_role_guid, organisation_guid, title, given_name,"
                    + " surname, job_category_code, job_category_name, contract_start_date, contract_end_date,"
                    + " published_file_id, published_file_record_number, dt_last_updated)"
                    + " SELECT UserInRoleGuid,"
                    + " IF(OrganisationGuid != '', TRIM(OrganisationGuid), null),"
                    + " IF(Title != '', TRIM(Title), null),"
                    + " IF(GivenName != '', TRIM(GivenName), null),"
                    + " IF(Surname != '', TRIM(Surname), null),"
                    + " IF(JobCategoryCode != '', TRIM(JobCategoryCode), null),"
                    + " IF(JobCategoryName != '', TRIM(JobCategoryName), null),"
                    + " " + RdbmsEmisLocationDal.getSqlForEmisDates("ContractStartDate") + ","
                    + " " + RdbmsEmisLocationDal.getSqlForEmisDates("ContractEndDate") + ","
                    + publishedFileId + ", record_number, " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName
                    + " WHERE key_exists = false";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();


            //update any records that previously existed, but have a changed term
            //LOG.debug("Updating existing records in target table emis_user_in_role");
            sql = "UPDATE emis_user_in_role t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.user_in_role_guid = s.UserInRoleGuid"
                    + " SET"
                    + " t.organisation_guid = IF(s.OrganisationGuid != '', s.OrganisationGuid, null),"
                    + " t.title = IF(s.Title != '', TRIM(s.Title), null),"
                    + " t.given_name = IF(s.GivenName != '', TRIM(s.GivenName), null),"
                    + " t.surname = IF(s.Surname != '', TRIM(s.Surname), null),"
                    + " t.job_category_code = IF(s.JobCategoryCode != '', TRIM(s.JobCategoryCode), null),"
                    + " t.job_category_name = IF(s.JobCategoryName != '', TRIM(s.JobCategoryName), null),"
                    + " t.contract_start_date = " + RdbmsEmisLocationDal.getSqlForEmisDates("s.ContractStartDate") + ","
                    + " t.contract_end_date = " + RdbmsEmisLocationDal.getSqlForEmisDates("s.ContractEndDate") + ","
                    + " t.published_file_id = " + publishedFileId + ","
                    + " t.published_file_record_number = s.record_number,"
                    + " t.dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true)
                    + " WHERE t.dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            long msEnd = System.currentTimeMillis();
            LOG.debug("Update of emis_user_in_role Completed in " + ((msEnd-msStart)/1000) + "s");

        } finally {
            //MUST change this back to false
            connection.setAutoCommit(false);
            connection.close();

            ConnectionManager.dropTempTable(tempTableName, ConnectionManager.Db.PublisherCommon);

            //delete the temp file
            FileHelper.deleteFileFromTempDirIfNecessary(f);
        }
    }
}
