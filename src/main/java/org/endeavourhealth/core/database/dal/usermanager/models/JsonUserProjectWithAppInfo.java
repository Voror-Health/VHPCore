package org.endeavourhealth.core.database.dal.usermanager.models;

import org.endeavourhealth.core.database.rdbms.usermanager.models.UserApplicationProjectEntity;
import org.endeavourhealth.core.database.rdbms.usermanager.models.UserProjectEntity;

public class JsonUserProjectWithAppInfo {
    private String userProjectId;
    private String userId;
    private String projectId;
    private String projectName;
    private String organisationId;
    private String organisationName;
    private String appId;
    private String appName;
    private boolean isDefault;
    private boolean isDeleted;

    public JsonUserProjectWithAppInfo() {
    }

    public JsonUserProjectWithAppInfo(UserApplicationProjectEntity entity) {
        this.userProjectId = entity.getUserProjectId();
        this.userId = entity.getUserId();
        this.projectId = entity.getProjectId();
        this.organisationId = entity.getOrganisationId();
        this.appId = entity.getAppId();
        this.appName = entity.getAppName();
        this.isDeleted = entity.getIsDeleted().equals((byte) 1) ? true : false;
        this.isDefault = entity.getIsDefault().equals((byte) 1) ? true : false;
    }

    public String getUserProjectId() {
        return userProjectId;
    }

    public void setUserProjectId(String userProjectId) {
        this.userProjectId = userProjectId;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}


