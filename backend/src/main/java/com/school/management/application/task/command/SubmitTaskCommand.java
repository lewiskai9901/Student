package com.school.management.application.task.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 提交任务命令
 */
@Data
@Builder
public class SubmitTaskCommand {

    private Long taskId;
    private Long submitterId;
    private String submitterName;
    private String content;
    private List<Long> attachmentIds;
}
