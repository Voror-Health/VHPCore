package org.endeavourhealth.core.database.rdbms.audit;

import org.endeavourhealth.core.database.dal.audit.LastDataDalI;
import org.endeavourhealth.core.database.dal.audit.models.LastDataInSubscriber;
import org.endeavourhealth.core.database.dal.audit.models.LastDataProcessed;
import org.endeavourhealth.core.database.dal.audit.models.LastDataReceived;
import org.endeavourhealth.core.database.dal.audit.models.LastDataToSubscriber;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RdbmsLastDataDal implements LastDataDalI {


    @Override
    public void save(LastDataReceived dataReceived) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {

            //below syntax does an insert, and if the insert fails, does an upsert but only if
            //the new data_date is newer than what's on the database. Does in one transaction what would otherwise be several.
            String sql = "INSERT INTO latest_data_received"
                    + " (service_id, system_id, received_date, exchange_id, extract_date, extract_cutoff)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " received_date = IF (VALUES(extract_date) > extract_date, VALUES(received_date), received_date),"
                    + " exchange_id = IF (VALUES(extract_date) > extract_date, VALUES(exchange_id), exchange_id),"
                    + " extract_cutoff = IF (VALUES(extract_date) > extract_date, VALUES(extract_cutoff), extract_cutoff),"
                    + " extract_date = IF (VALUES(extract_date) > extract_date, VALUES(extract_date), extract_date)"; //the field being compared MUST be the last one updated;

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, dataReceived.getServiceId().toString());
            ps.setString(col++, dataReceived.getSystemId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataReceived.getReceivedDate().getTime()));
            ps.setString(col++, dataReceived.getExchangeId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataReceived.getExtractDate().getTime()));
            ps.setTimestamp(col++, new java.sql.Timestamp(dataReceived.getExtractCutoff().getTime()));

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
    public List<LastDataReceived> getLastDataReceived() throws Exception {
        return getLastDataReceivedImpl(null);
    }

    @Override
    public List<LastDataReceived> getLastDataReceived(UUID serviceId) throws Exception {
        return getLastDataReceivedImpl(serviceId);
    }

    private List<LastDataReceived> getLastDataReceivedImpl(UUID serviceId) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT service_id, system_id, received_date, exchange_id, extract_date, extract_cutoff"
                    + " FROM latest_data_received";
            if (serviceId != null) {
                sql += " WHERE service_id = ?";
            }

            ps = connection.prepareStatement(sql);
            if (serviceId != null) {
                ps.setString(1, serviceId.toString());
            }

            List<LastDataReceived> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int col = 1;
                LastDataReceived r = new LastDataReceived();
                r.setServiceId(UUID.fromString(rs.getString(col++)));
                r.setSystemId(UUID.fromString(rs.getString(col++)));
                r.setReceivedDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                r.setExchangeId(UUID.fromString(rs.getString(col++)));
                r.setExtractDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                r.setExtractCutoff(new java.util.Date(rs.getTimestamp(col++).getTime()));
                ret.add(r);
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
    public void save(LastDataProcessed dataProcessed) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            //below syntax does an insert, and if the insert fails, does an upsert but only if
            //the new data_date is newer than what's on the database. Does in one transaction what would otherwise be several.
            String sql = "INSERT INTO latest_data_processed"
                    + " (service_id, system_id, processed_date, exchange_id, extract_date, extract_cutoff)"
                    + " VALUES (?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " processed_date = IF (VALUES(extract_date) > extract_date, VALUES(processed_date), processed_date),"
                    + " exchange_id = IF (VALUES(extract_date) > extract_date, VALUES(exchange_id), exchange_id),"
                    + " extract_cutoff = IF (VALUES(extract_date) > extract_date, VALUES(extract_cutoff), extract_cutoff),"
                    + " extract_date = IF (VALUES(extract_date) > extract_date, VALUES(extract_date), extract_date)"; //the field being compared MUST be the last one updated

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, dataProcessed.getServiceId().toString());
            ps.setString(col++, dataProcessed.getSystemId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataProcessed.getProcessedDate().getTime()));
            ps.setString(col++, dataProcessed.getExchangeId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataProcessed.getExtractDate().getTime()));
            ps.setTimestamp(col++, new java.sql.Timestamp(dataProcessed.getExtractCutoff().getTime()));

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
    public List<LastDataProcessed> getLastDataProcessed() throws Exception {
        return getLastDataProcessedImpl(null);
    }

    @Override
    public List<LastDataProcessed> getLastDataProcessed(UUID serviceId) throws Exception {
        return getLastDataProcessedImpl(serviceId);
    }

    private List<LastDataProcessed> getLastDataProcessedImpl(UUID serviceId) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT service_id, system_id, processed_date, exchange_id, extract_date, extract_cutoff"
                    + " FROM latest_data_processed";
            if (serviceId != null) {
                sql += " WHERE service_id = ?";
            }

            ps = connection.prepareStatement(sql);
            if (serviceId != null) {
                ps.setString(1, serviceId.toString());
            }

            List<LastDataProcessed> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int col = 1;
                LastDataProcessed r = new LastDataProcessed();
                r.setServiceId(UUID.fromString(rs.getString(col++)));
                r.setSystemId(UUID.fromString(rs.getString(col++)));
                r.setProcessedDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                r.setExchangeId(UUID.fromString(rs.getString(col++)));
                r.setExtractDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                r.setExtractCutoff(new java.util.Date(rs.getTimestamp(col++).getTime()));
                ret.add(r);
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
    public void save(LastDataToSubscriber dataSent) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {

            //below syntax does an insert, and if the insert fails, does an upsert but only if
            //the new data_date is newer than what's on the database. Does in one transaction what would otherwise be several.
            String sql = "INSERT INTO latest_data_to_subscriber"
                    + " (subscriber_config_name, service_id, system_id, sent_date, exchange_id, extract_date, extract_cutoff)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " sent_date = IF (VALUES(extract_date) > extract_date, VALUES(sent_date), sent_date),"
                    + " exchange_id = IF (VALUES(extract_date) > extract_date, VALUES(exchange_id), exchange_id),"
                    + " extract_cutoff = IF (VALUES(extract_date) > extract_date, VALUES(extract_cutoff), extract_cutoff),"
                    + " extract_date = IF (VALUES(extract_date) > extract_date, VALUES(extract_date), extract_date)"; //the field being compared MUST be the last one updated;
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, dataSent.getSubscriberConfigName());
            ps.setString(col++, dataSent.getServiceId().toString());
            ps.setString(col++, dataSent.getSystemId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getSentDate().getTime()));
            ps.setString(col++, dataSent.getExchangeId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getExtractDate().getTime()));
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getExtractCutoff().getTime()));

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
    public List<LastDataToSubscriber> getLastDataToSubscriber() throws Exception {
        return getLastDataToSubscriberImpl(null, null);
    }

    @Override
    public List<LastDataToSubscriber> getLastDataToSubscriber(UUID serviceId) throws Exception {
        return getLastDataToSubscriberImpl(serviceId, null);
    }

    @Override
    public List<LastDataToSubscriber> getLastDataToSubscriber(String subscriberConfigName) throws Exception {
        return getLastDataToSubscriberImpl(null, subscriberConfigName);
    }

    @Override
    public void save(LastDataInSubscriber dataSent) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {

            //below syntax does an insert, and if the insert fails, does an upsert but only if
            //the new data_date is newer than what's on the database. Does in one transaction what would otherwise be several.
            String sql = "INSERT INTO latest_data_in_subscriber"
                    + " (subscriber_config_name, service_id, system_id, written_date, exchange_id, extract_date, extract_cutoff)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE"
                    + " written_date = IF (VALUES(extract_date) > extract_date, VALUES(written_date), written_date),"
                    + " exchange_id = IF (VALUES(extract_date) > extract_date, VALUES(exchange_id), exchange_id),"
                    + " extract_cutoff = IF (VALUES(extract_date) > extract_date, VALUES(extract_cutoff), extract_cutoff),"
                    + " extract_date = IF (VALUES(extract_date) > extract_date, VALUES(extract_date), extract_date)"; //the field being compared MUST be the last one updated;
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, dataSent.getSubscriberConfigName());
            ps.setString(col++, dataSent.getServiceId().toString());
            ps.setString(col++, dataSent.getSystemId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getWrittenDate().getTime()));
            ps.setString(col++, dataSent.getExchangeId().toString());
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getExtractDate().getTime()));
            ps.setTimestamp(col++, new java.sql.Timestamp(dataSent.getExtractCutoff().getTime()));

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
    public List<LastDataInSubscriber> getLastDataInSubscriber() throws Exception {
        return getLastDataInSubscriberImpl(null, null);
    }

    @Override
    public List<LastDataInSubscriber> getLastDataInSubscriber(UUID serviceId) throws Exception {
        return getLastDataInSubscriberImpl(serviceId, null);
    }

    @Override
    public List<LastDataInSubscriber> getLastDataInSubscriber(String subscriberConfigName) throws Exception {
        return getLastDataInSubscriberImpl(null, subscriberConfigName);
    }

    private List<LastDataInSubscriber> getLastDataInSubscriberImpl(UUID serviceId, String subscriberConfigName) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT subscriber_config_name, service_id, system_id, written_date, exchange_id, extract_date, extract_cutoff"
                    + " FROM latest_data_in_subscriber";

            if (serviceId != null || subscriberConfigName != null) {
                sql += " WHERE";

                if (serviceId != null) {
                    sql += " service_id = ?";
                    if (subscriberConfigName != null) {
                        sql += " AND";
                    }
                }

                if (subscriberConfigName != null) {
                    sql += " subscriber_config_name = ?";
                }
            }
            ps = connection.prepareStatement(sql);

            int col = 1;

            if (serviceId != null) {
                ps.setString(col++, serviceId.toString());
            }

            if (subscriberConfigName != null) {
                ps.setString(col++, subscriberConfigName);
            }

            List<LastDataInSubscriber> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;

                LastDataInSubscriber d = new LastDataInSubscriber();
                d.setSubscriberConfigName(rs.getString(col++));
                d.setServiceId(UUID.fromString(rs.getString(col++)));
                d.setSystemId(UUID.fromString(rs.getString(col++)));
                d.setWrittenDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setExchangeId(UUID.fromString(rs.getString(col++)));
                d.setExtractDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setExtractCutoff(new java.util.Date(rs.getTimestamp(col++).getTime()));

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

    private static List<LastDataToSubscriber> getLastDataToSubscriberImpl(UUID serviceId, String subscriberConfigName) throws Exception {
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT subscriber_config_name, service_id, system_id, sent_date, exchange_id, extract_date, extract_cutoff"
                    + " FROM latest_data_to_subscriber";

            if (serviceId != null || subscriberConfigName != null) {
                sql += " WHERE";

                if (serviceId != null) {
                    sql += " service_id = ?";
                    if (subscriberConfigName != null) {
                        sql += " AND";
                    }
                }

                if (subscriberConfigName != null) {
                    sql += " subscriber_config_name = ?";
                }
            }
            ps = connection.prepareStatement(sql);

            int col = 1;

            if (serviceId != null) {
                ps.setString(col++, serviceId.toString());
            }

            if (subscriberConfigName != null) {
                ps.setString(col++, subscriberConfigName);
            }

            List<LastDataToSubscriber> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;

                LastDataToSubscriber d = new LastDataToSubscriber();
                d.setSubscriberConfigName(rs.getString(col++));
                d.setServiceId(UUID.fromString(rs.getString(col++)));
                d.setSystemId(UUID.fromString(rs.getString(col++)));
                d.setSentDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setExchangeId(UUID.fromString(rs.getString(col++)));
                d.setExtractDate(new java.util.Date(rs.getTimestamp(col++).getTime()));
                d.setExtractCutoff(new java.util.Date(rs.getTimestamp(col++).getTime()));

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
}
