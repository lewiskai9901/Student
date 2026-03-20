package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * access_relations 持久化对象
 */
@Data
@TableName("access_relations")
public class AccessRelationPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String resourceType;
    private Long resourceId;
    private Long tenantId;
    private String relation;
    private String subjectType;
    private Long subjectId;
    private Boolean includeChildren;
    private Integer accessLevel;

    /** JSON 字符串，映射时转 Map */
    private String metadata;

    private String remark;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
