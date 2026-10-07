package com.m2i.exam.exam.domain;

import com.m2i.exam.common.domain.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One answered question of an exam attempt - {@code exam_info}. All rows of one attempt
 * share the same {@code exam_session_id}.
 */
@Entity
@Table(name = "exam_info")
public class ExamAnswer extends AssignedIdEntity {

    @Id
    @Column(name = "pk_exam_id", length = 50)
    private String id;

    @Column(name = "exam_session_id", nullable = false, length = 50)
    private String sessionId;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "fk_question_id", nullable = false, length = 50)
    private String questionId;

    @Column(name = "fk_user_selected_answer_id", nullable = false, length = 50)
    private String selectedOptionId;

    @Column(name = "exam_taken_by", nullable = false, length = 50)
    private String takenBy;

    @Column(name = "exam_taken_date_time", nullable = false)
    private Instant takenAt;

    @Column(name = "exam_status", nullable = false, length = 50)
    private String status;

    protected ExamAnswer() {
    }

    public ExamAnswer(String id, String sessionId, String orgId, String classId, String subjectId, String questionId,
                      String selectedOptionId, String takenBy, Instant takenAt, String status) {
        this.id = id;
        this.sessionId = sessionId;
        this.orgId = orgId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.takenBy = takenBy;
        this.takenAt = takenAt;
        this.status = status;
    }

    @PrePersist
    void defaults() {
        if (takenAt == null) {
            takenAt = Instant.now();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
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

    public String getQuestionId() {
        return questionId;
    }

    public String getSelectedOptionId() {
        return selectedOptionId;
    }

    public String getTakenBy() {
        return takenBy;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public String getStatus() {
        return status;
    }
}
