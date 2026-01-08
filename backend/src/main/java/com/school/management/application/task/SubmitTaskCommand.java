package com.school.management.application.task;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Command for submitting task work.
 */
@Data
@Builder
public class SubmitTaskCommand {
    private Long taskId;
    private String content;
    private List<Long> attachmentIds;
    private Long submittedBy;
    private String submittedByName;
}
