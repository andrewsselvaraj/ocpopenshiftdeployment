package com.m2i.exam.exam.domain;

import java.time.Instant;

/** Aggregated view of one exam attempt (JPQL constructor projection). */
public record SessionSummary(String sessionId, String orgId, String subjectId, String takenBy, Instant takenAt,
                             long answeredQuestions) {
}
