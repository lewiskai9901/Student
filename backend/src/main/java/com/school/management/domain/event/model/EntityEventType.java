package com.school.management.domain.event.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 事件类型实体
 */
@Getter
@Builder
public class EntityEventType {

    private Long id;
    private Long tenantId;
    private String categoryCode;
    private String categoryName;
    private String typeCode;
    private String typeName;
    private Boolean hasScore;
    private Boolean hasSeverity;
    private String severityLevels;       // JSON数组
    private String icon;
    private String color;
    private String applicableSubjects;   // JSON: ["USER","ORG","PLACE"]
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
