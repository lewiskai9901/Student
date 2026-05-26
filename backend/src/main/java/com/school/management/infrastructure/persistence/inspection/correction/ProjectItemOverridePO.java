package com.school.management.infrastructure.persistence.inspection.correction;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级题目个例整改规则 (架构 E).
 * <p>每个 (project_id, template_item_id) 唯一一条规则.
 * <p>优先级高于按题型规则.
 */
@Data
@TableName("insp_project_item_overrides")
public class ProjectItemOverridePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;
    private Long templateItemId;

    private String ruleJson;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long orgUnitId;

    // 注: 题目个例规则是"项目级开关 + 阈值"的快照, 关闭即硬删
    // 软删 (@TableLogic) 会让 uk_project_item (含 deleted) 冲突, 改硬删避免重复键
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
