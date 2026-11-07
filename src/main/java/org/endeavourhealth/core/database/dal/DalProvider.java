package org.endeavourhealth.core.database.dal;

import org.endeavourhealth.core.database.dal.admin.LibraryDalI;
import org.endeavourhealth.core.database.dal.admin.LinkDistributorPopulatorDalI;
import org.endeavourhealth.core.database.dal.admin.LinkDistributorTaskListDalI;
import org.endeavourhealth.core.database.dal.admin.ServiceDalI;
import org.endeavourhealth.core.database.dal.audit.*;
import org.endeavourhealth.core.database.dal.audit.models.IAuditModule;
import org.endeavourhealth.core.database.dal.datagenerator.SubscriberZipFileUUIDsDalI;
import org.endeavourhealth.core.database.dal.datasharingmanager.*;
import org.endeavourhealth.core.database.dal.eds.PatientLinkDalI;
import org.endeavourhealth.core.database.dal.eds.PatientSearchDalI;
import org.endeavourhealth.core.database.dal.ehr.CoreFilerDalI;
import org.endeavourhealth.core.database.dal.ehr.ResourceDalI;
import org.endeavourhealth.core.database.dal.hl7receiver.Hl7ResourceIdDalI;
import org.endeavourhealth.core.database.dal.informationmodel.EmisClinicalCodesIMUpdaterDalI;
import org.endeavourhealth.core.database.dal.informationmodel.TppClinicalCodesIMUpdaterDalI;
import org.endeavourhealth.core.database.dal.informationmodel.VisionClinicalCodesIMUpdaterDalI;
import org.endeavourhealth.core.database.dal.jdbcreader.JDBCReaderDalI;
import org.endeavourhealth.core.database.dal.logback.LogbackDalI;
import org.endeavourhealth.core.database.dal.publisherCommon.*;
import org.endeavourhealth.core.database.dal.publisherStaging.*;
import org.endeavourhealth.core.database.dal.publisherTransform.*;
import org.endeavourhealth.core.database.dal.reference.*;
import org.endeavourhealth.core.database.dal.subscriberTransform.*;
import org.endeavourhealth.core.database.dal.usermanager.*;
import org.endeavourhealth.core.database.rdbms.admin.RdbmsLibraryDal;
import org.endeavourhealth.core.database.rdbms.admin.RdbmsLinkDistributorPopulatorDal;
import org.endeavourhealth.core.database.rdbms.admin.RdbmsLinkDistributorTaskListDal;
import org.endeavourhealth.core.database.rdbms.admin.RdbmsServiceDal;
import org.endeavourhealth.core.database.rdbms.audit.*;
import org.endeavourhealth.core.database.rdbms.datagenerator.RdbmsSubscriberZipFileUUIDsDal;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.*;
import org.endeavourhealth.core.database.rdbms.eds.RdbmsPatientLinkDal;
import org.endeavourhealth.core.database.rdbms.eds.RdbmsPatientSearchDal;
import org.endeavourhealth.core.database.rdbms.ehr.RdbmsCoreFilerDal;
import org.endeavourhealth.core.database.rdbms.ehr.RdbmsResourceDal;
import org.endeavourhealth.core.database.rdbms.hl7receiver.RdbmsHl7ResourceIdDal;
import org.endeavourhealth.core.database.rdbms.informationmodel.RdbmsEmisClinicalCodesIMUpdaterDal;
import org.endeavourhealth.core.database.rdbms.informationmodel.RdbmsTppClinicalCodesIMUpdaterDal;
import org.endeavourhealth.core.database.rdbms.informationmodel.RdbmsVisionClinicalCodesIMUpdaterDal;
import org.endeavourhealth.core.database.rdbms.jdbcreader.RdbmsJDBCReaderDal;
import org.endeavourhealth.core.database.rdbms.logback.RdbmsLogbackDal;
import org.endeavourhealth.core.database.rdbms.publisherCommon.*;
import org.endeavourhealth.core.database.rdbms.publisherStaging.*;
import org.endeavourhealth.core.database.rdbms.publisherTransform.*;
import org.endeavourhealth.core.database.rdbms.reference.*;
import org.endeavourhealth.core.database.rdbms.subscriberTransform.*;
import org.endeavourhealth.core.database.rdbms.usermanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DalProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DalProvider.class);

    /*private static Boolean cachedUseCassandra = null;
    private static final Object sync = new Object();*/

    public static ResourceDalI factoryResourceDal() {
        return new RdbmsResourceDal();
    }

    /*public static VitruCareTransformDalI factoryVitruCareTransformDal(String subscriberConfigName) {
        return new RdbmsVitruCareTransformDal(subscriberConfigName);
    }*/

    public static EmisAdminCacheDalI factoryEmisTransformDal() {
        return new RdbmsEmisAdminCacheDal();
    }

    public static TppCtv3LookupDalI factoryTppCtv3LookupDal() {
        return new RdbmsTppCtv3LookupDal();
    }

    public static ResourceIdTransformDalI factoryResourceIdTransformDal() {
        return new RdbmsResourceIdDal();
    }

    public static QueuedMessageDalI factoryQueuedMessageDal() {
        return new RdbmsQueuedMessageDal();
    }

    public static ExchangeBatchDalI factoryExchangeBatchDal() {
        return new RdbmsExchangeBatchDal();
    }

    public static ExchangeGeneralErrorDalI factoryExchangeGeneralErrorDal() {
        return new RdbmsExchangeGeneralErrorDal();
    }

    public static ExchangeProtocolErrorDalI factoryExchangeProtocolErrorDal() {
        return new RdbmsExchangeProtocolErrorDal();
    }

    public static ExchangeDalI factoryExchangeDal() {
        return new RdbmsExchangeDal();
    }

    public static UserAuditDalI factoryUserAuditDal(IAuditModule auditModule) {
        return new RdbmsUserAuditDal(auditModule);
    }

    public static SusResourceMapDalI factoryBartsSusResourceMapDal() {
        return new RdbmsSusResourceMapDal();
    }

    public static CernerCodeValueRefDalI factoryCernerCodeValueRefDal() {
        return new RdbmsCernerCodeValueRefDal();
    }

    public static TppMappingRefDalI factoryTppMappingRefDal() {
        return new RdbmsTppMappingRefDal();
    }

    public static TppImmunisationContentDalI factoryTppImmunisationContentDal() {
        return new RdbmsTppImmunisationContentDal();
    }

    public static TppConfigListOptionDalI factoryTppConfigListOptionDal() {
        return new RdbmsTppConfigListOptionDal();
    }

    public static JDBCReaderDalI factoryJDBCReaderDal() {
        return new RdbmsJDBCReaderDal();
    }

    public static ServiceDalI factoryServiceDal() {
        return new RdbmsServiceDal();
    }

    public static LibraryDalI factoryLibraryDal() {
        return new RdbmsLibraryDal();
    }

    public static SnomedDalI factorySnomedDal() {
        return new RdbmsSnomedDal();
    }

    public static LinkDistributorTaskListDalI factoryLinkDistributorTaskListDal() {
        return new RdbmsLinkDistributorTaskListDal();
    }

    public static LinkDistributorPopulatorDalI factoryLinkDistributorPopulatorDal() {
        return new RdbmsLinkDistributorPopulatorDal();
    }

    public static TransformWarningDalI factoryTransformWarningDal() {
        return new RdbmsTransformWarningDal();
    }

    public static TppMultilexLookupDalI factoryTppMultiLexDal() {
        return new RdbmsTppMultilexLookupDal();
    }

    public static TppCtv3HierarchyRefDalI factoryTppCtv3HierarchyRefDal() {
        return new RdbmsTppCtv3HierarchyRefDal();
    }

    public static CernerClinicalEventMappingDalI factoryCernerClinicalEventMappingDal() {
        return new RdbmsCernerClinicalEventMappingDal();
    }

    public static SubscriberApiAuditDalI factorySubscriberAuditApiDal() {
        return new RdbmsSubscriberApiAuditDal();
    }

    public static CernerProcedureMapDalI factoryCernerProcedureMapDal() {
        return new CernerProcedureMapDal();
    }

    public static CohortDalI factoryDSMCohortDal() {
        return new RdbmsCoreCohortDal();
    }

    public static DataProcessingAgreementDalI factoryDSMDataProcessingAgreementDal() {
        return new RdbmsCoreDataProcessingAgreementDal();
    }

    public static DataSetDalI factoryDSMDataSetDal() {
        return new RdbmsCoreDataSetDal();
    }

    public static DataSharingAgreementDalI factoryDSMDataSharingAgreementDal() {
        return new RdbmsCoreDataSharingAgreementDal();
    }

    public static DocumentationDalI factoryDSMDocumentationDal() {
        return new RdbmsCoreDocumentationDal();
    }

    public static ExtractTechnicalDetailsDalI factoryDSMExtractTechnicalDetailsDal() {
        return new RdbmsCoreExtractTechnicalDetailsDal();
    }

    public static MasterMappingDalI factoryDSMMasterMappingDal() {
        return new RdbmsCoreMasterMappingDal();
    }

    public static OrganisationDalI factoryDSMOrganisationDal() {
        return new RdbmsCoreOrganisationDal();
    }

    public static ProjectApplicationPolicyDalI factoryDSMProjectApplicationPolicyDal() {
        return new RdbmsCoreProjectApplicationPolicyDal();
    }

    public static ProjectDalI factoryDSMProjectDal() {
        return new RdbmsCoreProjectDal();
    }

    public static ProjectScheduleDalI factoryDSMProjectScheduleDal() {
        return new RdbmsCoreProjectScheduleDal();
    }

    public static PurposeDalI factoryDSMPurposeDal() {
        return new RdbmsCorePurposeDal();
    }

    public static RegionDalI factoryDSMRegionDal() {
        return new RdbmsCoreRegionDal();
    }

    public static ValueSetsDalI factoryDSMValueSetsDal() {
        return new RdbmsCoreValueSetsDal();
    }

    public static ApplicationAccessProfileDalI factoryUMApplicationAccessProfileDal() {
        return new RdbmsCoreApplicationAccessProfileDal();
    }

    public static ApplicationDalI factoryUMApplicationDal() {
        return new RdbmsCoreApplicationDal();
    }

    public static ApplicationPolicyAttributeDalI factoryUMApplicationPolicyAttributeDal() {
        return new RdbmsCoreApplicationPolicyAttributeDal();
    }

    public static ApplicationPolicyDalI factoryUMApplicationPolicyDal() {
        return new RdbmsCoreApplicationPolicyDal();
    }

    public static DelegationDalI factoryUMDelegationDal() {
        return new RdbmsCoreDelegationDal();
    }

    public static DelegationRelationshipDalI factoryUMDelegationRelationshipDal() {
        return new RdbmsCoreDelegationRelationshipDal();
    }

    public static UserApplicationPolicyDalI factoryUMUserApplicationPolicyDal() {
        return new RdbmsCoreUserApplicationPolicyDal();
    }

    public static UserProjectDalI factoryUMUserProjectDal() {
        return new RdbmsCoreUserProjectDal();
    }

    public static UserRegionDalI factoryUMUserRegionDal() {
        return new RdbmsCoreUserRegionDal();
    }

    /*public static ResourceDalI factoryResourceDal() {
        if (useCassandra()) {
            return new CassandraResourceRepository();

        } else {
            return new RdbmsResourceDal();
        }
    }

    public static VitruCareTransformDalI factoryVitruCareTransformDal(String subscriberConfigName) {
        if (useCassandra()) {
            return new CassandraVitruCareRepository();

        } else {
            return new RdbmsVitruCareTransformDal(subscriberConfigName);
        }
    }

    public static EmisTransformDalI factoryEmisTransformDal() {
        if (useCassandra()) {
            return new CassandraEmisRepository();

        } else {
            return new RdbmsEmisTransformDal();
        }
    }

    public static ResourceIdTransformDalI factoryResourceIdTransformDal() {
        if (useCassandra()) {
            return new CassandraResourceIdMapRepository();

        } else {
            return new RdbmsResourceIdDal();
        }
    }

    public static QueuedMessageDalI factoryQueuedMessageDal() {
        if (useCassandra()) {
            return new CassandraQueuedMessageRepository();

        } else {
            return new RdbmsQueuedMessageDal();
        }
    }

    public static ExchangeBatchDalI factoryExchangeBatchDal() {
        if (useCassandra()) {
            return new CassandraExchangeBatchRepository();

        } else {
            return new RdbmsExchangeBatchDal();
        }
    }

    public static ExchangeDalI factoryExchangeDal() {
        if (useCassandra()) {
            return new CassandraExchangeRespository();

        } else {
            return new RdbmsExchangeDal();
        }
    }

    public static UserAuditDalI factoryUserAuditDal(IAuditModule auditModule) {
        if (useCassandra()) {
            return new CassandraUserAuditRepository(auditModule);

        } else {
            return new RdbmsUserAuditDal(auditModule);
        }
    }

    public static RdbmsBartsSusResourceMapDal factoryBartsSusResourceMapDal() {
        if (useCassandra()) {
            return null;
        } else {
            return new RdbmsBartsSusResourceMapDal();
        }
    }

    public static RdbmsCernerCodeValueRefDal factoryCernerCodeValueRefDal() {
        if (useCassandra()) {
            return null;
        } else {
            return new RdbmsCernerCodeValueRefDal();
        }
    }

    public static RdbmsJDBCReaderDal factoryJDBCReaderDal() {
        if (useCassandra()) {
            return null;
        } else {
            return new RdbmsJDBCReaderDal();
        }
    }

    public static ServiceDalI factoryServiceDal() {
        if (useCassandra()) {
            return new CassandraServiceRepository();

        } else {
            return new RdbmsServiceDal();
        }
    }

    public static OrganisationDalI factoryOrganisationDal() {
        if (useCassandra()) {
            return new CassandraOrganisationRepository();

        } else {
            return new RdbmsOrganisationDal();
        }
    }

    public static LibraryDalI factoryLibraryDal() {
        if (useCassandra()) {
            return new CassandraLibraryRepository();

        } else {
            return new RdbmsLibraryDal();
        }
    }

    public static SnomedDalI factorySnomedDal() {
        if (useCassandra()) {
            return new CassandraSnomedRepository();

        } else {
            return new RdbmsSnomedDal();
        }
    }

    public static PatientCohortDalI factoryPatientCohortDal() {
        if (useCassandra()) {
            return new CassandraPatientCohortRepository();

        } else {
            return new RdbmsPatientCohortDal();
        }
    }*/


    public static Read2ToSnomedMapDalI factoryRead2ToSnomedMapDal() {
        return new RdbmsRead2ToSnomedMapDal();
    }

    public static CTV3ToSnomedMapDalI factoryCTV3ToSnomedMapDal() {
        return new RdbmsCTV3ToSnomedMapDal();
    }

    public static PatientLinkDalI factoryPatientLinkDal() {
        return new RdbmsPatientLinkDal();
    }

    public static PatientSearchDalI factoryPatientSearchDal() {
        return new RdbmsPatientSearchDal();
    }

    public static EncounterCodeDalI factoryEncounterCodeDal() {
        return new RdbmsEncounterCodeDal();
    }

    public static PseudoIdDalI factoryPseudoIdDal(String subscriberConfigName) {
        return new RdbmsPseudoIdDal(subscriberConfigName);
    }

    public static EnterprisePersonUpdaterHistoryDalI factoryEnterprisePersonUpdateHistoryDal(String subscriberConfigName) {
        return new RdbmsEnterprisePersonUpdaterHistoryDal(subscriberConfigName);
    }

    public static EnterpriseAgeUpdaterlDalI factoryEnterpriseAgeUpdaterlDal(String subscriberConfigName) {
        return new RdbmsEnterpriseAgeUpdaterDal(subscriberConfigName);
    }

    /*public static PcrPersonUpdaterHistoryDalI factoryPcrPersonUpdateHistoryDal(String subscriberConfigName) {
        return new RdbmsPcrPersonUpdaterHistoryDal(subscriberConfigName);
    }

    public static PcrIdDalI factoryPcrIdDal(String subscriberConfigName) {
        return new RdbmsPcrIdDal(subscriberConfigName);
    }

    public static PcrAgeUpdaterlDalI factoryPcrAgeUpdaterlDal(String subscriberConfigName) {
        return new RdbmsPcrAgeUpdaterDal(subscriberConfigName);
    }*/

    public static PostcodeDalI factoryPostcodeDal() {
        return new RdbmsPostcodeDal();
    }

    public static ReferenceUpdaterDalI factoryReferenceUpdaterDal() {
        return new RdbmsReferenceUpdaterDal();
    }

    public static ReferenceCopierDalI factoryReferenceCopierDal() {
        return new RdbmsReferenceCopierDal();
    }

    public static LogbackDalI factoryLogbackDal() {
        return new RdbmsLogbackDal();
    }

    public static CodingDalI factoryCodingDal() {
        return new RdbmsCodingDal();
    }

    public static ExchangeBatchExtraResourceDalI factoryExchangeBatchExtraResourceDal(String subscriberConfigName) {
        return new RdbmsExchangeBatchExtraResourcesDal(subscriberConfigName);
    }

    public static ResourceMergeDalI factoryResourceMergeDal() {
        return new RdbmsResourceMergeDal();
    }

    public static InternalIdDalI factoryInternalIdDal() {
        return new RdbmsInternalIdDal();
    }

    public static Hl7ResourceIdDalI factoryHL7ResourceDal() {
        return new RdbmsHl7ResourceIdDal();
    }

    public static SourceFileMappingDalI factorySourceFileMappingDal() {
        return new RdbmsSourceFileMappingDal();
    }

    public static Opcs4DalI factoryOpcs4Dal() {
        return new RdbmsOpcs4Dal();
    }

    public static SnomedToBnfChapterDalI factorySnomedToBnfChapter() {
        return new RdbmsSnomedToBnfChapterDal();
    }

    public static Lsoa2001DalI factoryLsoa2001() {
        return new RdbmsLsoa2001Dal();
    }

    public static Lsoa2011DalI factoryLsoa2011() {
        return new RdbmsLsoa2011Dal();
    }

    public static Msoa2001DalI factoryMsoa2001() {
        return new RdbmsMsoa2001Dal();
    }

    public static Msoa2011DalI factoryMsoa2011() {
        return new RdbmsMsoa2011Dal();
    }

    public static SubscriberZipFileUUIDsDalI factorySubscriberZipFileUUIDs() {
        return new RdbmsSubscriberZipFileUUIDsDal();
    }

    public static Icd10DalI factoryIcd10Dal() {
        return new RdbmsIcd10Dal();
    }

    public static PublishedFileDalI factoryPublishedFileDal() {
        return new RdbmsPublishedFileDal();
    }

    public static StagingCdsDalI factoryStagingCdsDalI() {
        return new RdbmsStagingCdsDal();
    }

    public static StagingCdsTailDalI factoryStagingCdsTailDalI() {
        return new RdbmsStagingCdsTailDal();
    }

    public static StagingProcedureDalI factoryBartsStagingProcedureDalI() {
        return new RdbmsStagingProcedureDal();
    }

    public static StagingPROCEDalI factoryBartsStagingPROCEDalI() {
        return new RdbmsStagingPROCEDal();
    }

    public static StagingSURCCDalI factoryStagingSURCCDalI() {
        return new RdbmsStagingSURCCDal();
    }

    public static StagingSURCPDalI factoryStagingSURCPDalI() {
        return new RdbmsStagingSURCPDal();
    }

    public static StagingDiagnosisDalI factoryBartsStagingDiagnosisDalI() {
        return new RdbmsStagingDiagnosisDal();
    }

    public static StagingDIAGNDalI factoryBartsStagingDIAGNDalI() {
        return new RdbmsStagingDIAGNDal();
    }

    public static StagingDIAGNDalI factoryNwlExtractsStagingDIAGNDalI() {
        return new RdbmsStagingDIAGNDal();
    }

    public static StagingProblemDalI factoryBartsStagingProblemDalI() {
        return new RdbmsStagingProblemDal();
    }

    public static StagingTargetDalI factoryStagingTargetDalI() {
        return new RdbmsStagingTargetDal();
    }

    public static StagingClinicalEventDalI factoryBartsStagingClinicalEventDalI() {
        return new RdbmsStagingClinicalEventsDal();
    }

    public static StagingClinicalEventDalI factoryNwlExtractsStagingClinicalEventDalI() {
        return new RdbmsStagingClinicalEventsDal();
    }

    public static SubscriberInstanceMappingDalI factorySubscriberInstanceMappingDal(String subscriberConfigName) {
        return new RdbmsSubscriberInstanceMappingDalI(subscriberConfigName);
    }

    public static SubscriberOrgMappingDalI factorySubscriberOrgMappingDal(String subscriberConfigName) {
        return new RdbmsSubscriberOrgMappingDalI(subscriberConfigName);
    }

    public static SubscriberPersonMappingDalI factorySubscriberPersonMappingDal(String subscriberConfigName) {
        return new RdbmsSubscriberPersonMappingDal(subscriberConfigName);
    }

    public static SubscriberResourceMappingDalI factorySubscriberResourceMappingDal(String subscriberConfigName) {
        return new RdbmsSubscriberResourceMappingDal(subscriberConfigName);
    }

    public static ApplicationHeartbeatDalI factoryApplicationHeartbeatDal() {
        return new RdbmsApplicationHeartbeatDal();
    }

    public static CoreFilerDalI factoryCoreFilerDal() {
        return new RdbmsCoreFilerDal();
    }

    public static TppStaffDalI factoryTppStaffMemberDal() {
        return new RdbmsTppStaffDal();
    }

    public static EmisLocationDalI factoryEmisLocationDal() {
        return new RdbmsEmisLocationDal();
    }

    public static EmisOrganisationDalI factoryEmisOrganisationDal() {
        return new RdbmsEmisOrganisationDal();
    }

    public static EmisUserInRoleDalI factoryEmisUserInRoleDal() {
        return new RdbmsEmisUserInRoleDal();
    }

    public static EmisMissingCodeDalI factoryEmisMissingCodeDal() {
        return new RdbmsEmisMissingCodeDal();
    }

    public static EmisCodeDalI factoryEmisCodeDal() {
        return new RdbmsEmisCodeDal();
    }

    public static SubscriberPatientDateDalI factorySubscriberDateDal() {
        return new RdbmsSubscriberPatientDateDal();
    }

    public static SubscriberCohortDalI factorySubscriberCohortDal() {
        return new RdbmsSubscriberCohortDal();
    }

    public static VisionCodeDalI factoryVisionCodeDal() {
        return new RdbmsVisionCodeDal();
    }

    public static TppCtv3SnomedRefDalI factoryTppCtv3SnomedRefDal() {
        return new RdbmsTppCtv3SnomedRefDal();
    }

    public static ServiceSubscriberAuditDalI factoryServiceSubscriberAuditDal() {
        return new RdbmsServiceSubscriberAuditDal();
    }

    public static ServicePublisherAuditDalI factoryServicePublisherAuditDal() {
        return new RdbmsPublisherAuditDal();
    }

    public static ScheduledTaskAuditDalI factoryScheduledTaskAuditDal() {
        return new RdbmsScheduledTaskAuditDal();
    }

    public static SimplePropertyDalI factorySimplePropertyDal() {
        return new RdbmsSimplePropertyDal();
    }

    public static EmisClinicalCodesIMUpdaterDalI factoryEmisClinicalCodesIMUpdaterDal() {
        return new RdbmsEmisClinicalCodesIMUpdaterDal();
    }

    public static TppClinicalCodesIMUpdaterDalI factoryTppClinicalCodesIMUpdaterDal() {
        return new RdbmsTppClinicalCodesIMUpdaterDal();
    }

    public static VisionClinicalCodesIMUpdaterDalI factoryVisionClinicalCodesIMUpdaterDal() {
        return new RdbmsVisionClinicalCodesIMUpdaterDal();
    }

    public static LastDataDalI factoryLastDataDal() {
        return new RdbmsLastDataDal();
    }
}
