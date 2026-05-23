package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度组-分区关系 PO (V20260524_4 引入, 替代 inspection_plans.section_ids JSON 列).
 */
@Data
@TableName("insp_plan_sections")
public class InspectionPlanSectionPO {

    private Long planId;
    private Long sectionId;
    private Long tenantId;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
