package com.school.management.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.task.TaskApprovalDTO;
import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;
import com.school.management.entity.User;
import com.school.management.entity.task.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.UserMapper;
import com.school.management.mapper.task.*;
import com.school.management.service.task.TaskApprovalService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务审批服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskApprovalServiceImpl implements TaskApprovalService {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskApprovalRecordMapper taskApprovalRecordMapper;
    private final TaskApprovalConfigMapper taskApprovalConfigMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(Long taskId, TaskSubmitRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 查询任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(String.format("任务不存在，任务ID: %d", taskId));
        }

        // 2. 查询当前用户的任务执行人记录
        LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
        assigneeWrapper.eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getAssigneeId, currentUserId);
        TaskAssignee assignee = taskAssigneeMapper.selectOne(assigneeWrapper);

        if (assignee == null) {
            throw new BusinessException("您不是该任务的执行人");
        }

        if (assignee.getStatus() == 0) {
            throw new BusinessException("请先接收任务");
        }

        if (assignee.getStatus() >= 2) {
            throw new BusinessException("任务已提交，请勿重复提交");
        }

        // 3. 获取用户信息
        String submitterName = getUserRealName(currentUserId);

        // 4. 创建提交记录
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(taskId);
        submission.setTaskAssigneeId(assignee.getId());
        submission.setSubmitterId(currentUserId);
        submission.setSubmitterName(submitterName);
        submission.setContent(request.getContent());
        submission.setAttachmentIds(request.getAttachmentIds());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setReviewStatus(0); // 待审核
        submission.setRejectCount(0);

        taskSubmissionMapper.insert(submission);

        // 5. 查询审批配置，确定是否需要审批以及审批级别
        Long orgUnitId = assignee.getOrgUnitId();
        if (orgUnitId == null) {
            throw new BusinessException("执行人未关联部门，无法查询审批配置");
        }

        LambdaQueryWrapper<TaskApprovalConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(TaskApprovalConfig::getTaskId, taskId)
                .eq(TaskApprovalConfig::getOrgUnitId, orgUnitId)
                .orderByAsc(TaskApprovalConfig::getApprovalLevel);
        List<TaskApprovalConfig> configs = taskApprovalConfigMapper.selectList(configWrapper);

        if (configs.isEmpty()) {
            // 无需审批，直接完成
            assignee.setStatus(3); // 已完成
            assignee.setSubmittedAt(LocalDateTime.now());
            assignee.setCompletedAt(LocalDateTime.now());
            assignee.setCurrentApprovalLevel(0);
            taskAssigneeMapper.updateById(assignee);

            submission.setReviewStatus(2); // 通过
            taskSubmissionMapper.updateById(submission);

            log.info("任务提交成功（无需审批），任务ID: {}, 执行人: {}", taskId, currentUserId);
        } else {
            // 需要审批，更新执行人状态为待审核
            assignee.setStatus(2); // 待审核
            assignee.setSubmittedAt(LocalDateTime.now());
            assignee.setCurrentApprovalLevel(1); // 设置为1，进入第一级审批
            taskAssigneeMapper.updateById(assignee);

            submission.setReviewStatus(1); // 审核中
            taskSubmissionMapper.updateById(submission);

            // 6. 创建第一级审批记录
            TaskApprovalConfig firstLevelConfig = configs.get(0);
            createApprovalRecord(submission.getId(), taskId, assignee.getId(),
                    firstLevelConfig, 1);

            log.info("任务提交成功（等待审批），任务ID: {}, 执行人: {}, 审批级别: {}",
                    taskId, currentUserId, configs.size());
        }

        // 7. 更新任务整体状态
        updateTaskOverallStatus(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long recordId, TaskApproveRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 查询审批记录
        TaskApprovalRecord currentRecord = taskApprovalRecordMapper.selectById(recordId);
        if (currentRecord == null) {
            throw new BusinessException(String.format("审批记录不存在，记录ID: %d", recordId));
        }

        // 2. 验证审批状态
        if (currentRecord.getApprovalStatus() != 0) {
            throw new BusinessException("该审批记录已处理，请勿重复审批");
        }

        // 3. 验证审批人权限
        if (!currentRecord.getApproverId().equals(currentUserId)) {
            throw new BusinessException("您不是当前节点的审批人");
        }

        // 4. 查询提交记录
        TaskSubmission submission = taskSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new BusinessException(String.format("提交记录不存在，提交ID: %d", request.getSubmissionId()));
        }

        // 5. 查询任务执行人记录
        TaskAssignee assignee = taskAssigneeMapper.selectById(submission.getTaskAssigneeId());
        if (assignee == null) {
            throw new BusinessException(String.format("任务执行人记录不存在，执行人ID: %d", submission.getTaskAssigneeId()));
        }

        // 6. 获取用户信息
        String approverName = getUserRealName(currentUserId);

        // 7. 更新审批记录
        currentRecord.setApprovalStatus(request.getAction()); // 1-通过, 2-打回
        currentRecord.setApprovalComment(request.getComment());
        currentRecord.setApprovalTime(LocalDateTime.now());
        currentRecord.setApproverName(approverName);

        if (request.getAction() == 2) {
            // 打回时，打回原因和打回节点应为必填
            if (!StringUtils.hasText(request.getComment())) {
                throw new BusinessException("打回时必须填写打回原因");
            }
            if (!StringUtils.hasText(request.getRejectToNode())) {
                throw new BusinessException("打回时必须指定打回节点");
            }
            currentRecord.setRejectToNode(request.getRejectToNode());
            currentRecord.setRejectReason(request.getComment());
        }

        taskApprovalRecordMapper.updateById(currentRecord);

        // 8. 根据审批结果处理
        if (request.getAction() == 2) {
            // 打回，更新执行人和提交记录状态
            assignee.setStatus(4); // 已打回
            assignee.setCurrentApprovalLevel(0);
            taskAssigneeMapper.updateById(assignee);

            submission.setReviewStatus(3); // 打回
            submission.setRejectCount(submission.getRejectCount() + 1);
            submission.setRejectToNode(request.getRejectToNode());
            submission.setFinalReviewerId(currentUserId);
            submission.setFinalReviewerName(approverName);
            submission.setFinalReviewComment(request.getComment());
            submission.setFinalReviewedAt(LocalDateTime.now());
            taskSubmissionMapper.updateById(submission);

            log.info("任务审批打回，任务ID: {}, 审批人: {}, 审批级别: {}",
                    request.getTaskId(), currentUserId, currentRecord.getNodeOrder());
        } else {
            // 通过，处理审批流程
            handleApprovalPassed(assignee, currentRecord);
        }

        // 9. 更新任务整体状态
        updateTaskOverallStatus(request.getTaskId());
    }

    @Override
    public List<TaskApprovalDTO> getMyPendingApprovals(Long userId) {
        // 1. 查询当前用户待审批的记录
        LambdaQueryWrapper<TaskApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskApprovalRecord::getApproverId, userId)
                .eq(TaskApprovalRecord::getApprovalStatus, 0) // 待审批
                .orderByDesc(TaskApprovalRecord::getCreatedAt);
        List<TaskApprovalRecord> records = taskApprovalRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 批量查询相关数据
        Set<Long> taskIds = records.stream()
                .map(TaskApprovalRecord::getTaskId)
                .collect(Collectors.toSet());
        Set<Long> submissionIds = records.stream()
                .map(TaskApprovalRecord::getSubmissionId)
                .collect(Collectors.toSet());

        Map<Long, Task> taskMap = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        Map<Long, TaskSubmission> submissionMap = taskSubmissionMapper.selectBatchIds(submissionIds).stream()
                .collect(Collectors.toMap(TaskSubmission::getId, Function.identity()));

        Set<Long> assigneeIds = submissionMap.values().stream()
                .map(TaskSubmission::getTaskAssigneeId)
                .collect(Collectors.toSet());

        Map<Long, TaskAssignee> assigneeMap = taskAssigneeMapper.selectBatchIds(assigneeIds).stream()
                .collect(Collectors.toMap(TaskAssignee::getId, Function.identity()));

        // 3. 组装DTO（现在只需要Map查询，非常快）
        List<TaskApprovalDTO> result = new ArrayList<>();
        for (TaskApprovalRecord record : records) {
            TaskApprovalDTO dto = new TaskApprovalDTO();
            dto.setRecordId(record.getId());
            dto.setTaskId(record.getTaskId());
            dto.setSubmissionId(record.getSubmissionId());
            dto.setNodeName(record.getNodeName());
            dto.setNodeOrder(record.getNodeOrder());
            dto.setApproverRole(record.getApproverRole());

            // 从Map中获取数据，而不是再次查询数据库
            Task task = taskMap.get(record.getTaskId());
            if (task != null) {
                dto.setTaskCode(task.getTaskCode());
                dto.setTitle(task.getTitle());
                dto.setDescription(task.getDescription());
                dto.setPriority(task.getPriority());
                dto.setDeadline(task.getDueDate());
                dto.setPriorityText(getPriorityText(task.getPriority()));
            }

            // 从Map中获取提交记录信息
            TaskSubmission submission = submissionMap.get(record.getSubmissionId());
            if (submission != null) {
                dto.setSubmitterId(submission.getSubmitterId());
                dto.setSubmitterName(submission.getSubmitterName());
                dto.setContent(submission.getContent());
                dto.setSubmittedAt(submission.getSubmittedAt());

                // 从Map中获取执行人信息
                TaskAssignee assignee = assigneeMap.get(submission.getTaskAssigneeId());
                if (assignee != null) {
                    dto.setOrgUnitId(assignee.getOrgUnitId());
                    dto.setOrgUnitName(assignee.getOrgUnitName());
                }
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * 处理审批通过的逻辑
     *
     * @param assignee      任务执行人
     * @param currentRecord 当前审批记录
     */
    private void handleApprovalPassed(TaskAssignee assignee, TaskApprovalRecord currentRecord) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String approverName = getUserRealName(currentUserId);

        Integer currentLevel = currentRecord.getNodeOrder();
        Long taskId = currentRecord.getTaskId();
        Long submissionId = currentRecord.getSubmissionId();

        // 查询审批配置，判断是否还有下一级审批
        LambdaQueryWrapper<TaskApprovalConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(TaskApprovalConfig::getTaskId, taskId)
                .eq(TaskApprovalConfig::getOrgUnitId, assignee.getOrgUnitId())
                .eq(TaskApprovalConfig::getApprovalLevel, currentLevel + 1);
        TaskApprovalConfig nextLevelConfig = taskApprovalConfigMapper.selectOne(configWrapper);

        TaskSubmission submission = taskSubmissionMapper.selectById(submissionId);

        if (nextLevelConfig != null) {
            // 还有下一级审批，创建下一级审批记录
            createApprovalRecord(submissionId, taskId, assignee.getId(),
                    nextLevelConfig, currentLevel + 1);

            assignee.setCurrentApprovalLevel(currentLevel + 1);
            taskAssigneeMapper.updateById(assignee);

            log.info("任务审批通过，进入下一级审批，任务ID: {}, 审批人: {}, 当前级别: {}, 下一级别: {}",
                    taskId, currentUserId, currentLevel, currentLevel + 1);
        } else {
            // 没有下一级审批，任务完成
            assignee.setStatus(3); // 已完成
            assignee.setCompletedAt(LocalDateTime.now());
            assignee.setCurrentApprovalLevel(currentLevel);
            taskAssigneeMapper.updateById(assignee);

            submission.setReviewStatus(2); // 通过
            submission.setFinalReviewerId(currentUserId);
            submission.setFinalReviewerName(approverName);
            submission.setFinalReviewComment(currentRecord.getApprovalComment());
            submission.setFinalReviewedAt(LocalDateTime.now());
            taskSubmissionMapper.updateById(submission);

            log.info("任务审批完成，任务ID: {}, 最终审批人: {}, 审批级别: {}",
                    taskId, currentUserId, currentLevel);
        }
    }

    /**
     * 更新任务的整体状态
     *
     * @param taskId 任务ID
     */
    private void updateTaskOverallStatus(Long taskId) {
        // 1. 查询该任务的所有执行人
        LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
        assigneeWrapper.eq(TaskAssignee::getTaskId, taskId);
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(assigneeWrapper);

        if (assignees.isEmpty()) {
            return;
        }

        // 2. 判断是否全部完成
        boolean allCompleted = assignees.stream()
                .allMatch(assignee -> assignee.getStatus() == 3); // 已完成

        // 3. 判断是否有人在进行中
        boolean anyInProgress = assignees.stream()
                .anyMatch(assignee -> assignee.getStatus() == 1 || assignee.getStatus() == 2); // 进行中或待审核

        // 4. 更新任务整体状态
        Task task = taskMapper.selectById(taskId);
        if (task != null) {
            if (allCompleted) {
                task.setStatus(3); // 已完成
            } else if (anyInProgress) {
                task.setStatus(1); // 进行中
            } else {
                task.setStatus(0); // 待接收
            }
            taskMapper.updateById(task);

            log.debug("更新任务整体状态，任务ID: {}, 新状态: {}", taskId, task.getStatus());
        }
    }

    /**
     * 获取优先级文本
     *
     * @param priority 优先级
     * @return 优先级文本
     */
    private String getPriorityText(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        switch (priority) {
            case 1:
                return "紧急";
            case 2:
                return "普通";
            case 3:
                return "低";
            default:
                return "未知";
        }
    }

    /**
     * 创建审批记录
     *
     * @param submissionId 提交记录ID
     * @param taskId       任务ID
     * @param assigneeId   执行人ID
     * @param config       审批配置
     * @param nodeOrder    审批顺序
     */
    private void createApprovalRecord(Long submissionId, Long taskId, Long assigneeId,
                                      TaskApprovalConfig config, Integer nodeOrder) {
        TaskApprovalRecord record = new TaskApprovalRecord();
        record.setTaskId(taskId);
        record.setSubmissionId(submissionId);
        record.setNodeName(config.getApproverRole());
        record.setNodeOrder(nodeOrder);
        record.setApproverId(config.getApproverId());
        record.setApproverName(config.getApproverName());
        record.setApproverRole(config.getApproverRole());
        record.setApprovalStatus(0); // 待审批

        taskApprovalRecordMapper.insert(record);

        log.debug("创建审批记录，提交ID: {}, 审批人: {}, 审批级别: {}",
                submissionId, config.getApproverName(), nodeOrder);
    }

    /**
     * 获取用户真实姓名
     *
     * @param userId 用户ID
     * @return 用户真实姓名，如果用户不存在返回"未知"
     */
    private String getUserRealName(Long userId) {
        if (userId == null) {
            return "未知";
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知";
    }
}
