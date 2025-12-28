package com.school.management.dto.task;

import lombok.Data;

import java.util.List;

/**
 * 任务提交请求DTO
 */
@Data
public class TaskSubmitRequest {

    /**
     * 完成情况说明
     */
    private String content;

    /**
     * 附件ID列表(文件/照片)
     */
    private List<Long> attachmentIds;
}
