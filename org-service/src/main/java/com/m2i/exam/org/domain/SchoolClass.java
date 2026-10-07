package com.m2i.exam.org.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** A class / batch inside an organisation - {@code class_info}. */
@Entity
@Table(name = "class_info")
public class SchoolClass extends AssignedIdEntity {

    @Id
    @Column(name = "pk_class_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "class_name", nullable = false, length = 50)
    private String name;

    @Column(name = "class_updated", nullable = false)
    private Instant updatedAt;

    @Column(name = "class_status", nullable = false, length = 50)
    private String status;

    protected SchoolClass() {
    }

    public SchoolClass(String id, String orgId, String name, String status) {
        this.id = id;
        this.orgId = orgId;
        this.name = name;
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
