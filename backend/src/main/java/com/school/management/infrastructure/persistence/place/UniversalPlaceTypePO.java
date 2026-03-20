package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用场所类型持久化对象（统一类型系统 Phase 3）
 */
@Data
@TableName("place_types")
public class UniversalPlaceTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;
    private String typeName;
    private String description;
    private Integer sortOrder;
    private Boolean isSystem;
    private Boolean isEnabled;

    // ==================== 统一类型字段 ====================

    /** 分类编码 (was base_category) */
    private String category;

    /** 父类型编码 (was parent_type_id Long) */
    private String parentTypeCode;

    /** 行为特征 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String features;

    /** 扩展属性 JSON Schema (was attribute_schema) */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String metadataSchema;

    /** 允许的子类型编码 JSON (was allowed_child_types) */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String allowedChildTypeCodes;

    /** 最大层级深度 */
    private Integer maxDepth;

    /** 关联用户类型编码 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultUserTypeCodes;

    /** 关联组织类型编码 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String defaultOrgTypeCodes;

    // ==================== 场所特有字段 ====================

    private Boolean isRootType;
    private String capacityUnit;
    private Integer defaultCapacity;

    // ==================== 审计 ====================

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Long deleted;
}
