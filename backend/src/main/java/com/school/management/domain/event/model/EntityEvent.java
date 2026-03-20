package com.school.management.domain.event.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实体事件聚合根
 */
@Getter
@Builder
public class EntityEvent {

    private Long id;
    private Long tenantId;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String eventCategory;
    private String eventType;
    private String eventLabel;
    private String payload;           // JSON
    private String sourceModule;
    private String sourceRefType;
    private Long sourceRefId;
    private String tags;              // JSON数组
    private Long createdBy;
    private String createdByName;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;

    // 关联主体（非持久化，仅内存）
    private List<EntityEventRelation> relations;

    /**
     * 工厂方法：创建新事件
     */
    public static EntityEvent create(String subjectType, Long subjectId, String subjectName,
                                     String eventCategory, String eventType, String eventLabel,
                                     String payload, String sourceModule, String sourceRefType,
                                     Long sourceRefId, String tags, Long createdBy, String createdByName) {
        return EntityEvent.builder()
                .subjectType(subjectType)
                .subjectId(subjectId)
                .subjectName(subjectName)
                .eventCategory(eventCategory)
                .eventType(eventType)
                .eventLabel(eventLabel)
                .payload(payload)
                .sourceModule(sourceModule)
                .sourceRefType(sourceRefType)
                .sourceRefId(sourceRefId)
                .tags(tags)
                .createdBy(createdBy)
                .createdByName(createdByName)
                .occurredAt(LocalDateTime.now())
                .tenantId(0L)
                .build();
    }
}
