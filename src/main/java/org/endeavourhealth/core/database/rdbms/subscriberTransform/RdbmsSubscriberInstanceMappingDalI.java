package org.endeavourhealth.core.database.rdbms.subscriberTransform;

import com.google.common.base.Strings;
import org.endeavourhealth.common.fhir.ReferenceHelper;
import org.endeavourhealth.core.database.dal.subscriberTransform.SubscriberInstanceMappingDalI;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.endeavourhealth.core.database.rdbms.subscriberTransform.models.RdbmsEnterpriseInstanceMap;
import org.hl7.fhir.instance.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class RdbmsSubscriberInstanceMappingDalI implements SubscriberInstanceMappingDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsSubscriberInstanceMappingDalI.class);
    private static final String DUPLICATE_KEY_ERR = "Duplicate entry .* for key .*";    // 'PRIMARY'";  SD-1130
    private String subscriberConfigName = null;

    public RdbmsSubscriberInstanceMappingDalI(String subscriberConfigName) {
        this.subscriberConfigName = subscriberConfigName;
    }

    @Override
    public UUID findInstanceMappedId(ResourceType resourceType, UUID resourceId) throws Exception {
        EntityManager entityManager = ConnectionManager.getSubscriberTransformEntityManager(subscriberConfigName);

        try {
            //first see if we've got a mapping by our resource ID
            RdbmsEnterpriseInstanceMap mapping = findMappingByResourceId(entityManager, resourceType, resourceId);
            if (mapping != null) {
                return UUID.fromString(mapping.getResourceIdTo());

            } else {
                return null;
            }

        } finally {
            entityManager.close();
        }
    }

    @Override
    public UUID findOrCreateInstanceMappedId(ResourceType resourceType, UUID resourceId, String mappingValue) throws Exception {

        /*if (Strings.isNullOrEmpty(mappingValue)) {
            throw new IllegalArgumentException("Cannot create instance mapping without a mapping value");
        }*/
        DeadlockHandler handler = new DeadlockHandler();
        handler.setRetryDelayMillis(500);
        handler.addErrorMessageToHandler(DUPLICATE_KEY_ERR);
        while (true) {
            try {
                return tryFindOrCreateInstanceMappedIds(resourceType, resourceId, mappingValue);

            } catch (Exception ex) {
                handler.handleError(ex);
            }
        }

    }

    public UUID tryFindOrCreateInstanceMappedIds(ResourceType resourceType, UUID resourceId, String mappingValue) throws Exception {
        EntityManager entityManager = ConnectionManager.getSubscriberTransformEntityManager(subscriberConfigName);

        try {
            //first see if we've got a mapping by our resource ID
            RdbmsEnterpriseInstanceMap mapping = findMappingByResourceId(entityManager, resourceType, resourceId);

            //if this is the first time we've transformed this resource, then we won't have a mapping, so want to see if we should map it
            if (mapping == null) {

                mapping = new RdbmsEnterpriseInstanceMap();
                mapping.setResourceType(resourceType.toString());
                mapping.setResourceIdFrom(resourceId.toString());

                if (Strings.isNullOrEmpty(mappingValue)) {
                    //if we have a null mapping value (e.g. no ODS code for an org) then we can't accurately map
                    //to another instance of that org so just let it map to itself
                    mapping.setResourceIdTo(resourceId.toString());

                } else {
                    //if we have a mapping value (e.g. ODS code) then we can look to see if there's any other mappings
                    //with that same mapping value
                    mapping.setMappingValue(mappingValue);

                    RdbmsEnterpriseInstanceMap otherMapping = findMappingByMappedValue(entityManager, resourceType, mappingValue);
                    if (otherMapping == null) {
                        //if there's no other record with the same mapping value, then we create one for our resource mapping to itself
                        mapping.setResourceIdTo(resourceId.toString());

                    } else {
                        //if we have a mapping with the same mapping value, then we create one for our resource mapping to that other resource
                        String mappedResourceId = otherMapping.getResourceIdTo();
                        mapping.setResourceIdTo(mappedResourceId);
                    }
                }

                entityManager.getTransaction().begin();
                entityManager.persist(mapping);
                entityManager.getTransaction().commit();
            }

            return UUID.fromString(mapping.getResourceIdTo());

        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;

        } finally {
            entityManager.close();
        }
    }

    @Override
    public void takeOverInstanceMapping(ResourceType resourceType, UUID oldMappedResourceId, UUID newMappedResourceId) throws Exception {
        Connection connection = ConnectionManager.getSubscriberTransformConnection(subscriberConfigName);
        PreparedStatement psInstanceMap = null;
        PreparedStatement psEnterpriseIdMap = null;
        PreparedStatement psEnterpriseIdMap3 = null;
        PreparedStatement psSubscriberIdMap = null;
        PreparedStatement psSubscriberIdMap3 = null;
        try {
            //we need to update BOTH the instance mapping table, to map everything to our new resource
            //but also the resource_id_map so that the new resource ID links to the same enterprise ID

            String sql = "update enterprise_instance_map"
                    + " set resource_id_to = ?"
                    + " where resource_type = ?"
                    + " and resource_id_to = ?";
            psInstanceMap = connection.prepareStatement(sql);

            int col = 1;
            psInstanceMap.setString(col++, newMappedResourceId.toString());
            psInstanceMap.setString(col++, resourceType.toString());
            psInstanceMap.setString(col++, oldMappedResourceId.toString());
            psInstanceMap.executeUpdate();

            sql = "update enterprise_id_map"
                    + " set resource_id = ?"
                    + " where resource_type = ?"
                    + " and resource_id = ?";
            psEnterpriseIdMap = connection.prepareStatement(sql);

            col = 1;
            psEnterpriseIdMap.setString(col++, newMappedResourceId.toString());
            psEnterpriseIdMap.setString(col++, resourceType.toString());
            psEnterpriseIdMap.setString(col++, oldMappedResourceId.toString());
            psEnterpriseIdMap.executeUpdate();

            sql = "update enterprise_id_map_3"
                    + " set resource_id = ?"
                    + " where resource_type = ?"
                    + " and resource_id = ?";
            psEnterpriseIdMap3 = connection.prepareStatement(sql);

            col = 1;
            psEnterpriseIdMap3.setString(col++, newMappedResourceId.toString());
            psEnterpriseIdMap3.setString(col++, resourceType.toString());
            psEnterpriseIdMap3.setString(col++, oldMappedResourceId.toString());
            psEnterpriseIdMap3.executeUpdate();

            //this same instance mapping architecture is used for Compass v2
            //so we need to update the tables used for that
            sql = "update subscriber_id_map"
                    + " set source_id = ?"
                    + " where source_id = ?";
            psSubscriberIdMap = connection.prepareStatement(sql);

            col = 1;
            psSubscriberIdMap.setString(col++, ReferenceHelper.createResourceReference(resourceType, newMappedResourceId.toString()));
            psSubscriberIdMap.setString(col++, ReferenceHelper.createResourceReference(resourceType, oldMappedResourceId.toString()));
            psSubscriberIdMap.executeUpdate();

            sql = "update subscriber_id_map_3"
                    + " set source_id = ?"
                    + " where source_id = ?";
            psSubscriberIdMap3 = connection.prepareStatement(sql);

            col = 1;
            psSubscriberIdMap3.setString(col++, ReferenceHelper.createResourceReference(resourceType, newMappedResourceId.toString()));
            psSubscriberIdMap3.setString(col++, ReferenceHelper.createResourceReference(resourceType, oldMappedResourceId.toString()));
            psSubscriberIdMap3.executeUpdate();

            connection.commit();

        } catch (Exception ex) {
            connection.rollback();
            throw ex;

        } finally {
            if (psInstanceMap != null) {
                psInstanceMap.close();
            }
            if (psEnterpriseIdMap != null) {
                psEnterpriseIdMap.close();
            }
            if (psEnterpriseIdMap3 != null) {
                psEnterpriseIdMap3.close();
            }
            if (psSubscriberIdMap != null) {
                psSubscriberIdMap.close();
            }
            if (psSubscriberIdMap3 != null) {
                psSubscriberIdMap3.close();
            }
            connection.close();
        }
    }


    @Override
    public UUID findResourceIdFromInstanceMapping(ResourceType resourceType, String mappingValue) throws Exception {
        EntityManager entityManager = ConnectionManager.getSubscriberTransformEntityManager(subscriberConfigName);

        try {
            RdbmsEnterpriseInstanceMap existingMapping = findMappingByMappedValue(entityManager, resourceType, mappingValue);
            if (existingMapping != null) {
                return UUID.fromString(existingMapping.getResourceIdTo());

            } else {
                return null;
            }

        } finally {
            entityManager.close();
        }
    }


    private static RdbmsEnterpriseInstanceMap findMappingByMappedValue(EntityManager entityManager, ResourceType resourceType, String mappingValue) {

        String sql = "select c"
                + " from"
                + " RdbmsEnterpriseInstanceMap c"
                + " where c.resourceType = :resourceType"
                + " and c.mappingValue = :mappingValue";

        Query query = entityManager.createQuery(sql, RdbmsEnterpriseInstanceMap.class)
                .setParameter("resourceType", resourceType.toString())
                .setParameter("mappingValue", mappingValue)
                .setMaxResults(1);

        try {
            RdbmsEnterpriseInstanceMap result = (RdbmsEnterpriseInstanceMap) query.getSingleResult();
            return result;

        } catch (NoResultException ex) {
            return null;
        }
    }

    private static RdbmsEnterpriseInstanceMap findMappingByResourceId(EntityManager entityManager, ResourceType resourceType, UUID resourceId) {


        String sql = "select c"
                + " from"
                + " RdbmsEnterpriseInstanceMap c"
                + " where c.resourceType = :resourceType"
                + " and c.resourceIdFrom = :resourceIdFrom";

        Query query = entityManager.createQuery(sql, RdbmsEnterpriseInstanceMap.class)
                .setParameter("resourceType", resourceType.toString())
                .setParameter("resourceIdFrom", resourceId.toString())
                .setMaxResults(1);

        try {
            RdbmsEnterpriseInstanceMap result = (RdbmsEnterpriseInstanceMap) query.getSingleResult();
            return result;

        } catch (NoResultException ex) {
            return null;
        }
    }
}
