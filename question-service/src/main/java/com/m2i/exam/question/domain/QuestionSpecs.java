package com.m2i.exam.question.domain;

import com.m2i.exam.common.domain.RecordStatus;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the question bank listing. Null arguments are ignored. */
public final class QuestionSpecs {

    private QuestionSpecs() {
    }

    public static Specification<Question> active() {
        return (root, query, cb) -> cb.equal(root.get("status"), RecordStatus.ACTIVE);
    }

    public static Specification<Question> attributeEquals(String attribute, String value) {
        return (root, query, cb) -> value == null || value.isBlank() ? null : cb.equal(root.get(attribute), value);
    }
}
