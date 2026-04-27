package com.school.management.domain.inspection.model.appeal;

/**
 * 申诉状态 (P1#8).
 * 状态机: PENDING → APPROVED / REJECTED / WITHDRAWN
 */
public enum AppealStatus {
    /** 待审核 */
    PENDING,
    /** 审核通过 (扣分调整生效) */
    APPROVED,
    /** 审核驳回 (维持原扣分) */
    REJECTED,
    /** 提交人主动撤回 */
    WITHDRAWN
}
