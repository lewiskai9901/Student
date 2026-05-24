package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_inspection_plans")
public class InspectionPlanPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private String planName;
    private Long rootSectionId;    // V66: 该计划使用的模板（根分区ID）
    // 2026-05-24 V20260524_5: sectionIds JSON 列已 DROP, 关系存 insp_plan_sections
    private String scheduleMode;
    private String cycleType;
    private Integer frequency;
    private String scheduleDays;
    private String timeSlots;
    private Boolean skipHolidays;
    // 2026-05-24 V20260524_5: inspectorIds JSON 列已 DROP, 关系存 insp_plan_inspectors
    /** smell A 修复 (V20260524_3): 显式指派策略, 替代 inspector_ids 空字符串 falsy 语义. */
    private String assignStrategy;
    // scoringProfileId 已撤销 (评级引擎完美架构 2026-05-23, V20260523_4 DROP 列)
    private Integer ratersPerTarget;     // 每目标检查员份数, 默认 1 — 真调度问题, 保留
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
