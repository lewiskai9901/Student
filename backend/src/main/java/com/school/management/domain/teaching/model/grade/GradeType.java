package com.school.management.domain.teaching.model.grade;

import com.fasterxml.jackson.annotation.JsonValue;
import com.school.management.domain.shared.ValueObject;

public enum GradeType implements ValueObject {
    REGULAR(1, "平时成绩"),
    MIDTERM(2, "期中成绩"),
    FINAL(3, "期末成绩"),
    TOTAL(4, "总评成绩");

    private final Integer code;
    private final String description;

    GradeType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    public static GradeType fromCode(Integer code) {
        if (code == null) return null;
        for (GradeType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return FINAL;
    }
}
