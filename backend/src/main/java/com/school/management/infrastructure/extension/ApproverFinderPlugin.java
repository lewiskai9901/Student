package com.school.management.infrastructure.extension;

import com.school.management.domain.access.model.entity.PendingRelationApproval;

import java.util.List;

/**
 * 审批人查找 SPI.
 *
 * <p>给定一个 pending 关系申请, 决定哪些用户能审批.
 * 各行业可注册自己的 finder (如教育: 班主任申请由年级主任审批; 医疗: 护士长 admit 由科室主任审批).
 *
 * <p>默认实现 {@code DefaultApproverFinder} (application/access): 按申请人所在 org 的 admin.
 *
 * <p>Phase 7 W7.4 引入.
 */
public interface ApproverFinderPlugin {

    /** 适配的关系码列表; null 表示通用 fallback (兜底). */
    List<String> applicableRelations();

    /** 找该申请的可审批 user id 列表; 空表示无人能审批(申请会一直 PENDING). */
    List<Long> findApprovers(PendingRelationApproval p);

    /** 优先级,数值越小越靠前. */
    default int order() { return 100; }
}
