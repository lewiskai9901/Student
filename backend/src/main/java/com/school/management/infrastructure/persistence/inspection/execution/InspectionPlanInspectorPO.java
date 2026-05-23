package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度组-检查员关系 PO (V20260524_2 引入, 替代 inspection_plans.inspector_ids JSON 列).
 *
 * <p>注: 数据权限边界由 plan_id → plan.org_unit_id 间接获得, 这里不重复存 org_unit_id
 * (关系表保持窄表语义, 也避免 InspectionWriteMustSetOrgUnitIdTest 守护误判).
 */
@Data
@TableName("insp_plan_inspectors")
public class InspectionPlanInspectorPO {

    private Long planId;
    private Long userId;
    private Long tenantId;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
