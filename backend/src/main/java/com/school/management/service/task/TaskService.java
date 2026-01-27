package com.school.management.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.task.*;
import com.school.management.entity.task.Task;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService extends IService<Task> {

    /**
     * 分页查询任务
     */
    IPage<TaskDTO> pageQuery(TaskQueryRequest request, Long currentUserId, Long orgUnitId);

    /**
     * 获取任务详情
     */
    TaskDTO getDetail(Long id);

    /**
     * 创建任务
     */
    TaskDTO create(TaskCreateRequest request, Long userId, String userName);

    /**
     * 接收任务
     */
    TaskDTO acceptTask(Long taskId, Long userId, String userName);

    /**
     * 提交任务
     */
    TaskDTO submitTask(TaskSubmitRequest request, Long userId, String userName);

    /**
     * 审批任务
     */
    TaskDTO approveTask(TaskApproveRequest request, Long userId, String userName);

    /**
     * 取消任务
     */
    boolean cancelTask(Long taskId, String reason, Long userId, String userName);

    /**
     * 获取我的待办任务
     */
    IPage<TaskDTO> getMyTasks(Integer pageNum, Integer pageSize, Integer status, Long userId);

    /**
     * 获取待我审批的任务
     */
    IPage<TaskDTO> getPendingApprovalTasks(Integer pageNum, Integer pageSize, Long userId);

    /**
     * 获取任务统计
     */
    TaskStatisticsDTO getStatistics(Long userId, Long orgUnitId);

    /**
     * 转换为DTO
     */
    TaskDTO convertToDTO(Task task);

    /**
     * 获取任务流程进度
     */
    List<TaskProgressNodeDTO> getTaskProgress(Long taskId);

    /**
     * 获取任务详情（含卡片式执行人数据）
     */
    TaskDetailDTO getTaskDetail(Long taskId);
}
