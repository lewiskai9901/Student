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

    /**
     * 组织编码
     */
    @TableField("unit_code")
    private String unitCode;

    /**
     * 组织名称
     */
    @TableField("unit_name")
    private String unitName;

    /**
     * 组织类型: SCHOOL, COLLEGE, DEPARTMENT, TEACHING_GROUP（旧字段）
     */
    @TableField("unit_type")
    private String unitType;

    /**
     * 组织类型编码（新，引用 org_types 表）
     */
    @TableField("type_code")
    private String typeCode;

    /**
     * 组织类别: academic, functional, administrative
     */
    @TableField("unit_category")
    private String unitCategory;

    /**
     * 父组织ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 树路径 (如: /1/5/12/)
     */
    @TableField("tree_path")
    private String treePath;

    /**
     * 层级深度
     */
    @TableField("tree_level")
    private Integer treeLevel;

    /**
     * 负责人ID
     */
    @TableField("leader_id")
    private Long leaderId;

    /**
     * 副职ID列表 (JSON)
     */
    @TableField("deputy_leader_ids")
    private String deputyLeaderIds;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    @TableField("status")
    private Integer status;

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
    private Integer deleted;
}
