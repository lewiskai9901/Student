package com.school.management.application.task;

import lombok.Builder;
import lombok.Data;

/**
 * Command for rejecting a task submission.
 */
@Data
@Builder
public class RejectTaskCommand {
    private Long taskId;
    private Long assigneeUserId;
    private String reason;
    private Long reviewerId;
    private String reviewerName;
}
