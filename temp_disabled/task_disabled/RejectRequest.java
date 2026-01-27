package com.school.management.interfaces.rest.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for rejecting a task submission.
 */
@Data
public class RejectRequest {

    @NotNull(message = "Assignee user ID is required")
    private Long assigneeUserId;

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
