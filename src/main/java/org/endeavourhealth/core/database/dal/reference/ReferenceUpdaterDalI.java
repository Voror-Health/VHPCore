package org.endeavourhealth.core.database.dal.reference;

import org.endeavourhealth.core.database.dal.reference.models.DeprivationLookup;

public interface ReferenceUpdaterDalI {

    void updateLosaMap(String lsoaCode, String lsoaName) throws Exception;
    void updateMosaMap(String msoaCode, String msoaName) throws Exception;
    void updateCcgMap(String ccgCode, String ccgName) throws Exception;
    void updateWardMap(String wardCode, String wardName) throws Exception;
    void updateLocalAuthorityMap(String localAuthorityCode, String localAuthorityName) throws Exception;
    void updateDeprivationMap(DeprivationLookup deprivationLookup) throws Exception;

}
