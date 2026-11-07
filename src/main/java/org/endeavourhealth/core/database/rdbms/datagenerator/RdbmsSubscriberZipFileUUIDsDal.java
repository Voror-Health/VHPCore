package org.endeavourhealth.core.database.rdbms.datagenerator;

import org.endeavourhealth.core.database.dal.audit.models.LastDataInSubscriber;
import org.endeavourhealth.core.database.dal.datagenerator.SubscriberZipFileUUIDsDalI;
import org.endeavourhealth.core.database.dal.datagenerator.models.RemoteFilingRecord;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RdbmsSubscriberZipFileUUIDsDal implements SubscriberZipFileUUIDsDalI {


    @Override
    public synchronized void createSubscriberZipFileUUIDsEntity(int subscriberId, UUID serviceId, UUID systemId, UUID exchangeId, UUID batchId,
                                                                UUID queuedMessageId, String queuedMessageBody) throws Exception {

        long maxFilingOrder = findMaxFilingOrder();

        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {
            String sql = "INSERT INTO subscriber_zip_file_uuids"
                    + " (subscriber_id, batch_uuid, queued_message_uuid, queued_message_body, filing_order, service_id, system_id, exchange_id, dt_inserted)"
                    + " VALUES"
                    + " (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setInt(col++, subscriberId);
            ps.setString(col++, batchId.toString());
            ps.setString(col++, queuedMessageId.toString());
            ps.setString(col++, queuedMessageBody);
            ps.setLong(col++, maxFilingOrder+1);
            if (serviceId != null) {
                ps.setString(col++, serviceId.toString());
            } else {
                ps.setNull(col++, Types.VARCHAR);
            }
            if (systemId != null) {
                ps.setString(col++, systemId.toString());
            } else {
                ps.setNull(col++, Types.VARCHAR);
            }
            if (exchangeId != null) {
                ps.setString(col++, exchangeId.toString());
            } else {
                ps.setNull(col++, Types.VARCHAR);
            }
            ps.setTimestamp(col++, new java.sql.Timestamp(new Date().getTime()));

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

    private long findMaxFilingOrder() throws Exception {

        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT MAX(filing_order) FROM subscriber_zip_file_uuids";
            ps = connection.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else {
                return 0;
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    /**
     * after writing to the subscriber_zip_file_uuids table with new outbound data, this table may be written to
     * to log that it's the last message in the set of messages derived from an inbound exchange
     */
    @Override
    public void logLastMessageStaged(int subscriberId, UUID queuedMessageId, LastDataInSubscriber audit) throws Exception {
        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {

            String sql = "INSERT IGNORE INTO last_queued_message_sent"
                    + " (subscriber_id, queued_message_id, service_id, system_id, exchange_id, extract_date, extract_cutoff)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setInt(col++, subscriberId);
            ps.setString(col++, queuedMessageId.toString());
            ps.setString(col++, audit.getServiceId().toString());
            ps.setString(col++, audit.getSystemId().toString());
            ps.setString(col++, audit.getExchangeId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(audit.getExtractDate().getTime()));
            ps.setTimestamp(col++, new java.sql.Timestamp(audit.getExtractCutoff().getTime()));

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
    public List<LastDataInSubscriber> getLastMessagesWritten(int subscriberId) throws Exception {

        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {

            String sql = "SELECT service_id, system_id, exchange_id, extract_date, extract_cutoff, written_date"
                    + " FROM last_queued_message_sent"
                    + " WHERE subscriber_id = ?"
                    + " AND written_date IS NOT NULL";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setInt(col++, subscriberId);

            List<LastDataInSubscriber> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                col = 1;
                LastDataInSubscriber d = new LastDataInSubscriber();
                d.setServiceId(UUID.fromString(rs.getString(col++)));
                d.setSystemId(UUID.fromString(rs.getString(col++)));
                d.setExchangeId(UUID.fromString(rs.getString(col++)));
                d.setExtractDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setExtractCutoff(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setWrittenDate(new java.util.Date(rs.getTimestamp(col++).getTime()));

                ret.add(d);
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
    public void deleteLastMessagesWritten(int subscriberId, Date maxWrittenDate) throws Exception {
        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {

            //note, the below explicitly only delete ones OLDER than the date, and skip ones equal. If the feedback processing is happening
            //when this function is being run, then we don't want to delete anything too soon. There's no harm in leaving
            //extra records in the last_queued_message_sent table for another day or so.
            String sql = "DELETE FROM last_queued_message_sent"
                    + " WHERE subscriber_id = ?"
                    + " AND written_date IS NOT NULL"
                    + " AND written_date < ?";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setInt(col++, subscriberId);
            ps.setTimestamp(col++, new java.sql.Timestamp(maxWrittenDate.getTime()));

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
    public List<RemoteFilingRecord> getFailures(int subscriberId, int maxRecordsPerService) throws Exception {

        Connection connection = ConnectionManager.getDataGeneratorConnection();
        PreparedStatement ps = null;
        try {
            //we need to find out the distinct services in error first
            String sql = "SELECT DISTINCT service_id"
                    + " FROM subscriber_zip_file_uuids"
                    + " WHERE subscriber_id = ?"
                    + " AND file_filing_success = 0";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setInt(col++, subscriberId);

            List<UUID> serviceIds = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                serviceIds.add(ConnectionManager.stringToUuid(rs.getString(col++)));
            }

            //we're done with this statement
            ps.close();
            ps = null;

            //for each service ID found, retrieve the errors (to up the max)
            List<RemoteFilingRecord> ret = new ArrayList<>();

            for (UUID serviceId: serviceIds) {

                //note, this SQL retrieves all columns EXCEPT the payload Base64
                sql = "SELECT subscriber_id, batch_uuid, queued_message_uuid, filing_order, file_sent, file_filing_attempted, "
                        + " filing_failure_message, service_id, system_id, exchange_id, dt_inserted"
                        + " FROM subscriber_zip_file_uuids"
                        + " WHERE subscriber_id = ?"
                        + " AND service_id = ?"
                        + " AND file_filing_success = 0"
                        + " ORDER BY dt_inserted DESC" //get newest ones
                        + " LIMIT ?";
                ps = connection.prepareStatement(sql);

                col = 1;
                ps.setInt(col++, subscriberId);
                ps.setString(col++, ConnectionManager.uuidToString(serviceId));
                ps.setInt(col++, maxRecordsPerService);

                rs = ps.executeQuery();
                while (rs.next()) {
                    col = 1;

                    RemoteFilingRecord r = new RemoteFilingRecord();
                    r.setSubscriberId(rs.getInt(col++));
                    r.setBatchId(ConnectionManager.stringToUuid(rs.getString(col++)));
                    r.setQueuedMessageId(ConnectionManager.stringToUuid(rs.getString(col++)));
                    r.setFilingOrder(rs.getLong(col++));
                    r.setDtFileSent(ConnectionManager.timestampToDate(rs.getTimestamp(col++)));
                    r.setDtFileFilingAttempted(ConnectionManager.timestampToDate(rs.getTimestamp(col++)));
                    r.setFileFilingSuccess(Boolean.FALSE);
                    r.setFilingFailureMessage(rs.getString(col++));
                    r.setServiceId(ConnectionManager.stringToUuid(rs.getString(col++)));
                    r.setSystemId(ConnectionManager.stringToUuid(rs.getString(col++)));
                    r.setExchangeId(ConnectionManager.stringToUuid(rs.getString(col++)));
                    r.setDtInserted(ConnectionManager.timestampToDate(rs.getTimestamp(col++)));
                    ret.add(r);
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

    /*public RdbmsSubscriberZipFileUUIDs getSubscriberZipFileUUIDsEntity(String queuedMessageUUID) throws Exception {

        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        try {
            RdbmsSubscriberZipFileUUIDs ret = entityManager.find(RdbmsSubscriberZipFileUUIDs.class, queuedMessageUUID);
            return ret;

        } catch (Exception ex){
            return null;

        } finally {
            entityManager.close();
        }

    }

    public List<RdbmsSubscriberZipFileUUIDs> getPagedSubscriberZipFileUUIDsEntities(Integer pageNumber, Integer pageSize) throws Exception {

        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<RdbmsSubscriberZipFileUUIDs> cq = cb.createQuery(RdbmsSubscriberZipFileUUIDs.class);
            Root<RdbmsSubscriberZipFileUUIDs> rootEntry = cq.from(RdbmsSubscriberZipFileUUIDs.class);
            CriteriaQuery<RdbmsSubscriberZipFileUUIDs> all = cq.select(rootEntry);
            cq.orderBy(cb.asc(rootEntry.get("filingOrder")));
            TypedQuery<RdbmsSubscriberZipFileUUIDs> allQuery = entityManager.createQuery(all);

            allQuery.setFirstResult((pageNumber - 1) * pageSize);
            allQuery.setMaxResults(pageSize);

            List<RdbmsSubscriberZipFileUUIDs> ret = allQuery.getResultList();
            return ret;

        } catch (Exception ex) {
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    public List<RdbmsSubscriberZipFileUUIDs> getPagedSubscriberZipFileUUIDsEntities(Integer subscriberId, Integer pageNumber, Integer pageSize) throws Exception {

        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<RdbmsSubscriberZipFileUUIDs> cq = cb.createQuery(RdbmsSubscriberZipFileUUIDs.class);
            Root<RdbmsSubscriberZipFileUUIDs> rootEntry = cq.from(RdbmsSubscriberZipFileUUIDs.class);
            CriteriaQuery<RdbmsSubscriberZipFileUUIDs> all = cq.select(rootEntry);
            cq.where(cb.equal(rootEntry.get("subscriberId"), subscriberId));
            cq.orderBy(cb.asc(rootEntry.get("filingOrder")));
            TypedQuery<RdbmsSubscriberZipFileUUIDs> allQuery = entityManager.createQuery(all);

            allQuery.setFirstResult((pageNumber - 1) * pageSize);
            allQuery.setMaxResults(pageSize);

            List<RdbmsSubscriberZipFileUUIDs> ret = allQuery.getResultList();
            return ret;

        } catch (Exception ex) {
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    public Long getTotalNumberOfSubscriberFiles() throws Exception {
        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<RdbmsSubscriberZipFileUUIDs> rootEntry = cq.from(RdbmsSubscriberZipFileUUIDs.class);

        cq.select((cb.countDistinct(rootEntry)));

        Long ret = entityManager.createQuery(cq).getSingleResult();

        entityManager.close();

        return ret;
    }

    public Long getTotalNumberOfSubscriberFiles(Integer subscriberId) throws Exception {
        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<RdbmsSubscriberZipFileUUIDs> rootEntry = cq.from(RdbmsSubscriberZipFileUUIDs.class);
        cq.where(cb.equal(rootEntry.get("subscriberId"), subscriberId));
        cq.select((cb.countDistinct(rootEntry)));

        Long ret = entityManager.createQuery(cq).getSingleResult();

        entityManager.close();

        return ret;
    }


    @Override
    public List<RemoteFilingSubscriber> getSubscribers() throws Exception {

        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();
            String sql = "select subscriber_id, subscriber_live, definition "
                    + "from subscriber_file_sender ";

            ps = connection.prepareStatement(sql);

            List<RemoteFilingSubscriber> subscribers = new ArrayList<>();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int col = 1;

                RemoteFilingSubscriber subscriber = new RemoteFilingSubscriber();
                subscriber.setId(rs.getInt(col++));
                subscriber.setIsLive(rs.getBoolean(col++));
                subscriber.setJsonDefinition(rs.getString(col++));

                subscribers.add(subscriber);
            }

            return subscribers;

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }

    @Override
    public List<RemoteFilingStatistics> getSubscriberStatistics(Integer subscriberId, String timeFrame) throws Exception {

        List<RemoteFilingStatistics> stats = new ArrayList<>();

        stats.add(getUnSentStats(timeFrame, subscriberId));
        stats.add(getSentStats(timeFrame, subscriberId));
        stats.add(getAwaitingProcessingStats(timeFrame, subscriberId));
        //stats.add(getSuccessfullyFiledStats(timeFrame, subscriberId));
        stats.add(getFailedFilingStats(timeFrame, subscriberId));

        return stats;
    }

    @Override
    public List<RemoteFilingStatistics> getStatistics(String timeFrame) throws Exception {
        List<RemoteFilingStatistics> stats = new ArrayList<>();
        stats.add(getUnSentStats(timeFrame, null));
        stats.add(getSentStats(timeFrame, null));
        stats.add(getAwaitingProcessingStats(timeFrame, null));
        //stats.add(getSuccessfullyFiledStats(timeFrame, null));
        stats.add(getFailedFilingStats(timeFrame, null));

        return stats;
    }

    private RemoteFilingStatistics getUnSentStats(String timeFrame, Integer subscriberId) throws Exception {
        String sql = "select 'File batches awaiting sending (total)',  count(s)"
                + " from"
                + " RdbmsSubscriberZipFileUUIDs s"
                + " where s.fileSent is null";
        if (subscriberId != null) {
            sql = sql +" and s.subscriberId = :subscriberId";
        }

        return executeSQL(sql, timeFrame, true, subscriberId);
    }

    private RemoteFilingStatistics getSentStats(String timeFrame, Integer subscriberId) throws Exception {
        String sql = "select 'Files batches sent (time frame)',  count(s)"
                + " from"
                + " RdbmsSubscriberZipFileUUIDs s"
                + " where s.fileSent >= :date";
        if (subscriberId != null) {
            sql = sql +" and s.subscriberId = :subscriberId ";
        }
        return executeSQL(sql, timeFrame, false, subscriberId);
    }

    private RemoteFilingStatistics getAwaitingProcessingStats(String timeFrame, Integer subscriberId) throws Exception {
        String sql = "select ' - Awaiting processing',  count(s)"
                + " from"
                + " RdbmsSubscriberZipFileUUIDs s"
                + " where s.fileSent >= :date"
                + " and s.fileFilingAttempted is null";
        if (subscriberId != null) {
            sql = sql +" and s.subscriberId = :subscriberId ";
        }

        return executeSQL(sql, timeFrame, false, subscriberId);
    }

    private RemoteFilingStatistics getSuccessfullyFiledStats(String timeFrame, Integer subscriberId) throws Exception {
        String sql = "select ' - Successfully filed',  count(s)"
                + " from"
                + " RdbmsSubscriberZipFileUUIDs s"
                + " where s.fileSent >= :date"
                + " and s.fileFilingSuccess = 1";
        if (subscriberId != null) {
            sql = sql +" and s.subscriberId = :subscriberId ";
        }

        return executeSQL(sql, timeFrame, false, subscriberId);
    }

    private RemoteFilingStatistics getFailedFilingStats(String timeFrame, Integer subscriberId) throws Exception {
        String sql = "select ' - Filing errors',  count(s)"
                + " from"
                + " RdbmsSubscriberZipFileUUIDs s"
                + " where s.fileSent >= :date"
                + " and s.fileFilingSuccess = 0";
        if (subscriberId != null) {
            sql = sql +" and s.subscriberId = :subscriberId ";
        }

        return executeSQL(sql, timeFrame, false, subscriberId);
    }

    private RemoteFilingStatistics executeSQL(String sql, String timeFrame, Boolean ignoreDate, Integer subscriberId) throws Exception {
        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        Calendar cal = Calendar.getInstance();
        if (timeFrame.equals("day")) {
            cal.add(Calendar.HOUR, -24);
        } else if (timeFrame.equals("month")) {
            cal.add(Calendar.MONTH, -1);
        } else {
            cal.add(Calendar.YEAR, -1);
        }
        Date date = cal.getTime();

        Query query = entityManager.createQuery(sql);

        if (!ignoreDate) {
            query.setParameter("date", date);
        }

        if (subscriberId != null) {
            query.setParameter("subscriberId", subscriberId);
        }

        try {
            List<Object[]> result = query.getResultList();
            RemoteFilingStatistics stats = new RemoteFilingStatistics();
            stats.setStatisticsText(result.get(0)[0].toString());
            stats.setStatisticsValue(result.get(0)[1].toString());
            stats.setSubscriberId(subscriberId);

            return stats;

        } catch (NoResultException ex) {
            return null;

        }
    }

    public List<RdbmsSubscriberZipFileUUIDs> getFailedFilingUUIDEntities(String timeFrame, Integer subscriberId) throws Exception {

        EntityManager entityManager = ConnectionManager.getDataGeneratorEntityManager();

        try {
            String sql = "select s"
                    + " from"
                    + " RdbmsSubscriberZipFileUUIDs s"
                    + " where s.fileSent >= :date"
                    + " and s.fileFilingSuccess = 0"
                    + " and s.subscriberId = :subscriberId "
                    + " order by s.filingOrder asc ";

            Calendar cal = Calendar.getInstance();
            if (timeFrame.equals("day")) {
                cal.add(Calendar.HOUR, -24);
            } else if (timeFrame.equals("month")) {
                cal.add(Calendar.MONTH, -1);
            } else {
                cal.add(Calendar.YEAR, -1);
            }
            Date date = cal.getTime();

            Query query = entityManager.createQuery(sql);
            query.setParameter("date", date);
            query.setParameter("subscriberId", subscriberId);

            List<RdbmsSubscriberZipFileUUIDs> ret = query.getResultList();
            return ret;

        } catch (Exception ex) {
            throw ex;

        } finally {
            entityManager.close();
        }
    }



*/

}