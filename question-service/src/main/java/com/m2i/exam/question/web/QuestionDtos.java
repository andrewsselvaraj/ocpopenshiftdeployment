package com.m2i.exam.question.web;

import com.m2i.exam.common.domain.RecordStatus;
import com.m2i.exam.question.domain.AnswerOption;
import com.m2i.exam.question.domain.Question;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request/response payloads of question-service. */
public final class QuestionDtos {

    private QuestionDtos() {
    }

    /** {@code id} is set when editing an existing option; omit it for a new option. */
    public record OptionRequest(@Size(max = 50) String id,
                                @NotBlank @Size(max = 5000) String text,
                                boolean correct) {
    }

    public record QuestionRequest(
            @Size(max = 50) String orgId,
            @NotBlank @Size(max = 50) String classId,
            @NotBlank @Size(max = 50) String subjectId,
            @Size(max = 50) String chapterId,
            @NotBlank @Size(max = 10000) String text,
            @Pattern(regexp = "text|image", message = "must be text or image") String type,
            @NotNull @Size(min = 2, max = 10) List<@Valid OptionRequest> options) {
    }

    public record OptionResponse(String id, String text, boolean correct) {

        static OptionResponse from(AnswerOption o) {
            return new OptionResponse(o.getId(), o.getText(), o.isCorrect());
        }
    }

    public record QuestionResponse(String id, String orgId, String classId, String subjectId, String chapterId,
                                   String text, String type, String status, String updatedBy, Instant updatedAt,
                                   List<OptionResponse> options) {

        public static QuestionResponse from(Question q) {
            return new QuestionResponse(q.getId(), q.getOrgId(), q.getClassId(), q.getSubjectId(), q.getChapterId(),
                    q.getText(), q.getType(), q.getStatus(), q.getUpdatedBy(), q.getUpdatedAt(),
                    q.activeOptions().stream().map(OptionResponse::from).toList());
        }
    }

    /** Exam-taker view: options without the correct flag. */
    public record PaperOption(String id, String text) {
    }

    public record PaperQuestion(String id, String text, String type, List<PaperOption> options) {

        public static PaperQuestion from(Question q) {
            return new PaperQuestion(q.getId(), q.getText(), q.getType(),
                    q.activeOptions().stream().map(o -> new PaperOption(o.getId(), o.getText())).toList());
        }
    }

    public record QuestionPaper(String orgId, String subjectId, String chapterId, int totalAvailable,
                                List<PaperQuestion> questions) {
    }

    // ---- internal API used by exam-service ----------------------------------------------

    public record AnswerKeyRequest(@NotEmpty @Size(max = 500) List<@NotBlank String> questionIds) {
    }

    public record KeyOption(String optionId, boolean correct) {
    }

    public record AnswerKey(String questionId, String orgId, String classId, String subjectId, String chapterId,
                            boolean active, List<KeyOption> options) {

        public static AnswerKey from(Question q) {
            return new AnswerKey(q.getId(), q.getOrgId(), q.getClassId(), q.getSubjectId(), q.getChapterId(),
                    RecordStatus.isActive(q.getStatus()),
                    q.getOptions().stream().map(o -> new KeyOption(o.getId(), o.isCorrect())).toList());
        }
    }
}
