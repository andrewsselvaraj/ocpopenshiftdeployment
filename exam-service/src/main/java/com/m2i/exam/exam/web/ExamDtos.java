package com.m2i.exam.exam.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request/response payloads of exam-service. */
public final class ExamDtos {

    private ExamDtos() {
    }

    public record AnswerSubmission(@NotBlank @Size(max = 50) String questionId,
                                   @NotBlank @Size(max = 50) String selectedOptionId) {
    }

    public record SubmitExamRequest(@NotBlank @Size(max = 50) String subjectId,
                                    @NotNull @Size(min = 1, max = 200) List<@Valid AnswerSubmission> answers) {
    }

    public record AnswerResult(String questionId, String selectedOptionId, boolean correct,
                               List<String> correctOptionIds) {
    }

    public record ExamResult(String sessionId, String orgId, String subjectId, String takenBy, Instant takenAt,
                             int totalQuestions, int correctAnswers, double scorePercent,
                             List<AnswerResult> answers) {
    }
}
