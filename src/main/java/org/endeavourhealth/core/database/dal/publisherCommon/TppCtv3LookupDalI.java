package org.endeavourhealth.core.database.dal.publisherCommon;

import org.endeavourhealth.core.database.dal.publisherCommon.models.TppCtv3Lookup;

import java.util.Date;

public interface TppCtv3LookupDalI {

    TppCtv3Lookup getContentFromCtv3Code(String ctv3Code) throws Exception;

    void updateLookupTable(String s3FilePath, Date dataDate) throws Exception;

    Date getMostRecentCtv3CodeDate() throws Exception;
}
