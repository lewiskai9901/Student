package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织单元持久化对象
 * 映射到 org_units 表
 */
@Data
@TableName("org_units")
public class OrgUnitPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("unit_code")
    private String unitCode;

    @TableField("unit_name")
    private String unitName;

    @TableField("unit_type")
    private String unitType;

    @TableField("parent_id")
    private Long parentId;

    @TableField("tree_path")
    private String treePath;

    @TableField("tree_level")
    private Integer treeLevel;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private String status;  // DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED

    @TableField("headcount")
    private Integer headcount;

    @TableField("attributes")
    private String attributes;  // JSON string

    @TableField("merged_into_id")
    private Long mergedIntoId;

    @TableField("split_from_id")
    private Long splitFromId;

    @TableField("dissolved_at")
    private LocalDateTime dissolvedAt;

    @TableField("dissolved_reason")
    private String dissolvedReason;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableLogic
    @TableField("deleted")
    private Long deleted;

    @Version
    @TableField("version")
    private Integer version;
}
