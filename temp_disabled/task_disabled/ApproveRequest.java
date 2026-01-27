package com.school.management.interfaces.rest.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for approving a task submission.
 */
@Data
public class ApproveRequest {

    @NotNull(message = "Assignee user ID is required")
    private Long assigneeUserId;

    private String comment;
}
