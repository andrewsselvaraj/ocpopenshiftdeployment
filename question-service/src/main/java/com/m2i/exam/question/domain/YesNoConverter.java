package com.m2i.exam.question.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** {@code answer_info_v_two.correct_answer} stores "Yes" / "No". */
@Converter
public class YesNoConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }

    @Override
    public Boolean convertToEntityAttribute(String value) {
        return value != null && value.trim().equalsIgnoreCase("Yes");
    }
}
