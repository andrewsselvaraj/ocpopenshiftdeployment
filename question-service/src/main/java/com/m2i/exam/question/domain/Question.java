package com.m2i.exam.question.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import com.m2i.exam.common.domain.RecordStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.annotations.BatchSize;

/** A question in the bank - {@code question_info_v_two}. Its answer options live in {@code answer_info_v_two}. */
@Entity
@Table(name = "question_info_v_two")
public class Question extends AssignedIdEntity {

    /** Legacy rows use "None" when a question is not tied to a chapter. */
    public static final String NO_CHAPTER = "None";

    @Id
    @Column(name = "pk_question_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "fk_chapter_id", nullable = false, length = 50)
    private String chapterId;

    @Column(name = "question_name", nullable = false, length = 10000)
    private String text;

    @Column(name = "question_type", nullable = false, length = 50)
    private String type;

    @Column(name = "question_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @Column(name = "question_updated", nullable = false)
    private Instant updatedAt;

    @Column(name = "question_status", nullable = false, length = 50)
    private String status;

    /**
     * Options are never physically deleted: exam_info rows reference them, so removed options
     * are soft-deleted. {@link BatchSize} avoids N+1 selects when a page of questions is rendered.
     */
    @OneToMany(mappedBy = "question", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("updatedAt ASC, id ASC")
    @BatchSize(size = 100)
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() {
    }

    public Question(String id, String orgId, String classId, String subjectId, String chapterId, String text,
                    String type, String updatedBy) {
        this.id = id;
        this.orgId = orgId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.chapterId = chapterId == null || chapterId.isBlank() ? NO_CHAPTER : chapterId;
        this.text = text;
        this.type = type;
        this.updatedBy = updatedBy;
        this.status = RecordStatus.ACTIVE;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public AnswerOption addOption(String optionId, String text, boolean correct) {
        AnswerOption option = new AnswerOption(optionId, this, text, correct, updatedBy);
        options.add(option);
        return option;
    }

    public Optional<AnswerOption> findOption(String optionId) {
        return options.stream().filter(o -> o.getId().equals(optionId)).findFirst();
    }

    public List<AnswerOption> activeOptions() {
        return options.stream().filter(o -> RecordStatus.isActive(o.getStatus())).toList();
    }

    public void deactivate(String by) {
        this.status = RecordStatus.DELETED;
        this.updatedBy = by;
        options.forEach(o -> o.deactivate(by));
    }

    public void edit(String text, String type, String by) {
        this.text = text;
        this.type = type;
        this.updatedBy = by;
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

    public String getChapterId() {
        return chapterId;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public List<AnswerOption> getOptions() {
        return options;
    }
}
