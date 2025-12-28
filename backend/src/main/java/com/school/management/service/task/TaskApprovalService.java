package com.school.management.service.task;

import com.school.management.dto.task.TaskApprovalDTO;
import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;

import java.util.List;

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
     * @param taskId  任务ID
     * @param request 提交请求
     */
    void submitTask(Long taskId, TaskSubmitRequest request);

    /**
     * 审批任务(一级或二级审批)
     *
     * @param recordId 审批记录ID
     * @param request  审批请求
     */
    void approveTask(Long recordId, TaskApproveRequest request);

    /**
     * 获取我的待审批列表
     *
     * @param userId 用户ID
     * @return 待审批任务列表
     */
    List<TaskApprovalDTO> getMyPendingApprovals(Long userId);
}
