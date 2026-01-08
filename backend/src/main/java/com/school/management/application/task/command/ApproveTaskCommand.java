package com.school.management.application.task.command;

import lombok.Builder;
import lombok.Data;

/**
 * 审批任务命令
 */
@Data
@Builder
public class ApproveTaskCommand {

    private Long taskId;
    private Long approverId;
    private String approverName;
    private Boolean approved;
    private String comment;
    private String rejectToNode;
}
