package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件类型持久化对象
 */
@Data
@TableName("entity_event_types")
public class EntityEventTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String categoryCode;
    private String categoryName;
    private String typeCode;
    private String typeName;
    private Integer hasScore;
    private Integer hasSeverity;
    private String severityLevels;
    private String icon;
    private String color;
    private String applicableSubjects;
    private Integer isSystem;
    private Integer isEnabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
