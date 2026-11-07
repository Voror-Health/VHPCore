package org.endeavourhealth.core.database.rdbms.ehr;

import com.google.common.base.Strings;
import org.endeavourhealth.common.cache.ParserPool;
import org.endeavourhealth.common.fhir.ReferenceComponents;
import org.endeavourhealth.common.fhir.ReferenceHelper;
import org.endeavourhealth.common.utility.MetricsHelper;
import org.endeavourhealth.common.utility.MetricsTimer;
import org.endeavourhealth.core.database.dal.ehr.ResourceDalI;
import org.endeavourhealth.core.database.dal.ehr.models.ResourceWrapper;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.DeadlockHandler;
import org.endeavourhealth.core.database.rdbms.ehr.models.AdminResourceRetrieverI;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class RdbmsResourceDal implements ResourceDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsResourceDal.class);

    private static final ParserPool PARSER_POOL = new ParserPool();

    private static Date cachedDtNewAuditStarted = null;

    @Override
    public void save(List<ResourceWrapper> wrappers) throws Exception {
        //allow several attempts if it fails due to a deadlock
        DeadlockHandler h = new DeadlockHandler();

        while (true) {
            try {
                trySave(wrappers);
                break;

            } catch (Exception ex) {
                h.handleError(ex);
            }
        }
    }

    private UUID findServiceId(List<ResourceWrapper> wrappers) throws Exception {

        if (wrappers == null || wrappers.isEmpty()) {
            throw new IllegalArgumentException("trying to save null or empty resources");
        }

        UUID serviceId = null;
        for (ResourceWrapper wrapper : wrappers) {
            if (serviceId == null) {
                serviceId = wrapper.getServiceId();
            } else if (!serviceId.equals(wrapper.getServiceId())) {
                throw new IllegalArgumentException("Can't save resources for different services");
            }
        }
        return serviceId;
    }

    private void trySave(List<ResourceWrapper> wrappers) throws Exception {

        UUID serviceId = findServiceId(wrappers);
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;

        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.put.saveFhirResources")) {

            //we only need to write to resource_current as the trigger will sort resource_history for us
            ps = createUpsertResourceCurrentPreparedStatement(connection);

            for (ResourceWrapper wrapper : wrappers) {
                populateUpsertResourceCurrentPreparedStatement(wrapper, ps);
                ps.addBatch();
            }
            ps.executeBatch();

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
    public void delete(List<ResourceWrapper> wrappers) throws Exception {

        //simply clear down some fields and save as normal
        for (ResourceWrapper wrapper : wrappers) {
            wrapper.setDeleted(true);
            wrapper.setResourceData(null);
            wrapper.setResourceChecksum(null);
        }

        save(wrappers);
    }

    @Override
    public void save(ResourceWrapper resourceEntry) throws Exception {
        List<ResourceWrapper> l = new ArrayList<>();
        l.add(resourceEntry);
        save(l);
    }


    private static PreparedStatement createUpsertResourceCurrentPreparedStatement(Connection connection) throws Exception {

        String sql = "INSERT INTO resource_current"
                + " (service_id, system_id, resource_type, resource_id, updated_at, patient_id, resource_data, resource_checksum, resource_metadata)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE"
                + " patient_id = VALUES(patient_id)," //although part of the primary key, the unique index on the table means we can update the patient_id on a resource with this
                + " system_id = VALUES(system_id),"
                + " updated_at = VALUES(updated_at),"
                + " resource_data = VALUES(resource_data),"
                + " resource_checksum = VALUES(resource_checksum),"
                + " resource_metadata = VALUES(resource_metadata)";

        return connection.prepareStatement(sql);
    }

    private static void populateUpsertResourceCurrentPreparedStatement(ResourceWrapper wrapper, PreparedStatement ps) throws Exception {

        int col = 1;

        ps.setString(col++, wrapper.getServiceId().toString());
        ps.setString(col++, wrapper.getSystemId().toString());
        ps.setString(col++, wrapper.getResourceType());
        ps.setString(col++, wrapper.getResourceId().toString());
        ps.setTimestamp(col++, new java.sql.Timestamp(wrapper.getCreatedAt().getTime()));
        if (wrapper.getPatientId() != null) {
            ps.setString(col++, wrapper.getPatientId().toString());
        } else {
            //the patient_id column doesn't allow nulls, as it's part of an index, so insert empty String instead
            ps.setString(col++, "");
            //ps.setNull(col++, Types.VARCHAR);
        }
        if (!Strings.isNullOrEmpty(wrapper.getResourceData())) {
            ps.setString(col++, wrapper.getResourceData());
        } else {
            ps.setNull(col++, Types.VARCHAR);
        }
        if (wrapper.getResourceChecksum() != null) {
            ps.setLong(col++, wrapper.getResourceChecksum());
        } else {
            ps.setNull(col++, Types.BIGINT);
        }

        //to allow the trigger to populate resource history properly even though there are column differences
        //to resource current, we use the otherwise unused metadata field to store some data
        String metadataStr = wrapper.getExchangeBatchId().toString() + "|" + wrapper.getVersion().toString();
        ps.setString(col++, metadataStr);
    }

    /**
     * logical delete, when we want to delete a resource but maintain our audits
     */
    @Override
    public void delete(ResourceWrapper resourceEntry) throws Exception {
        List<ResourceWrapper> l = new ArrayList<>();
        l.add(resourceEntry);
        delete(l);
    }


    /**
     * physical delete, when we want to remove all trace of data from DDS
     *
     * only needs to delete from resource_current as resource_history will be sorted by the trigger
     */
    @Override
    public void hardDeleteResourceAndAllHistory(ResourceWrapper wrapper) throws Exception {

        UUID serviceId = wrapper.getServiceId();
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;

        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.delete.hardDeleteFhirResource")) {

            String sql = "DELETE FROM resource_current"
                    + " WHERE service_id = ?"
                    + " AND patient_id = ?"
                    + " AND resource_type = ?"
                    + " AND resource_id = ?";

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, wrapper.getServiceId().toString());
            if (wrapper.getPatientId() != null) {
                ps.setString(col++, wrapper.getPatientId().toString());
            } else {
                ps.setString(col++, ""); //DB field doesn't allow nulls so save as empty String
            }
            ps.setString(col++, wrapper.getResourceType());
            ps.setString(col++, wrapper.getResourceId().toString());

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





    /**
     * convenience fn to save repetitive code
     */
    @Override
    public Resource getCurrentVersionAsResource(UUID serviceId, ResourceType resourceType, String resourceIdStr) throws Exception {
        ResourceWrapper resourceHistory = getCurrentVersion(serviceId, resourceType.toString(), UUID.fromString(resourceIdStr));

        if (resourceHistory == null) {
            return null;
        } else {
            return PARSER_POOL.parse(resourceHistory.getResourceData());
        }
    }

    @Override
    public ResourceWrapper getCurrentVersion(UUID serviceId, String resourceType, UUID resourceId) throws Exception {
        //even though we now keep "deleted" records in resource_current
        //we should mimic previous behaviour and return null for deleted ones
        return getCurrentVersionImpl(serviceId, resourceType, resourceId, false);
    }

    private ResourceWrapper getCurrentVersionImpl(UUID serviceId, String resourceType, UUID resourceId, boolean returnDeletedResourceCurrent) throws Exception {
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResource")) {

            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE resource_type = ?"
                    + " AND resource_id = ?"
                    + " LIMIT 1"; //we know there'll only be one
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, resourceType);
            ps.setString(col++, resourceId.toString());

            ResultSet rs = ps.executeQuery();
            List<ResourceWrapper> l = readResourceWrappersFromResourceCurrentResultSet(rs, returnDeletedResourceCurrent);
            if (l.isEmpty()) {
                return null;

            } else {
                return l.get(0);
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    private static List<ResourceWrapper> readResourceWrappersFromResourceCurrentResultSet(ResultSet rs, boolean returnDeletedResources) throws Exception {
        return readResourceWrappersFromResourceCurrentResultSet(rs, returnDeletedResources, Integer.MAX_VALUE);
    }

    private static List<ResourceWrapper> readResourceWrappersFromResourceCurrentResultSet(ResultSet rs, boolean returnDeletedResources, int maxResources) throws Exception {
        List<ResourceWrapper> ret = new ArrayList<>();

        //MUST check the size before calling next() otherwise we advance in the result set
        //before we know we want to, and end up missing resources
        //while (rs.next() && ret.size() < maxResources) {
        while (ret.size() < maxResources && rs.next()) {
            int col = 1;

            ResourceWrapper w = new ResourceWrapper();
            w.setServiceId(UUID.fromString(rs.getString(col++)));
            w.setSystemId(UUID.fromString(rs.getString(col++)));
            w.setResourceType(rs.getString(col++));
            w.setResourceId(UUID.fromString(rs.getString(col++)));
            w.setCreatedAt(new java.util.Date(rs.getTimestamp(col++).getTime()));

            String patientIdStr = rs.getString(col++);
            if (!Strings.isNullOrEmpty(patientIdStr)) {
                w.setPatientId(UUID.fromString(patientIdStr));
            }

            String resourceData = rs.getString(col++);
            long resourceChecksum = rs.getLong(col++);

            //if the resource data is null, then the resource has been deleted
            if (Strings.isNullOrEmpty(resourceData)) {
                w.setResourceData(null);
                w.setResourceChecksum(null);
                w.setDeleted(true);

            } else {
                w.setResourceData(resourceData);
                w.setResourceChecksum(resourceChecksum);
                w.setDeleted(false);
            }

            //only add if not deleted or we want deleted ones
            if (returnDeletedResources
                    || !w.isDeleted()) {
                ret.add(w);
            }
        }

        return ret;
    }

    private static String getResourceCurrentSelectPrefix() {
        return getResourceCurrentSelectPrefix(null);
    }

    private static String getResourceCurrentSelectPrefix(String tableAlias) {

        String pre;
        String post;
        if (Strings.isNullOrEmpty(tableAlias)) {
            pre = "";
            post = "";
        } else {
            pre = tableAlias + ".";
            post = " " + tableAlias;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append(pre);
        sb.append("service_id, ");
        sb.append(pre);
        sb.append("system_id, ");
        sb.append(pre);
        sb.append("resource_type, ");
        sb.append(pre);
        sb.append("resource_id, ");
        sb.append(pre);
        sb.append("updated_at, ");
        sb.append(pre);
        sb.append("patient_id, ");
        sb.append(pre);
        sb.append("resource_data, ");
        sb.append(pre);
        sb.append("resource_checksum ");
        sb.append("FROM resource_current");
        sb.append(post);

        return sb.toString();
    }

    @Override
    public Map<String, ResourceWrapper> getCurrentVersionForReferences(UUID serviceId, List<String> references) throws Exception {

        if (references.isEmpty()) {
            return new HashMap<>();
        }

        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResources")) {

            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE (";
            for (int i=0; i<references.size(); i++) {
                if (i>0) {
                    sql += ") OR (";
                }
                sql += "resource_type = ? AND resource_id = ?";
            }
            sql += ")";

            ps = connection.prepareStatement(sql);

            int col = 1;

            for (String referenceStr: references) {
                Reference reference = ReferenceHelper.createReference(referenceStr);
                ReferenceComponents comps = ReferenceHelper.getReferenceComponents(reference);

                ps.setString(col++, comps.getResourceType().toString());
                ps.setString(col++, comps.getId());
            }

            Map<String, ResourceWrapper> ret = new HashMap<>();

            ResultSet rs = ps.executeQuery();
            List<ResourceWrapper> l = readResourceWrappersFromResourceCurrentResultSet(rs, false);
            for (ResourceWrapper w: l) {
                String reference = ReferenceHelper.createResourceReference(w.getResourceType(), w.getResourceIdStr());
                ret.put(reference, w);
            }

            return ret;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

    }

    /**
     * gets the history of a resource
     *
     * note this fn handles both old and new ways of storing history:
     * old way - resource history contains all history
     * new way - resource history contains all old versions, but resource current has the latest version
     */
    @Override
    public List<ResourceWrapper> getResourceHistory(UUID serviceId, String resourceType, UUID resourceId) throws Exception {

        //get the current version of the resource
        ResourceWrapper current = getCurrentVersionImpl(serviceId, resourceType, resourceId, true);

        //get the history records from the DB
        List<ResourceWrapper> history = getResourceHistoryRaw(serviceId, resourceType, resourceId);

        //go through history and work out where new way of auditing took over, then adjust from that point onwards
        int index = findIndexOfHistoryChangeover(history, current);
        adjustHistoryForChangeover(history, index, current);

        //this function is expected to return the history most-recent-first, so we need to reverse it
        return reverse(history);
    }

    private static List<ResourceWrapper> reverse(List<ResourceWrapper> history) {
        List<ResourceWrapper> ret = new ArrayList<>(history.size()); //may as well create with the right capacity

        for (int i=history.size()-1; i>=0; i--) { //history is sorted most-recent-first, so go backwards from the index to the latest version
            ResourceWrapper h = history.get(i);
            ret.add(h);
        }

        return ret;
    }

    @Override
    public List<ResourceWrapper> getAllAdminResourcesByType(String service_id, ResourceType resourceType, boolean returnDeletedResourceCurrent) throws Exception {
        return getAllAdminResourcesByTypeImpl(service_id, resourceType, returnDeletedResourceCurrent);
    }
    private List<ResourceWrapper> getAllAdminResourcesByTypeImpl(String service_id, ResourceType resourceType, boolean returnDeletedResourceCurrent) throws Exception {
        Connection connection = ConnectionManager.getEhrConnection(UUID.fromString(service_id));
        String resource_type = resourceType.toString();
        String sql = getResourceCurrentSelectPrefix()
                + " WHERE service_id = ?"
                + " AND patient_id = ''"
                + " AND resource_type = ?;";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            int col = 1;
            ps.setString(col++, service_id);
            ps.setString(col++, resource_type);

            ResultSet result = ps.executeQuery();
            return readResourceWrappersFromResourceCurrentResultSet(result, returnDeletedResourceCurrent);
        } finally {
            if(ps != null){
                ps.close();
            }
            connection.close();
        }

    }

    private static void adjustHistoryForChangeover(List<ResourceWrapper> history, int indexFrom, ResourceWrapper current) {

        //if the index was -1, then it means the history has never been written to in the new
        //way so shouldn't be adjusted at all
        if (indexFrom == -1) {
            return;
        }

        //for Barts and Homerton, there are resources where we have a delete and upsert in the same second,
        //due to ADT merges. So we need to potentially correct the sorting of these (since the sorting is only to the second)
        //to make sure that the history makes sense.
        //affected TPP too, so removing service-specific code
        /*ResourceWrapper first = history.get(0);
        String serviceIdStr = first.getServiceId().toString();
        if (serviceIdStr.equalsIgnoreCase("b5a08769-cbbe-4093-93d6-b696cd1da483") //barts
                || serviceIdStr.equalsIgnoreCase("962d6a9a-5950-47ac-9e16-ebee56f9507a") //homerton
                || serviceIdStr.equalsIgnoreCase("3ec22b12-5b2e-4665-bc87-34072725ef21")) { //last one is for testing*/

        Date sameDate = null;
        int sameDateStart = -1;

        for (int i=indexFrom; i<history.size(); i++) {
            ResourceWrapper w = history.get(i);
            Date d = w.getCreatedAt();
            if (sameDate == null
                    || !sameDate.equals(d)) {

                if (sameDate != null
                        && !sameDate.equals(d)) {
                    sortWrappersOnSameDate(history, sameDateStart, i-1, w.getResourceData() != null);
                }

                sameDate = d;
                sameDateStart = i;
            }
        }

        //once we've reached the end, make sure to sort the last ones
        sortWrappersOnSameDate(history, sameDateStart, history.size()-1, current.getResourceData() != null);

        //}

        //now we've got them in the right order, we can move the JSON around so it looks like the old-style auditing
        for (int i = indexFrom; i < history.size(); i++) {
            ResourceWrapper h = history.get(i);

            ResourceWrapper copyFrom = null;
            if (i+1 < history.size()) {
                //move the resource JSON and checksum from the next record
                copyFrom = history.get(i+1);

            } else {
                //if the most recent record, then populate with what we got from resource current
                copyFrom = current;
            }

            h.setResourceData(copyFrom.getResourceData());
            h.setResourceChecksum(copyFrom.getResourceChecksum());
        }
    }

    private static void sortWrappersOnSameDate(List<ResourceWrapper> history, int sameDateStart, int sameDateEnd, boolean nextOneHasJson) {

        //if there's only one history item in our range, return
        int range = sameDateEnd - sameDateStart;
        if (range < 1) { //note this is correct since both ints are indexes
            return;
        }

        //SD-185 Vision sends two deletes for the same patient in the same file, and both end up on the DB
        //with the same date time. So detect if our entire range is deletes and handle that differently to the existing logic
        boolean allDeletes = true;
        for (int i=sameDateStart; i<=sameDateEnd; i++) {
            ResourceWrapper w = history.get(i);
            if (!w.isDeleted()) {
                allDeletes = false;
                break;
            }
        }

        if (allDeletes) {

            //if they're all deletes then the first one should be the one with the JSON of the one that was present before that.
            //So find the one with JSON and bump that to be the first one in the range.
            LOG.trace("Got " + (range+1) + " deletes all with same datetime");
            for (int i=sameDateStart+1; i<=sameDateEnd; i++) {
                ResourceWrapper w = history.get(i);
                if (w.getResourceData() != null) {
                    history.remove(i);
                    history.add(sameDateStart, w);
                    LOG.trace("Moving index " + i + " to " + sameDateStart + " so delete with JSON is first");
                    break;
                }
            }

        } else {

            //if they're not all deletes, then we should have one that's an upsert and one that's a delete
            //and if the next record has no json then it tells us the last one in this subset should be a delete
            //and since two deletes can't be next to each other, we can then sort by alternating that
            LOG.trace("Got " + (range+1) + " deletes/upserts all with same datetime");
            boolean wantDelete = !nextOneHasJson;

            for (int i = sameDateEnd; i >= sameDateStart; i--) {
                ResourceWrapper w = history.get(i);

                if (w.isDeleted() != wantDelete) {

                    int swapIndex = -1;
                    for (int j = i - 1; j >= sameDateStart; j--) {
                        ResourceWrapper w2 = history.get(j);
                        if (w2.isDeleted() == wantDelete) {
                            swapIndex = j;
                            break;
                        }
                    }

                    if (swapIndex == -1) {
                        //if we've not found one to swap with, something is wrong
                        LOG.error("Failed to sort history for " + w.getResourceType() + " " + w.getResourceId() + " at index " + i + " for subset range " + sameDateStart + " to " + sameDateEnd);
                        return;
                    }

                    //swap them around
                    ResourceWrapper w2 = history.get(swapIndex);
                    history.set(swapIndex, w);
                    history.set(i, w2);
                    LOG.trace("Swapping position of " + i + " and " + swapIndex);
                }

                //the next one should be the opposite
                wantDelete = !wantDelete;
            }
        }
    }

    /**
     * works out where in the history list the new-style of resource_history auditing took over (if at all)
     */
    private static int findIndexOfHistoryChangeover(List<ResourceWrapper> history, ResourceWrapper current) throws Exception {

        //if there's no current version it means that the resource was deleted from resource_current
        //which means that the resource has never been updated in the new way, so nothing needs modifying
        if (current == null) {
            return -1;
        }

        //if the first instance was not deleted but has null JSON then it was initially saved in the new way
        ResourceWrapper first = history.get(0);
        if (!first.isDeleted()
                && Strings.isNullOrEmpty(first.getResourceData())) {
            return 0;
        }

        Date changeoverDate = getDtNewAuditStarted();

        for (int i=0; i<history.size(); i++) {
            ResourceWrapper h = history.get(i);
            Date d = h.getCreatedAt();

            if (d.after(changeoverDate)) {
                return i;
            }
        }

        //if we make it here then the new auditing has never been used for this resource so return -1 to indicate this
        return -1;
    }

    private static Date getDtNewAuditStarted() throws Exception {
        if (cachedDtNewAuditStarted == null) {
            cachedDtNewAuditStarted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2020-03-17 16:00:00");
        }
        return cachedDtNewAuditStarted;
    }

    private List<ResourceWrapper> getResourceHistoryRaw(UUID serviceId, String resourceType, UUID resourceId) throws Exception {
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResourceHistory")) {

            String sql = "SELECT service_id, system_id, resource_type, resource_id, created_at, patient_id, resource_data, resource_checksum, is_deleted, exchange_batch_id, version"
                    + " FROM resource_history"
                    + " WHERE resource_type = ?"
                    + " AND resource_id = ?"
                    + " ORDER BY created_at ASC, resource_checksum"; //explicitly sort so ordered most-recent-last
            //resource_checksum is included in the sorting so that if two history records have the same created_at
            //one with a null checksum (i.e. deleted one or one from the initial insert) will be first
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, resourceType);
            ps.setString(col++, resourceId.toString());

            List<ResourceWrapper> ret = new ArrayList<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                col = 1;
                ResourceWrapper w = new ResourceWrapper();
                w.setServiceId(UUID.fromString(rs.getString(col++)));
                w.setSystemId(UUID.fromString(rs.getString(col++)));
                w.setResourceType(rs.getString(col++));
                w.setResourceId(UUID.fromString(rs.getString(col++)));
                w.setCreatedAt(new java.util.Date(rs.getTimestamp(col++).getTime()));

                //this field is an empty string for non-patient resources
                String patientIdStr = rs.getString(col++);
                if (!Strings.isNullOrEmpty(patientIdStr)) {
                    w.setPatientId(UUID.fromString(patientIdStr));
                }

                w.setResourceData(rs.getString(col++));
                w.setResourceChecksum(rs.getLong(col++));
                w.setDeleted(rs.getBoolean(col++));
                w.setExchangeBatchId(UUID.fromString(rs.getString(col++)));
                w.setVersion(UUID.fromString(rs.getString(col++)));

                ret.add(w);
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
    public List<ResourceWrapper> getResourcesByPatient(UUID serviceId, UUID patientId) throws Exception {
        return getResourcesByPatientImpl(serviceId, patientId, false);
    }

    @Override
    public List<ResourceWrapper> getResourcesByPatientIncludingDeleted(UUID serviceId, UUID patientId) throws Exception {
        return getResourcesByPatientImpl(serviceId, patientId, true);
    }

    private List<ResourceWrapper> getResourcesByPatientImpl(UUID serviceId, UUID patientId, boolean includeDeleted) throws Exception {
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResourcesForPatient")) {

            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE service_id = ?"
                    + " AND patient_id = ?";

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, serviceId.toString());
            if (patientId != null) {
                ps.setString(col++, patientId.toString());
            } else {
                //for admin resourecs, the patient ID will be a non-null empty string
                ps.setString(col++, "");
            }

            ResultSet rs = ps.executeQuery();
            return readResourceWrappersFromResourceCurrentResultSet(rs, includeDeleted);

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }


    @Override
    public List<ResourceWrapper> getResourcesByPatient(UUID serviceId, UUID patientId, String resourceType) throws Exception {
        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResourcesForPatientAndType")) {
            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE service_id = ?"
                    + " AND patient_id = ?"
                    + " AND resource_type = ?";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, serviceId.toString());
            ps.setString(col++, patientId.toString());
            ps.setString(col++, resourceType.toString());

            ResultSet rs = ps.executeQuery();
            return readResourceWrappersFromResourceCurrentResultSet(rs, false);

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }




    /**
     * getResourcesForBatch(..) returns the resources as they exactly were when the batch was created,
     * so can return an older version than is currently on the DB. This function returns the CURRENT version
     * of each resource that's in the batch.
     */
    @Override
    public List<ResourceWrapper> getCurrentVersionOfResourcesForBatch(UUID serviceId, UUID batchId) throws Exception {

        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResourcesForBatch")) {

            //deleted resources are removed from resource_current, so we need a left outer join
            //and have to select enough columns from resource_history to be able to spot the deleted
            //resources and create resource wrappers for them
            //changed the order of columns so that we never get a null string in the last column,
            //which exposed a bug in the MySQL driver (https://bugs.mysql.com/bug.php?id=84084)
            String sql = "SELECT h.service_id, h.system_id, h.patient_id, "
                    + " c.system_id, c.updated_at, c.patient_id, c.resource_data, h.resource_type, h.resource_id"
                    + " FROM resource_history h"
                    + " LEFT OUTER JOIN resource_current c"
                    + " ON h.resource_id = c.resource_id"
                    + " AND h.resource_type = c.resource_type"
                    + " WHERE h.exchange_batch_id = ?";

            ps = connection.prepareStatement(sql);
            ps.setString(1, batchId.toString());

            List<ResourceWrapper> ret = new ArrayList<>();

            //some transforms can end up saving the same resource multiple times in a batch, so we'll have duplicate
            //rows in our resource_history table. Since they all join to the latest data in resource_current table, we
            //don't need to worry about which we keep, so just use this to avoid duplicates
            Set<String> resourcesDone = new HashSet<>();

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int col = 1;
                String historyServiceId = rs.getString(col++);
                String historySystemId = rs.getString(col++);
                /*String historyResourceType = rs.getString(col++);
                String historyResourceId = rs.getString(col++);*/
                String historyPatientId = rs.getString(col++);

                //since we're not dealing with any primitive types, we can just use getString(..)
                //and check that the result is null or not, without needing to use wasNull(..)
                String currentSystemId = rs.getString(col++);
                Date currentUpdatedAt = null;
                java.sql.Timestamp ts = rs.getTimestamp(col++);
                if (ts != null) {
                    currentUpdatedAt = new Date(ts.getTime());
                }
                String currentPatientId = rs.getString(col++);
                String currentResourceData = rs.getString(col++);

                //moved these columns to be the last in the result set, to avoid MySQL bug https://bugs.mysql.com/bug.php?id=84084
                String historyResourceType = rs.getString(col++);
                String historyResourceId = rs.getString(col++);

                //skip if we've already done this resource
                String referenceStr = ReferenceHelper.createResourceReference(historyResourceType, historyResourceId);
                if (resourcesDone.contains(referenceStr)) {
                    continue;
                }
                resourcesDone.add(referenceStr);

                //populate the resource wrapper with what we've got, depending on what's null or not.
                //NOTE: the resource wrapper will have the following fields null:
                //UUID version;
                //Date createdAt;
                //String resourceMetadata;
                //Long resourceChecksum;
                //UUID exchangeId;

                ResourceWrapper wrapper = new ResourceWrapper();
                wrapper.setServiceId(UUID.fromString(historyServiceId));
                wrapper.setResourceType(historyResourceType);
                wrapper.setResourceId(UUID.fromString(historyResourceId));
                wrapper.setExchangeBatchId(batchId); //not sure about setting this... seems to imply that this is the current batch ID

                //if we have no resource data, the resource is deleted, so populate with what we've got from the history table
                if (currentResourceData == null) {
                    wrapper.setDeleted(true);
                    wrapper.setSystemId(UUID.fromString(historySystemId));
                    if (!Strings.isNullOrEmpty(historyPatientId)) {
                        wrapper.setPatientId(UUID.fromString(historyPatientId));
                    }

                } else {
                    //if we have resource data, then populate with what we've got from resource_current
                    wrapper.setSystemId(UUID.fromString(currentSystemId));
                    wrapper.setResourceData(currentResourceData);
                    wrapper.setCreatedAt(currentUpdatedAt);
                    if (!Strings.isNullOrEmpty(currentPatientId)) {
                        wrapper.setPatientId(UUID.fromString(currentPatientId));
                    }
                }

                ret.add(wrapper);
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
    public Long getResourceChecksum(UUID serviceId, String resourceType, UUID resourceId) throws Exception {

        String resourceReference = ReferenceHelper.createResourceReference(resourceType, resourceId.toString());
        List<String> l = new ArrayList<>();
        l.add(resourceReference);

        Map<String, Long> map = getResourceChecksumsForReferences(serviceId, l);
        return map.get(resourceReference);
    }

    @Override
    public Map<String, Long> getResourceChecksumsForReferences(UUID serviceId, List<String> references) throws Exception {

        if (references.isEmpty()) {
            return new HashMap<>();
        }

        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirResourceChecksums")) {

            String sql = "SELECT resource_type, resource_id, resource_checksum"
                    + " FROM resource_current"
                    + " WHERE (";
            for (int i=0; i<references.size(); i++) {
                if (i>0) {
                    sql += ") OR (";
                }
                sql += "resource_type = ? and resource_id = ?";
            }
            sql += ")";

            ps = connection.prepareStatement(sql);

            int col = 1;

            for (String referenceStr: references) {
                Reference reference = ReferenceHelper.createReference(referenceStr);
                ReferenceComponents comps = ReferenceHelper.getReferenceComponents(reference);

                ps.setString(col++, comps.getResourceType().toString());
                ps.setString(col++, comps.getId());
            }

            Map<String, Long> ret = new HashMap<>();

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                col = 1;
                String type = rs.getString(col++);
                String id = rs.getString(col++);
                long checksum = rs.getLong(col++);

                //the checksum will be null for deleted resources so skip them
                if (rs.wasNull()) {
                    continue;
                }

                String reference = ReferenceHelper.createResourceReference(type, id);
                ret.put(reference, new Long(checksum));
            }

            return ret;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

    }

    /**
     * tests if we have any patient data stored for the given service
     * note this purposefully doesn't exclude "deleted" resource_current records since it's used to test
     * for the absolute presence of any data rather than whether there's data that has been logically deleted or not
     */
    public boolean dataExists(UUID serviceId) throws Exception {

        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.checkDataExistsForService")) {
            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE service_id = ?"
                    + " LIMIT 1";

            ps = connection.prepareStatement(sql);

            ps.setString(1, serviceId.toString());

            ResultSet rs = ps.executeQuery();
            List<ResourceWrapper> l = readResourceWrappersFromResourceCurrentResultSet(rs, true);
            return !l.isEmpty();

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }


    @Override
    public List<ResourceWrapper> getMedicationOrderResourcesForPatientAndMedicationStatement(UUID serviceId, UUID patientId, UUID medicationStatementId) throws Exception {

        String reference = ReferenceHelper.createResourceReference(ResourceType.MedicationStatement, medicationStatementId.toString());

        Connection connection = ConnectionManager.getEhrConnection(serviceId);
        PreparedStatement ps = null;
        try (MetricsTimer timer = MetricsHelper.recordTime("database.ehr.get.getFhirMedicationOrdersForPatientAndStatement")) {

            String sql = getResourceCurrentSelectPrefix()
                    + " WHERE service_id = ?"
                    + " AND patient_id = ?"
                    + " AND resource_type = ?"
                    + " AND resource_data LIKE ?";
            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, serviceId.toString());
            ps.setString(col++, patientId.toString());
            ps.setString(col++, ResourceType.MedicationOrder.toString());
            ps.setString(col++, "%" + reference + "%");

            ResultSet rs = ps.executeQuery();
            return readResourceWrappersFromResourceCurrentResultSet(rs, false);

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

    }

    @Override
    public AdminResourceRetrieverI startRetrievingAdminResources(UUID serviceId, int batchSize) throws Exception {

        return new AdminResourceRetrieverImpl(serviceId, batchSize);

    }


    static class AdminResourceRetrieverImpl implements AdminResourceRetrieverI {

        private UUID serviceId;
        private int batchSize;
        private String tempTable;
        private Integer maxRows;
        private int nextBatch = 0;
        private boolean finished = false;

        public AdminResourceRetrieverImpl(UUID serviceId, int batchSize) throws Exception {
            this.serviceId = serviceId;
            this.batchSize = batchSize;
            this.tempTable = ConnectionManager.generateTempTableName("Admin_Resources");
            createTable();
        }

        @Override
        public List<ResourceWrapper> getNextBatch() throws Exception {

            //if we've already finished everything, there is no next batch
            if (finished) {
                return null;
            }

            int minRow = nextBatch * batchSize;
            int maxRow = minRow + batchSize;
            //LOG.debug("Retrieving admin batch " + nextBatch + " resource from " + minRow + " to " + maxRow + " / " + maxRows);
            nextBatch ++;

            List<ResourceWrapper> resourceWrappers = null;

            Connection connection = ConnectionManager.getEhrNonPooledConnection(serviceId); //don't want a pooled connection
            Statement statement = connection.createStatement(); //all SQL will be unique, so don't bother with prepared ones

            try {
                String sql = getResourceCurrentSelectPrefix("r")
                        + " INNER JOIN " + tempTable + " t"
                        + " ON"
                        + " t.resource_id = r.resource_id"
                        + " AND t.resource_type = r.resource_type"
                        + " WHERE t.row_num > " + minRow
                        + " AND t.row_num <= " + maxRow
                        + " AND r.service_id = '" + serviceId.toString() + "'"
                        + " AND r.patient_id = ''";

                ResultSet rs = statement.executeQuery(sql);
                resourceWrappers = readResourceWrappersFromResourceCurrentResultSet(rs, false, batchSize);

            } finally {
                statement.close();
                connection.close();
            }

            //if we've reached the end, close everything down
            if (resourceWrappers.size() < batchSize) {
                close();
            }

            return resourceWrappers;
        }

        @Override
        public void close() throws Exception {
            Connection connection = ConnectionManager.getEhrNonPooledConnection(serviceId); //don't want a pooled connection
            Statement statement = connection.createStatement(); //all SQL will be unique, so don't bother with prepared ones
            LOG.debug("Dropping table " + tempTable);
            try {
                String sql = "DROP TABLE " + tempTable;
                statement.executeUpdate(sql);

            } finally {
                statement.close();
                connection.close();
            }

            finished = true;
        }

        private void createTable() throws Exception {
            Connection connection = ConnectionManager.getEhrNonPooledConnection(serviceId); //don't want a pooled connection
            Statement statement = connection.createStatement(); //all SQL will be unique, so don't bother with prepared ones

            try {
                String sql = "set @row = 0";
                statement.executeUpdate(sql);

                sql = "CREATE TABLE " + tempTable + " AS"
                        + " SELECT resource_type, resource_id, @row:=@row+1 as `row_num`"
                        + " FROM resource_current"
                        + " WHERE service_id = '" + serviceId.toString() + "'"
                        + " AND patient_id = ''"
                        + " AND resource_type != '" + ResourceType.Slot.toString() + "'" //we never need Slot resources when doing this
                        + " AND resource_data != ''";   //exclude deleted resources (DDSEUS-262)
                statement.executeUpdate(sql);

                sql = "CREATE INDEX ix ON " + tempTable + " (row_num)";
                statement.executeUpdate(sql);

                sql = "SELECT MAX(row_num) FROM " + tempTable;
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) {
                    maxRows = new Integer(rs.getInt(1));
                } else {
                    maxRows = new Integer(0);
                }

            } finally {
                statement.close();
                connection.close();
            }
        }
    }
}
