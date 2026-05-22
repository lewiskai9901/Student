package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 检查提交被跳过 — PENDING/LOCKED → SKIPPED (P2#15).
 *
 * <p>下游影响: 跳过的目标不产生分数, 任务完成度 / 覆盖率统计需把它计入分母外,
 * 分析层的"未检查目标"清单据此更新.
 */
public class SubmissionSkippedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long submissionId;
    private final Long taskId;
    private final Long targetId;

    public SubmissionSkippedEvent(Long submissionId, Long taskId, Long targetId) {
        super("InspSubmission", submissionId);
        this.submissionId = submissionId;
        this.taskId = taskId;
        this.targetId = targetId;
    }

    public Long getSubmissionId() { return submissionId; }
    public Long getTaskId() { return taskId; }
    public Long getTargetId() { return targetId; }
}
