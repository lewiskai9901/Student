package com.school.management.interfaces.rest.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Request for submitting task work.
 */
@Data
public class SubmitTaskRequest {

    @NotBlank(message = "Content is required")
    private String content;

    private List<Long> attachmentIds;
}
