package com.school.management.interfaces.rest.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批任务请求
 */
@Data
public class ApproveTaskRequest {

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    private String comment;
}
