package com.m2i.exam.auth.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Organisation member ({@code user_info}): Admin, Data Entry Operator or User (exam taker). */
@Entity
@Table(name = "user_info")
public class UserAccount extends AssignedIdEntity {

    @Id
    @Column(name = "pk_user_id", length = 50)
    private String id;

    @Column(name = "user_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "user_name", nullable = false, length = 50)
    private String name;

    @Column(name = "user_email", nullable = false, length = 50)
    private String email;

    @Column(name = "user_password", nullable = false, length = 100)
    private String password;

    /** Legacy label, e.g. "Data Entry Operator"; see {@link com.m2i.exam.common.security.Role}. */
    @Column(name = "user_role", nullable = false, length = 50)
    private String role;

    @Column(name = "user_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @Column(name = "user_updated_date_time", nullable = false)
    private Instant updatedAt;

    @Column(name = "user_status", nullable = false, length = 50)
    private String status;

    protected UserAccount() {
    }

    public UserAccount(String id, String orgId, String name, String email, String password, String role,
                       String updatedBy, String status) {
        this.id = id;
        this.orgId = orgId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.updatedBy = updatedBy;
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
