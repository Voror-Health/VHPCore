package org.endeavourhealth.core.database.rdbms.publisherTransform;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.utility.FileHelper;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.audit.models.PublishedFile;
import org.endeavourhealth.core.database.dal.audit.models.PublishedFileColumn;
import org.endeavourhealth.core.database.dal.audit.models.PublishedFileRecord;
import org.endeavourhealth.core.database.dal.audit.models.PublishedFileType;
import org.endeavourhealth.core.database.dal.ehr.ResourceDalI;
import org.endeavourhealth.core.database.dal.ehr.models.ResourceWrapper;
import org.endeavourhealth.core.database.dal.publisherTransform.SourceFileMappingDalI;
import org.endeavourhealth.core.database.dal.publisherTransform.models.ResourceFieldMapping;
import org.endeavourhealth.core.database.dal.publisherTransform.models.ResourceFieldMappingAudit;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.publisherTransform.models.RdbmsResourceFieldMappings;
import org.hibernate.internal.SessionImpl;
import org.hl7.fhir.instance.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class RdbmsSourceFileMappingDal implements SourceFileMappingDalI {
    private static final Logger LOG = LoggerFactory.getLogger(RdbmsSourceFileMappingDal.class);

    /*private final Random random = new Random(System.currentTimeMillis());
    private static Integer cachedPercentageToSendToFhirAudit = null;
    private static long cachedPercentageExpiry = -1;*/

    private static String cachedFileStoragePath = null;
    private static Map<UUID, Boolean> hmDatabasesWithNewAuditTable = new HashMap<>();

    @Override
    public List<ResourceFieldMapping> findFieldMappings(UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {
        return findFieldMappingsForField(serviceId, resourceType, resourceId, null);
    }

    @Override
    public List<ResourceFieldMapping> findFieldMappingsForField(UUID serviceId, ResourceType resourceType, UUID resourceId, String specificField) throws Exception {

        //retrieve the resource mappings audit objects
        List<AuditWrapper> resourceAudits = findResourceMappings(serviceId, resourceType, resourceId);

        //from the audits, find the distinct IDs of the audit records we'll need
        List<PublishedFileRecord> publishedRecords = findPublisherRecords(resourceAudits, specificField, serviceId);

        //retrieve the record and file meta data from the audit DB
        Map<Integer, PublishedFile> hmPublishedFiles = findPublishedFiles(publishedRecords);
        Map<Integer, PublishedFileType> hmPublishedFileTypes = findPublishedFileTypes(hmPublishedFiles);

        List<ResourceFieldMapping> ret = createFileMappingObjects(resourceAudits, publishedRecords, hmPublishedFiles, hmPublishedFileTypes, specificField);

        //the above fn doesn't ahve the resource ID or type in scope so we them on the objects now
        for (ResourceFieldMapping obj: ret) {
            obj.setResourceId(resourceId);
            obj.setResourceType(resourceType.toString());
        }

        return ret;
    }

    @Override
    public Map<UUID, ResourceFieldMappingAudit> findAllMappingsForResource(UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {

        Map<UUID, ResourceFieldMappingAudit> ret = new HashMap<>();

        List<AuditWrapper> auditWrappers = findResourceMappings(serviceId, resourceType, resourceId);
        for (AuditWrapper w: auditWrappers) {
            UUID versionId = w.getVersion();
            ResourceFieldMappingAudit audit = w.getAudit();
            ret.put(versionId, audit);
        }

        return ret;
    }


    private Map<Integer, PublishedFileType> findPublishedFileTypes(Map<Integer, PublishedFile> hmPublishedFiles) throws Exception {

        Map<Integer, PublishedFileType> ret = new HashMap<>();
        if (hmPublishedFiles.isEmpty()) {
            return ret;
        }

        //find distinct file IDs from records
        Set<Integer> distinctTypeIds = new HashSet<>();
        for (Integer fileId: hmPublishedFiles.keySet()) {
            PublishedFile file = hmPublishedFiles.get(fileId);
            distinctTypeIds.add(file.getPublishedFileTypeId());
        }

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "SELECT id, file_type, variable_column_delimiter, variable_column_quote, variable_column_escape,"
                    + " column_name, fixed_column_start, fixed_column_length"
                    + " FROM published_file_type t"
                    + " INNER JOIN published_file_type_column c"
                    + " ON c.published_file_type_id = t.id"
                    + " WHERE t.id IN (";
            for (int i=0; i<distinctTypeIds.size(); i++) {
                if (i>0) {
                    sql += ", ";
                }
                sql += "?";
            }
            sql += ")";
            sql += " ORDER BY t.id, c.column_index";

            ps = connection.prepareStatement(sql);

            int col = 1;
            for (Integer id: distinctTypeIds) {
                ps.setLong(col++, id);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                int id = rs.getInt(col++);
                String fileType = rs.getString(col++);
                String variableColDelimiter = rs.getString(col++);
                String variableColQuote = rs.getString(col++);
                String variableColEscape = rs.getString(col++);
                String columnName = rs.getString(col++);

                Integer fixedColStart = null;
                int val = rs.getInt(col++);
                if (!rs.wasNull()) {
                    fixedColStart = new Integer(val);
                }

                Integer fixedColLength = null;
                val = rs.getInt(col++);
                if (!rs.wasNull()) {
                    fixedColLength = new Integer(val);
                }

                PublishedFileType typeObj = ret.get(new Integer(id));
                if (typeObj == null) {
                    typeObj = new PublishedFileType();
                    typeObj.setFileType(fileType);;
                    if (variableColDelimiter != null) {
                        typeObj.setVariableColumnDelimiter(variableColDelimiter.charAt(0));
                    }
                    if (variableColQuote != null) {
                        typeObj.setVariableColumnQuote(variableColQuote.charAt(0));
                    }
                    if (variableColEscape != null) {
                        typeObj.setVariableColumnEscape(variableColEscape.charAt(0));
                    }

                    ret.put(new Integer(id), typeObj);
                }

                PublishedFileColumn colObj = new PublishedFileColumn();
                colObj.setColumnName(columnName);
                colObj.setFixedColumnStart(fixedColStart);
                colObj.setFixedColumnLength(fixedColLength);

                typeObj.getColumns().add(colObj);
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }

        return ret;
    }

    private List<ResourceFieldMapping> createFileMappingObjects(List<AuditWrapper> resourceAudits,
                                                                List<PublishedFileRecord> publishedRecords,
                                                                Map<Integer, PublishedFile> hmPublishedFiles,
                                                                Map<Integer, PublishedFileType> hmPublishedFileTypes,
                                                                String specificField) throws Exception {

        List<ResourceFieldMapping> ret = new ArrayList<>();

        Set<String> fieldsDoneSet = new HashSet<>();

        //hash the published records so we don't have to iterate through the list
        Map<String, PublishedFileRecord> hmPublishedRecords = new HashMap<>();
        for (PublishedFileRecord record: publishedRecords) {
            hmPublishedRecords.put("" + record.getPublishedFileId() + "_" + record.getRecordNumber(), record);
        }

        for (int i=resourceAudits.size()-1; i>=0; i--) {
            AuditWrapper auditWrapper = resourceAudits.get(i);
            ResourceFieldMappingAudit audit = auditWrapper.getAudit();

            Set<String> fieldsDoneThisAudit = new HashSet<>();

            for (ResourceFieldMappingAudit.ResourceFieldMappingAuditRow row: audit.getAudits()) {

                for (ResourceFieldMappingAudit.ResourceFieldMappingAuditCol col: row.getCols()) {
                    String field = col.getField();
                    int colIndex = col.getCol();

                    //if we're only interested in a specific field, then skip everything else
                    if (specificField != null
                            && !specificField.equalsIgnoreCase(field)) {
                        continue;
                    }

                    //only include mappings for a field for the first audit object we hit that field name in
                    if (fieldsDoneSet.contains(field)) {
                        continue;
                    }
                    fieldsDoneThisAudit.add(field); //add to a set for this specific audit

                    ResourceFieldMapping obj = new ResourceFieldMapping();
                    obj.setCreatedAt(auditWrapper.getCreated());
                    obj.setVersion(auditWrapper.getVersion());
                    obj.setResourceField(field);
                    obj.setSourceFileColumn(new Integer(colIndex));

                    //if we have a publisher record ID, then it's a new-style audit
                    PublishedFileRecord record = hmPublishedRecords.get("" + row.getFileId() + "_" + row.getRecord());
                    int fileId = record.getPublishedFileId();
                    PublishedFile file = hmPublishedFiles.get(new Integer(fileId));
                    int fileTypeId = file.getPublishedFileTypeId();
                    PublishedFileType fileType = hmPublishedFileTypes.get(new Integer(fileTypeId));

                    String sourceFilePath = file.getFilePath();
                    obj.setSourceFileName(sourceFilePath);

                    int recordNumber = record.getRecordNumber();
                    obj.setSourceFileRow(new Integer(recordNumber));

                    String sourceValue = findSourceValue(record, file, fileType, colIndex);
                    obj.setValue(sourceValue);

                    ret.add(obj);
                }
            }

            //once we've finished this audit object, add the set of all fields encountered to the larger set
            fieldsDoneSet.addAll(fieldsDoneThisAudit);
        }

        return ret;
    }

    private String findSourceValue(PublishedFileRecord record, PublishedFile file, PublishedFileType fileType, int colIndex) throws Exception {

        String filePath = file.getFilePath();
        long byteStart = record.getByteStart();
        int byteLen = record.getByteLength();

        String line = FileHelper.readCharactersFromSharedStorage(filePath, byteStart, byteLen);

        String value = null;

        //if we have no delimiter, it's a fixed, width file
        if (fileType.getVariableColumnDelimiter() == null) {
            PublishedFileColumn col = fileType.getColumns().get(colIndex);
            int colStart = col.getFixedColumnStart() - 1; //the column start is one-based, so subtract one
            int colEnd = col.getFixedColumnLength() + colStart;
            value = line.substring(colStart, colEnd);

        } else {
            //if not fixed-width, then parse using a CSV parser
            //compose a suitable CSV format from what we know of the type
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .withDelimiter(fileType.getVariableColumnDelimiter())
                    .withQuote(fileType.getVariableColumnQuote())
                    .withEscape(fileType.getVariableColumnEscape())
                    .withQuoteMode(QuoteMode.MINIMAL);

            CSVParser parser = CSVParser.parse(line, csvFormat);
            Iterator<CSVRecord> iterator = parser.iterator();
            CSVRecord csvRecord = iterator.next();
            value = csvRecord.get(colIndex);
            parser.close();
        }

        return value;
    }

    private Map<Integer, PublishedFile> findPublishedFiles(List<PublishedFileRecord> publishedRecords) throws Exception {

        Map<Integer, PublishedFile> ret = new HashMap<>();
        if (publishedRecords.isEmpty()) {
            return ret;
        }

        //find distinct file IDs from records
        Set<Integer> distinctFileIds = new HashSet<>();
        for (PublishedFileRecord publishedRecord: publishedRecords) {
            distinctFileIds.add(publishedRecord.getPublishedFileId());
        }

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "SELECT id, file_path, published_file_type_id"
                    + " FROM published_file"
                    + " WHERE id IN (";
            for (int i=0; i<distinctFileIds.size(); i++) {
                if (i>0) {
                    sql += ", ";
                }
                sql += "?";
            }
            sql += ")";

            ps = connection.prepareStatement(sql);

            int col = 1;
            for (Integer id: distinctFileIds) {
                ps.setLong(col++, id);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                int id = rs.getInt(col++);
                String filePath = rs.getString(col++);
                int typeId = rs.getInt(col++);

                PublishedFile obj = new PublishedFile();
                obj.setId(id);
                obj.setFilePath(filePath);
                obj.setPublishedFileTypeId(typeId);

                ret.put(new Integer(id), obj);
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }

        return ret;
    }

    private void populatePublishedFileRecords(List<PublishedFileRecord> publishedRecords) throws Exception {

        if (publishedRecords.isEmpty()) {
            return;
        }

        EntityManager entityManager = ConnectionManager.getAuditEntityManager();

        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl) entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "SELECT published_file_id, record_number, byte_start, byte_length"
                    + " FROM published_file_record"
                    + " WHERE";

            for (int i=0; i<publishedRecords.size(); i++) {
                if (i>0) {
                    sql += " OR";
                }
                sql += " (published_file_id = ? AND record_number = ?)";
            }

            ps = connection.prepareStatement(sql);

            int col = 1;
            for (int i=0; i<publishedRecords.size(); i++) {
                PublishedFileRecord record = publishedRecords.get(i);
                ps.setInt(col++, record.getPublishedFileId());
                ps.setInt(col++, record.getRecordNumber());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                col = 1;
                int publishedFileId = rs.getInt(col++);
                int recordNumber = rs.getInt(col++);
                long byteStart = rs.getLong(col++);
                int byteLength = rs.getInt(col++);

                for (PublishedFileRecord record: publishedRecords) {
                    if (record.getPublishedFileId() == publishedFileId
                            && record.getRecordNumber() == recordNumber) {
                        record.setByteStart(byteStart);
                        record.setByteLength(byteLength);
                    }
                }
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }


    private List<AuditWrapper> findResourceMappings(UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {

        List<AuditWrapper> ret = new ArrayList<>();

        //look in the global FHIR audit DB
        try {
            EntityManager entityManager = ConnectionManager.getFhirAuditEntityManager();
            try {
                findResourceMappingsFromDatabase(ret, entityManager, serviceId, resourceType, resourceId);
            } finally {
                entityManager.close();
            }

        } catch (Exception ex) {
            //if the DB isn't configured, we'll get an exception
        }

        //also look in publisher transform DB for any FHIR mapping audits
        EntityManager entityManager = ConnectionManager.getPublisherTransformEntityManager(serviceId);
        try {
            findResourceMappingsFromDatabase(ret, entityManager, serviceId, resourceType, resourceId);
        } finally {
            entityManager.close();
        }

        //look in the new-style short-term staging table if present
        if (hasNewTable(serviceId)) {
            findResourceMappingsFromNewTable(ret, serviceId, resourceType, resourceId);
        }

        //look in the new-style long-term S3 file store
        findResourceMappingsFromFileStore(ret, serviceId, resourceType, resourceId);

        //we always need the mappings in date order, since we only need to retrieve the most recent audit for each JSON field in the resource
        ret.sort((a, b) -> a.getCreated().compareTo(b.getCreated()));

        return ret;
    }

    @Override
    public String getPermanentStoragePath() throws Exception {
        //see if we're configured to use an S3 archive
        if (cachedFileStoragePath == null) {
            JsonNode json = ConfigManager.getConfigurationAsJson("common_config", "queuereader");
            if (json != null) {
                JsonNode node = json.get("mapping_storage_path");
                if (node != null) {
                    cachedFileStoragePath = node.asText();
                }
            }
        }
        return cachedFileStoragePath;
    }

    /**
     * finds any mappings that have been archived off to S3
     */
    private void findResourceMappingsFromFileStore(List<AuditWrapper> ret, UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {

        String storagePath = getPermanentStoragePath();
        if (Strings.isNullOrEmpty(storagePath)) {
            LOG.warn("No mapping_storage_path set in config for archived transform mappings");
            return;
        }

        //the mappings are archived by service date and time (hour & minute), so we need
        //to find all the distinct date times our resource was changed
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        DateFormat timeFormat = new SimpleDateFormat("HHmm");
        Set<String> filePaths = new HashSet<>();

        ResourceDalI resourceDal = DalProvider.factoryResourceDal();
        List<ResourceWrapper> history = resourceDal.getResourceHistory(serviceId, resourceType.toString(), resourceId);
        for (ResourceWrapper w: history) {
            Date d = w.getCreatedAt();
            String dateStr = dateFormat.format(d);
            String timeStr = timeFormat.format(d);

            String filePath = FileHelper.concatFilePath(storagePath, serviceId.toString(), dateStr, timeStr + ".csv");
            filePaths.add(filePath);
        }

        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String filePath: filePaths) {

            if (!FileHelper.fileExists(filePath)) {
                continue;
            }

            InputStreamReader isr = FileHelper.readFileReaderFromSharedStorage(filePath);
            CSVParser parser = new CSVParser(isr, CSVFormat.DEFAULT.withHeader());
            Iterator<CSVRecord> iterator = parser.iterator();

            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                UUID recordResourceId = UUID.fromString(record.get("resource_id"));
                ResourceType recordResourceType = ResourceType.valueOf(record.get("resource_type"));

                if (!recordResourceId.equals(resourceId)
                        || recordResourceType != resourceType) {
                    continue;
                }

                UUID recordVersion = UUID.fromString(record.get("version"));
                Date recordDate = dateTimeFormat.parse(record.get("created_at"));
                String mappingsJson = record.get("mappings_json");

                ResourceFieldMappingAudit audit = ResourceFieldMappingAudit.readFromJson(mappingsJson);

                AuditWrapper wrapper = new AuditWrapper(audit, recordVersion, recordDate);
                ret.add(wrapper);
            }
        }
    }

    /**
     * finds any mappings stored in the newer fhir_mapping_audit table (content in this table is archived off to S3 every day)
     */
    private void findResourceMappingsFromNewTable(List<AuditWrapper> ret, UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {

        Connection connection = ConnectionManager.getPublisherTransformConnection(serviceId);
        PreparedStatement ps = null;
        try {

            String sql = "SELECT version, created_at, mappings_json"
                    + " FROM fhir_mapping_audit"
                    + " WHERE resource_id = ?"
                    + " AND resource_type = ?";

            ps = connection.prepareStatement(sql);

            int col = 1;
            ps.setString(col++, resourceId.toString());
            ps.setString(col++, resourceType.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                col = 1;

                UUID version = UUID.fromString(rs.getString(col++));
                java.util.Date createdAt = new java.util.Date(rs.getTimestamp(col++).getTime());
                String mappingsJson = rs.getString(col++);
                ResourceFieldMappingAudit audit = ResourceFieldMappingAudit.readFromJson(mappingsJson);

                AuditWrapper wrapper = new AuditWrapper(audit, version, createdAt);
                ret.add(wrapper);
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

    }

    private void findResourceMappingsFromDatabase(List<AuditWrapper> wrappers, EntityManager entityManager, UUID serviceId, ResourceType resourceType, UUID resourceId) throws Exception {
        String sql = "select c"
                + " from"
                + " RdbmsResourceFieldMappings c"
                + " where c.resourceId = :resource_id"
                + " and c.resourceType = :resource_type";

        Query query = entityManager.createQuery(sql, RdbmsResourceFieldMappings.class)
                .setParameter("resource_id", resourceId.toString())
                .setParameter("resource_type", resourceType.toString());

        List<RdbmsResourceFieldMappings> dbObjs = query.getResultList();
        for (RdbmsResourceFieldMappings dbObj: dbObjs) {

            Date d = dbObj.getCreatedAt();
            String json = dbObj.getMappingsJson();
            ResourceFieldMappingAudit audit = ResourceFieldMappingAudit.readFromJson(json);
            String version = dbObj.getVersion();

            AuditWrapper wrapper = new AuditWrapper(audit, UUID.fromString(version), d);
            wrappers.add(wrapper);
        }
    }


    /**
     * finds the distinct publisher records IDs (and old-style record IDs) for the given resource audits,
     * but has extra logic to ignore any record IDs that won't be required because a later audit overwrites that data
     */
    private List<PublishedFileRecord> findPublisherRecords(List<AuditWrapper> resourceAudits, String specificField, UUID serviceId) throws Exception {

        List<PublishedFileRecord> publishedRecords = new ArrayList<>();

        Set<String> fieldsDoneSet = new HashSet<>();

        for (int i=resourceAudits.size()-1; i>=0; i--) {
            AuditWrapper auditWrapper = resourceAudits.get(i);
            ResourceFieldMappingAudit audit = auditWrapper.getAudit();

            Set<String> fieldsDoneThisAudit = new HashSet<>();

            for (ResourceFieldMappingAudit.ResourceFieldMappingAuditRow row: audit.getAudits()) {

                for (ResourceFieldMappingAudit.ResourceFieldMappingAuditCol col: row.getCols()) {
                    String field = col.getField();

                    //if only looking for a specific field, and we don't match, skip it
                    if (specificField != null
                            && !field.equalsIgnoreCase(specificField)) {
                        continue;
                    }

                    //if we've already handled this field on a previous audit, skip it
                    if (fieldsDoneSet.contains(field)) {
                        continue;
                    }

                    fieldsDoneThisAudit.add(field);

                    //until all old-style audits are converted over, we need to handle both the new style
                    //and the old style (that audited the full publisher record in the publisher transform DB)
                    if (row.getOldStyleAuditId() != null) {
                        //if we've got an unconverted old-style audit, then we can look up the new-style details
                        //in the mapping table on each database
                        convertOldStyleAuditToNew(row, serviceId);
                        //oldStyleRecordIds.add(oldStyleAuditId);
                    }

                    int publishedFileId = row.getFileId();
                    int recordNumber = row.getRecord();

                    if (publishedFileId == 0) {
                        throw new Exception("No published file ID or old-style audit ID for audit for resource");
                    }

                    PublishedFileRecord record = new PublishedFileRecord();
                    record.setPublishedFileId(publishedFileId);
                    record.setRecordNumber(recordNumber);
                    publishedRecords.add(record);
                }
            }

            //append all the fields we just covered to the set so far
            fieldsDoneSet.addAll(fieldsDoneThisAudit);
        }

        //we've only populated some values on the records, so we need to hit the DB to get the rest
        populatePublishedFileRecords(publishedRecords);

        return publishedRecords;
    }

    private void convertOldStyleAuditToNew(ResourceFieldMappingAudit.ResourceFieldMappingAuditRow row, UUID serviceId) throws Exception {
        Long oldSourceRecordId = row.getOldStyleAuditId();

        EntityManager entityManager = ConnectionManager.getPublisherTransformEntityManager(serviceId);
        PreparedStatement ps = null;
        try {
            SessionImpl session = (SessionImpl)entityManager.getDelegate();
            Connection connection = session.connection();

            String sql = "SELECT record_number, published_file_id"
                    + " FROM source_file_record_audit"
                    + " WHERE id = ?";
            ps = connection.prepareStatement(sql);

            ps.setLong(1, oldSourceRecordId.longValue());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int recordNumber = rs.getInt(1);
                int publishedFileId = rs.getInt(2);

                row.setFileId(publishedFileId);
                row.setRecord(recordNumber);
                row.setOldStyleAuditId(null);
                return;
            }

            throw new Exception("Failed to find published file details for old-style audit ID " + oldSourceRecordId);

        } finally {
            if (ps != null) {
                ps.close();
            }
            entityManager.close();
        }
    }


    @Override
    public void saveResourceMappings(Map<ResourceWrapper, ResourceFieldMappingAudit> audits) throws Exception {

        if (audits == null || audits.isEmpty()) {
            return;
        }

        //find service id and verify it's consistent
        UUID serviceId = null;
        for (ResourceWrapper wrapper : audits.keySet()) {
            if (serviceId == null) {
                serviceId = wrapper.getServiceId();
            } else if (!serviceId.equals(wrapper.getServiceId())) {
                throw new IllegalArgumentException("Can't save audits for multiple services at once");
            }
        }

        if (hasNewTable(serviceId)) {
            saveResourceMappingsNewWay(audits, serviceId);

        } else {
            saveResourceMappingsOldWay(audits, serviceId);
        }

    }

    /**
     * saves the FHIR resource mappings to the older resource_field_mappings table
     */
    private void saveResourceMappingsOldWay(Map<ResourceWrapper, ResourceFieldMappingAudit> audits, UUID serviceId) throws Exception {

        Connection connection = ConnectionManager.getPublisherTransformConnection(serviceId);
        PreparedStatement ps = null;
        try {

            String sql = "INSERT INTO resource_field_mappings"
                    + " (resource_id, resource_type, created_at, version, mappings_json)"
                    + " VALUES (?, ?, ?, ?, ?)";
            //note this entity is always inserted, never updated, so there's no handler for errors with an insert, like resource_current

            ps = connection.prepareStatement(sql);

            for (ResourceWrapper wrapper : audits.keySet()) {
                ResourceFieldMappingAudit audit = audits.get(wrapper);
                String mappingJson = audit.writeToJson();

                int col = 1;
                ps.setString(col++, wrapper.getResourceId().toString());
                ps.setString(col++, wrapper.getResourceType());
                ps.setTimestamp(col++, new java.sql.Timestamp(wrapper.getCreatedAt().getTime()));
                ps.setString(col++, wrapper.getVersion().toString());
                ps.setString(col++, mappingJson);

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

    /**
     * saves the FHIR resource mappings to the new fhir_mapping_audit table (so it can then be archived to S3 later)
     */
    private void saveResourceMappingsNewWay(Map<ResourceWrapper, ResourceFieldMappingAudit> audits, UUID serviceId) throws Exception {

        Connection connection = ConnectionManager.getPublisherTransformConnection(serviceId);
        PreparedStatement ps = null;
        try {

            String sql = "INSERT INTO fhir_mapping_audit"
                    + " (service_id, resource_id, resource_type, version, created_at, mappings_json)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";
            //note this entity is always inserted, never updated, so there's no handler for errors with an insert, like resource_current

            ps = connection.prepareStatement(sql);


            for (ResourceWrapper wrapper : audits.keySet()) {
                ResourceFieldMappingAudit audit = audits.get(wrapper);
                String mappingJson = audit.writeToJson();

                int col = 1;
                ps.setString(col++, serviceId.toString());
                ps.setString(col++, wrapper.getResourceId().toString());
                ps.setString(col++, wrapper.getResourceType());
                ps.setString(col++, wrapper.getVersion().toString());
                ps.setTimestamp(col++, new java.sql.Timestamp(wrapper.getCreatedAt().getTime()));
                ps.setString(col++, mappingJson);

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

    /*public void saveResourceMappings(Map<ResourceWrapper, ResourceFieldMappingAudit> audits) throws Exception {

        if (audits == null || audits.isEmpty()) {
            return;
        }

        //if configured, use the common FHIR audit DB
        //for the time-being, attempt to distribute the load between the publisher transform DB and the global
        //FHIR audit DB. The load is too much for the global one, as it causes a massive bottleneck, but it's there
        //so send some traffic to it.
        EntityManager entityManager = null;
        int r = random.nextInt(100);
        int percentage = getPercentageToSendToFhirAudit();
        boolean useFhirAuditDb = r < percentage;

        //call separate functions like this so we can see which route it's taking in stack dumps
        if (useFhirAuditDb) {
            saveResourceMappingsToFhirAudit(audits);
        } else {
            saveResourceMappingsToPublisherTransform(audits);
        }
    }

    private void saveResourceMappingsToPublisherTransform(Map<ResourceWrapper, ResourceFieldMappingAudit> audits) throws Exception {
        saveResourceMappingsToDatabase(audits, false);
    }

    private void saveResourceMappingsToFhirAudit(Map<ResourceWrapper, ResourceFieldMappingAudit> audits) throws Exception {
        saveResourceMappingsToDatabase(audits, true);
    }

    private void saveResourceMappingsToDatabase(Map<ResourceWrapper, ResourceFieldMappingAudit> audits, boolean useFhirAuditDb) throws Exception {

        UUID serviceId = null;
        for (ResourceWrapper wrapper : audits.keySet()) {
            if (serviceId == null) {
                serviceId = wrapper.getServiceId();
            } else if (!serviceId.equals(wrapper.getServiceId())) {
                throw new IllegalArgumentException("Can't save audits for multiple services at once");
            }
        }

        EntityManager entityManager = null;
        if (useFhirAuditDb) {
            entityManager = ConnectionManager.getFhirAuditEntityManager();
        } else {
            entityManager = ConnectionManager.getPublisherTransformEntityManager(serviceId);
        }

        SessionImpl session = (SessionImpl) entityManager.getDelegate();
        Connection connection = session.connection();

        PreparedStatement ps = null;
        try {

            String sql = "INSERT INTO resource_field_mappings"
                    + " (resource_id, resource_type, created_at, version, mappings_json)"
                    + " VALUES (?, ?, ?, ?, ?)";
            //note this entity is always inserted, never updated, so there's no handler for errors with an insert, like resource_current

            ps = connection.prepareStatement(sql);

            entityManager.getTransaction().begin();

            for (ResourceWrapper wrapper : audits.keySet()) {
                ResourceFieldMappingAudit audit = audits.get(wrapper);
                String mappingJson = audit.writeToJson();

                int col = 1;
                ps.setString(col++, wrapper.getResourceId().toString());
                ps.setString(col++, wrapper.getResourceType());
                ps.setTimestamp(col++, new java.sql.Timestamp(wrapper.getCreatedAt().getTime()));
                ps.setString(col++, wrapper.getVersion().toString());
                ps.setString(col++, mappingJson);

                ps.addBatch();
            }

            ps.executeBatch();

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

    public static int getPercentageToSendToFhirAudit() {
        if (cachedPercentageToSendToFhirAudit == null
                || System.currentTimeMillis() > cachedPercentageExpiry) {

            //default to 10
            cachedPercentageToSendToFhirAudit = new Integer(10);

            //then overwrite with the setting
            try {
                JsonNode json = ConfigManager.getConfigurationAsJson("common_config", "queuereader");

                JsonNode node = json.get("fhir_audit_db_percentage");
                if (node != null) {
                    cachedPercentageToSendToFhirAudit = node.asInt();
                }

            } catch (Exception ex) {
                //if the config record is there, just log it out rather than throw an exception
                LOG.warn("No common queuereader config found in config DB with app_id queuereader and config_id common_config");
            }

            cachedPercentageExpiry = System.currentTimeMillis() + (5l * 60l * 1000l);
        }

        return cachedPercentageToSendToFhirAudit.intValue();
    }*/

    /**
     * to minimise any disruption with the new FHIR audit table, continue supporting databases without the new table
     */
    private static boolean hasNewTable(UUID serviceId) throws Exception {

        Boolean b = hmDatabasesWithNewAuditTable.get(serviceId);
        if (b == null) {

            String sql = "SELECT 1"
                    + " FROM information_schema.TABLES"
                    + " WHERE TABLE_SCHEMA = DATABASE()"
                    + " AND TABLE_NAME = 'fhir_mapping_audit'";
            Connection connection = ConnectionManager.getPublisherTransformConnection(serviceId);
            PreparedStatement ps = null;
            try {
                ps = connection.prepareStatement(sql);

                ResultSet rs = ps.executeQuery();
                boolean hasTable = rs.next();
                b = Boolean.valueOf(hasTable);
                hmDatabasesWithNewAuditTable.put(serviceId, b);
                LOG.debug("Publisher_transform DB for service " + serviceId + " has new fhir_mapping_audit = " + b);

            } finally {
                if (ps != null) {
                    ps.close();
                }
                connection.close();
            }

        }
        return b.booleanValue();

    }
}

class AuditWrapper {
    private ResourceFieldMappingAudit audit = null;
    private UUID version = null;
    private Date created = null;

    public AuditWrapper(ResourceFieldMappingAudit audit, UUID version, Date created) {
        this.audit = audit;
        this.version = version;
        this.created = created;
    }

    public ResourceFieldMappingAudit getAudit() {
        return audit;
    }

    public UUID getVersion() {
        return version;
    }

    public Date getCreated() {
        return created;
    }
}