package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 整改单责任人退出 / 重派 事件 (P1#6).
 * 当 assignee 因离职 / 退出项目 / 调整等原因不再负责该案例时发出.
 * 监听器可据此发通知给项目管理员请求重新分派.
 */
public class AssigneeUnassignedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Long previousAssigneeId;
    private final String reason;

    public AssigneeUnassignedEvent(Long caseId, String caseCode,
                                    Long previousAssigneeId, String reason) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.previousAssigneeId = previousAssigneeId;
        this.reason = reason;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Long getPreviousAssigneeId() { return previousAssigneeId; }
    public String getReason() { return reason; }
}
