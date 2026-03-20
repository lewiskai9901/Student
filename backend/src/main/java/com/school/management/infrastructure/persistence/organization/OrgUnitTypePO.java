package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织类型持久化对象
 */
@Data
@TableName("org_unit_types")
public class OrgUnitTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("type_code")
    private String typeCode;

    @TableField(value = "type_name", updateStrategy = FieldStrategy.ALWAYS)
    private String typeName;

    @TableField(value = "category", updateStrategy = FieldStrategy.ALWAYS)
    private String category;

    @TableField(value = "parent_type_code", updateStrategy = FieldStrategy.ALWAYS)
    private String parentTypeCode;

    @TableField(value = "icon", updateStrategy = FieldStrategy.ALWAYS)
    private String icon;

    @TableField(value = "description", updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 行为特征 JSON: {"inspectionTarget": true, "memberManagement": true, ...} */
    @TableField(value = "features", updateStrategy = FieldStrategy.ALWAYS)
    private String features;

    /** 动态扩展属性定义 JSON (AttributeSchema) */
    @TableField(value = "metadata_schema", updateStrategy = FieldStrategy.ALWAYS)
    private String metadataSchema;

    /** 允许的子类型编码列表 JSON: ["DEPARTMENT", "SECTION"] */
    @TableField(value = "allowed_child_type_codes", updateStrategy = FieldStrategy.ALWAYS)
    private String allowedChildTypeCodes;

    @TableField(value = "max_depth", updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxDepth;

    /** 关联的默认用户类型编码 JSON: ["TEACHER", "STUDENT"] */
    @TableField(value = "default_user_type_codes", updateStrategy = FieldStrategy.ALWAYS)
    private String defaultUserTypeCodes;

    /** 关联的默认场所类型编码 JSON: ["CLASSROOM", "OFFICE"] */
    @TableField(value = "default_place_type_codes", updateStrategy = FieldStrategy.ALWAYS)
    private String defaultPlaceTypeCodes;

    /** 默认岗位模板 JSON数组 */
    @TableField(value = "default_positions", updateStrategy = FieldStrategy.ALWAYS)
    private String defaultPositions;

    @TableField("is_system")
    private Boolean isSystem;

    @TableField("is_enabled")
    private Boolean isEnabled;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Long deleted;
}
