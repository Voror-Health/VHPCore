package org.endeavourhealth.core.database.rdbms.informationmodel;

import org.endeavourhealth.core.database.dal.informationmodel.EmisClinicalCodesIMUpdaterDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.models.EmisClinicalCodeForIMUpdate;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.UUID;

public class RdbmsEmisClinicalCodesIMUpdaterDal implements EmisClinicalCodesIMUpdaterDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsEmisClinicalCodesIMUpdaterDal.class);

    @Override
    public void updateIMForEmisClinicalCodes(List<EmisClinicalCodeForIMUpdate> codeList) throws Exception {

        Connection connection = ConnectionManager.getInformationModelConnection();
        try {

            String tempTableName = generateTempTableName("emis_clinical_codes");

            String sql = "CREATE TABLE `" + tempTableName + "` ("
                    + "read_term VARCHAR(500) DEFAULT NULL, "
                    + "read_code VARCHAR(250) CHARACTER SET latin1 COLLATE latin1_bin DEFAULT NULL, "
                    + "snomed_concept_id BIGINT(20) DEFAULT NULL, "
                    + "is_emis_code TINYINT (1) NOT NULL, "
                    + "dt_last_updated DATETIME NOT NULL)";

            Statement statement = connection.createStatement(); // one-off SQL due to table name
            statement.executeUpdate(sql);
            connection.commit();
            statement.close();

            statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            int i = 0;

            for (EmisClinicalCodeForIMUpdate code : codeList) {

                String readTerm = code.getReadTerm();
                String readCode = code.getReadCode();
                Long snomedConceptId = code.getSnomedConceptId();
                boolean isEmisCode = code.getIsEmisCode();
                Timestamp dateLastUpdated = code.getDateLastUpdated();

                //SD-856 - remove replacement chars � from source
                sql = "INSERT INTO `" + tempTableName + "`"
                        + " SELECT "
                        + "'" + readTerm.replaceAll("'","''").replace("\\xFFFD", "").replace("�", "") + "', "
                        + "'" + readCode.replaceAll("'","''").replace("\\xFFFD", "").replace("�", "") + "', "
                        + snomedConceptId + ","
                        + isEmisCode + ","
                        + "'" + dateLastUpdated + "'";

                try {

                    i++;
                    statement.addBatch(sql);

                    if(i % 10000 == 0 ) {
                        int[] executed = statement.executeBatch();
                        LOG.info("Executed statements:" + executed.length);
                        connection.commit();
                        System.gc();
                    }

                } catch (SQLException e) {
                    LOG.error("Reason: " + e.getMessage());
                    break;
                }
            }

            int[] executed = statement.executeBatch();
            LOG.info("Executed statements:" + executed.length);
            connection.commit();
            statement.close();

            sql = "CALL emisToConceptUpdate('`" + tempTableName + "`')";
            statement = connection.createStatement();  // one-off SQL due to table name
            statement.executeUpdate(sql);
            connection.commit();
            statement.close();

            sql = "DROP TABLE `" + tempTableName + "`";
            statement = connection.createStatement(); //one-off SQL due to table name
            statement.executeUpdate(sql);
            connection.commit();
            statement.close();

        } finally {

            connection.close();
        }
    }

    private String generateTempTableName(String baseName) {

        return baseName + "_" + UUID.randomUUID().toString();
    }

}
