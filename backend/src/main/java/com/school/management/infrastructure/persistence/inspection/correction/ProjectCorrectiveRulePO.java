package com.school.management.infrastructure.persistence.inspection.correction;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目级按题型整改规则 (架构 E).
 * <p>每个 (project_id, scoring_mode) 唯一一条规则.
 */
@Data
@TableName("insp_project_corrective_rules")
public class ProjectCorrectiveRulePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;
    private String scoringMode;

    /** ItemRule JSON: {criticality, neverCorrect, baseSeverityMap, singleThreshold, deadlineOverrideDays}. */
    private String ruleJson;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long orgUnitId;

    // 同 ProjectItemOverridePO: 改硬删避免 uk_project_mode 重复键冲突
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
