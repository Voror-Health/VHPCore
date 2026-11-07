package org.endeavourhealth.core.database.dal.publisherCommon;

import java.util.Date;

public interface TppCtv3HierarchyRefDalI {

    boolean isChildCodeUnderParentCode(String childReadCode, String ParentReadCode) throws Exception;

    void updateHierarchyTable(String s3FilePath, Date dataDate) throws Exception;

    Date getMostRecentCtv3HierarchyDate() throws Exception;
}
