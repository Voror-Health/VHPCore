package org.endeavourhealth.core.database.dal.usermanager.caching;


import org.endeavourhealth.common.security.keycloak.client.KeycloakAdminClient;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.usermanager.UserApplicationPolicyDalI;
import org.endeavourhealth.core.database.dal.usermanager.UserProjectDalI;
import org.endeavourhealth.core.database.dal.usermanager.UserRegionDalI;
import org.endeavourhealth.core.database.dal.usermanager.models.JsonApplicationPolicyAttribute;
import org.endeavourhealth.core.database.dal.usermanager.models.JsonUser;
import org.endeavourhealth.core.database.rdbms.usermanager.RdbmsCoreUserProjectDal;
import org.endeavourhealth.core.database.rdbms.usermanager.models.*;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserCache {
    private static final Logger LOG = LoggerFactory.getLogger(UserCache.class);

    private static Map<String, UserRepresentation> userMap = new ConcurrentHashMap<>();
    private static Map<String, String> userApplicationPolicyIdMap = new ConcurrentHashMap<>();
    private static Map<String, UserApplicationPolicyEntity> userApplicationPolicyMap = new ConcurrentHashMap<>();
    private static Map<String, Boolean> userProjectApplicationAccessMap = new ConcurrentHashMap<>();
    private static Map<String, Boolean> externalUserApplicationAccessMap = new ConcurrentHashMap<>();
    private static Map<String, Boolean> userProjectApplicationAttributeAccessMap = new ConcurrentHashMap<>();
    private static Map<String, UserRegionEntity> userRegionMap = new ConcurrentHashMap<>();
    private static Map<String, UserProjectEntity> userProjectMap = new ConcurrentHashMap<>();
    private static Map<String, List<UserProjectEntity>> userProjectUserMap = new ConcurrentHashMap<>();
    private static Map<String, List<JsonApplicationPolicyAttribute>> userProjectMergedAttributeMap = new ConcurrentHashMap<>();

    private static UserApplicationPolicyDalI userAppPolicyRepository = DalProvider.factoryUMUserApplicationPolicyDal();
    private static UserProjectDalI userProjectRepository = DalProvider.factoryUMUserProjectDal();
    private static UserRegionDalI userRegionRepository = DalProvider.factoryUMUserRegionDal();

    public static UserRepresentation getUserDetails(String userId) throws Exception {
        UserRepresentation foundUser;

        foundUser = userMap.get(userId);
        if (foundUser == null) {

            KeycloakAdminClient keycloakClient = new KeycloakAdminClient();

            try {
                UserRepresentation user = keycloakClient.realms().users().getUser(userId);

                //SD-518 - This bug meant it was impossible to edit machine-realm users in User Manager
                //this defaults to the default realm only so just doesn't work for any user in the "machine" realm
                //so as a quick fix, I'm changing it to iterate through all known realms if the original lookup fails
                if (user == null) {
                    LOG.trace("Failed to find user " + userId + " in default realm so checking other realms");
                    List<RealmRepresentation> realms = keycloakClient.realms().getRealms();
                    for (RealmRepresentation realm : realms) {
                        String realmStr = realm.getRealm();
                        LOG.trace("Checking realm [" + realmStr + "]");
                        user = keycloakClient.realms().users().getUser(realmStr, userId);
                        if (user != null) {
                            LOG.trace("Found user " + userId + " in realm " + realmStr);
                            break;
                        }
                    }
                }

                if (user != null) {
                    userMap.put(user.getId(), user);
                    foundUser = user;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }
        }

        CacheManager.startScheduler();

        return foundUser;

    }

    public static List<JsonUser> getAllUsers() throws Exception {

        List<JsonUser> userList = new ArrayList<>();
        List<UserRepresentation> users;
        KeycloakAdminClient keycloakClient = new KeycloakAdminClient();

        try {
            users = keycloakClient.realms().users().getUsers("", 0, 100);

            for (UserRepresentation user : users) {
                userMap.put(user.getId(), user);
                userList.add(new JsonUser(user));
            }
        } catch (Exception e) {

        }

        CacheManager.startScheduler();

        return userList;
    }

    public static String getUserApplicationPolicyId(String userId) throws Exception {

        String foundPolicy = userApplicationPolicyIdMap.get(userId);
        if (foundPolicy == null) {
            UserApplicationPolicyEntity userApp = userAppPolicyRepository.getUserApplicationPolicy(userId);
            foundPolicy = userApp.getApplicationPolicyId();
            userApplicationPolicyIdMap.put(userId, foundPolicy);
        }

        CacheManager.startScheduler();

        return foundPolicy;
    }

    public static UserApplicationPolicyEntity getUserApplicationPolicy(String userId) throws Exception {

        UserApplicationPolicyEntity foundPolicy = userApplicationPolicyMap.get(userId);
        if (foundPolicy == null) {
            UserApplicationPolicyEntity userApp = userAppPolicyRepository.getUserApplicationPolicy(userId);
            if (userApp != null) {
                foundPolicy = userApp;
                userApplicationPolicyMap.put(userId, userApp);
            }
        }

        CacheManager.startScheduler();

        return foundPolicy;
    }

    public static UserProjectEntity getUserProject(String userProjectId) throws Exception {
        UserProjectEntity foundUserProject = userProjectMap.get(userProjectId);

        if (foundUserProject == null) {
            UserProjectEntity userProj = userProjectRepository.getUserProject(userProjectId);
            foundUserProject = userProj;
            userProjectMap.put(userProjectId, userProj);
        }

        CacheManager.startScheduler();

        return foundUserProject;
    }

    public static List<UserProjectEntity> getUserProjectForUserId(String userId) throws Exception {
        List<UserProjectEntity> foundUserProjects = userProjectUserMap.get(userId);

        if (foundUserProjects == null) {
            List<UserProjectEntity> userProj = userProjectRepository.getUserProjectsForUser(userId);
            foundUserProjects = userProj;
            userProjectUserMap.put(userId, userProj);
        }

        CacheManager.startScheduler();

        return foundUserProjects;
    }

    public static UserRegionEntity getUserRegion(String userId) throws Exception {

        UserRegionEntity foundRegion = userRegionMap.get(userId);
        if (foundRegion == null) {
            UserRegionEntity userRegion = userRegionRepository.getUserRegion(userId);
            if (userRegion != null) {
                foundRegion = userRegion;
                userRegionMap.put(userId, userRegion);
            }
        }

        CacheManager.startScheduler();

        return foundRegion;
    }

    public static Boolean getUserProjectApplicationAccess(String userId, String projectId, String appName) throws Exception {
        String upa = userId + "|" + projectId + "|" + appName;

        Boolean accessToApp = userProjectApplicationAccessMap.get(upa);
        if (accessToApp == null) {
            accessToApp = userProjectRepository.checkUserProjectApplicationAccess(userId, projectId, appName);
            userProjectApplicationAccessMap.put(upa, accessToApp);
        }

        CacheManager.startScheduler();

        return accessToApp;
    }

    public static UserApplicationProjectEntity getUserProjectApplicationInfo(String userId, String appName) throws Exception {
        return userProjectRepository.getUserProjectWithAppInfo(userId, appName);

    }


    public static Boolean getUserProjectApplicationAttributeAccess(String userId, String projectId, String appName, String attributeName) throws Exception {
        String upa = userId + "|" + projectId + "|" + appName + "|" + attributeName;

        Boolean accessToApp = userProjectApplicationAttributeAccessMap.get(upa);
        if (accessToApp == null) {
            accessToApp = userProjectRepository.checkUserProjectApplicationAttributeAccess(userId, projectId, appName, attributeName);
            userProjectApplicationAttributeAccessMap.put(upa, accessToApp);
        }

        CacheManager.startScheduler();

        return accessToApp;
    }

    public static List<JsonApplicationPolicyAttribute> getUserProjectMergedAttributes(String userId, String projectId) throws Exception {
        String upa = userId + "|" + projectId;

        List<JsonApplicationPolicyAttribute> mergedAttributes = userProjectMergedAttributeMap.get(upa);
        if (mergedAttributes == null) {
            mergedAttributes = userProjectRepository.getUserProjectsMergedAttributes(userId, projectId);
            userProjectMergedAttributeMap.put(upa, mergedAttributes);
        }

        CacheManager.startScheduler();

        return mergedAttributes;
    }

    public static Boolean getExternalUserApplicationAccess(String userId, String appName) throws Exception {
        String upa = userId + "|" + appName;

        Boolean accessToApp = externalUserApplicationAccessMap.get(upa);
        if (accessToApp == null) {
            accessToApp = userProjectRepository.checkExternalUserApplicationAccess(userId, appName);
            userProjectApplicationAccessMap.put(upa, accessToApp);
        }

        CacheManager.startScheduler();

        return accessToApp;
    }

    public static void clearUserCache(String userId) throws Exception {
        userApplicationPolicyMap.remove(userId);
        userApplicationPolicyIdMap.remove(userId);
        userProjectApplicationAccessMap.remove(userId);
        externalUserApplicationAccessMap.remove(userId);
        userProjectApplicationAttributeAccessMap.remove(userId);
        userMap.remove(userId);
        userRegionMap.remove(userId);
        userProjectMap.clear();
        userProjectUserMap.remove(userId);
    }

    public static void flushCache() throws Exception {
        userMap.clear();
        userApplicationPolicyIdMap.clear();
        userProjectApplicationAccessMap.clear();
        userApplicationPolicyMap.clear();
        userRegionMap.clear();
        userProjectMap.clear();
        externalUserApplicationAccessMap.clear();
        userProjectUserMap.clear();
        userProjectApplicationAttributeAccessMap.clear();
    }
}
