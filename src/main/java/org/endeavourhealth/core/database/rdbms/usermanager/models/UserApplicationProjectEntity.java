package org.endeavourhealth.core.database.rdbms.usermanager.models;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class UserApplicationProjectEntity {

    private String userProjectId;
    private String userId;
    private String organisationId;
    private String projectId;
    private Byte isDefault;
    private Byte isDeleted;

    private String appId;
    private String appName;

    @Id
    @Column(name = "user_project_id")
    public String getUserProjectId() {
        return userProjectId;
    }

    public void setUserProjectId(String id) {
        this.userProjectId = id;
    }

    @Basic
    @Column(name = "app_id")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Basic
    @Column(name = "app_name")
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Basic
    @Column(name = "user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "organisation_id")
    public String getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    @Basic
    @Column(name = "project_id")
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Basic
    @Column(name = "is_default")
    public Byte getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Byte isDefault) {
        this.isDefault = isDefault;
    }


    @Basic
    @Column(name = "is_deleted")
    public Byte getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Byte isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserApplicationProjectEntity that
                = (UserApplicationProjectEntity) o;
        return Objects.equals(userProjectId, that.userProjectId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(organisationId, that.organisationId) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(isDefault, that.isDefault) &&
                Objects.equals(isDeleted, that.isDeleted) &&
                Objects.equals(appId, that.appId) &&
                Objects.equals(appName, that.appName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(userProjectId, userId, organisationId, projectId, isDefault, isDeleted);
    }
}

