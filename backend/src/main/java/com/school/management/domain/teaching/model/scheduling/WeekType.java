package com.school.management.domain.teaching.model.scheduling;

import com.fasterxml.jackson.annotation.JsonValue;
import com.school.management.domain.shared.ValueObject;

public enum WeekType implements ValueObject {
    EVERY(0, "每周"),
    ODD(1, "单周"),
    EVEN(2, "双周");

    private final Integer code;
    private final String description;

    WeekType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    public static WeekType fromCode(Integer code) {
        if (code == null) return EVERY;
        for (WeekType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return EVERY;
    }
}
