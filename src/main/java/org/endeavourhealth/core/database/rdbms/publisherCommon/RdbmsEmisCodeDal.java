package org.endeavourhealth.core.database.rdbms.publisherCommon;

import org.apache.commons.io.FilenameUtils;
import org.endeavourhealth.common.utility.FileHelper;
import org.endeavourhealth.core.database.dal.publisherCommon.EmisCodeDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.models.EmisClinicalCode;
import org.endeavourhealth.core.database.dal.publisherCommon.models.EmisClinicalCodeForIMUpdate;
import org.endeavourhealth.core.database.dal.publisherCommon.models.EmisDrugCode;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RdbmsEmisCodeDal implements EmisCodeDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsEmisCodeDal.class);

    /**
     * unlike all the other similar functions, this one is slightly different, needing TWO raw files to
     * populate the staging table. The second file is generated in realtime by the Emis ClinicalCodeTransformer class
     */
    @Override
    public void updateClinicalCodeTable(String filePath, String validReadCodesFile, Date dataDate) throws Exception {
        DeadlockHandler h = new DeadlockHandler();
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                updateClinicalCodeTableImpl(filePath, validReadCodesFile, dataDate);
                break;

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    private void updateClinicalCodeTableImpl(String filePath, String validReadCodesFile, Date dataDate) throws Exception {
        long msStart = System.currentTimeMillis();

        //copy the file from S3 to local disk
        File f = FileHelper.copyFileFromStorageToTempDirIfNecessary(filePath);
        filePath = f.getAbsolutePath();

        Connection connection = ConnectionManager.getPublisherCommonNonPooledConnection();
        //create a temporary table to load the data into
        String tempTableName = ConnectionManager.generateTempTableName(FilenameUtils.getBaseName(filePath));
        //we also have a second file, containing additional columns that we've generated in the code
        String extraTempTableName = ConnectionManager.generateTempTableName(FilenameUtils.getBaseName(validReadCodesFile));

        try {
            //turn on auto commit so we don't need to separately commit these large SQL operations
            connection.setAutoCommit(true);

            
            //LOG.debug("Loading " + f + " into " + tempTableName);
            String sql = "CREATE TABLE " + tempTableName + " ("
                    + "CodeId bigint, "
                    + "Term varchar(500), "
                    + "ReadTermId varchar(255), "
                    + "SnomedCTConceptId varchar(255), "
                    + "SnomedCTDescriptionId varchar(255), "
                    + "NationalCode varchar(255), "
                    + "NationalCodeCategory varchar(255), "
                    + "NationalDescription varchar(255), "
                    + "EmisCodeCategoryDescription varchar(255), "
                    + "ProcessingId int, "
                    + "ParentCodeId bigint, "
                    + "key_exists boolean DEFAULT FALSE, "
                    + "CONSTRAINT pk PRIMARY KEY (CodeId, ParentCodeId), "
                    + "KEY ix_key_exists (key_exists))";
            Statement statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //bulk load temp table, adding record number as we go
            //LOG.debug("Starting bulk load into " + tempTableName);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            sql = "LOAD DATA LOCAL INFILE '" + filePath.replace("\\", "\\\\") + "'"
                    + " INTO TABLE " + tempTableName
                    + " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\\\"' ESCAPED BY '\b'" //escaping stops if going wrong if slashes are in the file
                    + " LINES TERMINATED BY '\\r\\n'"
                    + " IGNORE 1 LINES";
            statement.executeUpdate(sql);
            statement.close();

            //LOG.debug("Loading " + validReadCodesFile + " into " + extraTempTableName);
            sql = "CREATE TABLE " + extraTempTableName + " ("
                    + "CodeId bigint, "
                    + "AdjustedCode varchar(255), "
                    + "IsEmisCode boolean, "
                    + "SnomedTerm varchar(500), "
                    + "CONSTRAINT pk PRIMARY KEY (CodeId))";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //bulk load temp table, adding record number as we go
            //LOG.debug("Starting bulk load into " + extraTempTableName);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            sql = "LOAD DATA LOCAL INFILE '" + validReadCodesFile.replace("\\", "\\\\") + "'"
                    + " INTO TABLE " + extraTempTableName
                    + " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\\\"' ESCAPED BY '\b'" //escaping stops if going wrong if slashes are in the file
                    + " LINES TERMINATED BY '\\r\\n'"
                    + " IGNORE 1 LINES";
            statement.executeUpdate(sql);
            statement.close();

            //work out which records already exist in the target table
            //LOG.debug("Finding records that exist in emis_clinical_code");
            sql = "UPDATE " + tempTableName + " s"
                    + " INNER JOIN emis_clinical_code t"
                    + " ON t.code_id = s.CodeId"
                    + " SET s.key_exists = true";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //insert records into the target table where the staging has new records
            //LOG.debug("Copying new records into target table emis_clinical_code");
            sql = "INSERT IGNORE INTO emis_clinical_code (code_id, code_type, read_term, read_code, snomed_concept_id,"
                    + " snomed_description_id, snomed_term, national_code, national_code_category,"
                    //+ " national_code_description, parent_code_id, adjusted_code, is_emis_code, dt_last_updated)"
                    + " national_code_description, adjusted_code, is_emis_code, dt_last_updated)"
                    + " SELECT s.CodeId,"
                    + " IF(s.EmisCodeCategoryDescription != '', TRIM(EmisCodeCategoryDescription), null),"
                    + " IF(s.Term != '', TRIM(s.Term), null),"
                    + " IF(s.ReadTermId != '', TRIM(s.ReadTermId), null),"
                    + " IF(s.SnomedCTConceptId != '', TRIM(s.SnomedCTConceptId), null),"
                    + " IF(s.SnomedCTDescriptionId != '', TRIM(s.SnomedCTDescriptionId), null),"
                    + " IF(x.SnomedTerm != '', TRIM(x.SnomedTerm), null),"
                    + " IF(s.NationalCode != '', TRIM(s.NationalCode), null),"
                    + " IF(s.NationalCodeCategory != '', TRIM(s.NationalCodeCategory), null),"
                    + " IF(s.NationalDescription != '', TRIM(s.NationalDescription), null),"
                    //+ " IF(s.ParentCodeId != '', s.ParentCodeId, null),"
                    + " IF(x.AdjustedCode != '', TRIM(x.AdjustedCode), null),"
                    + " x.IsEmisCode,"
                    + " " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName + " s "
                    + " LEFT OUTER JOIN " + extraTempTableName + " x"
                    + " ON s.CodeId = x.CodeId"
                    + " WHERE key_exists = false";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //update any records that previously existed, but have a changed term
            //LOG.debug("Updating existing records in target table emis_clinical_code");
            sql = "UPDATE emis_clinical_code t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.code_id = s.CodeId"
                    + " LEFT OUTER JOIN " + extraTempTableName + " x"
                    + " ON s.CodeId = x.CodeId"
                    + " SET"
                    + " t.code_type = IF(s.EmisCodeCategoryDescription != '', TRIM(EmisCodeCategoryDescription), null),"
                    + " t.read_term = IF(s.Term != '', TRIM(s.Term), null),"
                    + " t.read_code = IF(s.ReadTermId != '', TRIM(s.ReadTermId), null),"
                    + " t.snomed_concept_id = IF(s.SnomedCTConceptId != '', TRIM(s.SnomedCTConceptId), null),"
                    + " t.snomed_description_id = IF(s.SnomedCTDescriptionId != '', TRIM(s.SnomedCTDescriptionId), null),"
                    + " t.snomed_term = IF(x.SnomedTerm != '', TRIM(x.SnomedTerm), null),"
                    + " t.national_code = IF(s.NationalCode != '', TRIM(s.NationalCode), null),"
                    + " t.national_code_category = IF(s.NationalCodeCategory != '', TRIM(s.NationalCodeCategory), null),"
                    + " t.national_code_description = IF(s.NationalDescription != '', TRIM(s.NationalDescription), null),"
                    //+ " t.parent_code_id = IF(s.ParentCodeId != '', s.ParentCodeId, null),"
                    + " t.adjusted_code = IF(x.AdjustedCode != '', TRIM(x.AdjustedCode), null),"
                    + " t.is_emis_code = x.IsEmisCode,"
                    + " t.dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true)
                    + " WHERE t.dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //SD-378 - removed this REPLACE INTO and replaced with the below two statements. Doing a REPLACE INTO results in a MySQL delete and insert,
            //which is slow and locks the table and causes problems when multiple Queue Readers are doing it at the same time. Changed
            //do to an UPDATE and then an INSERT IGNORE. Gives same results but less chance of locking.
            /*sql = "REPLACE INTO emis_clinical_code_hiearchy (code_id, parent_code_id, dt_last_updated)"
                    + " SELECT"
                    + " s.CodeId,"
                    + " s.ParentCodeId,"
                    + " " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName + " s "
                    + " WHERE s.ParentCodeId != ''"; //all known ones have parents, but this will stop ones without parents causing problems
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();*/

            //update any existing ones with the latest date
            sql = "UPDATE emis_clinical_code_hiearchy h"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON s.CodeId = h.code_id"
                    + " AND s.ParentCodeId = h.parent_code_id"
                    + " SET dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //insert any new codes into the hierarchy table
            sql = "INSERT IGNORE INTO emis_clinical_code_hiearchy (code_id, parent_code_id, dt_last_updated)"
                    + " SELECT"
                    + " s.CodeId,"
                    + " s.ParentCodeId,"
                    + " " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName + " s "
                    + " WHERE s.ParentCodeId != ''"; //all known ones have parents, but this will stop ones without parents causing problems
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();



            //delete any existing records from the hierarchy table in case a code has been moved
            //since the above SQL updates the dt_last_updated for any codes in the extract, if there's any code ID
            //in the hierarchy table with an older timestamp, then it should be deleted
            sql = "DELETE t FROM emis_clinical_code_hiearchy t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.code_id = s.CodeId"
                    + " WHERE t.dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            long msEnd = System.currentTimeMillis();
            LOG.debug("Update of emis_clinical_code Completed in " + ((msEnd-msStart)/1000) + "s");

        } finally {
            //MUST change this back to false
            connection.setAutoCommit(false);
            connection.close();

            ConnectionManager.dropTempTable(tempTableName, ConnectionManager.Db.PublisherCommon);
            ConnectionManager.dropTempTable(extraTempTableName, ConnectionManager.Db.PublisherCommon);

            //delete the temp file
            FileHelper.deleteFileFromTempDirIfNecessary(f);
        }
    }

    @Override
    public void updateDrugCodeTable(String filePath, Date dataDate) throws Exception {
        DeadlockHandler h = new DeadlockHandler();
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                updateDrugCodeTableImpl(filePath, dataDate);
                break;

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    @Override
    public Date getMostRecentClinicalCodeDate() throws Exception {
        DeadlockHandler h = new DeadlockHandler();
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                return getMostRecentClinicalCodeDateImpl();

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }


    public Date getMostRecentClinicalCodeDateImpl() throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT MAX(dt_last_updated)"
                    + " FROM emis_clinical_code";

            ps = connection.prepareStatement(sql);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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

    @Override
    public Date getMostRecentDrugCodeDate() throws Exception {
        DeadlockHandler h = new DeadlockHandler();
        h.setRetryDelaySeconds(30); //give it long enough for the other thing to finish

        while (true) {
            try {
                return getMostRecentDrugCodeDateImpl();

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    private Date getMostRecentDrugCodeDateImpl() throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT MAX(dt_last_updated)"
                    + " FROM emis_drug_code";

            ps = connection.prepareStatement(sql);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
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

    private void updateDrugCodeTableImpl(String filePath, Date dataDate) throws Exception {
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
                    + "CodeId bigint, "
                    + "Term varchar(500), "
                    + "DmdProductCodeId varchar(255), "
                    + "BNFChapterRef varchar(12), "    //SD-1073
                    + "ProcessingId int, "
                    + "key_exists boolean DEFAULT FALSE, "
                    + "CONSTRAINT pk PRIMARY KEY (CodeId), "
                    + "KEY ix_key_exists (key_exists))";
            Statement statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //bulk load temp table, adding record number as we go
            //LOG.debug("Starting bulk load into " + tempTableName);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            sql = "LOAD DATA LOCAL INFILE '" + filePath.replace("\\", "\\\\") + "'"
                    + " INTO TABLE " + tempTableName
                    + " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\\\"' ESCAPED BY '\b'" //escaping stops if going wrong if slashes are in the file
                    + " LINES TERMINATED BY '\\r\\n'"
                    + " IGNORE 1 LINES";
            statement.executeUpdate(sql);
            statement.close();

            //work out which records already exist in the target table
            //LOG.debug("Finding records that exist in emis_drug_code");
            sql = "UPDATE " + tempTableName + " s"
                    + " INNER JOIN emis_drug_code t"
                    + " ON t.code_id = s.CodeId"
                    + " SET s.key_exists = true";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //insert records into the target table where the staging has new records
            //LOG.debug("Copying new records into target table emis_drug_code");
            sql = "INSERT IGNORE INTO emis_drug_code (code_id, dmd_concept_id, dmd_term, dt_last_updated)"
                    + " SELECT CodeId,"
                    + " IF(DmdProductCodeId != '', DmdProductCodeId, null),"
                    + " IF(Term != '', Term, null),"
                    + " " + ConnectionManager.formatDateString(dataDate, true)
                    + " FROM " + tempTableName
                    + " WHERE key_exists = false";
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            //update any records that previously existed, but have a changed term
            //LOG.debug("Updating existing records in target table emis_drug_code");
            sql = "UPDATE emis_drug_code t"
                    + " INNER JOIN " + tempTableName + " s"
                    + " ON t.code_id = s.CodeId"
                    + " SET"
                    + " t.dmd_concept_id = IF(s.DmdProductCodeId != '', s.DmdProductCodeId, null),"
                    + " t.dmd_term = IF(s.Term != '', s.Term, null),"
                    + " t.dt_last_updated = " + ConnectionManager.formatDateString(dataDate, true)
                    + " WHERE t.dt_last_updated < " + ConnectionManager.formatDateString(dataDate, true);
            statement = connection.createStatement(); //one-off SQL due to table name, so don't use prepared statement
            statement.executeUpdate(sql);
            statement.close();

            long msEnd = System.currentTimeMillis();
            LOG.debug("Update of emis_drug_code Completed in " + ((msEnd-msStart)/1000) + "s");

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
    public EmisDrugCode getDrugCode(long codeId) throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT code_id, dmd_concept_id, dmd_term, dt_last_updated"
                    + " FROM emis_drug_code"
                    + " WHERE code_id = ?";
            ps = connection.prepareStatement(sql);

            ps.setLong(1, codeId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int col = 1;
                EmisDrugCode ret = new EmisDrugCode();
                ret.setCodeId(rs.getLong(col++));

                //this may be null
                long dmdId = rs.getLong(col++);
                if (!rs.wasNull()) {
                    ret.setDmdConceptId(dmdId);
                }

                ret.setDmdTerm(rs.getString(col++));

                rs.getTimestamp(col++);
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

    @Override
    public EmisClinicalCode getClinicalCode(long codeId) throws Exception {
        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT c.code_type, c.read_term, c.read_code, c.snomed_concept_id,"
                    + " c.snomed_description_id, c.snomed_term, c.national_code, c.national_code_category,"
                    + " c.national_code_description, c.adjusted_code, c.is_emis_code, h.parent_code_id"
                    + " FROM emis_clinical_code c"
                    + " LEFT OUTER JOIN emis_clinical_code_hiearchy h"
                    + " ON h.code_id = c.code_id"
                    + " WHERE c.code_id = ?";
            ps = connection.prepareStatement(sql);

            ps.setLong(1, codeId);

            EmisClinicalCode ret = null;

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int col = 1;

                EmisClinicalCode c = new EmisClinicalCode();
                c.setCodeId(codeId);
                c.setCodeType(rs.getString(col++));
                c.setReadTerm(rs.getString(col++));
                c.setReadCode(rs.getString(col++));

                long l = rs.getLong(col++);
                if (!rs.wasNull()) {
                    c.setSnomedConceptId(l);
                }

                l = rs.getLong(col++);
                if (!rs.wasNull()) {
                    c.setSnomedDescriptionId(l);
                }

                c.setSnomedTerm(rs.getString(col++));
                c.setNationalCode(rs.getString(col++));
                c.setNationalCodeCategory(rs.getString(col++));
                c.setNationalCodeDescription(rs.getString(col++));
                c.setAdjustedCode(rs.getString(col++));
                c.setEmisCode(rs.getBoolean(col++));

                //due to each code potentially having multiple parents, we may get multiple records
                //back from the above SQL with the same details duplicated (except for the parent ID)
                if (ret == null) {
                    ret = c;
                }

                //add any parent codes to the object instance to be returned
                l = rs.getLong(col++);
                if (!rs.wasNull()) {
                    ret.getParentCodes().add(new Long(l));
                }


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
    public List<EmisClinicalCodeForIMUpdate> getClinicalCodesForIMUpdate() throws Exception {

        Connection connection = ConnectionManager.getPublisherCommonConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT read_term, read_code, snomed_concept_id,"
                    + " is_emis_code, dt_last_updated"
                    + " FROM emis_clinical_code "
                    + " WHERE is_emis_code = 1 ";

            ps = connection.prepareStatement(sql);
            ps.executeQuery();
            ResultSet resultSet = ps.executeQuery();

            List<EmisClinicalCodeForIMUpdate> returnList = new ArrayList<>();

            while (resultSet.next()) {

                EmisClinicalCodeForIMUpdate code = new EmisClinicalCodeForIMUpdate();

                code.setReadTerm(resultSet.getString("read_term"));
                code.setReadCode(resultSet.getString("read_code"));
                code.setSnomedConceptId(resultSet.getLong("snomed_concept_id"));
                code.setIsEmisCode(resultSet.getBoolean("is_emis_code"));
                code.setDateLastUpdated(resultSet.getTimestamp("dt_last_updated"));

                returnList.add(code);
            }

            return returnList;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

}
