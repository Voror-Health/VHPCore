package org.endeavourhealth.core.database.rdbms.audit;

import com.google.common.base.Strings;
import org.endeavourhealth.core.database.dal.audit.ExchangeDalI;
import org.endeavourhealth.core.database.dal.audit.models.*;
import org.endeavourhealth.core.database.dal.datasharingmanager.models.JsonDDSOrganisationStatus;
import org.endeavourhealth.core.database.dal.datasharingmanager.models.JsonOrganisationCCG;
import org.endeavourhealth.core.database.dal.usermanager.caching.OrganisationCache;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsExchange;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsExchangeEvent;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsExchangeTransformAudit;
import org.endeavourhealth.core.database.rdbms.audit.models.RdbmsExchangeTransformErrorState;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.OrganisationEntity;
import org.endeavourhealth.core.xml.TransformErrorSerializer;
import org.endeavourhealth.core.xml.transformError.TransformError;
import org.hibernate.internal.SessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RdbmsExchangeDal implements ExchangeDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsExchangeDal.class);

    public void save(Exchange exchange) throws Exception {

        RdbmsExchange dbObj = new RdbmsExchange(exchange);

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;

        try {
            //have to use prepared statement as JPA doesn't support upserts
            //entityManager.persist(dbObj);

            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO exchange"
                    + " (id, timestamp, headers, service_id, system_id, body)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " timestamp = VALUES(timestamp),"
                    + " headers = VALUES(headers),"
                    + " service_id = VALUES(service_id),"
                    + " system_id = VALUES(system_id),"
                    + " body = VALUES(body)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            ps.setString(1, dbObj.getId());
            ps.setTimestamp(2, new java.sql.Timestamp(dbObj.getTimestamp().getTime()));
            ps.setString(3, dbObj.getHeaders());
            ps.setString(4, dbObj.getServiceId());
            ps.setString(5, dbObj.getSystemId());
            ps.setString(6, dbObj.getBody());

            ps.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    public void save(ExchangeEvent event) throws Exception {

        RdbmsExchangeEvent dbObj = new RdbmsExchangeEvent(event);
        dbObj.setId(UUID.randomUUID().toString()); //not set by the proxy constructor but needed

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(dbObj);
            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }

    }

    public void save(ExchangeTransformAudit exchangeTransformAudit) throws Exception {

        RdbmsExchangeTransformAudit dbObj = new RdbmsExchangeTransformAudit(exchangeTransformAudit);

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;

        try {
            //have to use prepared statement as JPA doesn't support upserts
            //entityManager.persist(dbObj);

            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO exchange_transform_audit"
                    + " (id, service_id, system_id, exchange_id, started, ended, error_xml, resubmitted, deleted, number_batches_created)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " started = VALUES(started),"
                    + " ended = VALUES(ended),"
                    + " error_xml = VALUES(error_xml),"
                    + " resubmitted = VALUES(resubmitted),"
                    + " deleted = VALUES(deleted),"
                    + " number_batches_created = VALUES(number_batches_created)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            ps.setString(1, dbObj.getId());
            ps.setString(2, dbObj.getServiceId());
            ps.setString(3, dbObj.getSystemId());
            ps.setString(4, dbObj.getExchangeId());
            if (dbObj.getStarted() != null) {
                ps.setTimestamp(5, new java.sql.Timestamp(dbObj.getStarted().getTime()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            if (dbObj.getEnded() != null) {
                ps.setTimestamp(6, new java.sql.Timestamp(dbObj.getEnded().getTime()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            if (!Strings.isNullOrEmpty(dbObj.getErrorXml())) {
                ps.setString(7, dbObj.getErrorXml());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            ps.setBoolean(8, dbObj.isResubmitted());
            if (dbObj.getDeleted() != null) {
                ps.setTimestamp(9, new java.sql.Timestamp(dbObj.getDeleted().getTime()));
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }
            if (dbObj.getNumberBatchesCreated() != null) {
                ps.setInt(10, dbObj.getNumberBatchesCreated());
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            ps.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }

    }

    public void save(ExchangeTransformErrorState errorState) throws Exception {

        RdbmsExchangeTransformErrorState dbObj = new RdbmsExchangeTransformErrorState(errorState);

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;

        try {
            //have to use prepared statement as JPA doesn't support upserts
            //entityManager.persist(dbObj);

            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO exchange_transform_error_state"
                    + " (service_id, system_id, exchange_ids_in_error)"
                    + " VALUES (?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " exchange_ids_in_error = VALUES(exchange_ids_in_error)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            ps.setString(1, dbObj.getServiceId());
            ps.setString(2, dbObj.getSystemId());
            ps.setString(3, dbObj.getExchangeIdsInError());

            ps.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }


    public Exchange getExchange(UUID exchangeId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchange c"
                    + " where c.id = :id";

            Query query = entityManager.createQuery(sql, RdbmsExchange.class)
                    .setParameter("id", exchangeId.toString());

            RdbmsExchange result = (RdbmsExchange)query.getSingleResult();
            return new Exchange(result);

        } catch (NoResultException ex) {
            return null;

        } finally {
            entityManager.close();
        }
    }


    public void delete(ExchangeTransformErrorState errorState) throws Exception {

        RdbmsExchangeTransformErrorState dbObj = new RdbmsExchangeTransformErrorState(errorState);

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;
        try {
            //have to use prepared statement as JPA doesn't support deleting without retrieving
            //entityManager.remove(dbObj);

            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "DELETE FROM exchange_transform_error_state"
                    + " WHERE service_id = ?"
                    + " AND system_id = ?";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            ps.setString(1, dbObj.getServiceId());
            ps.setString(2, dbObj.getSystemId());

            ps.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    public ExchangeTransformAudit getMostRecentExchangeTransform(UUID serviceId, UUID systemId, UUID exchangeId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.exchangeId = :exchange_id"
                    + " order by c.started DESC";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setParameter("exchange_id", exchangeId.toString())
                    .setMaxResults(1);

            RdbmsExchangeTransformAudit result = (RdbmsExchangeTransformAudit)query.getSingleResult();
            return new ExchangeTransformAudit(result);

        } catch (NoResultException ex) {
            return null;

        } finally {
            entityManager.close();
        }
    }

    public ExchangeTransformErrorState getErrorState(UUID serviceId, UUID systemId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformErrorState c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformErrorState.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setMaxResults(1);

            RdbmsExchangeTransformErrorState result = (RdbmsExchangeTransformErrorState)query.getSingleResult();
            return new ExchangeTransformErrorState(result);

        } catch (NoResultException ex) {
            return null;

        } finally {
            entityManager.close();
        }
    }

    @Override
    public List<ExchangeTransformErrorState> getErrorStatesForService(UUID serviceId, UUID systemId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformErrorState c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformErrorState.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString());

            List<RdbmsExchangeTransformErrorState> results = query.getResultList();

            //can't use stream() here as the constructor can throw an exception
            List<ExchangeTransformErrorState> ret = new ArrayList<>();
            for (RdbmsExchangeTransformErrorState result: results) {
                ret.add(new ExchangeTransformErrorState(result));
            }
            return ret;

        } finally {
            entityManager.close();
        }
    }

    public List<ExchangeTransformErrorState> getAllErrorStates() throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformErrorState c";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformErrorState.class);
            List<RdbmsExchangeTransformErrorState> results = query.getResultList();

            //can't use streams as the constructor is declared as throwing an exception
            List<ExchangeTransformErrorState> ret = new ArrayList<>();
            for (RdbmsExchangeTransformErrorState result : results) {
                ret.add(new ExchangeTransformErrorState(result));
            }
            return ret;

        } finally {
            entityManager.close();
        }
    }

    /*@Override
    public UUID getFirstExchangeId(UUID serviceId, UUID systemId) throws Exception {

        //we assume a service is started if we've previously processed an exchange without error
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchange c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " order by c.timestamp ASC";

            Query query = entityManager.createQuery(sql, RdbmsExchange.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setMaxResults(1);

            RdbmsExchange o = (RdbmsExchange)query.getSingleResult();
            return UUID.fromString(o.getId());

        } catch (NoResultException ex) {
            return null;

        } finally {
            entityManager.close();
        }
    }*/


    /*public boolean hasProcessedExchangeOk(UUID serviceId, UUID systemId) throws Exception {

        //we assume a service is started if we've previously processed an exchange without error
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.errorXml is null" //no errors
                    + " and c.deleted is null" //wasn't deleted
                    + " and c.started is not null" //was actually run (probably not required)
                    + " and c.ended is not null"; //actually finished

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setMaxResults(1);

            RdbmsExchangeTransformAudit o = (RdbmsExchangeTransformAudit)query.getSingleResult();
            //if we find an audit, we've started
            return true;

        } catch (NoResultException ex) {
            return false;

        } finally {
            entityManager.close();
        }
    }*/


    public List<ExchangeTransformAudit> getAllExchangeTransformAudits(UUID serviceId, UUID systemId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " order by c.started asc";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString());

            List<RdbmsExchangeTransformAudit> ret = query.getResultList();

            return ret
                    .stream()
                    .map(T -> new ExchangeTransformAudit(T))
                    .collect(Collectors.toList());

        } finally {
            entityManager.close();
        }
    }

    public List<ExchangeTransformAudit> getAllExchangeTransformAudits(UUID serviceId, UUID systemId, UUID exchangeId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.exchangeId = :exchange_id"
                    + " order by c.started asc";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setParameter("exchange_id", exchangeId.toString());

            List<RdbmsExchangeTransformAudit> ret = query.getResultList();

            return ret
                    .stream()
                    .map(T -> new ExchangeTransformAudit(T))
                    .collect(Collectors.toList());

        } finally {
            entityManager.close();
        }
    }

    @Override
    public ExchangeTransformAudit getLatestExchangeTransformAudit(UUID serviceId, UUID systemId, UUID exchangeId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.exchangeId = :exchange_id"
                    + " order by c.started desc";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setParameter("exchange_id", exchangeId.toString())
                    .setMaxResults(1);

            try {
                RdbmsExchangeTransformAudit result = (RdbmsExchangeTransformAudit)query.getSingleResult();
                return new ExchangeTransformAudit(result);
            } catch (NoResultException e) {
                return null;
            }

        } finally {
            entityManager.close();
        }
    }

    public List<Exchange> getExchangesByService(UUID serviceId, UUID systemId, int maxRows, Date dateFrom, Date dateTo) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchange c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.timestamp >= :date_from"
                    + " and c.timestamp <= :date_to"
                    + " order by c.timestamp desc";

            Query query = entityManager.createQuery(sql, RdbmsExchange.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setParameter("date_from", dateFrom)
                    .setParameter("date_to", dateTo)
                    .setMaxResults(maxRows);

            List<RdbmsExchange> results = query.getResultList();

            List<Exchange> ret = new ArrayList<>();
            for (RdbmsExchange result : results) {
                ret.add(new Exchange(result));
            }
            return ret;

        } finally {
            entityManager.close();
        }
    }

    public List<Exchange> getExchangesByService(UUID serviceId, UUID systemId, int maxRows) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchange c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " order by c.timestamp desc";

            Query query = entityManager.createQuery(sql, RdbmsExchange.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setMaxResults(maxRows);

            List<RdbmsExchange> results = query.getResultList();

            List<Exchange> ret = new ArrayList<>();
            for (RdbmsExchange result : results) {
                ret.add(new Exchange(result));
            }
            return ret;

        } finally {
            entityManager.close();
        }
    }

    public List<ExchangeEvent> getExchangeEvents(UUID exchangeId) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeEvent c"
                    + " where c.exchangeId = :exchange_id"
                    + " order by c.timestamp ASC";

            Query query = entityManager.createQuery(sql, RdbmsExchangeEvent.class)
                    .setParameter("exchange_id", exchangeId.toString());

            List<RdbmsExchangeEvent> ret = query.getResultList();

            return ret
                    .stream()
                    .map(T -> new ExchangeEvent(T))
                    .collect(Collectors.toList());

        } finally {
            entityManager.close();
        }
    }



    public List<UUID> getExchangeIdsForService(UUID serviceId, UUID systemId) throws Exception {

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c.id"
                    + " from"
                    + " RdbmsExchange c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " order by c.timestamp ASC";

            Query query = entityManager.createQuery(sql)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString());

            List<String> list = query.getResultList();

            List<UUID> ret = new ArrayList<>();
            for (String s : list) {
                UUID uuid = UUID.fromString(s);
                ret.add(uuid);
            }
            return ret;

        } finally {
            entityManager.close();
        }
    }

    public ExchangeTransformAudit getExchangeTransformAudit(UUID serviceId, UUID systemId, UUID exchangeId, UUID id) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            String sql = "select c"
                    + " from"
                    + " RdbmsExchangeTransformAudit c"
                    + " where c.serviceId = :service_id"
                    + " and c.systemId = :system_id"
                    + " and c.exchangeId = :exchange_id"
                    + " and c.id = :id";

            Query query = entityManager.createQuery(sql, RdbmsExchangeTransformAudit.class)
                    .setParameter("service_id", serviceId.toString())
                    .setParameter("system_id", systemId.toString())
                    .setParameter("exchange_id", exchangeId.toString())
                    .setParameter("id", id.toString())
                    .setMaxResults(1);

            RdbmsExchangeTransformAudit result = (RdbmsExchangeTransformAudit)query.getSingleResult();
            return new ExchangeTransformAudit(result);

        } catch (NoResultException ex) {
            return null;

        } finally {
            entityManager.close();
        }
    }


    @Override
    public void save(ExchangeSubscriberTransformAudit audit) throws Exception {

        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "INSERT INTO exchange_subscriber_transform_audit"
                    + " (exchange_id, exchange_batch_id, subscriber_config_name, started, ended, error_xml, number_resources_transformed, queued_message_id)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " ended = VALUES(ended),"
                    + " error_xml = VALUES(error_xml),"
                    + " number_resources_transformed = VALUES(number_resources_transformed),"
                    + " queued_message_id = VALUES(queued_message_id)";

            ps = connection.prepareStatement(sql);

            ps.setString(1, audit.getExchangeId().toString());
            ps.setString(2, audit.getExchangeBatchId().toString());
            ps.setString(3, audit.getSubscriberConfigName());
            ps.setTimestamp(4, new java.sql.Timestamp(audit.getStarted().getTime()));
            if (audit.getEnded() != null) {
                ps.setTimestamp(5, new java.sql.Timestamp(audit.getEnded().getTime()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            if (!Strings.isNullOrEmpty(audit.getErrorXml())) {
                ps.setString(6, audit.getErrorXml());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            if (audit.getNumberResourcesTransformed() != null) {
                ps.setInt(7, audit.getNumberResourcesTransformed());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            if (audit.getQueuedMessageId() != null) {
                ps.setString(8, audit.getQueuedMessageId().toString());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            ps.executeUpdate();
            connection.commit();

        } catch (Exception ex) {
            connection.rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    @Override
    public List<ExchangeSubscriberTransformAudit> getSubscriberTransformAudits(UUID exchangeId) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT exchange_id, exchange_batch_id, subscriber_config_name, started, ended, error_xml, number_resources_transformed, queued_message_id"
                    + " FROM exchange_subscriber_transform_audit"
                    + " WHERE exchange_id = ?"
                    + " ORDER BY started";
            ps = connection.prepareStatement(sql);

            ps.setString(1, exchangeId.toString());

            List<ExchangeSubscriberTransformAudit> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                ExchangeSubscriberTransformAudit a = readSubscriberTransformAuditFromResultSet(rs);
                ret.add(a);
            }

            return ret;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    private static ExchangeSubscriberTransformAudit readSubscriberTransformAuditFromResultSet(ResultSet rs) throws Exception {
        int col = 1;

        ExchangeSubscriberTransformAudit a = new ExchangeSubscriberTransformAudit();
        a.setExchangeId(UUID.fromString(rs.getString(col++)));
        a.setExchangeBatchId(UUID.fromString(rs.getString(col++)));
        a.setSubscriberConfigName(rs.getString(col++));
        java.sql.Timestamp ts = rs.getTimestamp(col++);
        if (!rs.wasNull()) {
            a.setStarted(new java.util.Date(ts.getTime()));
        }
        ts = rs.getTimestamp(col++);
        if (!rs.wasNull()) {
            a.setEnded(new java.util.Date(ts.getTime()));
        }
        a.setErrorXml(rs.getString(col++));
        int countResources = rs.getInt(col++);
        if (!rs.wasNull()) {
            a.setNumberResourcesTransformed(new Integer(countResources));
        }
        String queuedMessageId = rs.getString(col++);
        if (!rs.wasNull()) {
            a.setQueuedMessageId(UUID.fromString(queuedMessageId));
        }

        return a;
    }


    @Override
    public List<ExchangeSubscriberTransformAudit> getSubscriberTransformAudits(UUID exchangeId, UUID exchangeBatchId) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT exchange_id, exchange_batch_id, subscriber_config_name, started, ended, error_xml, number_resources_transformed, queued_message_id"
                    + " FROM exchange_subscriber_transform_audit"
                    + " WHERE exchange_id = ?"
                    + " AND exchange_batch_id = ?"
                    + " ORDER BY started";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, exchangeId.toString());
            ps.setString(col++, exchangeBatchId.toString());

            List<ExchangeSubscriberTransformAudit> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                ExchangeSubscriberTransformAudit a = readSubscriberTransformAuditFromResultSet(rs);
                ret.add(a);
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
    public void save(ExchangeSubscriberSendAudit audit) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "INSERT INTO exchange_subscriber_send_audit"
                    + " (exchange_id, exchange_batch_id, subscriber_config_name, inserted_at, error_xml, queued_message_id)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            int col = 1;
            ps.setString(col++, audit.getExchangeId().toString());
            ps.setString(col++, audit.getExchangeBatchId().toString());
            ps.setString(col++, audit.getSubscriberConfigName());
            ps.setTimestamp(col++, new java.sql.Timestamp(audit.getInsertedAt().getTime()));

            if (audit.getError() == null) {
                ps.setNull(col++, Types.VARCHAR);
            } else {
                String xml = TransformErrorSerializer.writeToXml(audit.getError());
                ps.setString(col++, xml);
            }

            ps.setString(col++, audit.getQueuedMessageId().toString());

            ps.executeUpdate();

            entityManager.getTransaction().commit();

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public List<ExchangeSubscriberSendAudit> getSubscriberSendAudits(UUID exchangeId, UUID batchId, String subscriberConfigName) throws Exception {
        EntityManager entityManager = ConnectionManager.getAuditEntityManager();
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "SELECT inserted_at, error_xml, queued_message_id"
                    + " FROM exchange_subscriber_send_audit"
                    + " WHERE exchange_id = ?"
                    + " AND exchange_batch_id = ?"
                    + " AND subscriber_config_name = ?"
                    + " ORDER BY inserted_at ASC";

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, exchangeId.toString());
            ps.setString(col++, batchId.toString());
            ps.setString(col++, subscriberConfigName);

            List<ExchangeSubscriberSendAudit> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                Date d = new Date(rs.getTimestamp(col++).getTime());
                String errorXml = rs.getString(col++);
                UUID queuedMessageId = UUID.fromString(rs.getString(col++));

                ExchangeSubscriberSendAudit audit = new ExchangeSubscriberSendAudit();
                audit.setExchangeId(exchangeId);
                audit.setExchangeBatchId(batchId);
                audit.setSubscriberConfigName(subscriberConfigName);
                audit.setInsertedAt(d);
                audit.setQueuedMessageId(queuedMessageId);

                if (!Strings.isNullOrEmpty(errorXml)) {
                    TransformError error = TransformErrorSerializer.readFromXml(errorXml);
                    audit.setError(error);
                }

                ret.add(audit);
            }

            return ret;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public List<JsonDDSOrganisationStatus> getOrganisationStatus(List<String> odsCodes, String agreementName) throws Exception {

        List<JsonDDSOrganisationStatus> orgStatusList = new ArrayList<>();
        Set<String> foundOdsCodes = new HashSet<>();

        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT s.local_id, MAX(d.received_date),"
                    + " IF (err.exchange_ids_in_error IS NULL, 0, 1)"
                    + " FROM admin.service s"
                    + " INNER JOIN audit.latest_data_received d"
                    + " ON d.service_id = s.id"
                    + " LEFT OUTER JOIN audit.exchange_transform_error_state err"
                    + " ON err.service_id = s.id"
                    + " WHERE s.local_id IN (";
            for (int i=0; i<odsCodes.size(); i++) {
                if (i>0) {
                    sql += ", ";
                }
                sql += "?";
            }
            sql += ")"
                    + " GROUP BY s.local_id"
                    + " ORDER BY MAX(d.received_date) ASC"; //want most recent at the top
            ps = connection.prepareStatement(sql);

            int col = 1;
            for (String odsCode: odsCodes) {
                ps.setString(col++, odsCode);
            }

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                col = 1;

                String odsCode = rs.getString(col++);
                java.util.Date d = new java.util.Date(rs.getTimestamp(col++).getTime());
                int err = rs.getInt(col++);

                JsonDDSOrganisationStatus orgStatus = new JsonDDSOrganisationStatus();
                orgStatus.setOdsCode(odsCode);
                orgStatus.setLastReceived(df.format(d));
                orgStatus.setInError(err == 1);
                orgStatus.setReferenceAgreement(agreementName);
                orgStatusList.add(orgStatus);

                foundOdsCodes.add(odsCode);
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

        //add any entries for ODS codes that weren't found (e.g. not set up in DDS-UI yet)
        for (String odsCode: odsCodes) {
            if (!foundOdsCodes.contains(odsCode)) {
                JsonDDSOrganisationStatus orgStatus = new JsonDDSOrganisationStatus();
                orgStatus.setOdsCode(odsCode);
                orgStatus.setInError(false);
                orgStatus.setReferenceAgreement(agreementName);
                orgStatusList.add(orgStatus);
            }
        }

        //populate the org names, CCGs etc.
        fillOrganisationDetails(orgStatusList, odsCodes);
        getCCG(orgStatusList, odsCodes);

        return orgStatusList;
    }

    /*public List<JsonDDSOrganisationStatus> getOrganisationStatus(List<String> odsCodes, String agreementName) throws Exception {

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        try {
            Query query = entityManager.createQuery(
                    "select " +
                            "s.localId," +
                            "max(x.timestamp)," +
                            "case " +
                            "   when err.exchangeIdsInError is null then 0 " +
                            "   else 1 " +
                            "end" +
                            " from RdbmsService s " +
                            "inner join RdbmsExchange x on x.serviceId = s.id " +
                            "left outer join RdbmsExchangeTransformErrorState err on err.serviceId = s.id " +
                            "where s.localId IN (:odsList) " +
                            "group by s.localId " +
                            "order by x.timestamp desc");
            query.setParameter("odsList", odsCodes);

            List<Object[]> result = query.getResultList();

            return processOrganisationStatus(result, odsCodes, agreementName);
        } finally {
            entityManager.close();
        }
    }

    private List<JsonDDSOrganisationStatus> processOrganisationStatus(List<Object[]> resultList, List<String> odsCodes, String agreementName) throws Exception {
        List<JsonDDSOrganisationStatus> orgStatusList = new ArrayList<>();

        for (Object[] row : resultList) {
            JsonDDSOrganisationStatus orgStatus = new JsonDDSOrganisationStatus();
            orgStatus.setOdsCode((String)row[0]);

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            orgStatus.setLastReceived(df.format((Date)row[1]));
            orgStatus.setInError((Integer)row[2] == 1 ? true : false);
            orgStatus.setReferenceAgreement(agreementName);
            orgStatusList.add(orgStatus);
        }

        List<String> missingOrgs = new ArrayList<>();

        Set<String> foundOdsSet =
                orgStatusList.stream()
                        .map(JsonDDSOrganisationStatus::getOdsCode)
                        .collect(Collectors.toSet());

        missingOrgs = odsCodes.stream().filter(org -> !foundOdsSet.contains(org)).collect(Collectors.toList());

        if (!missingOrgs.isEmpty()) {
            for (String ods: missingOrgs) {
                JsonDDSOrganisationStatus orgStatus = new JsonDDSOrganisationStatus();
                orgStatus.setOdsCode(ods);
                orgStatus.setInError(false);
                orgStatus.setReferenceAgreement(agreementName);
                orgStatusList.add(orgStatus);
            }
        }

        fillOrganisationDetails(orgStatusList, odsCodes);
        getCCG(orgStatusList, odsCodes);
        return orgStatusList;
    }*/

    private void fillOrganisationDetails(List<JsonDDSOrganisationStatus> orgs, List<String> odsCodes) throws Exception {
        List<OrganisationEntity> organisationEntities = OrganisationCache.getOrganisationDetailsFromOdsCodeList(odsCodes);

        if (organisationEntities.isEmpty()) {
            return;
        }

        for (JsonDDSOrganisationStatus org : orgs) {
            OrganisationEntity organisationEntity
                    = organisationEntities.stream().filter(orgEnt -> orgEnt.getOdsCode().equals(org.getOdsCode())).findFirst().orElse(null);

            if (organisationEntity != null) {
                org.setPracticeName(organisationEntity.getName());
                org.setOrgUUID(organisationEntity.getUuid());
                org.setSystemSupplierType(
                        organisationEntity.getSystemSupplierSystemId() != null ? organisationEntity.getSystemSupplierSystemId() : 0);
                org.setSystemSupplierReference(organisationEntity.getSystemSupplierReference());
                org.setSharingActivated(organisationEntity.getSystemSupplierSharingActivated());
            }
        }
    }

    private void getCCG(List<JsonDDSOrganisationStatus> orgs, List<String> odsCodes) throws Exception {

        List<JsonOrganisationCCG> organisationCCGs = OrganisationCache.getCCGForOrganisationList(odsCodes);

        if (organisationCCGs.isEmpty()) {
            return;
        }

        for (JsonDDSOrganisationStatus org : orgs) {
            JsonOrganisationCCG orgCCG
                    = organisationCCGs.stream().filter(orgEnt -> orgEnt.getOdsCode().equals(org.getOdsCode())).findFirst().orElse(null);

            if (orgCCG != null) {
                org.setCcg(orgCCG.getCcgName());
            }
        }

    }

}
