package org.endeavourhealth.core.database.dal.ehr;

import org.endeavourhealth.core.database.dal.ehr.models.ResourceWrapper;
import org.endeavourhealth.core.database.rdbms.ehr.models.AdminResourceRetrieverI;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ResourceDalI {

    void save(List<ResourceWrapper> wrappers) throws Exception;
    void delete(List<ResourceWrapper> wrappers) throws Exception;

    void save(ResourceWrapper resourceEntry) throws Exception;
    void delete(ResourceWrapper resourceEntry) throws Exception;
    void hardDeleteResourceAndAllHistory(ResourceWrapper resourceEntry) throws Exception;

    Resource getCurrentVersionAsResource(UUID serviceId, ResourceType resourceType, String resourceIdStr) throws Exception;
    ResourceWrapper getCurrentVersion(UUID serviceId, String resourceType, UUID resourceId) throws Exception;
    Map<String, ResourceWrapper> getCurrentVersionForReferences(UUID serviceId, List<String> references) throws Exception;

    List<ResourceWrapper> getAllAdminResourcesByType(String service_id, ResourceType resourceType, boolean returnDeletedResourceCurrent) throws Exception;

    List<ResourceWrapper> getResourceHistory(UUID serviceId, String resourceType, UUID resourceId) throws Exception;
    List<ResourceWrapper> getResourcesByPatient(UUID serviceId, UUID patientId) throws Exception;
    List<ResourceWrapper> getResourcesByPatient(UUID serviceId, UUID patientId, String resourceType) throws Exception;
    List<ResourceWrapper> getResourcesByPatientIncludingDeleted(UUID serviceId, UUID patientId) throws Exception;

    List<ResourceWrapper> getCurrentVersionOfResourcesForBatch(UUID serviceId, UUID batchId) throws Exception;
    Long getResourceChecksum(UUID serviceId, String resourceType, UUID resourceId) throws Exception;
    Map<String, Long> getResourceChecksumsForReferences(UUID serviceId, List<String> references) throws Exception;
    boolean dataExists(UUID serviceId) throws Exception;

    //breaking the pattern of having fairly generic functions, this has been added to speed up the Emis transform
    //which needs to retrieve all MedicationOrders for a specific MedicationStatement, which is currently very slow
    //because it has to retrieve all MedicationOrders and then filter down
    List<ResourceWrapper> getMedicationOrderResourcesForPatientAndMedicationStatement(UUID serviceId, UUID patientId, UUID medicationStatement) throws Exception;

    AdminResourceRetrieverI startRetrievingAdminResources(UUID serviceId, int batchSize) throws Exception;
}
