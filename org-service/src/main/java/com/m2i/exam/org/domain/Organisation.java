package com.m2i.exam.org.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A tenant (school / institute) - {@code org_info}. The legacy {@code org_password} column is kept
 * but no longer used for login: organisation administrators sign in as ADMIN users via auth-service.
 */
@Entity
@Table(name = "org_info")
public class Organisation extends AssignedIdEntity {

    @Id
    @Column(name = "pk_org_id", length = 50)
    private String id;

    @Column(name = "org_name", nullable = false, length = 50)
    private String name;

    @Column(name = "org_email", nullable = false, length = 50)
    private String email;

    @Column(name = "org_password", nullable = false, length = 100)
    private String password;

    @Column(name = "org_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @Column(name = "org_updated_on", nullable = false)
    private Instant updatedAt;

    @Column(name = "org_status", nullable = false, length = 50)
    private String status;

    protected Organisation() {
    }

    public Organisation(String id, String name, String email, String password, String updatedBy, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
