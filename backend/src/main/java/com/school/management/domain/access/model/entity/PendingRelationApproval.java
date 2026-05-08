package com.school.management.domain.access.model.entity;

import com.school.management.domain.access.model.valueobject.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 关系授权 pending 审批申请.
 *
 * <p>当某关系类型 (RelationTypeDef) 的 approvalRequired=true 时,
 * grant() 不直接写 access_relations, 而是写此表 status=PENDING.
 * 审批人通过后, AccessRelationService.applyApprovedRequest(...) 把它落到 access_relations.
 */
@Data
@Builder
public class PendingRelationApproval {
    private Long id;
    // 5 元组
    private String resourceType;
    private Long resourceId;
    private String relation;
    private String subjectType;
    private Long subjectId;
    // 关系字段
    private AccessLevel accessLevel;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Map<String, Object> metadata;
    private String remark;
    // 审批
    private Status status;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private Long approverId;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Long grantedRelationId;
    private Long tenantId;

    public enum Status {
        PENDING, APPROVED, REJECTED, CANCELLED
    }
}
