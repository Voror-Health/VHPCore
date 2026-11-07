package org.endeavourhealth.core.database.rdbms.audit;

import org.endeavourhealth.core.database.dal.audit.TransformWarningDalI;
import org.endeavourhealth.core.database.dal.audit.models.TransformWarning;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsTransformWarning;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsTransformWarningType;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RdbmsTransformWarningDal implements TransformWarningDalI {

    private static Map<String, Integer> warningTypeCache = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(TransformWarningDalI.class); //Added for debugging NPE

    public void recordWarning(UUID serviceId, UUID systemId, UUID exchangeId, Integer publishedFileId, Integer recordNumber, String warningText, String... warningParams) throws Exception {

        //only support up to four params
        validateWarning(warningText, warningParams);

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        try {

            //note this function manages its own transaction so is before the begin() call
            int warningTypeId = findOrCreateWarningType(entityManager, warningText, true);
            Date now = new Date();

            //run the next two in a single transaction, rolling back if it fails
            try {
                entityManager.getTransaction().begin();

                saveNewWarning(entityManager, warningTypeId, serviceId, systemId, exchangeId, publishedFileId, recordNumber, now, warningParams);
                updateWarningTypeDate(entityManager, warningTypeId, now);

                entityManager.getTransaction().commit();

            } catch (Exception ex) {
                entityManager.getTransaction().rollback();
                throw ex;
            }

        } finally {
            entityManager.close();
        }
    }

    @Override
    public void recordWarnings(List<TransformWarning> warnings) throws Exception {

        //only support up to four params
        for (TransformWarning warning: warnings) {
            validateWarning(warning.getWarningText(), warning.getWarningParams());
        }

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;
        try {

            //find the type for each one
            Map<TransformWarning, Integer> hmTypes = new HashMap<>();
            Set<Integer> hsTypes = new HashSet<>();
            for (TransformWarning warning: warnings) {
                //note this function manages its own transaction so is before the begin() call
                int warningTypeId = findOrCreateWarningType(entityManager, warning.getWarningText(), true);
                hmTypes.put(warning, new Integer(warningTypeId));
                hsTypes.add(new Integer(warningTypeId));
            }

            Date now = new Date();

            //save each one
            String sql = "INSERT INTO transform_warning ("
                    + "service_id, system_id, exchange_id, source_file_record_id, inserted_at, transform_warning_type_id, "
                    + "param_1, param_2, param_3, param_4, published_file_id, record_number)"
                    + " VALUES ("
                    + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                    + ")";

            SessionImpl session = (SessionImpl)entityManager.getDelegate();
            Connection connection = session.connection();
            ps = connection.prepareStatement(sql);

            //run the next two in a single transaction, rolling back if it fails
            try {
                entityManager.getTransaction().begin();

                for (TransformWarning warning: warnings) {
                    Integer type = (Integer)hmTypes.get(warning);

                    int col = 1;
                    ps.setString(col++, warning.getServiceId().toString());
                    if (warning.getSystemId() == null) {
                        ps.setNull(col++, Types.VARCHAR);
                    } else {
                        ps.setString(col++, warning.getSystemId().toString());
                    }
                    ps.setString(col++, warning.getExchangeId().toString());
                    ps.setNull(col++, Types.BIGINT); //field no longer used
                    ps.setTimestamp(col++, new java.sql.Timestamp(now.getTime()));
                    ps.setInt(col++, type);
                    ps.setString(col++, findParam(warning.getWarningParams(), 0));
                    ps.setString(col++, findParam(warning.getWarningParams(), 1));
                    ps.setString(col++, findParam(warning.getWarningParams(), 2));
                    ps.setString(col++, findParam(warning.getWarningParams(), 3));
                    if (warning.getPublishedFileId() == null) {
                        ps.setNull(col++, Types.INTEGER);
                    } else {
                        ps.setInt(col++, warning.getPublishedFileId());
                    }
                    if (warning.getRecordNumber() == null) {
                        ps.setNull(col++, Types.INTEGER);
                    } else {
                        ps.setInt(col++, warning.getRecordNumber());
                    }

                    ps.addBatch();
                }

                ps.executeBatch();

                //update the warning types to say when they were last used
                for (Integer type: hsTypes) {
                    updateWarningTypeDate(entityManager, type.intValue(), now);
                }

                entityManager.getTransaction().commit();

            } catch (Exception ex) {
                entityManager.getTransaction().rollback();
                throw ex;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    private void validateWarning(String warningText, String[] warningParams) {

        //our table only supports four parameters
        int parameterCount = warningParams.length;
        if (parameterCount > 4) {
            throw new IllegalArgumentException("Transform Warning table only supports up to four parameters (trying to save " + warningParams.length + ")");
        }

        if (parameterCount < 1) {
            throw new IllegalArgumentException("At least one parameter must be supplied Transform Warnings");
        }

        //ensure that each parameter is referenced in the String (which also helps validate
        //that the warning String doesn't inadvertently contain parameters itself)
        int paramterReferences = 0;
        int nextIndex = 0;
        while (true) {
            int index = warningText.indexOf("{}", nextIndex);
            if (index > -1) {
                nextIndex = index + 2;
                paramterReferences ++;

            } else {
                break;
            }
        }

        //if we have fewer parameter references than parameters, something is wrong
        if (paramterReferences < parameterCount) {
            throw new IllegalArgumentException("Mismatch in warning String and number of parameters - warning String references " + paramterReferences + " but " + parameterCount + " parameters supplied");
        }
    }

    private void updateWarningTypeDate(EntityManager entityManager, int warningTypeId, Date now) throws Exception {
        PreparedStatement ps = null;
        try {

            SessionImpl session = (SessionImpl)entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "UPDATE transform_warning_type"
                    + " SET last_used_at = ?"
                    + " WHERE id = ?";
            ps = connection.prepareStatement(sql);

            ps.setDate(1, new java.sql.Date(now.getTime()));
            ps.setInt(2, warningTypeId);

            ps.executeUpdate();

        } finally {
            if (ps != null) {
                ps.close();
            }

        }
    }

    private void saveNewWarning(EntityManager entityManager, int warningTypeId, UUID serviceId, UUID systemId, UUID exchangeId, Integer publishedFileId, Integer recordNumber, Date now, String[] warningParams) {
        RdbmsTransformWarning newWarning = new RdbmsTransformWarning();
        newWarning.setServiceId(serviceId.toString());
        newWarning.setSystemId(systemId.toString());
        newWarning.setExchangeId(exchangeId.toString());
        newWarning.setPublishedFileId(publishedFileId); //may be null
        newWarning.setRecordNumber(recordNumber);
        newWarning.setInsertedAt(now);
        newWarning.setTransformWarningTypeId(warningTypeId);
        newWarning.setParam1(findParam(warningParams, 0));
        newWarning.setParam2(findParam(warningParams, 1));
        newWarning.setParam3(findParam(warningParams, 2));
        newWarning.setParam4(findParam(warningParams, 3));

        //transaction is started by the calling fn
        entityManager.persist(newWarning);
    }

    private static String findParam(String[] warningParams, int index) {
        if (index >= warningParams.length) {
            return null;
        } else {
            String ret = warningParams[index];
            return ret;
        }
    }

    private int findOrCreateWarningType(EntityManager entityManager, String warningText, boolean firstAttempt) {

        //check our cache
        Integer ret = warningTypeCache.get(warningText);
        if (ret != null) {
            return ret.intValue();
        }

        //hit the DB
        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsTransformWarningType c"
                    + " where c.warning = :warning_text";

            Query query = entityManager.createQuery(sql, RdbmsTransformWarningType.class)
                    .setParameter("warning_text", warningText);

            RdbmsTransformWarningType result = (RdbmsTransformWarningType)query.getSingleResult();
            ret = new Integer(result.getId());

        } catch (NoResultException neEx) {
            //if we get no result, then we want to CREATE a new warning type
            RdbmsTransformWarningType newType = new RdbmsTransformWarningType();
            newType.setWarning(warningText);
            newType.setLastUsedAt(new Date());

            try {
                entityManager.getTransaction().begin();
                entityManager.persist(newType);
                entityManager.getTransaction().commit();

                ret = new Integer(newType.getId());

            } catch (Exception ex) {
                entityManager.getTransaction().rollback();

                //if we get an exception saving the new type, then it'll be because another thread/app beat
                //us to it, in which case we try to search again (unless we've already tried again)
                if (firstAttempt) {
                    return findOrCreateWarningType(entityManager, warningText, false);

                } else {
                    throw ex;
                }
            }
        }

        //add to the cache
        warningTypeCache.put(warningText, ret);

        return ret.intValue();
    }
}
