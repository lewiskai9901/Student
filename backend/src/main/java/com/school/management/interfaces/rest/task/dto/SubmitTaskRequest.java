package com.school.management.interfaces.rest.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 提交任务请求
 */
@Data
public class SubmitTaskRequest {

    @NotBlank(message = "提交内容不能为空")
    private String content;

    private List<Long> attachmentIds;
}
