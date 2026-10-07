package com.m2i.exam.question.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import com.m2i.exam.common.domain.RecordStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One selectable answer of a question - {@code answer_info_v_two}. The organisation/class/subject/chapter
 * columns are denormalised copies of the parent question, kept for compatibility with the legacy schema.
 */
@Entity
@Table(name = "answer_info_v_two")
public class AnswerOption extends AssignedIdEntity {

    @Id
    @Column(name = "pk_answer_id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_question_id", nullable = false)
    private Question question;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "fk_chapter_id", nullable = false, length = 50)
    private String chapterId;

    @Column(name = "answers", nullable = false, columnDefinition = "mediumtext")
    private String text;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "correct_answer", nullable = false, length = 500)
    private Boolean correct;

    @Column(name = "answer_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @Column(name = "answer_updated_date_time", nullable = false)
    private Instant updatedAt;

    @Column(name = "answer_status", nullable = false, length = 50)
    private String status;

    protected AnswerOption() {
    }

    AnswerOption(String id, Question question, String text, boolean correct, String updatedBy) {
        this.id = id;
        this.question = question;
        this.orgId = question.getOrgId();
        this.classId = question.getClassId();
        this.subjectId = question.getSubjectId();
        this.chapterId = question.getChapterId();
        this.text = text;
        this.correct = correct;
        this.updatedBy = updatedBy;
        this.status = RecordStatus.ACTIVE;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public void edit(String text, boolean correct, String by) {
        this.text = text;
        this.correct = correct;
        this.updatedBy = by;
        this.status = RecordStatus.ACTIVE;
    }

    public void deactivate(String by) {
        this.status = RecordStatus.DELETED;
        this.updatedBy = by;
    }

    @Override
    public String getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(correct);
    }

    public String getStatus() {
        return status;
    }
}
