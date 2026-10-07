package com.m2i.exam.org.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** A chapter / unit of a subject - {@code chapter_info}. */
@Entity
@Table(name = "chapter_info")
public class Chapter extends AssignedIdEntity {

    @Id
    @Column(name = "pk_chapter_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "chapter_name", nullable = false, length = 50)
    private String name;

    @Column(name = "chapter_updated", nullable = false)
    private Instant updatedAt;

    @Column(name = "chapter_status", nullable = false, length = 50)
    private String status;

    protected Chapter() {
    }

    public Chapter(String id, String orgId, String classId, String subjectId, String name, String status) {
        this.id = id;
        this.orgId = orgId;
        this.classId = classId;
        this.subjectId = subjectId;
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

    public String getClassId() {
        return classId;
    }

    public String getSubjectId() {
        return subjectId;
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
