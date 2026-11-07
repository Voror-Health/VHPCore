package org.endeavourhealth.core.database.rdbms.publisherStaging;

import org.endeavourhealth.core.database.dal.publisherStaging.StagingCdsDalI;
import org.endeavourhealth.core.database.dal.publisherStaging.models.*;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RdbmsStagingCdsDal implements StagingCdsDalI {

    private static final Logger LOG = LoggerFactory.getLogger(RdbmsStagingCdsDal.class);

    private boolean wasCdsAlreadyFiled(UUID serviceId, StagingProcedureCds cds) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                        + "from procedure_cds "
                        + "where cds_unique_identifier = ? "
                        + "and sus_record_type = ? "
                        + "and procedure_seq_nbr = ? "
                        + "and dt_received <= ? "
                        + "order by dt_received desc "
                        + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cds.getCdsUniqueIdentifier());
            ps.setString(col++, cds.getSusRecordType());
            ps.setInt(col++, cds.getProcedureSeqNbr());
            ps.setTimestamp(col++, new java.sql.Timestamp(cds.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cds.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveProcedure(StagingProcedureCds cds, UUID serviceId) throws Exception {

        if (cds == null) {
            throw new IllegalArgumentException("cds object is null");
        }

        List<StagingProcedureCds> l = new ArrayList<>();
        l.add(cds);
        saveProcedures(l, serviceId);

    }

    private boolean wasConditionCdsAlreadyFiled(UUID serviceId, StagingConditionCds cdsCondition) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from condition_cds "
                    + "where cds_unique_identifier = ? "
                    + "and sus_record_type = ? "
                    + "and diagnosis_seq_nbr = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsCondition.getCdsUniqueIdentifier());
            ps.setString(col++, cdsCondition.getSusRecordType());
            ps.setInt(col++, cdsCondition.getDiagnosisSeqNbr());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsCondition.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsCondition.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasInpatientCdsAlreadyFiled(UUID serviceId, StagingInpatientCds cdsInpatient) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from cds_inpatient "
                    + "where cds_unique_identifier = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsInpatient.getCdsUniqueIdentifier());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsInpatient.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasOutpatientCdsAlreadyFiled(UUID serviceId, StagingOutpatientCds cdsOutpatient) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from cds_outpatient "
                    + "where cds_unique_identifier = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsOutpatient.getCdsUniqueIdentifier());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsOutpatient.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasEmergencyCdsAlreadyFiled(UUID serviceId, StagingEmergencyCds cdsEmergency) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from cds_emergency "
                    + "where cds_unique_identifier = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsEmergency.getCdsUniqueIdentifier());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsEmergency.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasCriticalCareCdsAlreadyFiled(UUID serviceId, StagingCriticalCareCds cdsCriticalCare) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from cds_critical_care "
                    + "where cds_unique_identifier = ? "
                    + "and critical_care_identifier = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsCriticalCare.getCdsUniqueIdentifier());
            ps.setString(col++, cdsCriticalCare.getCriticalCareIdentifier());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsCriticalCare.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsCriticalCare.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasHomeDelBirthCdsAlreadyFiled(UUID serviceId, StagingHomeDelBirthCds cdsHomeDelBirth) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from cds_home_delivery_birth "
                    + "where cds_unique_identifier = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsHomeDelBirth.getCdsUniqueIdentifier());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsHomeDelBirth.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveCondition(StagingConditionCds cdsCondition, UUID serviceId) throws Exception {

        if (cdsCondition == null) {
            throw new IllegalArgumentException("cdsCondition object is null");
        }

        List<StagingConditionCds> l = new ArrayList<>();
        l.add(cdsCondition);
        saveConditions(l, serviceId);
    }

    private boolean wasCdsCountAlreadyFiled(UUID serviceId, StagingProcedureCdsCount cdsCount) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from procedure_cds_count "
                    + "where cds_unique_identifier = ? "
                    + "and sus_record_type = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsCount.getCdsUniqueIdentifier());
            ps.setString(col++, cdsCount.getSusRecordType());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsCount.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsCount.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveProcedureCount(StagingProcedureCdsCount cdsCount, UUID serviceId) throws Exception {
        if (cdsCount == null) {
            throw new IllegalArgumentException("cdsCount object is null");
        }

        List<StagingProcedureCdsCount> l = new ArrayList<>();
        l.add(cdsCount);
        saveProcedureCounts(l, serviceId);
    }

    @Override
    public void saveProcedures(List<StagingProcedureCds> cdses, UUID serviceId) throws Exception {

        //SD-499 keep getting deadlocks here, to give it a few attempts
        DeadlockHandler h = new DeadlockHandler();

        while (true) {
            try {
                saveProceduresImpl(cdses, serviceId);
                break;

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    public void saveProceduresImpl(List<StagingProcedureCds> cdses, UUID serviceId) throws Exception {

        List<StagingProcedureCds> toSave = new ArrayList<>();

        for (StagingProcedureCds cds: cdses) {

            cds.setRecordChecksum(cds.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasCdsAlreadyFiled(serviceId, cds)) {
                //   LOG.warn("procedure_cds data already filed with record_checksum: "+cds.hashCode());
                //   LOG.warn("cds:>" + cds.toString());
                toSave.add(cds);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;

        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO procedure_cds  "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, sus_record_type, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, consultant_code, procedure_date, " +
                    " procedure_opcs_code, procedure_seq_nbr, primary_procedure_opcs_code, lookup_procedure_opcs_term, " +
                    " lookup_person_id, lookup_consultant_personnel_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " sus_record_type = VALUES(sus_record_type),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " consultant_code = VALUES(consultant_code),"
                    + " procedure_date = VALUES(procedure_date),"
                    + " procedure_opcs_code = VALUES(procedure_opcs_code),"
                    + " procedure_seq_nbr = VALUES(procedure_seq_nbr),"
                    + " primary_procedure_opcs_code = VALUES(primary_procedure_opcs_code),"
                    + " lookup_procedure_opcs_term = VALUES(lookup_procedure_opcs_term),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " lookup_consultant_personnel_id = VALUES(lookup_consultant_personnel_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            for (StagingProcedureCds cds: toSave) {

                int col = 1;

                //all columns except the last three are non-null
                ps.setString(col++, cds.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cds.getDtReceived().getTime()));
                ps.setInt(col++, cds.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cds.getCdsActivityDate().getTime()));
                ps.setString(col++, cds.getSusRecordType());
                ps.setString(col++, cds.getCdsUniqueIdentifier());
                ps.setInt(col++, cds.getCdsUpdateType());
                ps.setString(col++, cds.getMrn());
                ps.setString(col++, cds.getNhsNumber());
                if (cds.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cds.getWithheld().booleanValue());
                }
                if (cds.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cds.getDateOfBirth().getTime()));
                }
                ps.setString(col++, cds.getConsultantCode());

                if (cds.getProcedureDate() == null) {
                    ps.setNull(col++, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cds.getProcedureDate().getTime()));
                }

                ps.setString(col++, cds.getProcedureOpcsCode());
                ps.setInt(col++, cds.getProcedureSeqNbr());
                ps.setString(col++, cds.getPrimaryProcedureOpcsCode());
                ps.setString(col++, cds.getLookupProcedureOpcsTerm());

                if (cds.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cds.getLookupPersonId());
                }

                if (cds.getLookupConsultantPersonnelId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cds.getLookupConsultantPersonnelId());
                }

                if (cds.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cds.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            LOG.error("Error saving");
            for (StagingProcedureCds cds: toSave) {
                LOG.error("" + cds);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveProcedureCounts(List<StagingProcedureCdsCount> cdses, UUID serviceId) throws Exception {

        List<StagingProcedureCdsCount> toSave = new ArrayList<>();

        for (StagingProcedureCdsCount cdsCount: cdses) {

            cdsCount.setRecordChecksum(cdsCount.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasCdsCountAlreadyFiled(serviceId, cdsCount)) {
                toSave.add(cdsCount);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;

        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO procedure_cds_count  "
                    + " (exchange_id, dt_received, record_checksum, sus_record_type, cds_unique_identifier, procedure_count, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " sus_record_type = VALUES(sus_record_type),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " procedure_count = VALUES(procedure_count),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            for (StagingProcedureCdsCount cdsCount: toSave) {

                int col = 1;

                //all but the last of the columns allow nulls
                ps.setString(col++, cdsCount.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsCount.getDtReceived().getTime()));
                ps.setInt(col++, cdsCount.getRecordChecksum());
                ps.setString(col++, cdsCount.getSusRecordType());
                ps.setString(col++, cdsCount.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsCount.getProcedureCount());
                if (cdsCount.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsCount.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            LOG.error("Error saving");
            for (StagingProcedureCdsCount cdsCount: toSave) {
                LOG.error("" + cdsCount);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private boolean wasConditionCdsCountAlreadyFiled(UUID serviceId, StagingConditionCdsCount cdsConditionCount) throws Exception {

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select record_checksum "
                    + "from condition_cds_count "
                    + "where cds_unique_identifier = ? "
                    + "and sus_record_type = ? "
                    + "and dt_received <= ? "
                    + "order by dt_received desc "
                    + "limit 1";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, cdsConditionCount.getCdsUniqueIdentifier());
            ps.setString(col++, cdsConditionCount.getSusRecordType());
            ps.setTimestamp(col++, new java.sql.Timestamp(cdsConditionCount.getDtReceived().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int dbChecksum = rs.getInt(1);
                return dbChecksum == cdsConditionCount.getRecordChecksum();
            } else {
                return false;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveConditionCount(StagingConditionCdsCount cdsConditionCount, UUID serviceId) throws Exception {

        if (cdsConditionCount == null) {
            throw new IllegalArgumentException("cdsConditionCount object is null");
        }

        List<StagingConditionCdsCount> l = new ArrayList<>();
        l.add(cdsConditionCount);
        saveConditionCounts(l, serviceId);
    }

    @Override
    public void saveConditions(List<StagingConditionCds> cdses, UUID serviceId) throws Exception {

        List<StagingConditionCds> toSave = new ArrayList<>();

        for (StagingConditionCds cdsCondition: cdses) {

            cdsCondition.setRecordChecksum(cdsCondition.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasConditionCdsAlreadyFiled(serviceId, cdsCondition)) {
                //   LOG.warn("condition_cds data already filed with record_checksum: "+cdsCondition.hashCode());
                //   LOG.warn("cdsCondition:>" + cdsCondition.toString());
                toSave.add(cdsCondition);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;

        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO condition_cds  "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, sus_record_type, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, consultant_code, " +
                    " diagnosis_icd_code, diagnosis_seq_nbr, primary_diagnosis_icd_code, lookup_diagnosis_icd_term, " +
                    " lookup_person_id, lookup_consultant_personnel_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " sus_record_type = VALUES(sus_record_type),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " consultant_code = VALUES(consultant_code),"
                    + " diagnosis_icd_code = VALUES(diagnosis_icd_code),"
                    + " diagnosis_seq_nbr = VALUES(diagnosis_seq_nbr),"
                    + " primary_diagnosis_icd_code = VALUES(primary_diagnosis_icd_code),"
                    + " lookup_diagnosis_icd_term = VALUES(lookup_diagnosis_icd_term),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " lookup_consultant_personnel_id = VALUES(lookup_consultant_personnel_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            for (StagingConditionCds cdsCondition: toSave) {

                int col = 1;

                //all columns except the last three are non-null
                ps.setString(col++, cdsCondition.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsCondition.getDtReceived().getTime()));
                ps.setInt(col++, cdsCondition.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsCondition.getCdsActivityDate().getTime()));
                ps.setString(col++, cdsCondition.getSusRecordType());
                ps.setString(col++, cdsCondition.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsCondition.getCdsUpdateType());
                ps.setString(col++, cdsCondition.getMrn());
                ps.setString(col++, cdsCondition.getNhsNumber());
                if (cdsCondition.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cdsCondition.getWithheld().booleanValue());
                }
                if (cdsCondition.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsCondition.getDateOfBirth().getTime()));
                }
                ps.setString(col++, cdsCondition.getConsultantCode());

                ps.setString(col++, cdsCondition.getDiagnosisIcdCode());
                ps.setInt(col++, cdsCondition.getDiagnosisSeqNbr());
                ps.setString(col++, cdsCondition.getPrimaryDiagnosisIcdCode());
                ps.setString(col++, cdsCondition.getLookupDiagnosisIcdTerm());

                if (cdsCondition.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsCondition.getLookupPersonId());
                }

                if (cdsCondition.getLookupConsultantPersonnelId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsCondition.getLookupConsultantPersonnelId());
                }

                if (cdsCondition.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsCondition.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();

            LOG.error("Error saving");
            for (StagingConditionCds cdsCondition: toSave) {
                LOG.error("" + cdsCondition);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public void saveConditionCounts(List<StagingConditionCdsCount> cdses, UUID serviceId) throws Exception {

        List<StagingConditionCdsCount> toSave = new ArrayList<>();
        for (StagingConditionCdsCount cdsConditionCount: cdses) {

            cdsConditionCount.setRecordChecksum(cdsConditionCount.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasConditionCdsCountAlreadyFiled(serviceId, cdsConditionCount)) {
                toSave.add(cdsConditionCount);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;

        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO condition_cds_count  "
                    + " (exchange_id, dt_received, record_checksum, sus_record_type, cds_unique_identifier, condition_count, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " sus_record_type = VALUES(sus_record_type),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " condition_count = VALUES(condition_count),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            for (StagingConditionCdsCount cdsConditionCount: toSave) {

                int col = 1;

                //NONE of the columns allow nulls
                ps.setString(col++, cdsConditionCount.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsConditionCount.getDtReceived().getTime()));
                ps.setInt(col++, cdsConditionCount.getRecordChecksum());
                ps.setString(col++, cdsConditionCount.getSusRecordType());
                ps.setString(col++, cdsConditionCount.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsConditionCount.getConditionCount());
                if (cdsConditionCount.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsConditionCount.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            LOG.error("Error saving");
            for (StagingConditionCdsCount cdsConditionCount: toSave) {
                LOG.error("" + cdsConditionCount);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }


    @Override
    public void saveCDSInpatients(List<StagingInpatientCds> cdses, UUID serviceId) throws Exception {

        List<StagingInpatientCds> toSave = new ArrayList<>();

        for (StagingInpatientCds cdsInpatient: cdses) {

            cdsInpatient.setRecordChecksum(cdsInpatient.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasInpatientCdsAlreadyFiled(serviceId, cdsInpatient)) {

                toSave.add(cdsInpatient);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        //EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        Connection connection = ConnectionManager.getPublisherStagingConnection(serviceId);

        try {
            //SessionImpl session = (SessionImpl) entityManager.getDelegate();
            //Connection connection = session.connection();

            String sql = "INSERT INTO cds_inpatient "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, consultant_code, " +
                    " patient_pathway_identifier, spell_number, administrative_category_code, admission_method_code, "+
                    " admission_source_code, patient_classification, spell_start_date, episode_number, " +
                    " episode_start_site_code, episode_start_ward_code, episode_start_date, " +
                    " episode_end_site_code, episode_end_ward_code, episode_end_date, " +
                    " discharge_date, discharge_destination_code, discharge_method, " +
                    " maternity_data_birth, maternity_data_delivery, " +
                    " primary_diagnosis_ICD, secondary_diagnosis_ICD, other_diagnosis_ICD, primary_procedure_OPCS, " +
                    " primary_procedure_date, secondary_procedure_OPCS, secondary_procedure_date, other_procedures_OPCS, " +
                    " lookup_person_id, lookup_consultant_personnel_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " consultant_code = VALUES(consultant_code),"
                    + " patient_pathway_identifier = VALUES(patient_pathway_identifier),"
                    + " spell_number = VALUES(spell_number),"
                    + " administrative_category_code = VALUES(administrative_category_code),"
                    + " admission_method_code = VALUES(admission_method_code),"
                    + " admission_source_code = VALUES(admission_source_code),"
                    + " patient_classification = VALUES(patient_classification),"
                    + " spell_start_date = VALUES(spell_start_date),"
                    + " episode_number = VALUES(episode_number),"
                    + " episode_start_site_code = VALUES(episode_start_site_code),"
                    + " episode_start_ward_code = VALUES(episode_start_ward_code),"
                    + " episode_start_date = VALUES(episode_start_date),"
                    + " episode_end_site_code = VALUES(episode_end_site_code),"
                    + " episode_end_ward_code = VALUES(episode_end_ward_code),"
                    + " episode_end_date = VALUES(episode_end_date),"
                    + " discharge_date = VALUES(discharge_date),"
                    + " discharge_destination_code = VALUES(discharge_destination_code),"
                    + " discharge_method = VALUES(discharge_method),"
                    + " maternity_data_birth = VALUES(maternity_data_birth),"
                    + " maternity_data_delivery = VALUES(maternity_data_delivery),"
                    + " primary_diagnosis_ICD = VALUES(primary_diagnosis_ICD),"
                    + " secondary_diagnosis_ICD = VALUES(secondary_diagnosis_ICD),"
                    + " other_diagnosis_ICD = VALUES(other_diagnosis_ICD),"
                    + " primary_procedure_OPCS = VALUES(primary_procedure_OPCS),"
                    + " primary_procedure_date = VALUES(primary_procedure_date),"
                    + " secondary_procedure_OPCS = VALUES(secondary_procedure_OPCS),"
                    + " secondary_procedure_date = VALUES(secondary_procedure_date),"
                    + " other_procedures_OPCS = VALUES(other_procedures_OPCS),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " lookup_consultant_personnel_id = VALUES(lookup_consultant_personnel_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            //entityManager.getTransaction().begin();

            for (StagingInpatientCds cdsInpatient : toSave) {

                int col = 1;

                ps.setString(col++, cdsInpatient.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getDtReceived().getTime()));
                ps.setInt(col++, cdsInpatient.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getCdsActivityDate().getTime()));
                ps.setString(col++, cdsInpatient.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsInpatient.getCdsUpdateType());
                ps.setString(col++, cdsInpatient.getMrn());
                ps.setString(col++, cdsInpatient.getNhsNumber());
                if (cdsInpatient.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cdsInpatient.getWithheld().booleanValue());
                }
                if (cdsInpatient.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getDateOfBirth().getTime()));
                }
                ps.setString(col++, cdsInpatient.getConsultantCode());

                ps.setString(col++, cdsInpatient.getPatientPathwayIdentifier());
                ps.setString(col++, cdsInpatient.getSpellNumber());
                ps.setString(col++, cdsInpatient.getAdministrativeCategoryCode());
                ps.setString(col++, cdsInpatient.getAdmissionMethodCode());
                ps.setString(col++, cdsInpatient.getAdmissionSourceCode());
                ps.setString(col++, cdsInpatient.getPatientClassification());

                if (cdsInpatient.getSpellStartDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getSpellStartDate().getTime()));
                }

                ps.setString(col++, cdsInpatient.getEpisodeNumber());
                ps.setString(col++, cdsInpatient.getEpisodeStartSiteCode());
                ps.setString(col++, cdsInpatient.getEpisodeStartWardCode());

                if (cdsInpatient.getEpisodeStartDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getEpisodeStartDate().getTime()));
                }

                ps.setString(col++, cdsInpatient.getEpisodeEndSiteCode());
                ps.setString(col++, cdsInpatient.getEpisodeEndWardCode());

                if (cdsInpatient.getEpisodeEndDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getEpisodeEndDate().getTime()));
                }

                if (cdsInpatient.getDischargeDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getDischargeDate().getTime()));
                }

                ps.setString(col++, cdsInpatient.getDischargeDestinationCode());
                ps.setString(col++, cdsInpatient.getDischargeMethod());

                ps.setString(col++, cdsInpatient.getMaternityDataBirth());
                ps.setString(col++, cdsInpatient.getMaternityDataDelivery());

                ps.setString(col++, cdsInpatient.getPrimaryDiagnosisICD());
                ps.setString(col++, cdsInpatient.getSecondaryDiagnosisICD());
                ps.setString(col++, cdsInpatient.getOtherDiagnosisICD());
                ps.setString(col++, cdsInpatient.getPrimaryProcedureOPCS());

                if (cdsInpatient.getPrimaryProcedureDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getPrimaryProcedureDate().getTime()));
                }

                ps.setString(col++, cdsInpatient.getSecondaryProcedureOPCS());

                if (cdsInpatient.getSecondaryProcedureDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsInpatient.getSecondaryProcedureDate().getTime()));
                }

                ps.setString(col++, cdsInpatient.getOtherProceduresOPCS());

                if (cdsInpatient.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsInpatient.getLookupPersonId());
                }

                if (cdsInpatient.getLookupConsultantPersonnelId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsInpatient.getLookupConsultantPersonnelId());
                }

                if (cdsInpatient.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsInpatient.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            //entityManager.getTransaction().commit();

        } catch (Exception ex) {
            //entityManager.getTransaction().rollback();
            connection.rollback();

            LOG.error("Error saving");
            for (StagingInpatientCds cdsInPatient: toSave) {
                LOG.error("" + cdsInPatient);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            //entityManager.close();
            connection.close();
        }
    }

    @Override
    public void saveCDSOutpatients(List<StagingOutpatientCds> cdses, UUID serviceId) throws Exception {

        List<StagingOutpatientCds> toSave = new ArrayList<>();

        for (StagingOutpatientCds cdsOutpatient: cdses) {

            cdsOutpatient.setRecordChecksum(cdsOutpatient.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasOutpatientCdsAlreadyFiled(serviceId, cdsOutpatient)) {

                toSave.add(cdsOutpatient);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        //EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        Connection connection = ConnectionManager.getPublisherStagingConnection(serviceId);

        try {
            //SessionImpl session = (SessionImpl) entityManager.getDelegate();
            //Connection connection = session.connection();

            String sql = "INSERT INTO cds_outpatient "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, consultant_code, referral_source, " +
                    " patient_pathway_identifier, appt_attendance_identifier, administrative_category_code, " +
                    " appt_attended_code, appt_outcome_code, appt_date, appt_site_code, " +
                    " primary_diagnosis_ICD, secondary_diagnosis_ICD, other_diagnosis_ICD, primary_procedure_OPCS, " +
                    " primary_procedure_date, secondary_procedure_OPCS, secondary_procedure_date, other_procedures_OPCS, " +
                    " lookup_person_id, lookup_consultant_personnel_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " consultant_code = VALUES(consultant_code),"
                    + " referral_source = VALUES(referral_source),"
                    + " patient_pathway_identifier = VALUES(patient_pathway_identifier),"
                    + " appt_attendance_identifier = VALUES(appt_attendance_identifier),"
                    + " administrative_category_code = VALUES(administrative_category_code),"
                    + " appt_attended_code = VALUES(appt_attended_code),"
                    + " appt_outcome_code = VALUES(appt_outcome_code),"
                    + " appt_date = VALUES(appt_date),"
                    + " appt_site_code = VALUES(appt_site_code),"
                    + " primary_diagnosis_ICD = VALUES(primary_diagnosis_ICD),"
                    + " secondary_diagnosis_ICD = VALUES(secondary_diagnosis_ICD),"
                    + " other_diagnosis_ICD = VALUES(other_diagnosis_ICD),"
                    + " primary_procedure_OPCS = VALUES(primary_procedure_OPCS),"
                    + " primary_procedure_date = VALUES(primary_procedure_date),"
                    + " secondary_procedure_OPCS = VALUES(secondary_procedure_OPCS),"
                    + " secondary_procedure_date = VALUES(secondary_procedure_date),"
                    + " other_procedures_OPCS = VALUES(other_procedures_OPCS),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " lookup_consultant_personnel_id = VALUES(lookup_consultant_personnel_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            //entityManager.getTransaction().begin();

            for (StagingOutpatientCds cdsOutpatient : toSave) {

                int col = 1;

                ps.setString(col++, cdsOutpatient.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getDtReceived().getTime()));
                ps.setInt(col++, cdsOutpatient.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getCdsActivityDate().getTime()));
                ps.setString(col++, cdsOutpatient.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsOutpatient.getCdsUpdateType());
                ps.setString(col++, cdsOutpatient.getMrn());
                ps.setString(col++, cdsOutpatient.getNhsNumber());
                if (cdsOutpatient.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cdsOutpatient.getWithheld().booleanValue());
                }
                if (cdsOutpatient.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getDateOfBirth().getTime()));
                }
                ps.setString(col++, cdsOutpatient.getConsultantCode());
                ps.setString(col++, cdsOutpatient.getReferralSource());
                ps.setString(col++, cdsOutpatient.getPatientPathwayIdentifier());
                ps.setString(col++, cdsOutpatient.getApptAttendanceIdentifier());
                ps.setString(col++, cdsOutpatient.getAdministrativeCategoryCode());
                ps.setString(col++, cdsOutpatient.getApptAttendedCode());
                ps.setString(col++, cdsOutpatient.getApptOutcomeCode());

                if (cdsOutpatient.getApptDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getApptDate().getTime()));
                }
                ps.setString(col++, cdsOutpatient.getApptSiteCode());
                ps.setString(col++, cdsOutpatient.getPrimaryDiagnosisICD());
                ps.setString(col++, cdsOutpatient.getSecondaryDiagnosisICD());
                ps.setString(col++, cdsOutpatient.getOtherDiagnosisICD());
                ps.setString(col++, cdsOutpatient.getPrimaryProcedureOPCS());

                if (cdsOutpatient.getPrimaryProcedureDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getPrimaryProcedureDate().getTime()));
                }

                ps.setString(col++, cdsOutpatient.getSecondaryProcedureOPCS());

                if (cdsOutpatient.getSecondaryProcedureDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsOutpatient.getSecondaryProcedureDate().getTime()));
                }

                ps.setString(col++, cdsOutpatient.getOtherProceduresOPCS());

                if (cdsOutpatient.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsOutpatient.getLookupPersonId());
                }

                if (cdsOutpatient.getLookupConsultantPersonnelId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsOutpatient.getLookupConsultantPersonnelId());
                }

                if (cdsOutpatient.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsOutpatient.getAudit().writeToJson());
                }

                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            //entityManager.getTransaction().commit();

        } catch (Exception ex) {
            //entityManager.getTransaction().rollback();
            connection.rollback();

            LOG.error("Error saving");
            for (StagingOutpatientCds cdsOutpatient: toSave) {
                LOG.error("" + cdsOutpatient);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            //entityManager.close();
            connection.close();
        }
    }

    @Override
    public void saveCDSEmergencies(List<StagingEmergencyCds> cdses, UUID serviceId) throws Exception {

        List<StagingEmergencyCds> toSave = new ArrayList<>();

        for (StagingEmergencyCds cdsEmergency: cdses) {

            cdsEmergency.setRecordChecksum(cdsEmergency.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasEmergencyCdsAlreadyFiled(serviceId, cdsEmergency)) {

                toSave.add(cdsEmergency);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        //EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        Connection connection = ConnectionManager.getPublisherStagingConnection(serviceId);
        try {
            //SessionImpl session = (SessionImpl) entityManager.getDelegate();
            //Connection connection = session.connection();

            String sql = "INSERT INTO cds_emergency "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, patient_pathway_identifier, " +
                    " department_type, ambulance_incident_number, treatment_organisation_code, " +
                    " attendance_identifier, arrival_mode, attendance_category, attendance_source, " +
                    " arrival_date, initial_assessment_date, chief_complaint, seen_for_treatment_date, "+
                    " decided_to_admit_date, discharge_status, discharge_destination, discharge_destination_site_id, discharge_follow_up, " +
                    " conclusion_date, departure_date, mh_classifications, diagnosis, investigations, treatments, " +
                    " referred_to_services, safeguarding_concerns, lookup_person_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " patient_pathway_identifier = VALUES(patient_pathway_identifier),"
                    + " department_type = VALUES(department_type),"
                    + " ambulance_incident_number = VALUES(ambulance_incident_number),"
                    + " treatment_organisation_code = VALUES(treatment_organisation_code),"
                    + " attendance_identifier = VALUES(attendance_identifier),"
                    + " arrival_mode = VALUES(arrival_mode),"
                    + " attendance_category = VALUES(attendance_category),"
                    + " attendance_source = VALUES(attendance_source),"
                    + " arrival_date = VALUES(arrival_date),"
                    + " initial_assessment_date = VALUES(initial_assessment_date),"
                    + " chief_complaint = VALUES(chief_complaint),"
                    + " seen_for_treatment_date = VALUES(seen_for_treatment_date),"
                    + " decided_to_admit_date = VALUES(decided_to_admit_date),"
                    + " discharge_status = VALUES(discharge_status),"
                    + " discharge_destination = VALUES(discharge_destination),"
                    + " discharge_follow_up = VALUES(discharge_follow_up),"
                    + " conclusion_date = VALUES(conclusion_date),"
                    + " departure_date = VALUES(departure_date),"
                    + " mh_classifications = VALUES(mh_classifications),"
                    + " diagnosis = VALUES(diagnosis),"
                    + " investigations = VALUES(investigations),"
                    + " treatments = VALUES(treatments),"
                    + " referred_to_services = VALUES(referred_to_services),"
                    + " safeguarding_concerns = VALUES(safeguarding_concerns),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            for (StagingEmergencyCds cdsEmergency : toSave) {

                int col = 1;

                ps.setString(col++, cdsEmergency.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getDtReceived().getTime()));
                ps.setInt(col++, cdsEmergency.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getCdsActivityDate().getTime()));
                ps.setString(col++, cdsEmergency.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsEmergency.getCdsUpdateType());
                ps.setString(col++, cdsEmergency.getMrn());
                ps.setString(col++, cdsEmergency.getNhsNumber());
                if (cdsEmergency.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cdsEmergency.getWithheld().booleanValue());
                }
                if (cdsEmergency.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getDateOfBirth().getTime()));
                }

                ps.setString(col++, cdsEmergency.getPatientPathwayIdentifier());
                ps.setString(col++, cdsEmergency.getDepartmentType());
                ps.setString(col++, cdsEmergency.getAmbulanceIncidentNumber());
                ps.setString(col++, cdsEmergency.getTreatmentOrganisationCode());
                ps.setString(col++, cdsEmergency.getAttendanceIdentifier());
                ps.setString(col++, cdsEmergency.getArrivalMode());
                ps.setString(col++, cdsEmergency.getAttendanceCategory());
                ps.setString(col++, cdsEmergency.getAttendanceSource());

                if (cdsEmergency.getArrivalDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getArrivalDate().getTime()));
                }
                if (cdsEmergency.getInitialAssessmentDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getInitialAssessmentDate().getTime()));
                }
                ps.setString(col++, cdsEmergency.getChiefComplaint());

                if (cdsEmergency.getSeenForTreatmentDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getSeenForTreatmentDate().getTime()));
                }
                if (cdsEmergency.getDecidedToAdmitDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getDecidedToAdmitDate().getTime()));
                }
                ps.setString(col++, cdsEmergency.getDischargeStatus());
                ps.setString(col++, cdsEmergency.getDischargeDestination());
                ps.setString(col++, cdsEmergency.getDischargeDestinationSiteId());
                ps.setString(col++, cdsEmergency.getDischargeFollowUp());

                if (cdsEmergency.getConclusionDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getConclusionDate().getTime()));
                }
                if (cdsEmergency.getDepartureDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsEmergency.getDepartureDate().getTime()));
                }
                ps.setString(col++, cdsEmergency.getMhClassifications());
                ps.setString(col++, cdsEmergency.getDiagnosis());
                ps.setString(col++, cdsEmergency.getInvestigations());
                ps.setString(col++, cdsEmergency.getTreatments());
                ps.setString(col++, cdsEmergency.getReferredToServices());
                ps.setString(col++, cdsEmergency.getSafeguardingConcerns());

                if (cdsEmergency.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsEmergency.getLookupPersonId());
                }

                if (cdsEmergency.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsEmergency.getAudit().writeToJson());
                }

                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            //entityManager.getTransaction().commit();

        } catch (Exception ex) {
            //entityManager.getTransaction().rollback();
            connection.rollback();

            LOG.error("Error saving");
            for (StagingEmergencyCds cdsEmergency: toSave) {
                LOG.error("" + cdsEmergency);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            //entityManager.close();
            connection.close();;
        }
    }

    @Override
    public void saveCDSCriticalCares(List<StagingCriticalCareCds> cdses, UUID serviceId) throws Exception {

        List<StagingCriticalCareCds> toSave = new ArrayList<>();

        for (StagingCriticalCareCds cdsCriticalCare: cdses) {

            cdsCriticalCare.setRecordChecksum(cdsCriticalCare.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasCriticalCareCdsAlreadyFiled(serviceId, cdsCriticalCare)) {

                toSave.add(cdsCriticalCare);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        //EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        Connection connection = ConnectionManager.getPublisherStagingConnection(serviceId);

        try {
            //SessionImpl session = (SessionImpl) entityManager.getDelegate();
            //Connection connection = session.connection();

            String sql = "INSERT INTO cds_critical_care "
                    + " (exchange_id, dt_received, record_checksum, cds_unique_identifier, mrn, nhs_number, critical_care_type_id, " +
                      " spell_number, episode_number, critical_care_identifier, care_start_date, care_unit_function, " +
                      " admission_source_code, admission_type_code, admission_location, gestation_length_at_delivery, advanced_respiratory_support_days, " +
                      " basic_respiratory_supports_days, advanced_cardiovascular_support_days, basic_cardiovascular_support_days, " +
                      " renal_support_days, neurological_support_days, gastro_intestinal_support_days, dermatological_support_days, " +
                      " liver_support_days, organ_support_maximum, critical_care_level2_days, critical_care_level3_days, " +
                      " discharge_date, discharge_ready_date, discharge_status_code, discharge_destination, discharge_location, " +
                      " care_activity_1, care_activity_2100, lookup_person_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " critical_care_type_id = VALUES(critical_care_type_id),"
                    + " spell_number = VALUES(spell_number),"
                    + " episode_number = VALUES(episode_number),"
                    + " critical_care_identifier = VALUES(critical_care_identifier),"
                    + " care_start_date = VALUES(care_start_date),"
                    + " care_unit_function = VALUES(care_unit_function),"
                    + " admission_source_code = VALUES(admission_source_code),"
                    + " admission_type_code = VALUES(admission_type_code),"
                    + " admission_location = VALUES(admission_location),"
                    + " gestation_length_at_delivery = VALUES(gestation_length_at_delivery),"
                    + " advanced_respiratory_support_days = VALUES(advanced_respiratory_support_days),"
                    + " basic_respiratory_supports_days = VALUES(basic_respiratory_supports_days),"
                    + " advanced_cardiovascular_support_days = VALUES(advanced_cardiovascular_support_days),"
                    + " basic_cardiovascular_support_days = VALUES(basic_cardiovascular_support_days),"
                    + " renal_support_days = VALUES(renal_support_days),"
                    + " neurological_support_days = VALUES(neurological_support_days),"
                    + " gastro_intestinal_support_days = VALUES(gastro_intestinal_support_days),"
                    + " dermatological_support_days = VALUES(dermatological_support_days),"
                    + " liver_support_days = VALUES(liver_support_days),"
                    + " organ_support_maximum = VALUES(organ_support_maximum),"
                    + " critical_care_level2_days = VALUES(critical_care_level2_days),"
                    + " critical_care_level3_days = VALUES(critical_care_level3_days),"
                    + " discharge_date = VALUES(discharge_date),"
                    + " discharge_ready_date = VALUES(discharge_ready_date),"
                    + " discharge_status_code = VALUES(discharge_status_code),"
                    + " discharge_destination = VALUES(discharge_destination),"
                    + " discharge_location = VALUES(discharge_location),"
                    + " care_activity_1 = VALUES(care_activity_1),"
                    + " care_activity_2100 = VALUES(care_activity_2100),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            //entityManager.getTransaction().begin();

            for (StagingCriticalCareCds cdsCriticalCare : toSave) {

                int col = 1;

                ps.setString(col++, cdsCriticalCare.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsCriticalCare.getDtReceived().getTime()));
                ps.setInt(col++, cdsCriticalCare.getRecordChecksum());
                ps.setString(col++, cdsCriticalCare.getCdsUniqueIdentifier());
                ps.setString(col++, cdsCriticalCare.getMrn());
                ps.setString(col++, cdsCriticalCare.getNhsNumber());

                ps.setString(col++, cdsCriticalCare.getCriticalCareTypeId());
                ps.setString(col++, cdsCriticalCare.getSpellNumber());
                ps.setString(col++, cdsCriticalCare.getEpisodeNumber());
                ps.setString(col++, cdsCriticalCare.getCriticalCareIdentifier());

                if (cdsCriticalCare.getCareStartDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsCriticalCare.getCareStartDate().getTime()));
                }
                ps.setString(col++, cdsCriticalCare.getCareUnitFunction());
                ps.setString(col++, cdsCriticalCare.getAdmissionSourceCode());
                ps.setString(col++, cdsCriticalCare.getAdmissionTypeCode());
                ps.setString(col++, cdsCriticalCare.getAdmissionLocation());
                ps.setString(col++, cdsCriticalCare.getGestationLengthAtDelivery());

                if (cdsCriticalCare.getAdvancedRespiratorySupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getAdvancedRespiratorySupportDays());
                }
                if (cdsCriticalCare.getBasicRespiratorySupportsDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getBasicRespiratorySupportsDays());
                }
                if (cdsCriticalCare.getAdvancedCardiovascularSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getAdvancedCardiovascularSupportDays());
                }
                if (cdsCriticalCare.getBasicCardiovascularSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getBasicCardiovascularSupportDays());
                }
                if (cdsCriticalCare.getRenalSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getRenalSupportDays());
                }
                if (cdsCriticalCare.getNeurologicalSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getNeurologicalSupportDays());
                }
                if (cdsCriticalCare.getGastroIntestinalSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getGastroIntestinalSupportDays());
                }
                if (cdsCriticalCare.getDermatologicalSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getDermatologicalSupportDays());
                }
                if (cdsCriticalCare.getLiverSupportDays() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getLiverSupportDays());
                }
                if (cdsCriticalCare.getOrganSupportMaximum() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getOrganSupportMaximum());
                }
                if (cdsCriticalCare.getCriticalCareLevel2Days() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getCriticalCareLevel2Days());
                }
                if (cdsCriticalCare.getCriticalCareLevel3Days() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getCriticalCareLevel3Days());
                }
                if (cdsCriticalCare.getDischargeDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsCriticalCare.getDischargeDate().getTime()));
                }
                if (cdsCriticalCare.getDischargeReadyDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsCriticalCare.getDischargeReadyDate().getTime()));
                }
                ps.setString(col++, cdsCriticalCare.getDischargeStatusCode());
                ps.setString(col++, cdsCriticalCare.getDischargeDestination());
                ps.setString(col++, cdsCriticalCare.getDischargeLocation());

                ps.setString(col++, cdsCriticalCare.getCareActivity1());
                ps.setString(col++, cdsCriticalCare.getCareActivity2100());

                if (cdsCriticalCare.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsCriticalCare.getLookupPersonId());
                }

                if (cdsCriticalCare.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsCriticalCare.getAudit().writeToJson());
                }

                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            //entityManager.getTransaction().commit();

        } catch (Exception ex) {
            //entityManager.getTransaction().rollback();
            connection.rollback();

            LOG.error("Error saving");
            for (StagingCriticalCareCds cdsCritical: toSave) {
                LOG.error("" + cdsCritical);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            //entityManager.close();
            connection.close();
        }
    }

    @Override
    public void saveCDSHomeDelBirths(List<StagingHomeDelBirthCds> cdses, UUID serviceId) throws Exception {

        List<StagingHomeDelBirthCds> toSave = new ArrayList<>();

        for (StagingHomeDelBirthCds cdsHomeDelBirth : cdses) {

            cdsHomeDelBirth.setRecordChecksum(cdsHomeDelBirth.hashCode());

            //check if record already filed to avoid duplicates
            if (!wasHomeDelBirthCdsAlreadyFiled(serviceId, cdsHomeDelBirth)) {

                toSave.add(cdsHomeDelBirth);
            }
        }

        if (toSave.isEmpty()) {
            return;
        }

        //EntityManager entityManager = ConnectionManager.getPublisherStagingEntityManager(serviceId);
        PreparedStatement ps = null;
        Connection connection = ConnectionManager.getPublisherStagingConnection(serviceId);

        try {
            //SessionImpl session = (SessionImpl) entityManager.getDelegate();
            //Connection connection = session.connection();

            String sql = "INSERT INTO cds_home_delivery_birth  "
                    + " (exchange_id, dt_received, record_checksum, cds_activity_date, cds_unique_identifier, " +
                    " cds_update_type, mrn, nhs_number, withheld, date_of_birth, " +
                    " birth_weight, live_or_still_birth_indicator, total_previous_pregnancies, " +
                    " number_of_babies, first_antenatal_assessment_date, antenatal_care_practitioner, " +
                    " antenatal_care_practice, delivery_place_intended, delivery_place_change_reason_code, " +
                    " gestation_length_labour_onset, delivery_date, delivery_place_actual, delivery_method, " +
                    " mother_nhs_number, lookup_person_id, audit_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_id = VALUES(exchange_id),"
                    + " dt_received = VALUES(dt_received),"
                    + " record_checksum = VALUES(record_checksum),"
                    + " cds_activity_date=VALUES(cds_activity_date),"
                    + " cds_unique_identifier = VALUES(cds_unique_identifier),"
                    + " cds_update_type = VALUES(cds_update_type),"
                    + " mrn = VALUES(mrn),"
                    + " nhs_number = VALUES(nhs_number),"
                    + " withheld = VALUES(withheld),"
                    + " date_of_birth = VALUES(date_of_birth),"
                    + " birth_weight = VALUES(birth_weight),"
                    + " live_or_still_birth_indicator = VALUES(live_or_still_birth_indicator),"
                    + " total_previous_pregnancies = VALUES(total_previous_pregnancies),"
                    + " number_of_babies = VALUES(number_of_babies),"
                    + " first_antenatal_assessment_date = VALUES(first_antenatal_assessment_date),"
                    + " antenatal_care_practitioner = VALUES(antenatal_care_practitioner),"
                    + " antenatal_care_practice = VALUES(antenatal_care_practice),"
                    + " delivery_place_intended = VALUES(delivery_place_intended),"
                    + " delivery_place_change_reason_code = VALUES(delivery_place_change_reason_code),"
                    + " gestation_length_labour_onset = VALUES(gestation_length_labour_onset),"
                    + " delivery_date = VALUES(delivery_date),"
                    + " delivery_place_actual = VALUES(delivery_place_actual),"
                    + " delivery_method = VALUES(delivery_method),"
                    + " mother_nhs_number = VALUES(mother_nhs_number),"
                    + " lookup_person_id = VALUES(lookup_person_id),"
                    + " audit_json = VALUES(audit_json)";

            ps = connection.prepareStatement(sql);

            //entityManager.getTransaction().begin();

            for (StagingHomeDelBirthCds cdsHomeDelBirth : toSave) {

                int col = 1;

                ps.setString(col++, cdsHomeDelBirth.getExchangeId());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getDtReceived().getTime()));
                ps.setInt(col++, cdsHomeDelBirth.getRecordChecksum());
                ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getCdsActivityDate().getTime()));
                ps.setString(col++, cdsHomeDelBirth.getCdsUniqueIdentifier());
                ps.setInt(col++, cdsHomeDelBirth.getCdsUpdateType());
                ps.setString(col++, cdsHomeDelBirth.getMrn());
                ps.setString(col++, cdsHomeDelBirth.getNhsNumber());
                if (cdsHomeDelBirth.getWithheld() == null) {
                    ps.setNull(col++, Types.BOOLEAN);
                } else {
                    ps.setBoolean(col++, cdsHomeDelBirth.getWithheld().booleanValue());
                }
                if (cdsHomeDelBirth.getDateOfBirth() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getDateOfBirth().getTime()));
                }

                ps.setString(col++, cdsHomeDelBirth.getBirthWeight());
                ps.setString(col++, cdsHomeDelBirth.getLiveOrStillBirthIndicator());
                ps.setString(col++, cdsHomeDelBirth.getTotalPreviousPregnancies());

                if (cdsHomeDelBirth.getNumberOfBabies() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsHomeDelBirth.getNumberOfBabies());
                }
                if (cdsHomeDelBirth.getFirstAntenatalAssessmentDate() == null) {
                    ps.setNull(col++, Types.NULL);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getFirstAntenatalAssessmentDate().getTime()));
                }
                ps.setString(col++, cdsHomeDelBirth.getAntenatalCarePractitioner());
                ps.setString(col++, cdsHomeDelBirth.getAntenatalCarePractice());
                ps.setString(col++, cdsHomeDelBirth.getDeliveryPlaceIntended());
                ps.setString(col++, cdsHomeDelBirth.getDeliveryPlaceChangeReasonCode());
                ps.setString(col++, cdsHomeDelBirth.getGestationLengthLabourOnset());

                if (cdsHomeDelBirth.getDeliveryDate() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setTimestamp(col++, new java.sql.Timestamp(cdsHomeDelBirth.getDeliveryDate().getTime()));
                }
                ps.setString(col++, cdsHomeDelBirth.getDeliveryPlaceActual());
                ps.setString(col++, cdsHomeDelBirth.getDeliveryMethod());
                ps.setString(col++, cdsHomeDelBirth.getMotherNhsNumber());

                if (cdsHomeDelBirth.getLookupPersonId() == null) {
                    ps.setNull(col++, Types.INTEGER);
                } else {
                    ps.setInt(col++, cdsHomeDelBirth.getLookupPersonId());
                }

                if (cdsHomeDelBirth.getAudit() == null) {
                    ps.setNull(col++, Types.VARCHAR);
                } else {
                    ps.setString(col++, cdsHomeDelBirth.getAudit().writeToJson());
                }

                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
            //entityManager.getTransaction().commit();

        } catch (Exception ex) {
            //entityManager.getTransaction().rollback();
            connection.rollback();

            LOG.error("Error saving");
            for (StagingHomeDelBirthCds cdsHomeDelBirth: toSave) {
                LOG.error("" + cdsHomeDelBirth);
            }
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            //entityManager.close();
            connection.close();
        }
    }
}