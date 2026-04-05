package com.school.management.domain.teaching.model.exam;

import com.fasterxml.jackson.annotation.JsonValue;
import com.school.management.domain.shared.ValueObject;

public enum ExamType implements ValueObject {
    MIDTERM(1, "期中考试"),
    FINAL(2, "期末考试"),
    MAKEUP(3, "补考"),
    RETAKE(4, "重修考试");

    private final Integer code;
    private final String description;

    ExamType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    public static ExamType fromCode(Integer code) {
        if (code == null) return null;
        for (ExamType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return FINAL;
    }
}
