package com.school.management.domain.calendar.model.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.school.management.domain.shared.ValueObject;

public enum SemesterStatus implements ValueObject {
    ACTIVE(1, "正常"),
    ENDED(0, "已结束");

    private final Integer code;
    private final String description;

    SemesterStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    public static SemesterStatus fromCode(Integer code) {
        if (code == null) return null;
        for (SemesterStatus status : values()) {
            if (status.code.equals(code)) return status;
        }
        throw new IllegalArgumentException("未知的学期状态码: " + code);
    }

    public boolean isActive() { return this == ACTIVE; }
    public boolean isEnded() { return this == ENDED; }
}
