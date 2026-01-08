package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.inspector.InspectorCreateRequest;
import com.school.management.dto.inspector.InspectorDTO;
import com.school.management.dto.inspector.InspectorUpdateRequest;
import com.school.management.entity.CheckTaskAssignment;

import java.util.List;

/**
 * 检查计划打分人员Service
 */
public interface CheckPlanInspectorService {

    /**
     * 获取计划的打分人员列表
     */
    List<InspectorDTO> getInspectorsByPlanId(Long planId);

    /**
     * 获取打分人员详情
     */
    InspectorDTO getInspectorById(Long id);

    /**
     * 添加打分人员
     */
    Long addInspector(Long planId, InspectorCreateRequest request);

    /**
     * 更新打分人员配置
     */
    void updateInspector(Long planId, InspectorUpdateRequest request);

    /**
     * 删除打分人员
     */
    void deleteInspector(Long planId, Long id);

    /**
     * 为日常检查分配任务给打分人员
     * @param dailyCheckId 日常检查ID
     * @param planId 检查计划ID
     * @param targetClassIds 日常检查的目标班级ID列表
     * @param categoryIds 日常检查的检查类别ID列表
     */
    void assignTasksForDailyCheck(Long dailyCheckId, Long planId, List<Long> targetClassIds, List<String> categoryIds);

    /**
     * 分页获取用户的检查任务列表
     */
    IPage<CheckTaskAssignment> getMyTasks(Long userId, Integer status, Long planId, int pageNum, int pageSize);

    /**
     * 获取任务详情
     */
    CheckTaskAssignment getTaskDetail(Long taskId, Long userId);

    /**
     * 开始执行任务
     */
    void startTask(Long taskId, Long userId);

    /**
     * 完成任务
     */
    void completeTask(Long taskId, Long userId);

    /**
     * 统计用户的待处理任务数量
     */
    int countPendingTasks(Long userId);
}
