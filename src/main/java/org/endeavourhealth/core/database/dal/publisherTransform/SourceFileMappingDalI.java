package org.endeavourhealth.core.database.dal.publisherTransform;

import org.endeavourhealth.core.database.dal.ehr.models.ResourceWrapper;
import org.endeavourhealth.core.database.dal.publisherTransform.models.ResourceFieldMapping;
import org.endeavourhealth.core.database.dal.publisherTransform.models.ResourceFieldMappingAudit;
import org.hl7.fhir.instance.model.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SourceFileMappingDalI {


    void saveResourceMappings(Map<ResourceWrapper, ResourceFieldMappingAudit> audits) throws Exception;
    List<ResourceFieldMapping> findFieldMappings(UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception;
    List<ResourceFieldMapping> findFieldMappingsForField(UUID serviceId, ResourceType resourceType, UUID resourceId, String field) throws Exception;

    //returns all audits, without trying to find only the latest for each field etc.
    Map<UUID, ResourceFieldMappingAudit> findAllMappingsForResource(UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception;

    String getPermanentStoragePath() throws Exception;
}
