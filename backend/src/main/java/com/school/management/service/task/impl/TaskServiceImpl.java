package com.school.management.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.task.*;
import com.school.management.entity.task.*;
import com.school.management.entity.User;
import com.school.management.enums.*;
import com.school.management.mapper.task.*;
import com.school.management.mapper.UserMapper;
import com.school.management.service.task.*;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final TaskApprovalRecordMapper taskApprovalRecordMapper;
    private final TaskApprovalConfigMapper approvalConfigMapper;
    private final TaskLogMapper taskLogMapper;
    private final WorkflowTemplateService workflowTemplateService;
    private final FlowableProcessService flowableProcessService;
    private final SystemMessageService systemMessageService;
    private final UserMapper userMapper;

    @Override
    public IPage<TaskDTO> pageQuery(TaskQueryRequest request, Long currentUserId, Long departmentId) {
        Page<Task> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Task::getTitle, request.getKeyword())
                    .or().like(Task::getTaskCode, request.getKeyword()));
        }

        // 状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(Task::getStatus, request.getStatus());
        }

        // 优先级筛选
        if (request.getPriority() != null) {
            wrapper.eq(Task::getPriority, request.getPriority());
        }

        // 部门筛选(本部门可见)
        if (departmentId != null) {
            wrapper.eq(Task::getDepartmentId, departmentId);
        }

        // 我的任务
        if (Boolean.TRUE.equals(request.getMyTask())) {
            wrapper.eq(Task::getAssigneeId, currentUserId);
        }

        // 分配人筛选
        if (request.getAssignerId() != null) {
            wrapper.eq(Task::getAssignerId, request.getAssignerId());
        }

        // 时间范围
        if (request.getStartTime() != null) {
            wrapper.ge(Task::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(Task::getCreatedAt, request.getEndTime());
        }

        wrapper.orderByDesc(Task::getCreatedAt);

        IPage<Task> result = page(page, wrapper);
        return result.convert(this::convertToDTO);
    }

    @Override
    public TaskDTO getDetail(Long id) {
        Task task = getById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 权限校验
        Long currentUserId = getCurrentUserId();
        if (!canViewTask(task, currentUserId)) {
            throw new RuntimeException("无权查看此任务");
        }

        TaskDTO dto = convertToDTO(task);

        // 加载执行人列表
        if (task.getAssignType() == 2) {
            LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
            assigneeWrapper.eq(TaskAssignee::getTaskId, id);
            List<TaskAssignee> assignees = taskAssigneeMapper.selectList(assigneeWrapper);
            dto.setAssignees(assignees.stream().map(this::convertAssigneeToDTO).collect(Collectors.toList()));
        }

        // 加载提交记录
        LambdaQueryWrapper<TaskSubmission> submissionWrapper = new LambdaQueryWrapper<>();
        submissionWrapper.eq(TaskSubmission::getTaskId, id)
                .orderByDesc(TaskSubmission::getSubmittedAt)
                .last("LIMIT 1");
        TaskSubmission submission = taskSubmissionMapper.selectOne(submissionWrapper);
        if (submission != null) {
            dto.setSubmission(convertSubmissionToDTO(submission));
        }

        // 加载审批记录
        LambdaQueryWrapper<TaskApprovalRecord> approvalWrapper = new LambdaQueryWrapper<>();
        approvalWrapper.eq(TaskApprovalRecord::getTaskId, id)
                .orderByAsc(TaskApprovalRecord::getNodeOrder);
        List<TaskApprovalRecord> approvalRecords = taskApprovalRecordMapper.selectList(approvalWrapper);
        dto.setApprovalRecords(approvalRecords.stream().map(this::convertApprovalToDTO).collect(Collectors.toList()));

        return dto;
    }

    // ===== 旧版create方法（已废弃，保留备份） =====
    /*
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDTO create(TaskCreateRequest request, Long userId, String userName) {
        log.info("创建任务: title={}, userId={}, assignType={}", request.getTitle(), userId, request.getAssignType());

        // 获取流程模板
        WorkflowTemplate template = workflowTemplateService.getById(request.getWorkflowTemplateId());
        if (template == null) {
            throw new RuntimeException("流程模板不存在");
        }

        // 批量分配时为每人创建独立任务
        if (request.getAssignType() == 2 && request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
            log.info("批量创建独立任务，共{}人", request.getTargetIds().size());
            Task firstTask = null;
            for (Long targetId : request.getTargetIds()) {
                Task task = createSingleTask(request, template, userId, userName, targetId);
                if (firstTask == null) {
                    firstTask = task;
                }
            }
            log.info("批量任务创建完成，返回首个任务: taskId={}", firstTask.getId());
            return convertToDTO(firstTask);
        }

        // 单人分配
        Task task = createSingleTask(request, template, userId, userName, request.getAssigneeId());
        log.info("单人任务创建成功: taskId={}, taskCode={}", task.getId(), task.getTaskCode());
        return convertToDTO(task);
    }
    */

    /**
     * 创建任务（重构版 - 支持多人任务分配和部门审批配置）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDTO create(TaskCreateRequest request, Long userId, String userName) {
        log.info("创建任务: title={}, userId={}, assignType={}", request.getTitle(), userId, request.getAssignType());

        // 1. 获取当前用户信息
        Long currentUserId = (userId != null) ? userId : SecurityUtils.getCurrentUserId();
        String currentUserName = (userName != null) ? userName : SecurityUtils.getCurrentUsername();

        if (currentUserId == null || currentUserName == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        // 2. 创建Task主记录
        Task task = new Task();
        task.setTaskCode(taskMapper.generateTaskCode());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignerId(currentUserId);
        task.setAssignerName(currentUserName);
        // Note: workflowTemplateId is stored in task_approval_configs table, not in tasks table

        // 处理assignType和targetIds
        List<Long> assigneeIds = new ArrayList<>();
        if (request.getAssignType() == 2 && request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
            assigneeIds.addAll(request.getTargetIds());
            task.setAssignType(2); // 多人任务
        } else if (request.getAssigneeId() != null) {
            assigneeIds.add(request.getAssigneeId());
            task.setAssignType(1); // 单人任务
        } else {
            throw new RuntimeException("必须指定执行人");
        }

        task.setStatus(0); // 待接收

        // 处理附件
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            task.setAttachmentIds(request.getAttachmentIds());
        }

        taskMapper.insert(task);
        log.info("任务主记录创建成功: taskId={}, taskCode={}", task.getId(), task.getTaskCode());

        // 3. 按系部分组执行人
        Map<Long, List<User>> deptAssigneeMap = groupAssigneesByDepartment(assigneeIds);
        log.info("执行人按系部分组: 共{}个系部", deptAssigneeMap.size());

        // 4. 保存审批配置到task_approval_configs表
        if (request.getApprovalConfigs() != null && !request.getApprovalConfigs().isEmpty()) {
            List<TaskApprovalConfig> approvalConfigs = new ArrayList<>();
            for (DepartmentApprovalConfigDTO deptConfig : request.getApprovalConfigs()) {
                if (deptConfig.getLevels() != null) {
                    for (ApprovalConfigDTO level : deptConfig.getLevels()) {
                        TaskApprovalConfig config = new TaskApprovalConfig();
                        config.setTaskId(task.getId());
                        config.setDepartmentId(deptConfig.getDepartmentId());
                        config.setDepartmentName(deptConfig.getDepartmentName());
                        config.setApprovalLevel(level.getLevel());
                        config.setApproverId(level.getApproverId());
                        config.setApproverName(level.getApproverName());
                        config.setApproverRole(level.getApproverRole());
                        approvalConfigs.add(config);
                    }
                }
            }
            if (!approvalConfigs.isEmpty()) {
                approvalConfigMapper.batchInsert(approvalConfigs);
                log.info("审批配置保存成功: 共{}条记录", approvalConfigs.size());
            }
        }

        // 5. 创建TaskAssignee记录
        List<TaskAssignee> assignees = new ArrayList<>();
        for (Map.Entry<Long, List<User>> entry : deptAssigneeMap.entrySet()) {
            Long deptId = entry.getKey();
            List<User> users = entry.getValue();

            // 获取该系部的审批配置
            List<TaskApprovalConfig> deptApprovalConfigs = approvalConfigMapper
                .selectByTaskAndDept(task.getId(), deptId);

            // 转换为Map格式
            Map<String, Object> approvalConfigMap = convertApprovalConfigToMap(deptApprovalConfigs);

            for (User user : users) {
                TaskAssignee assignee = new TaskAssignee();
                assignee.setTaskId(task.getId());
                assignee.setAssigneeId(user.getId());
                assignee.setAssigneeName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                assignee.setDepartmentId(deptId);

                // 获取部门名称（从第一个用户的部门信息或审批配置中获取）
                String deptName = null;
                if (request.getApprovalConfigs() != null) {
                    for (DepartmentApprovalConfigDTO deptConfig : request.getApprovalConfigs()) {
                        if (deptConfig.getDepartmentId().equals(deptId)) {
                            deptName = deptConfig.getDepartmentName();
                            break;
                        }
                    }
                }
                assignee.setDepartmentName(deptName);

                assignee.setApprovalConfig(approvalConfigMap);
                assignee.setStatus(0); // 待接收
                assignee.setCurrentApprovalLevel(0); // 未提交
                assignees.add(assignee);
            }
        }

        // 批量插入
        if (!assignees.isEmpty()) {
            taskAssigneeMapper.batchInsert(assignees);
            log.info("执行人记录创建成功: 共{}条记录", assignees.size());
        }

        // 6. 发送通知给所有执行人
        for (Long assigneeId : assigneeIds) {
            systemMessageService.sendTaskAssignMessage(task, assigneeId);
        }
        log.info("任务分配通知已发送: 共{}人", assigneeIds.size());

        // 7. 记录操作日志
        saveLog(task.getId(), currentUserId, currentUserName, TaskAction.CREATE,
                null, TaskStatus.PENDING.getCode(), "创建任务: " + task.getTitle());

        // 清除统计缓存
        clearStatisticsCache();

        log.info("任务创建完成: taskId={}, taskCode={}", task.getId(), task.getTaskCode());
        return convertToDTO(task);
    }

    /**
     * 创建单个任务（内部方法）
     */
    private Task createSingleTask(TaskCreateRequest request, WorkflowTemplate template,
                                   Long userId, String userName, Long assigneeId) {
        Task task = new Task();
        BeanUtils.copyProperties(request, task);
        task.setTaskCode(taskMapper.generateTaskCode());
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setAssignerId(userId);
        task.setAssignerName(userName);
        task.setWorkflowTemplateId(template.getId());
        task.setAssignType(1); // 批量分配时每个任务都是单人任务
        task.setAssigneeId(assigneeId);

        save(task);

        // 记录日志
        saveLog(task.getId(), userId, userName, TaskAction.CREATE, null, TaskStatus.PENDING.getCode(), "创建任务");

        // 发送通知
        if (assigneeId != null) {
            systemMessageService.sendTaskAssignMessage(task, assigneeId);
        }

        // 清除统计缓存
        clearStatisticsCache();

        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDTO acceptTask(Long taskId, Long userId, String userName) {
        log.info("接收任务: taskId={}, userId={}", taskId, userId);

        Task task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 检查是否是任务执行人
        boolean isAssignee = false;
        if (task.getAssignType() == 1) {
            isAssignee = userId.equals(task.getAssigneeId());
        } else {
            // 批量分配，检查执行人表
            LambdaQueryWrapper<TaskAssignee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskAssignee::getTaskId, taskId)
                    .eq(TaskAssignee::getAssigneeId, userId);
            isAssignee = taskAssigneeMapper.selectCount(wrapper) > 0;
        }

        if (!isAssignee) {
            throw new RuntimeException("您不是该任务的执行人");
        }

        // 更新任务状态
        Integer oldStatus = task.getStatus();
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        task.setAcceptedAt(LocalDateTime.now());
        updateById(task);

        // 如果是批量分配，更新执行人状态
        if (task.getAssignType() == 2) {
            TaskAssignee assignee = new TaskAssignee();
            assignee.setStatus(TaskStatus.IN_PROGRESS.getCode());
            assignee.setAcceptedAt(LocalDateTime.now());
            LambdaQueryWrapper<TaskAssignee> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(TaskAssignee::getTaskId, taskId)
                    .eq(TaskAssignee::getAssigneeId, userId);
            taskAssigneeMapper.update(assignee, updateWrapper);
        }

        // 记录日志
        saveLog(taskId, userId, userName, TaskAction.ACCEPT, oldStatus, TaskStatus.IN_PROGRESS.getCode(), "接收任务");

        log.info("任务接收成功: taskId={}", taskId);

        // 清除统计缓存
        clearStatisticsCache();

        return convertToDTO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDTO submitTask(TaskSubmitRequest request, Long userId, String userName) {
        log.info("提交任务: taskId={}, userId={}", request.getTaskId(), userId);

        Task task = getById(request.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 获取流程模板
        WorkflowTemplate template = workflowTemplateService.getById(task.getWorkflowTemplateId());

        // 创建或更新提交记录
        LambdaQueryWrapper<TaskSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskSubmission::getTaskId, request.getTaskId())
                .eq(TaskSubmission::getSubmitterId, userId);
        TaskSubmission submission = taskSubmissionMapper.selectOne(wrapper);

        if (submission == null) {
            submission = new TaskSubmission();
            submission.setTaskId(request.getTaskId());
            submission.setSubmitterId(userId);
            submission.setSubmitterName(userName);
        }

        submission.setContent(request.getContent());
        submission.setAttachmentIds(request.getAttachmentIds());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setReviewStatus(0); // 待审核

        if (submission.getId() == null) {
            taskSubmissionMapper.insert(submission);
        } else {
            taskSubmissionMapper.updateById(submission);
        }

        // 启动审批流程
        Integer oldStatus = task.getStatus();
        if (template != null && StringUtils.hasText(template.getProcessDefinitionKey())) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("taskId", task.getId());
            variables.put("submitterId", userId);
            variables.put("submitterName", userName);
            variables.put("assignerId", task.getAssignerId());

            // 传入审批人配置，用于TaskListener动态分配审批人
            Map<String, Object> nodeConfig = template.getNodeConfig();
            if (nodeConfig != null && !nodeConfig.isEmpty()) {
                variables.put("nodeConfig", nodeConfig);
                log.info("传入审批人配置: {}", nodeConfig);
            }

            ProcessInstance processInstance = flowableProcessService.startProcess(
                    template.getProcessDefinitionKey(),
                    String.valueOf(task.getId()),
                    variables
            );

            task.setProcessInstanceId(processInstance.getId());
            task.setStatus(TaskStatus.APPROVING.getCode());

            // 根据审批人配置设置当前任务的审批人
            if (nodeConfig != null && !nodeConfig.isEmpty()) {
                flowableProcessService.assignApproverByConfig(processInstance.getId(), nodeConfig);
            }

            // 获取当前待办任务用于日志
            List<org.flowable.task.api.Task> currentTasks = flowableProcessService.getTasksByProcessInstanceId(processInstance.getId());
            if (!currentTasks.isEmpty()) {
                org.flowable.task.api.Task flowableTask = currentTasks.get(0);
                log.info("当前流程任务: id={}, name={}, assignee={}",
                        flowableTask.getId(), flowableTask.getName(), flowableTask.getAssignee());
            }
        } else {
            // 没有配置流程，直接进入待审核状态
            task.setStatus(TaskStatus.SUBMITTED.getCode());
        }

        task.setSubmittedAt(LocalDateTime.now());
        updateById(task);

        // 记录日志
        saveLog(request.getTaskId(), userId, userName, TaskAction.SUBMIT, oldStatus, task.getStatus(), "提交任务");

        // 发送通知给审批人
        systemMessageService.sendApprovalNotification(task, task.getAssignerId());

        log.info("任务提交成功: taskId={}", request.getTaskId());

        // 清除统计缓存
        clearStatisticsCache();

        return convertToDTO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDTO approveTask(TaskApproveRequest request, Long userId, String userName) {
        log.info("审批任务: taskId={}, action={}, userId={}", request.getTaskId(), request.getAction(), userId);

        Task task = getById(request.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        TaskSubmission submission = taskSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new RuntimeException("提交记录不存在");
        }

        // 创建审批记录
        TaskApprovalRecord record = new TaskApprovalRecord();
        record.setTaskId(request.getTaskId());
        record.setSubmissionId(request.getSubmissionId());
        record.setProcessInstanceId(task.getProcessInstanceId());
        record.setFlowableTaskId(request.getFlowableTaskId());
        record.setApproverId(userId);
        record.setApproverName(userName);
        record.setApprovalComment(request.getComment());
        record.setApprovalTime(LocalDateTime.now());

        Integer oldStatus = task.getStatus();

        if (request.getAction() == 1) {
            // 通过
            record.setApprovalStatus(ApprovalStatus.APPROVED.getCode());

            // 完成Flowable任务
            if (StringUtils.hasText(request.getFlowableTaskId())) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                flowableProcessService.completeTask(request.getFlowableTaskId(), variables, request.getComment());
            }

            // 检查流程是否结束
            boolean processEnded = true;
            if (StringUtils.hasText(task.getProcessInstanceId())) {
                processEnded = flowableProcessService.isProcessEnded(task.getProcessInstanceId());
            }

            if (processEnded) {
                // 流程结束，任务完成
                task.setStatus(TaskStatus.COMPLETED.getCode());
                task.setCompletedAt(LocalDateTime.now());
                submission.setReviewStatus(2); // 通过
                submission.setFinalReviewerId(userId);
                submission.setFinalReviewerName(userName);
                submission.setFinalReviewComment(request.getComment());
                submission.setFinalReviewedAt(LocalDateTime.now());

                // 发送完成通知
                systemMessageService.sendTaskCompleteMessage(task, submission.getSubmitterId());
            } else {
                // 流程未结束，为下一个节点设置审批人
                WorkflowTemplate template = workflowTemplateService.getById(task.getWorkflowTemplateId());
                if (template != null && template.getNodeConfig() != null && !template.getNodeConfig().isEmpty()) {
                    flowableProcessService.assignApproverByConfig(task.getProcessInstanceId(), template.getNodeConfig());
                }
            }
        } else {
            // 打回
            record.setApprovalStatus(ApprovalStatus.REJECTED.getCode());
            record.setRejectToNode(request.getRejectToNode());
            record.setRejectReason(request.getComment());

            // 驳回Flowable任务
            if (StringUtils.hasText(request.getFlowableTaskId()) && StringUtils.hasText(request.getRejectToNode())) {
                flowableProcessService.rejectToNode(request.getFlowableTaskId(), request.getRejectToNode(), request.getComment());
            }

            task.setStatus(TaskStatus.REJECTED.getCode());
            submission.setReviewStatus(3); // 打回
            submission.setRejectCount(submission.getRejectCount() == null ? 1 : submission.getRejectCount() + 1);
            submission.setFinalReviewComment(request.getComment());

            // 发送打回通知
            systemMessageService.sendTaskRejectMessage(task, submission.getSubmitterId(), request.getComment());
        }

        taskApprovalRecordMapper.insert(record);
        taskSubmissionMapper.updateById(submission);
        updateById(task);

        // 记录日志
        TaskAction action = request.getAction() == 1 ? TaskAction.APPROVE : TaskAction.REJECT;
        saveLog(request.getTaskId(), userId, userName, action, oldStatus, task.getStatus(), request.getComment());

        log.info("任务审批成功: taskId={}, action={}", request.getTaskId(), request.getAction());

        // 清除统计缓存
        clearStatisticsCache();

        return convertToDTO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Long taskId, String reason, Long userId, String userName) {
        log.info("取消任务: taskId={}, userId={}", taskId, userId);

        Task task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 只有任务创建者可以取消
        if (!userId.equals(task.getAssignerId())) {
            throw new RuntimeException("只有任务创建者可以取消任务");
        }

        // 已完成的任务不能取消
        if (task.getStatus() == TaskStatus.COMPLETED.getCode()) {
            throw new RuntimeException("已完成的任务不能取消");
        }

        Integer oldStatus = task.getStatus();
        task.setStatus(TaskStatus.CANCELLED.getCode());
        updateById(task);

        // 终止流程实例
        if (StringUtils.hasText(task.getProcessInstanceId())) {
            flowableProcessService.terminateProcess(task.getProcessInstanceId(), reason);
        }

        // 记录日志
        saveLog(taskId, userId, userName, TaskAction.CANCEL, oldStatus, TaskStatus.CANCELLED.getCode(), reason);

        log.info("任务取消成功: taskId={}", taskId);

        // 清除统计缓存
        clearStatisticsCache();

        return true;
    }

    @Override
    public IPage<TaskDTO> getMyTasks(Integer pageNum, Integer pageSize, Integer status, Long userId) {
        // 从task_assignees表查询当前用户的任务
        LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
        assigneeWrapper.eq(TaskAssignee::getAssigneeId, userId)
                .eq(status != null, TaskAssignee::getStatus, status)
                .orderByDesc(TaskAssignee::getCreatedAt);

        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(assigneeWrapper);

        if (assignees.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 获取任务ID列表（去重）
        List<Long> taskIds = assignees.stream()
                .map(TaskAssignee::getTaskId)
                .distinct()
                .collect(Collectors.toList());

        // 分页查询任务
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.in(Task::getId, taskIds)
                .orderByDesc(Task::getCreatedAt);

        IPage<Task> taskPage = page(page, taskWrapper);

        // 转换为DTO并填充额外信息
        return taskPage.convert(task -> {
            TaskDTO dto = convertToDTO(task);

            // 填充批量任务进度信息
            fillAssigneeProgress(dto, task.getId());

            // 填充当前用户的执行状态
            fillMyStatus(dto, task.getId(), userId);

            return dto;
        });
    }

    @Override
    public IPage<TaskDTO> getPendingApprovalTasks(Integer pageNum, Integer pageSize, Long userId) {
        Page<Task> page = new Page<>(pageNum, pageSize);

        // 查询待审批的任务(当前审批人包含当前用户)
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Task::getStatus, TaskStatus.SUBMITTED.getCode(), TaskStatus.APPROVING.getCode())
                .apply("JSON_CONTAINS(current_approvers, CAST({0} AS JSON), '$')", userId)
                .orderByDesc(Task::getSubmittedAt);

        IPage<Task> result = page(page, wrapper);
        return result.convert(this::convertToDTO);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(
        value = "taskStatistics",
        key = "#userId + ':' + (#departmentId != null ? #departmentId : 'null')",
        unless = "#result == null"
    )
    public TaskStatisticsDTO getStatistics(Long userId, Long departmentId) {
        log.debug("查询任务统计（未使用缓存）: userId={}, departmentId={}", userId, departmentId);
        TaskStatisticsDTO dto = new TaskStatisticsDTO();

        // 统计各状态数量
        List<Map<String, Object>> statusCounts = taskMapper.countByStatus(departmentId);
        long total = 0;
        for (Map<String, Object> item : statusCounts) {
            Integer status = ((Number) item.get("status")).intValue();
            Long count = ((Number) item.get("count")).longValue();
            total += count;

            switch (status) {
                case 0 -> dto.setPendingCount(count);
                case 1 -> dto.setInProgressCount(count);
                case 2, 6 -> dto.setSubmittedCount(dto.getSubmittedCount() + count);
                case 3 -> dto.setCompletedCount(count);
                case 4 -> dto.setRejectedCount(count);
                case 5 -> dto.setCancelledCount(count);
            }
        }
        dto.setTotalCount(total);

        // 统计超期数量
        dto.setOverdueCount(taskMapper.countOverdue(departmentId));

        // 计算完成率和超期率
        if (total > 0) {
            dto.setCompletionRate(dto.getCompletedCount() * 100.0 / total);
            dto.setOverdueRate(dto.getOverdueCount() * 100.0 / total);
        }

        // 统计待我审批数量
        if (userId != null) {
            dto.setPendingApprovalCount(taskMapper.countPendingApproval(userId));
        }

        return dto;
    }

    @Override
    public List<TaskProgressNodeDTO> getTaskProgress(Long taskId) {
        List<TaskProgressNodeDTO> nodes = new ArrayList<>();

        // 获取任务信息
        Task task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 权限校验：只有相关人员才能查看任务进度
        Long currentUserId = getCurrentUserId();
        if (!canViewTask(task, currentUserId)) {
            throw new RuntimeException("无权查看此任务");
        }

        // 1. 创建节点
        TaskProgressNodeDTO createNode = new TaskProgressNodeDTO();
        createNode.setOrder(1);
        createNode.setNodeName("任务创建");
        createNode.setNodeType("CREATE");
        createNode.setStatus("COMPLETED");
        createNode.setHandlerName(task.getAssignerName());
        createNode.setHandlerId(task.getAssignerId());
        createNode.setHandledAt(task.getCreatedAt());
        nodes.add(createNode);

        // 2. 执行节点（批量任务需要显示所有执行人）
        TaskProgressNodeDTO executeNode = new TaskProgressNodeDTO();
        executeNode.setOrder(2);
        executeNode.setNodeName("任务执行");
        executeNode.setNodeType("EXECUTE");

        // 查询所有执行人
        LambdaQueryWrapper<TaskAssignee> assigneeWrapper = new LambdaQueryWrapper<>();
        assigneeWrapper.eq(TaskAssignee::getTaskId, taskId);
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(assigneeWrapper);

        if (!assignees.isEmpty()) {
            // 多人节点
            executeNode.setTotalCount(assignees.size());
            long completedCount = assignees.stream()
                    .filter(a -> a.getStatus() != null && (a.getStatus() == 2 || a.getStatus() == 3))
                    .count();
            executeNode.setCompletedCount((int) completedCount);
            executeNode.setProgressPercent((int) (completedCount * 100 / assignees.size()));

            // 设置节点状态
            if (completedCount == assignees.size()) {
                executeNode.setStatus("COMPLETED");
            } else if (completedCount > 0) {
                executeNode.setStatus("PROCESSING");
            } else {
                executeNode.setStatus("PENDING");
            }

            // 设置处理人列表
            List<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                    .map(this::convertAssigneeToDTO)
                    .collect(Collectors.toList());
            executeNode.setHandlers(assigneeDTOs);

            // 如果是单人任务，设置处理人信息
            if (assignees.size() == 1) {
                TaskAssignee assignee = assignees.get(0);
                executeNode.setHandlerName(assignee.getAssigneeName());
                executeNode.setHandlerId(assignee.getAssigneeId());
                executeNode.setHandledAt(assignee.getSubmittedAt());
            }
        } else {
            executeNode.setStatus("PENDING");
        }
        nodes.add(executeNode);

        // 3. 审批节点
        LambdaQueryWrapper<TaskApprovalRecord> approvalWrapper = new LambdaQueryWrapper<>();
        approvalWrapper.eq(TaskApprovalRecord::getTaskId, taskId)
                .orderByAsc(TaskApprovalRecord::getNodeOrder);
        List<TaskApprovalRecord> approvals = taskApprovalRecordMapper.selectList(approvalWrapper);

        int nodeOrder = 3;
        for (TaskApprovalRecord approval : approvals) {
            TaskProgressNodeDTO approvalNode = new TaskProgressNodeDTO();
            approvalNode.setOrder(nodeOrder++);
            approvalNode.setNodeName("审批" + (approval.getNodeOrder() != null ? approval.getNodeOrder() : ""));
            approvalNode.setNodeType("APPROVE");
            approvalNode.setHandlerName(approval.getApproverName());
            approvalNode.setHandlerId(approval.getApproverId());
            approvalNode.setHandledAt(approval.getCreatedAt());
            approvalNode.setComment(approval.getApprovalComment());

            // 根据审批状态设置节点状态
            if (approval.getApprovalStatus() != null) {
                switch (approval.getApprovalStatus()) {
                    case 1: // 通过
                        approvalNode.setStatus("COMPLETED");
                        break;
                    case 2: // 拒绝
                        approvalNode.setStatus("REJECTED");
                        break;
                    default: // 待审批
                        approvalNode.setStatus("PROCESSING");
                }
            } else {
                approvalNode.setStatus("PENDING");
            }

            nodes.add(approvalNode);
        }

        // 4. 结束节点
        TaskProgressNodeDTO endNode = new TaskProgressNodeDTO();
        endNode.setOrder(nodeOrder);
        endNode.setNodeName("流程结束");
        endNode.setNodeType("END");

        if (task.getStatus() == TaskStatus.COMPLETED.getCode()) {
            endNode.setStatus("COMPLETED");
            endNode.setHandledAt(task.getCompletedAt());
        } else if (task.getStatus() == TaskStatus.REJECTED.getCode()) {
            endNode.setStatus("REJECTED");
        } else {
            endNode.setStatus("PENDING");
        }
        nodes.add(endNode);

        return nodes;
    }

    /**
     * 填充批量任务的进度信息
     */
    private void fillAssigneeProgress(TaskDTO dto, Long taskId) {
        // 查询该任务的所有执行人
        LambdaQueryWrapper<TaskAssignee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskAssignee::getTaskId, taskId);
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(wrapper);

        if (assignees.isEmpty()) {
            return;
        }

        // 设置总人数
        dto.setTotalAssignees(assignees.size());

        // 统计已提交人数（状态为2-待审核或3-已完成）
        long submittedCount = assignees.stream()
                .filter(a -> a.getStatus() != null && (a.getStatus() == 2 || a.getStatus() == 3))
                .count();
        dto.setSubmittedAssignees((int) submittedCount);

        // 统计已完成人数（状态为3-已完成）
        long completedCount = assignees.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 3)
                .count();
        dto.setCompletedAssignees((int) completedCount);

        // 设置执行人列表（如果需要）
        if (assignees.size() > 1) {
            List<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                    .map(this::convertAssigneeToDTO)
                    .collect(Collectors.toList());
            dto.setAssignees(assigneeDTOs);
        }
    }

    /**
     * 填充当前用户的执行状态
     */
    private void fillMyStatus(TaskDTO dto, Long taskId, Long userId) {
        // 查询当前用户的执行记录
        LambdaQueryWrapper<TaskAssignee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getAssigneeId, userId)
                .last("LIMIT 1");

        TaskAssignee myAssignee = taskAssigneeMapper.selectOne(wrapper);

        if (myAssignee != null) {
            dto.setMyAssigneeId(myAssignee.getId());
            dto.setMyStatus(myAssignee.getStatus());
            dto.setMyAcceptedAt(myAssignee.getAcceptedAt());
            dto.setMySubmittedAt(myAssignee.getSubmittedAt());
        }
    }

    public TaskDTO convertToDTO(Task task) {
        if (task == null) {
            return null;
        }
        TaskDTO dto = new TaskDTO();
        BeanUtils.copyProperties(task, dto);

        // 设置状态和优先级文本
        dto.setStatusText(TaskStatus.getDescByCode(task.getStatus()));
        dto.setPriorityText(TaskPriority.getDescByCode(task.getPriority()));

        // 判断是否超期
        if (task.getDueDate() != null && task.getStatus() != TaskStatus.COMPLETED.getCode()) {
            dto.setOverdue(LocalDateTime.now().isAfter(task.getDueDate()));
        } else {
            dto.setOverdue(false);
        }

        // 获取流程模板名称
        if (task.getWorkflowTemplateId() != null) {
            WorkflowTemplate template = workflowTemplateService.getById(task.getWorkflowTemplateId());
            if (template != null) {
                dto.setWorkflowTemplateName(template.getTemplateName());
            }
        }

        return dto;
    }

    private TaskAssigneeDTO convertAssigneeToDTO(TaskAssignee assignee) {
        TaskAssigneeDTO dto = new TaskAssigneeDTO();
        BeanUtils.copyProperties(assignee, dto);
        dto.setStatusText(TaskStatus.getDescByCode(assignee.getStatus()));
        return dto;
    }

    private TaskSubmissionDTO convertSubmissionToDTO(TaskSubmission submission) {
        TaskSubmissionDTO dto = new TaskSubmissionDTO();
        BeanUtils.copyProperties(submission, dto);

        String[] statusTexts = {"待审核", "审核中", "已通过", "已打回"};
        if (submission.getReviewStatus() != null && submission.getReviewStatus() < statusTexts.length) {
            dto.setReviewStatusText(statusTexts[submission.getReviewStatus()]);
        }

        return dto;
    }

    private TaskApprovalRecordDTO convertApprovalToDTO(TaskApprovalRecord record) {
        TaskApprovalRecordDTO dto = new TaskApprovalRecordDTO();
        BeanUtils.copyProperties(record, dto);
        dto.setApprovalStatusText(ApprovalStatus.getDescByCode(record.getApprovalStatus()));
        return dto;
    }

    private void saveLog(Long taskId, Long operatorId, String operatorName, TaskAction action,
                         Integer oldStatus, Integer newStatus, String remark) {
        TaskLog log = new TaskLog();
        log.setTaskId(taskId);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setAction(action.getCode());
        log.setActionDesc(action.getDesc());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        taskLogMapper.insert(log);
    }

    /**
     * 清除任务统计缓存
     */
    @org.springframework.cache.annotation.CacheEvict(value = "taskStatistics", allEntries = true)
    private void clearStatisticsCache() {
        log.debug("清除任务统计缓存");
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails
                    ? Long.parseLong(((org.springframework.security.core.userdetails.UserDetails)
                    org.springframework.security.core.context.SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .getPrincipal()).getUsername())
                    : null;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 检查用户是否有权查看任务
     * 创建人、执行人、审批人、相同部门的管理员可以查看
     */
    private boolean canViewTask(Task task, Long userId) {
        if (userId == null) {
            return false;
        }

        // 创建人可以查看
        if (task.getAssignerId() != null && task.getAssignerId().equals(userId)) {
            return true;
        }

        // 执行人可以查看（单人任务）
        if (task.getAssigneeId() != null && task.getAssigneeId().equals(userId)) {
            return true;
        }

        // 检查是否是批量任务的执行人
        if (task.getAssignType() == 2) {
            LambdaQueryWrapper<TaskAssignee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskAssignee::getTaskId, task.getId())
                   .eq(TaskAssignee::getAssigneeId, userId);
            TaskAssignee assignee = taskAssigneeMapper.selectOne(wrapper);
            if (assignee != null) {
                return true;
            }
        }

        // 检查是否是当前审批人
        if (task.getCurrentApprovers() != null && !task.getCurrentApprovers().isEmpty()) {
            if (task.getCurrentApprovers().contains(userId)) {
                return true;
            }
        }

        // 检查是否是历史审批人
        LambdaQueryWrapper<TaskApprovalRecord> approvalWrapper = new LambdaQueryWrapper<>();
        approvalWrapper.eq(TaskApprovalRecord::getTaskId, task.getId())
                       .eq(TaskApprovalRecord::getApproverId, userId);
        long approvalCount = taskApprovalRecordMapper.selectCount(approvalWrapper);
        if (approvalCount > 0) {
            return true;
        }

        // TODO: 相同部门的管理员可以查看（需要获取用户的部门和角色信息）

        return false;
    }

    /**
     * 检查用户是否是任务的执行人
     */
    private boolean isTaskAssignee(Long taskId, Long userId) {
        Task task = getById(taskId);
        if (task == null) {
            return false;
        }

        if (task.getAssignType() == 1) {
            return task.getAssigneeId() != null && task.getAssigneeId().equals(userId);
        } else {
            LambdaQueryWrapper<TaskAssignee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskAssignee::getTaskId, taskId)
                   .eq(TaskAssignee::getAssigneeId, userId);
            return taskAssigneeMapper.selectCount(wrapper) > 0;
        }
    }

    /**
     * 检查用户是否是任务的当前审批人
     */
    private boolean isCurrentApprover(Task task, Long userId) {
        if (task.getCurrentApprovers() == null || task.getCurrentApprovers().isEmpty()) {
            return false;
        }

        if (task.getCurrentApprovers() != null && task.getCurrentApprovers().contains(userId)) {
            return true;
        }
        return false;
    }

    /**
     * 按系部分组执行人
     */
    private Map<Long, List<User>> groupAssigneesByDepartment(List<Long> assigneeIds) {
        log.debug("开始按系部分组执行人，assigneeIds: {}", assigneeIds);

        if (assigneeIds == null || assigneeIds.isEmpty()) {
            throw new RuntimeException("执行人ID列表不能为空");
        }

        // 批量查询用户
        List<User> users = userMapper.selectBatchIds(assigneeIds);

        // 验证是否所有执行人都找到了
        if (users == null || users.isEmpty()) {
            throw new RuntimeException("未找到任何执行人信息，请检查执行人ID是否正确: " + assigneeIds);
        }

        if (users.size() != assigneeIds.size()) {
            Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
            List<Long> missingIds = assigneeIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());
            throw new RuntimeException("以下执行人ID不存在: " + missingIds);
        }

        // 检查是否有用户没有部门
        List<User> usersWithoutDept = users.stream()
            .filter(user -> user.getDepartmentId() == null)
            .collect(Collectors.toList());

        if (!usersWithoutDept.isEmpty()) {
            String names = usersWithoutDept.stream()
                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
                .collect(Collectors.joining(", "));
            throw new RuntimeException("以下执行人未分配系部，无法创建任务: " + names);
        }

        // 按部门分组
        Map<Long, List<User>> deptMap = users.stream()
            .collect(Collectors.groupingBy(User::getDepartmentId));

        log.debug("执行人按系部分组完成: {}", deptMap.keySet());
        return deptMap;
    }

    /**
     * 将审批配置转换为Map（因为TaskAssignee.approvalConfig是Map<String,Object>类型）
     */
    private Map<String, Object> convertApprovalConfigToMap(List<TaskApprovalConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return new HashMap<>();
        }

        List<Map<String, Object>> levelsList = configs.stream()
            .sorted(Comparator.comparing(TaskApprovalConfig::getApprovalLevel))
            .map(config -> {
                Map<String, Object> map = new HashMap<>();
                map.put("level", config.getApprovalLevel());
                map.put("approverId", config.getApproverId());
                map.put("approverName", config.getApproverName());
                map.put("approverRole", config.getApproverRole());
                return map;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("levels", levelsList);
        return result;
    }

    @Override
    public TaskDetailDTO getTaskDetail(Long taskId) {
        // 1. 查询任务基本信息
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException(String.format("任务不存在，任务ID: %d", taskId));
        }

        // 2. 查询所有执行人
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
            new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
        );

        // 3. 按系部分组
        Map<Long, List<TaskAssignee>> deptMap = assignees.stream()
            .collect(Collectors.groupingBy(TaskAssignee::getDepartmentId));

        // 4. 构建返回数据
        TaskDetailDTO detail = new TaskDetailDTO();
        detail.setTask(task);

        // 5. 构建审批流程配置（按系部）
        List<TaskDetailDTO.DepartmentApprovalFlowDTO> approvalFlows = new ArrayList<>();
        for (Long deptId : deptMap.keySet()) {
            List<TaskApprovalConfig> configs = approvalConfigMapper.selectByTaskAndDept(taskId, deptId);
            if (configs != null && !configs.isEmpty()) {
                TaskDetailDTO.DepartmentApprovalFlowDTO flowDTO = new TaskDetailDTO.DepartmentApprovalFlowDTO();
                flowDTO.setDepartmentId(deptId);
                flowDTO.setDepartmentName(configs.get(0).getDepartmentName());

                // 构建流程链
                List<ApprovalConfigDTO> configDTOs = configs.stream()
                    .sorted(Comparator.comparing(TaskApprovalConfig::getApprovalLevel))
                    .map(config -> {
                        ApprovalConfigDTO dto = new ApprovalConfigDTO();
                        dto.setLevel(config.getApprovalLevel());
                        dto.setApproverId(config.getApproverId());
                        dto.setApproverName(config.getApproverName());
                        dto.setApproverRole(config.getApproverRole());
                        return dto;
                    })
                    .collect(Collectors.toList());

                flowDTO.setFlowChain(buildFlowChain(configDTOs));
                approvalFlows.add(flowDTO);
            }
        }
        detail.setApprovalFlows(approvalFlows);

        // 6. 构建执行人列表（按系部）
        List<TaskDetailDTO.DepartmentAssigneesDTO> assigneesByDept = new ArrayList<>();
        for (Map.Entry<Long, List<TaskAssignee>> entry : deptMap.entrySet()) {
            Long deptId = entry.getKey();
            List<TaskAssignee> deptAssignees = entry.getValue();

            TaskDetailDTO.DepartmentAssigneesDTO deptDTO = new TaskDetailDTO.DepartmentAssigneesDTO();
            deptDTO.setDepartmentId(deptId);
            deptDTO.setDepartmentName(deptAssignees.get(0).getDepartmentName());
            deptDTO.setTotalCount(deptAssignees.size());

            // 统计已完成人数
            int completedCount = (int) deptAssignees.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 3)
                .count();
            deptDTO.setCompletedCount(completedCount);

            // 构建执行人详情列表
            List<TaskDetailDTO.TaskAssigneeDetailDTO> assigneeDetails = deptAssignees.stream()
                .map(this::buildAssigneeDetail)
                .collect(Collectors.toList());
            deptDTO.setAssignees(assigneeDetails);

            assigneesByDept.add(deptDTO);
        }
        detail.setAssigneesByDepartment(assigneesByDept);

        // 7. 统计信息
        TaskStatisticsDTO statistics = new TaskStatisticsDTO();
        statistics.setTotalCount((long) assignees.size());
        statistics.setCompletedCount((long) assignees.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() == 3)
            .count());
        statistics.setPendingCount((long) assignees.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() == 0)
            .count());
        statistics.setInProgressCount((long) assignees.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() == 1)
            .count());
        statistics.setSubmittedCount((long) assignees.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() == 2)
            .count());
        detail.setStatistics(statistics);

        return detail;
    }

    /**
     * 构建流程链描述
     */
    private String buildFlowChain(List<ApprovalConfigDTO> configs) {
        if (configs == null || configs.isEmpty()) {
            return "班主任提交";
        }

        return "班主任提交 → " + configs.stream()
            .sorted(Comparator.comparing(ApprovalConfigDTO::getLevel))
            .map(c -> c.getApproverName() + "(" + c.getApproverRole() + ")")
            .collect(Collectors.joining(" → "));
    }

    /**
     * 构建执行人详情
     */
    private TaskDetailDTO.TaskAssigneeDetailDTO buildAssigneeDetail(TaskAssignee assignee) {
        TaskDetailDTO.TaskAssigneeDetailDTO detail = new TaskDetailDTO.TaskAssigneeDetailDTO();
        detail.setAssigneeId(assignee.getAssigneeId());
        detail.setAssigneeName(assignee.getAssigneeName());
        detail.setStatus(assignee.getStatus());
        detail.setStatusText(getStatusText(assignee.getStatus()));
        detail.setCurrentApprovalLevel(assignee.getCurrentApprovalLevel());

        // 解析审批配置（从Map转换为List<ApprovalConfigDTO>）
        if (assignee.getApprovalConfig() != null) {
            Object levelsObj = assignee.getApprovalConfig().get("levels");
            if (levelsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> levelsList = (List<Map<String, Object>>) levelsObj;
                List<ApprovalConfigDTO> configDTOs = levelsList.stream()
                    .map(map -> {
                        ApprovalConfigDTO dto = new ApprovalConfigDTO();
                        dto.setLevel(((Number) map.get("level")).intValue());
                        dto.setApproverId(((Number) map.get("approverId")).longValue());
                        dto.setApproverName((String) map.get("approverName"));
                        dto.setApproverRole((String) map.get("approverRole"));
                        return dto;
                    })
                    .collect(Collectors.toList());
                detail.setApprovalConfig(configDTOs);
            }
        }

        // 查询该执行人的审批记录
        List<TaskApprovalRecord> records = taskApprovalRecordMapper.selectList(
            new LambdaQueryWrapper<TaskApprovalRecord>()
                .eq(TaskApprovalRecord::getTaskId, assignee.getTaskId())
                .eq(TaskApprovalRecord::getSubmissionId, assignee.getId())
                .orderByAsc(TaskApprovalRecord::getNodeOrder)
        );

        if (records != null && !records.isEmpty()) {
            List<TaskDetailDTO.ApprovalRecordDTO> recordDTOs = records.stream()
                .map(record -> {
                    TaskDetailDTO.ApprovalRecordDTO dto = new TaskDetailDTO.ApprovalRecordDTO();
                    dto.setApprovalLevel(record.getNodeOrder());
                    dto.setApprovalStatus(record.getApprovalStatus());
                    dto.setApprovalTime(record.getApprovalTime() != null ?
                        record.getApprovalTime().toString() : null);
                    return dto;
                })
                .collect(Collectors.toList());
            detail.setApprovalRecords(recordDTOs);
        }

        // 设置时间字段
        detail.setSubmittedAt(assignee.getSubmittedAt() != null ?
            assignee.getSubmittedAt().toString() : null);
        detail.setCompletedAt(assignee.getCompletedAt() != null ?
            assignee.getCompletedAt().toString() : null);

        return detail;
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }

        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待接收");
        statusMap.put(1, "进行中");
        statusMap.put(2, "待审核");
        statusMap.put(3, "已完成");
        statusMap.put(4, "已打回");

        return statusMap.getOrDefault(status, "未知");
    }
}
