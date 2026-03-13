package com.school.management.domain.space.model.valueobject;

import lombok.Getter;

/**
 * 占用者类型枚举
 */
@Getter
public enum OccupantType {
    STUDENT("学生"),
    TEACHER("教师"),
    STAFF("职工");

    private final String description;

    OccupantType(String description) {
        this.description = description;
    }
}
