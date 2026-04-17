package com.school.management.infrastructure.persistence.shared;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一类型配置持久化对象（表: entity_type_configs）
 */
@Data
@TableName("entity_type_configs")
public class EntityTypeConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String entityType;
    private String typeCode;
    private String typeName;
    private String description;
    private String icon;
    private String category;
    private String parentTypeCode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String allowedChildTypeCodes;

    private Integer maxDepth;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String metadataSchema;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String features;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String uiConfig;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultRoleCodes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultUserTypeCodes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultOrgTypeCodes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultPlaceTypeCodes;

    private Boolean isPluginRegistered;
    private String pluginClass;
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
