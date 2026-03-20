package com.school.management.application.inspection.v6;

import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionProjectRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTaskRepository;
import com.school.management.domain.inspection.repository.v6.InspectionTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * V6检查任务应用服务
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspectionTaskApplicationService {

    private final InspectionTaskRepository taskRepository;
    private final InspectionTargetRepository targetRepository;
    private final InspectionProjectRepository projectRepository;

    /**
     * 获取任务详情
     */
    public Optional<InspectionTask> getTask(Long taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * 获取项目的任务列表
     */
    public List<InspectionTask> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * 获取可领取的任务列表
     */
    public List<InspectionTask> getAvailableTasks(LocalDate date) {
        return taskRepository.findAvailableTasksForDate(date);
    }

    /**
     * 获取我的任务列表
     */
    public List<InspectionTask> getMyTasks(Long inspectorId) {
        return taskRepository.findMyTasks(inspectorId);
    }

    /**
     * 分页查询任务
     */
    public List<InspectionTask> listTasks(int page, int size, Long projectId, TaskStatus status,
                                           LocalDate startDate, LocalDate endDate, Long inspectorId) {
        return taskRepository.findPagedWithConditions(page, size, projectId, status, startDate, endDate, inspectorId);
    }

    /**
     * 统计任务数量
     */
    public long countTasks(Long projectId, TaskStatus status, LocalDate startDate,
                           LocalDate endDate, Long inspectorId) {
        return taskRepository.countWithConditions(projectId, status, startDate, endDate, inspectorId);
    }

    /**
     * 领取任务
     */
    @Transactional
    public InspectionTask claimTask(Long taskId, Long inspectorId, String inspectorName) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        // Validate and mutate domain state
        task.claim(inspectorId, inspectorName);

        // Persist the domain state change via save
        return taskRepository.save(task);
    }

    /**
     * 开始任务
     */
    @Transactional
    public InspectionTask startTask(Long taskId) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        task.start();
        return taskRepository.save(task);
    }

    /**
     * 提交任务
     */
    @Transactional
    public InspectionTask submitTask(Long taskId) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        // 更新目标统计
        int completed = targetRepository.countCompletedByTaskId(taskId);
        int skipped = targetRepository.countSkippedByTaskId(taskId);
        int total = targetRepository.countByTaskId(taskId);

        task.updateTargetStats(total, completed, skipped);
        task.submit();

        return taskRepository.save(task);
    }

    /**
     * 审核任务
     */
    @Transactional
    public InspectionTask reviewTask(Long taskId) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        task.review();
        return taskRepository.save(task);
    }

    /**
     * 发布任务
     */
    @Transactional
    public InspectionTask publishTask(Long taskId) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        task.publish();
        InspectionTask saved = taskRepository.save(task);

        // 更新项目完成任务数
        projectRepository.incrementCompletedTasks(task.getProjectId());

        return saved;
    }

    /**
     * 取消任务
     */
    @Transactional
    public InspectionTask cancelTask(Long taskId) {
        InspectionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        task.cancel();
        return taskRepository.save(task);
    }

    // ========== 检查目标操作 ==========

    /**
     * 获取任务的目标列表
     */
    public List<InspectionTarget> getTargetsByTask(Long taskId) {
        return targetRepository.findByTaskId(taskId);
    }

    /**
     * 获取目标详情
     */
    public Optional<InspectionTarget> getTarget(Long targetId) {
        return targetRepository.findById(targetId);
    }

    /**
     * 锁定目标
     */
    @Transactional
    public InspectionTarget lockTarget(Long targetId, Long inspectorId) {
        InspectionTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标不存在: " + targetId));

        boolean locked = targetRepository.lockTarget(targetId, inspectorId);
        if (!locked) {
            throw new IllegalStateException("目标锁定失败");
        }

        return targetRepository.findById(targetId).orElse(target);
    }

    /**
     * 解锁目标
     */
    @Transactional
    public InspectionTarget unlockTarget(Long targetId) {
        InspectionTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标不存在: " + targetId));

        targetRepository.unlockTarget(targetId);

        return targetRepository.findById(targetId).orElse(target);
    }

    /**
     * 完成目标检查
     */
    @Transactional
    public InspectionTarget completeTarget(Long targetId) {
        InspectionTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标不存在: " + targetId));

        targetRepository.completeTarget(targetId);
        taskRepository.incrementCompletedTargets(target.getTaskId());

        return targetRepository.findById(targetId).orElse(target);
    }

    /**
     * 跳过目标
     */
    @Transactional
    public InspectionTarget skipTarget(Long targetId, String reason) {
        InspectionTarget target = targetRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标不存在: " + targetId));

        targetRepository.skipTarget(targetId, reason);
        taskRepository.incrementSkippedTargets(target.getTaskId());

        return targetRepository.findById(targetId).orElse(target);
    }

    /**
     * 添加扣分
     */
    @Transactional
    public void addDeduction(Long targetId, BigDecimal deduction) {
        targetRepository.addDeduction(targetId, deduction);
    }

    /**
     * 添加加分
     */
    @Transactional
    public void addBonus(Long targetId, BigDecimal bonus) {
        targetRepository.addBonus(targetId, bonus);
    }
}
