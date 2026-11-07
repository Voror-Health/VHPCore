package org.endeavourhealth.core.database.dal.datagenerator;

import org.endeavourhealth.core.database.dal.audit.models.LastDataInSubscriber;
import org.endeavourhealth.core.database.dal.datagenerator.models.RemoteFilingRecord;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface SubscriberZipFileUUIDsDalI {

    //saves a message to the staging table for sending out to a subscriber
    void createSubscriberZipFileUUIDsEntity(int subscriberId, UUID serviceId, UUID systemId, UUID exchangeId, UUID batchId, UUID queuedMessageId, String queuedMessageBody) throws Exception;

    //logs to the data_generator database the details of the last batch queued up for sending to a subscriber
    void logLastMessageStaged(int subscriberId, UUID queuedMessageId, LastDataInSubscriber audit) throws Exception;
    List<LastDataInSubscriber> getLastMessagesWritten(int subscriberId) throws Exception;
    void deleteLastMessagesWritten(int subscriberId, Date maxWrittenDate) throws Exception;

    //retrieves errors from the data_generator database
    List<RemoteFilingRecord> getFailures(int subscriberId, int maxRecordsPerService) throws Exception;




    /*RdbmsSubscriberZipFileUUIDs getSubscriberZipFileUUIDsEntity(String queuedMessageUUID) throws Exception;
    List<RdbmsSubscriberZipFileUUIDs> getPagedSubscriberZipFileUUIDsEntities(Integer pageNumber, Integer pageSize) throws Exception;
    List<RdbmsSubscriberZipFileUUIDs> getPagedSubscriberZipFileUUIDsEntities(Integer subscriberId, Integer pageNumber, Integer pageSize) throws Exception;
    Long getTotalNumberOfSubscriberFiles() throws Exception;
    Long getTotalNumberOfSubscriberFiles(Integer subscriberId) throws Exception;
    List<RemoteFilingStatistics> getStatistics(String timeFrame) throws Exception;
    List<RemoteFilingStatistics> getSubscriberStatistics(Integer subscriberId, String timeFrame) throws Exception;
    List<RemoteFilingSubscriber> getSubscribers() throws Exception;
    List<RdbmsSubscriberZipFileUUIDs> getFailedFilingUUIDEntities(String timeFrame, Integer subscriberId) throws Exception;*/


}
