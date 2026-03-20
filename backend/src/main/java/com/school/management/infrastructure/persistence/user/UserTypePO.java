package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户类型持久化对象（统一类型系统 Phase 2）
 */
@Data
@TableName("user_types")
public class UserTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;
    private String typeName;
    private String category;
    private String parentTypeCode;
    private String icon;
    private String description;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String features;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String metadataSchema;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String allowedChildTypeCodes;

    private Integer maxDepth;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultRoleCodes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultOrgTypeCodes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultPlaceTypeCodes;

    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Long deleted;
}
