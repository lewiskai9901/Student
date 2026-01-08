package com.school.management.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 任务提交请求DTO
 */
@Data
public class TaskSubmitRequest {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 完成情况说明
     */
    @NotBlank(message = "完成情况说明不能为空")
    @Size(max = 2000, message = "完成情况说明不能超过2000字")
    private String content;

    /**
     * 附件ID列表(文件/照片)
     */
    private List<Long> attachmentIds;
}
