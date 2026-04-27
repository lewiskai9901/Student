package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 自动整改单创建最终失败事件 (P0#4 — 重试用尽后发出, 触发监控告警).
 *
 * <p>当 SubmissionCompletedEvent 监听器在 N 次重试后仍无法创建整改单时,
 * 发出此事件让监控/告警体系感知, 避免静默丢失.
 */
public class AutoCorrectiveCreationFailedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long submissionId;
    private final Long detailId;
    private final String itemName;
    private final int retryAttempts;
    private final String lastError;

    public AutoCorrectiveCreationFailedEvent(Long submissionId, Long detailId,
                                              String itemName, int retryAttempts,
                                              String lastError) {
        super("InspSubmission", submissionId != null ? submissionId.toString() : null);
        this.submissionId = submissionId;
        this.detailId = detailId;
        this.itemName = itemName;
        this.retryAttempts = retryAttempts;
        this.lastError = lastError;
    }

    public Long getSubmissionId() { return submissionId; }
    public Long getDetailId() { return detailId; }
    public String getItemName() { return itemName; }
    public int getRetryAttempts() { return retryAttempts; }
    public String getLastError() { return lastError; }
}
