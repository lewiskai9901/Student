package com.school.management.application.task;

import lombok.Builder;
import lombok.Data;

/**
 * Command for approving a task submission.
 */
@Data
@Builder
public class ApproveTaskCommand {
    private Long taskId;
    private Long assigneeUserId;
    private String comment;
    private Long reviewerId;
    private String reviewerName;
}
