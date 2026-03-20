package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体事件持久化对象
 */
@Data
@TableName("entity_events")
public class EntityEventPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String subjectType;
    private Long subjectId;
    private String subjectName;
    private String eventCategory;
    private String eventType;
    private String eventLabel;
    private String payload;
    private String sourceModule;
    private String sourceRefType;
    private Long sourceRefId;
    private String tags;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
