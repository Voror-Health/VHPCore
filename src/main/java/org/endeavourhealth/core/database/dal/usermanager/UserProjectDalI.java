package org.endeavourhealth.core.database.dal.usermanager;

import org.endeavourhealth.core.database.dal.usermanager.models.JsonApplicationPolicyAttribute;
import org.endeavourhealth.core.database.rdbms.usermanager.models.UserApplicationProjectEntity;
import org.endeavourhealth.core.database.rdbms.usermanager.models.UserProjectEntity;

import java.util.List;

public interface UserProjectDalI {

    public Boolean checkUserProjectApplicationAccess(String userId,
                                                     String projectId,
                                                     String applicationName) throws Exception;
    public Boolean checkUserProjectApplicationAttributeAccess(String userId,
                                                     String projectId,
                                                     String applicationName, String attributeName) throws Exception;
    public Boolean checkExternalUserApplicationAccess(String userId,
                                                     String applicationPolicyName) throws Exception;

    public List<UserProjectEntity> getUserProjectEntitiesForProject(String projectId) throws Exception;
    public List<Object[]> getUserProjects(String userId) throws Exception;
    public void setCurrentDefaultProject(String userId, String userProjectId) throws Exception;
    public void changeDefaultProject(String userId, String defaultRoleId, String userProjectId) throws Exception;
    public void removeCurrentDefaultProject(String userId) throws Exception;
    public UserProjectEntity getDefaultProject(String userId) throws Exception;
    public UserProjectEntity getUserProject(String userProjectId) throws Exception;
    public List<UserProjectEntity> getUserProjectEntities(String userId) throws Exception;
    public List<UserProjectEntity> getUserProjectsForUser(String userId) throws Exception;
    public UserApplicationProjectEntity getUserProjectWithAppInfo(String userId, String appName) throws Exception;
    public List<JsonApplicationPolicyAttribute> getUserProjectsMergedAttributes(String userId, String projectId) throws Exception;

}
