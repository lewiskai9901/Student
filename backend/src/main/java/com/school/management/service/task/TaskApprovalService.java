package com.school.management.service.task;

import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;

/**
 * 任务审批服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface TaskApprovalService {

    /**
     * 提交任务(班主任提交完成情况)
     *
     * @param request 提交请求
     * @return 提交记录ID
     */
    Long submitTask(TaskSubmitRequest request);

    /**
     * 审批任务(一级或二级审批)
     *
     * @param request 审批请求
     */
    void approveTask(TaskApproveRequest request);

    /**
     * 获取当前用户待审批的任务数量
     *
     * @return 待审批数量
     */
    Long getPendingApprovalCount();
}
