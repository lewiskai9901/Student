package com.school.management.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public Long submitTask(TaskSubmitRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 查询任务
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 2. 查询当前用户的任务执行人记录
        LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
        assigneeWrapper.eq(TaskAssignee::getTaskId, request.getTaskId())
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
        User user = userMapper.selectById(currentUserId);
        String submitterName = user != null ? user.getRealName() : "未知";

        // 4. 创建提交记录
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(request.getTaskId());
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
        LambdaQueryWrapper<TaskApprovalConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(TaskApprovalConfig::getTaskId, request.getTaskId())
                .eq(TaskApprovalConfig::getDepartmentId, assignee.getDepartmentId())
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

            log.info("任务提交成功（无需审批），任务ID: {}, 执行人: {}", request.getTaskId(), currentUserId);
        } else {
            // 需要审批，更新执行人状态为待审核
            assignee.setStatus(2); // 待审核
            assignee.setSubmittedAt(LocalDateTime.now());
            assignee.setCurrentApprovalLevel(0);
            taskAssigneeMapper.updateById(assignee);

            submission.setReviewStatus(1); // 审核中
            taskSubmissionMapper.updateById(submission);

            // 6. 创建第一级审批记录
            TaskApprovalConfig firstLevelConfig = configs.get(0);
            createApprovalRecord(submission.getId(), request.getTaskId(), assignee.getId(),
                    firstLevelConfig, 1);

            log.info("任务提交成功（等待审批），任务ID: {}, 执行人: {}, 审批级别: {}",
                    request.getTaskId(), currentUserId, configs.size());
        }

        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(TaskApproveRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 1. 查询提交记录
        TaskSubmission submission = taskSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }

        // 2. 查询任务执行人记录
        TaskAssignee assignee = taskAssigneeMapper.selectById(submission.getTaskAssigneeId());
        if (assignee == null) {
            throw new BusinessException("任务执行人记录不存在");
        }

        // 3. 查询当前待审批的记录
        LambdaQueryWrapper<TaskApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(TaskApprovalRecord::getSubmissionId, request.getSubmissionId())
                .eq(TaskApprovalRecord::getApprovalStatus, 0); // 待审批
        TaskApprovalRecord currentRecord = taskApprovalRecordMapper.selectOne(recordWrapper);

        if (currentRecord == null) {
            throw new BusinessException("没有待审批的记录");
        }

        // 4. 验证审批人权限
        if (!currentRecord.getApproverId().equals(currentUserId)) {
            throw new BusinessException("您不是当前节点的审批人");
        }

        // 5. 获取用户信息
        User user = userMapper.selectById(currentUserId);
        String approverName = user != null ? user.getRealName() : "未知";

        // 6. 更新审批记录
        currentRecord.setApprovalStatus(request.getAction()); // 1-通过, 2-打回
        currentRecord.setApprovalComment(request.getComment());
        currentRecord.setApprovalTime(LocalDateTime.now());
        currentRecord.setApproverName(approverName);

        if (request.getAction() == 2) {
            // 打回
            currentRecord.setRejectToNode(request.getRejectToNode());
            currentRecord.setRejectReason(request.getComment());
        }

        taskApprovalRecordMapper.updateById(currentRecord);

        // 7. 根据审批结果处理
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
            // 通过，检查是否还有下一级审批
            Integer currentLevel = currentRecord.getNodeOrder();
            LambdaQueryWrapper<TaskApprovalConfig> configWrapper = new LambdaQueryWrapper<>();
            configWrapper.eq(TaskApprovalConfig::getTaskId, request.getTaskId())
                    .eq(TaskApprovalConfig::getDepartmentId, assignee.getDepartmentId())
                    .eq(TaskApprovalConfig::getApprovalLevel, currentLevel + 1);
            TaskApprovalConfig nextLevelConfig = taskApprovalConfigMapper.selectOne(configWrapper);

            if (nextLevelConfig != null) {
                // 还有下一级审批，创建下一级审批记录
                createApprovalRecord(submission.getId(), request.getTaskId(), assignee.getId(),
                        nextLevelConfig, currentLevel + 1);

                assignee.setCurrentApprovalLevel(currentLevel + 1);
                taskAssigneeMapper.updateById(assignee);

                log.info("任务审批通过，进入下一级审批，任务ID: {}, 审批人: {}, 当前级别: {}, 下一级别: {}",
                        request.getTaskId(), currentUserId, currentLevel, currentLevel + 1);
            } else {
                // 没有下一级审批，任务完成
                assignee.setStatus(3); // 已完成
                assignee.setCompletedAt(LocalDateTime.now());
                assignee.setCurrentApprovalLevel(currentLevel);
                taskAssigneeMapper.updateById(assignee);

                submission.setReviewStatus(2); // 通过
                submission.setFinalReviewerId(currentUserId);
                submission.setFinalReviewerName(approverName);
                submission.setFinalReviewComment(request.getComment());
                submission.setFinalReviewedAt(LocalDateTime.now());
                taskSubmissionMapper.updateById(submission);

                log.info("任务审批完成，任务ID: {}, 最终审批人: {}, 审批级别: {}",
                        request.getTaskId(), currentUserId, currentLevel);
            }
        }
    }

    @Override
    public Long getPendingApprovalCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return 0L;
        }

        // 查询当前用户待审批的记录数
        LambdaQueryWrapper<TaskApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskApprovalRecord::getApproverId, currentUserId)
                .eq(TaskApprovalRecord::getApprovalStatus, 0); // 待审批

        return taskApprovalRecordMapper.selectCount(wrapper);
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
}
