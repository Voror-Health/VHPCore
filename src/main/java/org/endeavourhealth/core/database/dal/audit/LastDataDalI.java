package org.endeavourhealth.core.database.dal.audit;

import org.endeavourhealth.core.database.dal.audit.models.LastDataInSubscriber;
import org.endeavourhealth.core.database.dal.audit.models.LastDataProcessed;
import org.endeavourhealth.core.database.dal.audit.models.LastDataReceived;
import org.endeavourhealth.core.database.dal.audit.models.LastDataToSubscriber;

import java.util.List;
import java.util.UUID;

public interface LastDataDalI {


    void save(LastDataReceived dataReceived) throws Exception;
    List<LastDataReceived> getLastDataReceived() throws Exception;
    List<LastDataReceived> getLastDataReceived(UUID serviceId) throws Exception;

    void save(LastDataProcessed dataProcessed) throws Exception;
    List<LastDataProcessed> getLastDataProcessed() throws Exception;
    List<LastDataProcessed> getLastDataProcessed(UUID serviceId) throws Exception;

    void save(LastDataToSubscriber dataSent) throws Exception;
    List<LastDataToSubscriber> getLastDataToSubscriber() throws Exception;
    List<LastDataToSubscriber> getLastDataToSubscriber(UUID serviceId) throws Exception;
    List<LastDataToSubscriber> getLastDataToSubscriber(String subscriberConfigName) throws Exception;

    void save(LastDataInSubscriber dataSent) throws Exception;
    List<LastDataInSubscriber> getLastDataInSubscriber() throws Exception;
    List<LastDataInSubscriber> getLastDataInSubscriber(UUID serviceId) throws Exception;
    List<LastDataInSubscriber> getLastDataInSubscriber(String subscriberConfigName) throws Exception;
}
