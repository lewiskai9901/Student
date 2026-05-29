package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织单元分数汇总 PO (org_unit_scores, 规模公平性 Stage 5)
 *
 * <p>org_unit_id 是业务主键 (按 orgUnit 存的, 本身就是主体), 由 RepositoryImpl
 * 显式 set, 不走 InspectionDataPermissionFiller 横切填充 — 故不在 orgUnitId 上标
 * {@code @TableField(fill = FieldFill.INSERT)}, 也不需要在 InspectionUpstreamRouter 注册.
 * 表名不在 InspectionWriteMustSetOrgUnitIdTest 的期望清单内, 不受其守护约束.
 */
@Data
@TableName("org_unit_scores")
public class OrgUnitScorePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private Long orgUnitId;
    private LocalDate cycleDate;
    private BigDecimal score;
    private String grade;
    private Integer childCount;
    private Integer sourceCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
