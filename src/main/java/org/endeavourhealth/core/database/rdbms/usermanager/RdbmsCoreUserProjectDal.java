package org.endeavourhealth.core.database.rdbms.usermanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.endeavourhealth.core.database.dal.usermanager.UserProjectDalI;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.OrganisationEntity;
import org.endeavourhealth.core.database.rdbms.datasharingmanager.models.ProjectEntity;
import org.endeavourhealth.core.database.dal.usermanager.caching.ApplicationPolicyCache;
import org.endeavourhealth.core.database.dal.usermanager.caching.OrganisationCache;
import org.endeavourhealth.core.database.dal.usermanager.caching.ProjectCache;
import org.endeavourhealth.core.database.dal.usermanager.caching.UserCache;
import org.endeavourhealth.core.database.rdbms.usermanager.models.*;
import org.endeavourhealth.core.database.dal.usermanager.models.JsonApplicationPolicyAttribute;
import org.keycloak.representations.idm.UserRepresentation;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public class RdbmsCoreUserProjectDal implements UserProjectDalI {

    public Boolean checkUserProjectApplicationAccess(String userId,
                                                     String projectId,
                                                     String applicationName) throws Exception {

        List<JsonApplicationPolicyAttribute> mergedAttributes = getMergedAttributes(userId, projectId);

        if (mergedAttributes.stream().anyMatch(a -> a.getApplication().equals(applicationName))) {
            return true;
        }

        return false;
    }

    public Boolean checkUserProjectApplicationAttributeAccess(String userId,
                                                              String projectId,
                                                              String applicationName, String attributeName) throws Exception {

        List<JsonApplicationPolicyAttribute> mergedAttributes = getMergedAttributes(userId, projectId);

        java.util.function.Predicate<JsonApplicationPolicyAttribute> appName = a -> a.getApplication().equals(applicationName);
        java.util.function.Predicate<JsonApplicationPolicyAttribute> attName = a -> a.getApplicationAccessProfileName().equals(attributeName);

        if (mergedAttributes.stream().anyMatch(a -> appName.and(attName).test(a))) {
            return true;
        }

        return false;
    }

    private List<JsonApplicationPolicyAttribute> getMergedAttributes(String userId, String projectId) throws Exception {
        List<JsonApplicationPolicyAttribute> mergedAttributes = new ArrayList<>();

        String userAppPolicy = UserCache.getUserApplicationPolicyId(userId);
        String projectAppPolicy = ProjectCache.getProjectApplicationPolicy(projectId);

        if (userAppPolicy.equals(projectAppPolicy)) {
            // policies are the same so just look through one of them
            mergedAttributes = ApplicationPolicyCache.getApplicationPolicyAttributes(userAppPolicy);
        } else {
            List<JsonApplicationPolicyAttribute> userAttributes = ApplicationPolicyCache.getApplicationPolicyAttributes(userAppPolicy);
            List<JsonApplicationPolicyAttribute> projectAttributes = ApplicationPolicyCache.getApplicationPolicyAttributes(projectAppPolicy);

            for (JsonApplicationPolicyAttribute attribute : projectAttributes) {
                if (userAttributes.stream().filter(a -> a.getApplicationAccessProfileId().equals(attribute.getApplicationAccessProfileId())).findFirst().isPresent()) {
                    //user policy has the project attribute so add it to the list
                    mergedAttributes.add(attribute);
                }
            }
        }

        return mergedAttributes;
    }

    @Override
    public Boolean checkExternalUserApplicationAccess(String userId, String applicationName) throws Exception {

        String userAppPolicy = UserCache.getUserApplicationPolicyId(userId);
        List<JsonApplicationPolicyAttribute> attributeList = ApplicationPolicyCache.getApplicationPolicyAttributes(userAppPolicy);

        if (attributeList.stream().anyMatch(a -> a.getApplication().equals(applicationName))) {
            return true;
        }

        return false;
    }

    public List<UserProjectEntity> getUserProjectEntitiesForProject(String projectId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            String sql = "select up" +
                    " from UserProjectEntity up" +
                    " where up.projectId = :projectId" +
                    " and up.isDeleted = 0";

            Query query = entityManager.createQuery(sql);

            query.setParameter("projectId", projectId);

            List<UserProjectEntity> results = query.getResultList();

            return results;
        } finally {
            entityManager.close();
        }

    }

    public List<Object[]> getUserProjects(String userId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            String sql = "select " +
                    " up.id," +
                    " up.userId," +
                    " up.projectId," +
                    " up.organisationId," +
                    " up.isDeleted," +
                    " up.isDefault" +
                    " from UserProjectEntity up" +
                    " where up.userId = :userId" +
                    " and up.isDeleted = 0";

            Query query = entityManager.createQuery(sql);

            query.setParameter("userId", userId);

            List<Object[]> results = query.getResultList();

            return results;
        } finally {
            entityManager.close();
        }

    }

    public List<UserProjectEntity> getUserProjectsForUser(String userId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            String sql = "select up" +
                    " from UserProjectEntity up" +
                    " where up.userId = :userId" +
                    " and up.isDeleted = 0";

            Query query = entityManager.createQuery(sql);

            query.setParameter("userId", userId);

            List<UserProjectEntity> results = query.getResultList();

            return results;
        } finally {
            entityManager.close();
        }

    }

    public UserApplicationProjectEntity getUserProjectWithAppInfo(String userId, String appName) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            boolean hasAccessToApplication = false;
            String applicationProfileId = "";
            String applicationPolicyId = "";
            String applicationId = "";

            UserApplicationProjectEntity retVal = new UserApplicationProjectEntity();
            retVal.setAppName(appName);
            retVal.setUserId(userId);


            String sql = "Select uap from UserApplicationPolicyEntity uap where " +
                    "uap.userId = :userId";
            Query query0 = entityManager.createQuery(sql);
            query0.setParameter("userId", userId);
            List<UserApplicationPolicyEntity> res0 =
                    query0.getResultList();
            if (res0.size() > 0) {
                applicationPolicyId = res0.get(0).getApplicationPolicyId();

            } else
                throw new NullPointerException("no entries with given userId: \"" + userId + "\" from the user application policy entity");

            sql = "select application from ApplicationEntity application where " +
                    "application.name = :name and isDeleted = 0";
            Query query1 = entityManager.createQuery(sql);
            String param = "name";
            query1.setParameter(param, appName);
            List<ApplicationEntity> res1 = query1.getResultList();
            if (res1.size() > 0) {
                applicationId = res1.get(0).getId();
                retVal.setAppId(applicationId);
            } else
                throw new NullPointerException("no entries with given application name \"" + appName + "\" from the Application entity");

            sql = "select applicationAccessProfile from ApplicationAccessProfileEntity applicationAccessProfile " +
                    "where applicationAccessProfile.applicationId = :appId and " +
                    " name = :appName and " +
                    "applicationAccessProfile.isDeleted = 0";
            param = "appId";
            Query query2 = entityManager.createQuery(sql);
            query2.setParameter(param, applicationId);
            param = "appName";
            query2.setParameter(param, appName);
            List<ApplicationAccessProfileEntity> res2 = query2.getResultList();
            if (res2.size() > 0) {
                applicationProfileId = res2.get(0).getId();
            } else
                throw new NullPointerException("no entries with given applicationId \"" + applicationId + "\" in the application access profile entity");


            sql = "select apa from ApplicationPolicyAttributeEntity apa where " +
                    "apa.applicationPolicyId = :applicationPolicyId " +
                    "and applicationAccessProfileId = :appAccessProfileId " +
                    "and isDeleted = 0";
            Query query3 = entityManager.createQuery(sql);
            param = "applicationPolicyId";
            query3.setParameter(param, applicationPolicyId);
            param = "appAccessProfileId";
            query3.setParameter(param, applicationProfileId);
            List<ApplicationPolicyAttributeEntity> res3 = query3.getResultList();
            if (res3.size() > 0) {
                hasAccessToApplication = true;
            } else
                throw new NullPointerException("no entries found in the application polict attribute entity for application policy id \"" +
                        applicationPolicyId + "\" and application access profile id \"" + applicationProfileId + "\" " +
                        "you may not have authorisation");


            if (hasAccessToApplication) {
                sql = "select project from UserProjectEntity project where " +
                        "project.userId = :userId and project.isDeleted = 0 " +
                        "and project.isDefault = 1";
                Query query4 = entityManager.createQuery(sql);
                param = "userId";
                query4.setParameter(param, userId);
                List<UserProjectEntity> res4 = query4.getResultList();
                if (res4.size() > 0) {
                    retVal.setProjectId(res4.get(0).getProjectId());
                    retVal.setUserProjectId(res4.get(0).getId());
                    retVal.setOrganisationId(res4.get(0).getId());
                    retVal.setIsDefault(res4.get(0).getIsDefault());
                    byte isDel = 0;
                    retVal.setIsDeleted(isDel);
                } else throw new NullPointerException("no projects found for user id \"" + userId + "\"");

            }
