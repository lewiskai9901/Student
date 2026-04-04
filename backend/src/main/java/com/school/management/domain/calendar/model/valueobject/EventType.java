package com.school.management.domain.calendar.model.valueobject;

import com.school.management.domain.shared.ValueObject;

public enum EventType implements ValueObject {
    OPENING(1, "开学"),
    HOLIDAY(2, "放假"),
    EXAM(3, "考试"),
    ACTIVITY(4, "活动"),
    OTHER(5, "其他");

    private final Integer code;
    private final String description;

    EventType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() { return code; }
    public String getDescription() { return description; }

    public static EventType fromCode(Integer code) {
        if (code == null) return OTHER;
        for (EventType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return OTHER;
    }
}