//
//            sql = "SELECT ApplicationEntity.id, ApplicationEntity.name, " +
//                    "UserProjectEntity.id, UserProjectEntity.userId, " +
//                    "UserProjectEntity.projectId, " +
//                    "UserProjectEntity.organisationId, " +
//                    "UserProjectEntity.isDeleted, UserProjectEntity.isDefault " +
//                    "from UserApplicationPolicyEntity uap " +
//
//                    "inner join ApplicationPolicyAttributeEntity apa on apa.applicationPolicyId = " +
//                    "uap.applicationPolicyId and apa.isDeleted = 0 " +
//                    "inner join ApplicationAccessProfileEntity aap on aap.id = apa.applicationAccessProfileId " +
//                    "inner join ApplicationEntity ae on ae.id = aap.applicationId " +
//                    "inner join UserProjectEntity up on up.userId = uap.userId " +
//                    "where uap.userId = :userId and up.isDeleted = 0";
//
//            Query query = entityManager.createQuery(sql);
//
//            query.setParameter("userId", userId);
//
//            List<UserApplicationProjectEntity> results = query.getResultList();

            return retVal;
        } finally {
            entityManager.close();
        }

    }

    @Override
    public List<JsonApplicationPolicyAttribute> getUserProjectsMergedAttributes(String userId, String projectId) throws Exception {

        List<JsonApplicationPolicyAttribute> mergedAttributes = getMergedAttributes(userId, projectId);

        return mergedAttributes;
    }

    public void setCurrentDefaultProject(String userId, String userProjectId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            entityManager.getTransaction().begin();
            String sql = "update UserProjectEntity up" +
                    " set up.isDefault = 1 " +
                    " where up.userId = :user" +
                    " and up.id = :projectId";

            Query query = entityManager.createQuery(sql)
                    .setParameter("user", userId)
                    .setParameter("projectId", userProjectId);

            query.executeUpdate();
            entityManager.getTransaction().commit();

            UserCache.clearUserCache(userId);


        } finally {
            entityManager.close();
        }
    }

    public void changeDefaultProject(String userId, String defaultRoleId, String userProjectId) throws Exception {
        UserProjectEntity oldDefaultProject = getDefaultProject(userId);
        UserProjectEntity newDefaultRole = getUserProject(defaultRoleId);

        removeCurrentDefaultProject(userId);

        setCurrentDefaultProject(userId, defaultRoleId);

        String auditJson = getAuditJsonForDefaultRoleChange(oldDefaultProject, newDefaultRole);

        UserCache.clearUserCache(userId);

    }

    public void removeCurrentDefaultProject(String userId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            entityManager.getTransaction().begin();
            String sql = "update UserProjectEntity up" +
                    " set up.isDefault = 0 " +
                    " where up.userId = :user";

            Query query = entityManager.createQuery(sql)
                    .setParameter("user", userId);

            query.executeUpdate();
            entityManager.getTransaction().commit();

            UserCache.clearUserCache(userId);


        } finally {
            entityManager.close();
        }
    }

    public UserProjectEntity getDefaultProject(String userId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<UserProjectEntity> cq = cb.createQuery(UserProjectEntity.class);
            Root<UserProjectEntity> rootEntry = cq.from(UserProjectEntity.class);

            Predicate predicate = cb.and(cb.equal(rootEntry.get("userId"), userId),
                    (cb.equal(rootEntry.get("isDefault"), 1)),
                    (cb.equal(rootEntry.get("isDeleted"), 0)));

            cq.where(predicate);
            TypedQuery<UserProjectEntity> query = entityManager.createQuery(cq);
            UserProjectEntity ret = query.getSingleResult();

            return ret;
        } finally {
            entityManager.close();
        }
    }

    public UserProjectEntity getUserProject(String userProjectId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<UserProjectEntity> cq = cb.createQuery(UserProjectEntity.class);
            Root<UserProjectEntity> rootEntry = cq.from(UserProjectEntity.class);

            Predicate predicate = cb.equal(rootEntry.get("id"), userProjectId);

            cq.where(predicate);
            TypedQuery<UserProjectEntity> query = entityManager.createQuery(cq);
            UserProjectEntity ret = query.getSingleResult();

            return ret;
        } finally {
            entityManager.close();
        }
    }

    private String getAuditJsonForDefaultRoleChange(UserProjectEntity oldDefault, UserProjectEntity newDefault) throws Exception {

        JsonNode beforeJson = generateDefaultRoleJson(oldDefault);
        JsonNode afterJson = generateDefaultRoleJson(newDefault);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.createObjectNode();

        ((ObjectNode) rootNode).put("title", "Default project changed");

        if (afterJson != null) {
            ((ObjectNode) rootNode).set("after", afterJson);
        }

        if (beforeJson != null) {
            ((ObjectNode) rootNode).set("before", beforeJson);
        }

        return prettyPrintJsonString(rootNode);
    }

    private JsonNode generateDefaultRoleJson(UserProjectEntity userProject) throws Exception {
        UserRepresentation user = UserCache.getUserDetails(userProject.getUserId());
        OrganisationEntity organisation = OrganisationCache.getOrganisationDetails(userProject.getOrganisationId());
        ProjectEntity projectEntity = ProjectCache.getProjectDetails(userProject.getProjectId());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode auditJson = mapper.createObjectNode();

        ((ObjectNode) auditJson).put("id", userProject.getId());
        ((ObjectNode) auditJson).put("user", user.getUsername());
        ((ObjectNode) auditJson).put("project", projectEntity.getName());
        ((ObjectNode) auditJson).put("organisation", organisation.getName() + " (" + organisation.getOdsCode() + ")");

        return auditJson;
    }

    private static String prettyPrintJsonString(JsonNode jsonNode) throws Exception {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonNode.toString(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            throw new Exception("Converting Json to String failed : " + e.getMessage());
        }
    }

    public List<UserProjectEntity> getUserProjectEntities(String userId) throws Exception {
        EntityManager entityManager = ConnectionManager.getUmEntityManager();

        try {
            String sql = "select up" +
                    " from UserProjectEntity up" +
                    " where up.userId = :userId" +
                    " and up.isDeleted = 0";

            Query query = entityManager.createQuery(sql);

            query.setParameter("userId", userId);

            List<UserProjectEntity> results = query.getResultList();

            return results;
        } finally {
            entityManager.close();
        }

    }
}
