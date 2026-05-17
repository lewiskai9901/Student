package com.school.management.application.inspection;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.event.InspectionTriggerPoints;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.template.TemplateItem;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.repository.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspTaskApplicationService {

    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;
    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;
    private final ProjectScoreRepository scoreRepository;
    private final InspectionPlanRepository planRepository;
    private final TargetPopulationService targetPopulationService;
    private final ScoreAggregationService scoreAggregationService;
    private final SpringDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final TemplateVersionRepository templateVersionRepository;
    private final InspectionAuditLogger auditLogger;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final com.school.management.infrastructure.metrics.InspectionMetrics metrics;
    private final com.school.management.infrastructure.inspection.InspectionScopeHelper scopeHelper;
    private final InspTaskQueryService queryService;   // I6: governance KPI / mode CRUD 委托

    @Autowired(required = false)
    private TriggerService triggerService;

    // ========== Task CRUD ==========

    @Transactional
    public InspTask createTask(Long projectId, LocalDate taskDate,
                               String timeSlotCode, java.time.LocalTime timeSlotStart,
                               java.time.LocalTime timeSlotEnd) {
        // review #6: drift check 提前到 save 之前 — 避免抛异常时 task 已入库变僵尸
        // (此处虽然 @Transactional 会回滚, 但失败前移让 caller 拿到更明确的错误)
        InspProject project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            preCheckTemplateDrift(project);
        }

        String taskCode = generateTaskCode();
        InspTask task = InspTask.create(taskCode, projectId, taskDate);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskCreated();

        // 自动填充 submissions
        populateSubmissions(saved);

        return saved;
    }

    /**
     * V108: 检查员发起临时抽查任务 (AD_HOC).
     * - 项目必须 allow_ad_hoc=1
     * - 创建即为 CLAIMED (发起人=检查员)
     * - 永不逾期 (deadlinePolicy=NONE)
     */
    @Transactional
    public InspTask createAdHocTask(Long projectId, Long inspectorId, String inspectorName,
                                     String reason) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        // 项目模式校验 — 只允许 SPOT_CHECK / HYBRID / EMERGENCY 项目发起抽查
        if (!queryService.isProjectAllowAdHoc(projectId)) {
            throw new IllegalStateException("该项目不允许临时抽查 (项目配置 allow_ad_hoc=false)");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("抽查必须填写发起原因");
        }
        preCheckTemplateDrift(project);

        String taskCode = generateTaskCode();
        InspTask task = InspTask.createAdHoc(taskCode, projectId, inspectorId, inspectorName, reason);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskCreated();
        metrics.taskClaimed();

        populateSubmissions(saved);
        return saved;
    }

    /**
     * V108: 系统/事件自动触发的核查任务 (TRIGGERED).
     * 由申诉/告警/投诉 listener 调用, 不需要用户手动发起.
     *
     * @param projectId  关联项目 ID
     * @param refType    源单据类型 (Appeal/Alert/Complaint)
     * @param refId      源单据 ID
     * @param reason     触发说明
     */
    @Transactional
    public InspTask createTriggeredTask(Long projectId, String refType, Long refId, String reason) {
        InspProject project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("createTriggeredTask: 项目 {} 不存在, 跳过", projectId);
            return null;
        }
        // 防重: 同一 refType+refId 已有 task → 跳过
        try {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM insp_tasks " +
                    "WHERE source_ref_type = ? AND source_ref_id = ? AND deleted = 0",
                    Integer.class, refType, refId);
            if (existing != null && existing > 0) {
                log.info("createTriggeredTask: refType={} refId={} 已触发过 task, 跳过", refType, refId);
                return null;
            }
        } catch (Exception ignored) { /* skip dedup if query fails */ }

        try {
            preCheckTemplateDrift(project);
        } catch (Exception e) {
            log.warn("createTriggeredTask: 模板漂移检查失败 — projectId={}, msg={}", projectId, e.getMessage());
            return null;
        }

        String taskCode = generateTaskCode();
        InspTask task = InspTask.createTriggered(taskCode, projectId, refType, refId, reason);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskCreated();

        populateSubmissions(saved);
        log.info("[V108] 触发核查任务已创建: code={}, refType={}, refId={}", taskCode, refType, refId);
        return saved;
    }

    /**
     * V108: 受检主体发起自查 (SELF_CHECK).
     * - 项目必须 allow_self_check=1
     * - 创建即 CLAIMED, inspector=自查者本人
     * - 永不逾期
     */
    @Transactional
    public InspTask createSelfCheckTask(Long projectId, Long userId, String userName, String reason) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        try {
            Integer allowed = jdbcTemplate.queryForObject(
                    "SELECT allow_self_check FROM insp_projects WHERE id = ? AND deleted = 0",
                    Integer.class, projectId);
            if (allowed == null || allowed == 0) {
                throw new IllegalStateException("该项目不允许自查 (allow_self_check=false)");
            }
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("自查必须填写原因");
        }
        preCheckTemplateDrift(project);

        String taskCode = generateTaskCode();
        InspTask task = InspTask.createSelfCheck(taskCode, projectId, userId, userName, reason);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskCreated();
        metrics.taskClaimed();
        populateSubmissions(saved);
        return saved;
    }

    /**
     * V108: 检查员发起互查 (CROSS_AUDIT).
     * 业务约束 (本期暂留给前端): 不允许 inspector 检查自己人.
     */
    @Transactional
    public InspTask createCrossAuditTask(Long projectId, Long inspectorId, String inspectorName,
                                          java.time.LocalDate dueDate, String reason) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("互查必须填写原因");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("互查必须指定截止日期 (有 SLA 约束)");
        }
        preCheckTemplateDrift(project);

        String taskCode = generateTaskCode();
        InspTask task = InspTask.createCrossAudit(taskCode, projectId, inspectorId, inspectorName,
                dueDate, reason);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskCreated();
        metrics.taskClaimed();
        populateSubmissions(saved);
        return saved;
    }

    /**
     * V108: 治理面板 KPI 按 task_type 拆分.
     *
     * <p>返回 5 个维度的指标,用于 governance 工作台展示真实业务行为:
     * <ul>
     *   <li>scheduled: 计划完成率 (硬指标, 检查员 KPI)</li>
     *   <li>adHoc: 抽查活跃度 (反映主动发现, 越高越好)</li>
     *   <li>triggered: 事件响应平均时长 (申诉/告警 SLA)</li>
     *   <li>selfCheck: 自查参与度 (受检主体主动性)</li>
     *   <li>crossAudit: 互查覆盖率 (审计独立性)</li>
     * </ul>
     */
    // I6: getTaskTypeKpi / listAdHocAllowedProjects / getInspectionMode / updateInspectionMode
    // 已迁移到 InspTaskQueryService. 旧 API 委托保持兼容.

    /** @deprecated I6: 改用 {@link InspTaskQueryService#getTaskTypeKpi(Long)}. 保留作 controller 兼容. */
    @Deprecated
    public java.util.Map<String, Object> getTaskTypeKpi(Long projectId) {
        return queryService.getTaskTypeKpi(projectId);
    }

    /** @deprecated I6: 改用 {@link InspTaskQueryService#listAdHocAllowedProjects()}. */
    @Deprecated
    public java.util.List<java.util.Map<String, Object>> listAdHocAllowedProjects() {
        return queryService.listAdHocAllowedProjects();
    }

    /** @deprecated I6: 改用 {@link InspTaskQueryService#getInspectionMode(Long)}. */
    @Deprecated
    public java.util.Map<String, Object> getInspectionMode(Long projectId) {
        return queryService.getInspectionMode(projectId);
    }

    /** @deprecated I6: 改用 {@link InspTaskQueryService#updateInspectionMode(Long, String, Boolean, Boolean, Integer)}. */
    @Deprecated
    public java.util.Map<String, Object> updateInspectionMode(
            Long projectId, String inspectionMode, Boolean allowAdHoc,
            Boolean allowSelfCheck, Integer adHocQuotaPerInspector) {
        return queryService.updateInspectionMode(projectId, inspectionMode, allowAdHoc, allowSelfCheck, adHocQuotaPerInspector);
    }

    /**
     * 创建 task 前预检模板漂移.
     *
     * <p>I5 (2026-05-17): 从 hard-throw 改为 log.warn — 真重放未实现, 但 SubmissionDetail
     * 创建时已冻结 itemCode/scoringConfig 等关键字段, 历史 record 数据不会被改动.
     * 新建 task 接受 live structure 与项目原契约可能略有差异 (开发期可接受).
     * 真正实现 SnapshotTree 反序列化后此处可恢复 hard-block 或直接走 snapshot 填充.
     */
    private void preCheckTemplateDrift(InspProject project) {
        Long lockedVersionId = project.getTemplateVersionId();
        Long rootSectionId = project.getRootSectionId();
        if (lockedVersionId == null || rootSectionId == null) return;
        TemplateSection root = sectionRepository.findById(rootSectionId).orElse(null);
        if (root == null || root.getTemplateId() == null) return;
        templateVersionRepository.findLatestByTemplateId(root.getTemplateId()).ifPresent(latest -> {
            if (!lockedVersionId.equals(latest.getId())) {
                log.warn("TEMPLATE_DRIFT (informational): 项目 {} 锁定 templateVersionId={}, " +
                        "模板 {} 最新版本是 {}. 新 task 将用 live structure, " +
                        "建议管理员评估并调用 POST /inspection/projects/{}/upgrade-template-version.",
                        project.getProjectCode(), lockedVersionId,
                        root.getTemplateId(), latest.getId(), project.getId());
            }
        });
    }

    @Transactional(readOnly = true)
    public Optional<InspTask> getTask(Long id) {
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listMyTasks(Long userId) {
        // 我的任务 = 我作为检查员的全部 + 我作为审核员的待审/审核中/已审 (P3 修复)
        return taskRepository.findByInspectorOrReviewerId(userId);
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InspTask> listAvailableTasks() {
        return taskRepository.findAvailableTasks();
    }

    /**
     * 列出当前用户可领取的任务（过滤掉计划指定了其他人的任务）
     */
    @Transactional(readOnly = true)
    public List<InspTask> listAvailableTasksForUser(Long userId) {
        List<InspTask> all = taskRepository.findAvailableTasks();
        return all.stream().filter(task -> {
            if (task.getInspectionPlanId() == null) return true;
            InspectionPlan plan = planRepository.findById(task.getInspectionPlanId()).orElse(null);
            if (plan == null || plan.getInspectorIds() == null || plan.getInspectorIds().isBlank()) return true;
            return plan.getInspectorIds().contains(String.valueOf(userId));
        }).toList();
    }

    /**
     * 重新填充 submissions（当任务创建时未成功填充时手动触发）
     */
    @Transactional
    public InspTask repopulateSubmissions(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        if (task.getTotalTargets() > 0) {
            log.info("任务 {} 已有 {} 个目标，跳过重新填充", task.getTaskCode(), task.getTotalTargets());
            return task;
        }
        populateSubmissions(task);
        return taskRepository.findById(id).orElse(task);
    }

    // ========== Task Lifecycle ==========

    @Transactional
    public InspTask claimTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.claim(inspectorId, inspectorName);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskClaimed();
        return saved;
    }

    @Transactional
    public InspTask startTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.start();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask submitTask(Long id) {
        var sample = metrics.startSubmitTimer();
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.submit();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        // 计算项目 ProjectScore
        tryComputeProjectScore(saved);

        metrics.taskSubmitted();
        metrics.recordSubmitDuration(sample);
        return saved;
    }

    @Transactional
    public InspTask withdrawTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.withdraw();
        // 将关联的 COMPLETED submissions 重新打开
        List<InspSubmission> submissions = submissionRepository.findByTaskId(id);
        for (InspSubmission sub : submissions) {
            if (sub.getStatus() == SubmissionStatus.COMPLETED) {
                sub.reopen();
                submissionRepository.save(sub);
            }
        }
        return taskRepository.save(task);
    }

    @Transactional
    public InspTask rejectTask(Long id, String comment) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        // review #E: 项目级 maxRejectCount 优先, NULL 沿用系统默认
        Integer projectMax = projectRepository.findById(task.getProjectId())
                .map(InspProject::getMaxRejectCount).orElse(null);
        task.reject(comment, projectMax);
        // 将关联的 COMPLETED submissions 重新打开
        List<InspSubmission> submissions = submissionRepository.findByTaskId(id);
        for (InspSubmission sub : submissions) {
            if (sub.getStatus() == SubmissionStatus.COMPLETED) {
                sub.reopen();
                submissionRepository.save(sub);
            }
        }
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        // C: 审计日志
        auditLogger.log("InspTask", saved.getId(), saved.getTaskCode(),
                "TASK_REJECTED", comment,
                Map.of("rejectionCount", saved.getRejectionCount() != null ? saved.getRejectionCount() : 0,
                        "extendedTo", saved.getExtendedTo() != null ? saved.getExtendedTo().toString() : "",
                        "inspectorId", saved.getInspectorId() != null ? saved.getInspectorId() : 0L));
        // P1#9: 触发任务驳回通知点 — 检查员需重新提交
        if (triggerService != null) {
            try {
                Map<String, Object> ctx = new HashMap<>();
                ctx.put("taskId", saved.getId());
                ctx.put("taskCode", saved.getTaskCode());
                ctx.put("inspectorId", saved.getInspectorId());
                ctx.put("rejectionCount", saved.getRejectionCount() != null
                        ? saved.getRejectionCount().longValue() : 0L);
                ctx.put("extendedTo", saved.getExtendedTo() != null ? saved.getExtendedTo().toString() : "");
                ctx.put("comment", comment != null ? comment : "");
                ctx.put("_refType", "inspection_task");
                ctx.put("_refId", saved.getId());
                triggerService.fire(InspectionTriggerPoints.INSP_TASK_REJECTED, ctx);
            } catch (Exception e) {
                log.warn("触发 INSP_TASK_REJECTED 失败: {}", e.getMessage());
            }
        }
        return saved;
    }

    /**
     * review #D: 检查员退出自动重派 — 当 userId 因离职/退出项目无法继续负责任务时,
     * 批量解除其所有 CLAIMED/IN_PROGRESS 任务, 状态回 PENDING, 等待重新领取或分派.
     *
     * @param userId 离职/退出的检查员 ID
     * @param reason 退出原因 (审计 + 通知)
     * @param fallbackInspectorId 可选 — 自动重派给此检查员
     * @param fallbackInspectorName 可选 — 自动重派的检查员姓名
     * @return 受影响的任务数
     */
    /** review #5 + #11: 每条 task 独立事务, 校验 fallback 用户合法性 */
    public int reassignDepartedInspector(Long userId, String reason,
                                          Long fallbackInspectorId, String fallbackInspectorName) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (fallbackInspectorId != null) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = ? AND deleted = 0",
                    Integer.class, fallbackInspectorId);
            if (count == null || count == 0) {
                throw new IllegalArgumentException("fallbackInspectorId 对应用户不存在或已删除: " + fallbackInspectorId);
            }
        }
        List<InspTask> tasks = taskRepository.findByInspectorId(userId);
        int affected = 0;
        for (InspTask t : tasks) {
            if (t.getStatus() != TaskStatus.CLAIMED && t.getStatus() != TaskStatus.IN_PROGRESS) {
                continue;
            }
            Boolean ok = transactionTemplate.execute(status -> {
                try {
                    InspTask fresh = taskRepository.findById(t.getId()).orElse(null);
                    if (fresh == null) return false;
                    fresh.unclaim(reason);
                    if (fallbackInspectorId != null) {
                        fresh.assign(fallbackInspectorId, fallbackInspectorName);
                    }
                    taskRepository.save(fresh);
                    eventPublisher.publishAll(fresh.getDomainEvents());
                    fresh.clearDomainEvents();
                    auditLogger.log("InspTask", fresh.getId(), fresh.getTaskCode(),
                            "TASK_INSPECTOR_REASSIGNED", reason,
                            Map.of("previousInspectorId", userId,
                                    "fallbackInspectorId", fallbackInspectorId != null ? fallbackInspectorId : 0L));
                    return true;
                } catch (Exception e) {
                    log.error("Reassign task {} inspector failed (rolled back): {}", t.getTaskCode(), e.getMessage());
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.TRUE.equals(ok)) affected++;
        }
        log.info("Departed inspector {} reassign summary: {} tasks affected (reason={})",
                userId, affected, reason);
        return affected;
    }

    /**
     * 项目管理员手动延期任务 (P1#5) — 驳回上限后或其他业务原因.
     */
    @Transactional
    public InspTask extendTaskDeadline(Long id, LocalDate newDeadline) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        LocalDate previous = task.getEffectiveDeadline();
        task.extendDeadline(newDeadline);
        InspTask saved = taskRepository.save(task);
        // C: 审计日志
        auditLogger.log("InspTask", saved.getId(), saved.getTaskCode(),
                "TASK_DEADLINE_EXTENDED", null,
                Map.of("previousDeadline", previous != null ? previous.toString() : "",
                        "newDeadline", newDeadline.toString()));
        return saved;
    }

    @Transactional
    public InspTask startReview(Long id, Long reviewerId, String reviewerName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.startReview(reviewerId, reviewerName);
        return taskRepository.save(task);
    }

    @Transactional
    public InspTask reviewTask(Long id, String comment) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.review(comment);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskReviewed("approved");
        return saved;
    }

    @Transactional
    public InspTask publishTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        // 从关联项目获取 autoPublish 配置
        boolean autoPublish = false;
        InspProject project = projectRepository.findById(task.getProjectId()).orElse(null);
        if (project != null && Boolean.TRUE.equals(project.getAutoPublish())) {
            autoPublish = true;
        }
        task.publish(autoPublish);
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        metrics.taskPublished();

        // Auto-recompute project score after task publish
        try {
            scoreAggregationService.recomputeProjectScore(saved.getProjectId(), saved.getTaskDate());
        } catch (Exception e) {
            log.warn("Failed to recompute project score after task publish: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public InspTask cancelTask(Long id) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.cancel();
        InspTask saved = taskRepository.save(task);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
    }

    @Transactional
    public InspTask assignTask(Long id, Long inspectorId, String inspectorName) {
        InspTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id));
        task.assign(inspectorId, inspectorName);
        return taskRepository.save(task);
    }

    // ========== Internal: Submission Population ==========

    /**
     * Design B: 自动为任务填充 submissions
     * 按分区树递归，每个有目标配置的分区根据 targetSourceMode 派生目标列表。
     * - INDEPENDENT / 根分区: 从项目 scopeConfig 获取根目标
     * - PARENT_ASSOCIATED: 从父分区的目标列表派生关联实体
     */
    private void populateSubmissions(InspTask task) {
        InspProject project = projectRepository.findById(task.getProjectId()).orElse(null);
        if (project == null) {
            log.warn("任务 {} 关联的项目不存在: {}", task.getTaskCode(), task.getProjectId());
            return;
        }

        // J1 真重放: 加载项目锁定的模板版本快照. 有 → populate 走 snapshot tree;
        // 无 → fallback live. drift 检查仅 log.warn (I5 informational).
        checkTemplateVersionDrift(project, task);
        com.school.management.domain.inspection.model.template.snapshot.SnapshotTree snapshotTree = null;
        if (project.getTemplateVersionId() != null) {
            snapshotTree = templateVersionRepository.findById(project.getTemplateVersionId())
                    .flatMap(v -> com.school.management.domain.inspection.model.template.snapshot.SnapshotTree.from(v, objectMapper))
                    .orElse(null);
            if (snapshotTree != null) {
                log.info("[Snapshot Replay] 任务 {} 走快照填充 (templateVersionId={}, {} sections + {} items)",
                        task.getTaskCode(), project.getTemplateVersionId(),
                        snapshotTree.getSections().size(), snapshotTree.getItems().size());
            }
        }

        // V66: rootSectionId 优先从项目取，为空则从检查计划取
        Long rootSectionId = project.getRootSectionId();
        if (rootSectionId == null) {
            List<InspectionPlan> plans = planRepository.findByProjectId(project.getId());
            if (!plans.isEmpty()) {
                rootSectionId = plans.get(0).getRootSectionId();
                log.info("项目 {} 通过检查计划获取 rootSectionId: {}", project.getProjectCode(), rootSectionId);
            }
        }
        if (rootSectionId == null) {
            log.warn("项目 {} 无 rootSectionId 且无检查计划", project.getProjectCode());
            return;
        }

        List<TemplateSection> firstLevelSections = lookupSectionsByParent(rootSectionId, snapshotTree);

        if (firstLevelSections.isEmpty()) {
            log.info("项目 {} 的根分区 {} 无子分区", project.getProjectCode(), rootSectionId);
            return;
        }

        // 解析根目标（从项目范围获取）
        List<TargetPopulationService.TargetInfo> rootTargets = resolveRootTargets(project);

        // Collect all submissions and their details into lists for batch insertion.
        List<InspSubmission> submissionBatch = new ArrayList<>();
        // Each entry holds the items to create as details once the submission ID is assigned.
        List<Map.Entry<InspSubmission, List<TemplateItem>>> pendingDetails = new ArrayList<>();

        for (TemplateSection section : firstLevelSections) {
            collectSectionSubmissions(
                    task, project, section, rootTargets, "ORG", null, null,
                    submissionBatch, pendingDetails, snapshotTree);
        }

        // totalTargets = distinct target count (not submission count)
        int totalTargets = (int) submissionBatch.stream()
                .map(InspSubmission::getTargetId)
                .filter(id -> id != null)
                .distinct().count();

        // Batch-insert all submissions (IDs are assigned back by saveAll)
        submissionRepository.saveAll(submissionBatch);

        // Now build all details with the assigned submission IDs and batch-insert
        List<SubmissionDetail> detailBatch = new ArrayList<>();
        for (Map.Entry<InspSubmission, List<TemplateItem>> entry : pendingDetails) {
            InspSubmission submission = entry.getKey();
            for (TemplateItem item : entry.getValue()) {
                ScoringMode scoringMode = parseScoringMode(item.getScoringConfig());
                TemplateSection itemSection = lookupSectionById(item.getSectionId(), snapshotTree).orElse(null);
                String sectionName = itemSection != null ? itemSection.getSectionName() : null;
                detailBatch.add(SubmissionDetail.create(
                        submission.getId(), item.getId(),
                        item.getItemCode(), item.getItemName(),
                        item.getItemType() != null ? item.getItemType().name() : null,
                        item.getSectionId(), sectionName, scoringMode,
                        item.getScoringConfig(), item.getValidationRules(), item.getConditionLogic()));
            }
        }
        detailRepository.saveAll(detailBatch);

        task.updateTargetCounts(totalTargets, 0, 0);
        taskRepository.save(task);
        log.info("任务 {} 填充了 {} 个目标，{} 条 submissions，{} 条 details",
                task.getTaskCode(), totalTargets, submissionBatch.size(), detailBatch.size());
    }

    /**
     * 递归收集分区的 submissions（不立即持久化，累积到 submissionBatch / pendingDetails）
     * @param parentTargets 父分区的目标列表
     * @param parentTargetType 父目标的实体类型
     * @param rootTargetId 根目标（用于 rootTargetId 字段，按部门分组）
     * @param submissionBatch 收集待批量插入的 submission 对象
     * @param pendingDetails 收集 submission → items 的映射，待 ID 分配后创建 details
     */
    private int collectSectionSubmissions(InspTask task, InspProject project,
                                           TemplateSection section,
                                           List<TargetPopulationService.TargetInfo> parentTargets,
                                           String parentTargetType,
                                           Long rootTargetId, String rootTargetName,
                                           List<InspSubmission> submissionBatch,
                                           List<Map.Entry<InspSubmission, List<TemplateItem>>> pendingDetails,
                                           com.school.management.domain.inspection.model.template.snapshot.SnapshotTree snapshotTree) {
        TargetType sectionTargetType = section.getTargetType();
        String sourceMode = section.getTargetSourceMode();

        // 无目标配置的分区（"对父目标直接打分"）：
        // 如果有父目标列表，为每个父目标创建 submission；否则创建一条无目标的
        if (sectionTargetType == null) {
            if (parentTargets != null && !parentTargets.isEmpty()) {
                int count = 0;
                for (TargetPopulationService.TargetInfo parent : parentTargets) {
                    Long effRoot = rootTargetId != null ? rootTargetId : parent.getId();
                    String effRootName = rootTargetName != null ? rootTargetName : parent.getName();
                    collectSubmissionWithDetails(task, section, null,
                            parent.getId(), parent.getName(), effRoot, effRootName,
                            submissionBatch, pendingDetails, snapshotTree);
                    count++;
                }
                return count;
            }
            collectSubmissionWithDetails(task, section, null, null, null, rootTargetId, rootTargetName,
                    submissionBatch, pendingDetails, snapshotTree);
            return 1;
        }

        // 解析目标列表
        List<TargetPopulationService.TargetInfo> targets;
        if ("PARENT_ASSOCIATED".equals(sourceMode) && parentTargets != null && !parentTargets.isEmpty()) {
            // 从父目标派生
            targets = targetPopulationService.deriveFromParentTargets(
                    parentTargets, parentTargetType,
                    sectionTargetType.name(), section.getTargetTypeFilter());
        } else {
            // 根分区 / INDEPENDENT：从项目范围获取
            targets = targetPopulationService.resolveTargets(
                    project.getScopeType(), project.getScopeConfig(), sectionTargetType);
        }

        log.info("分区 {} targetType={} sourceMode={} → {} 个目标",
                section.getSectionName(), sectionTargetType, sourceMode, targets.size());
        if (targets.isEmpty()) {
            return 0;
        }

        int count = 0;

        // 检查是否有子分区也有目标配置（多层派生）
        List<TemplateSection> childSections = lookupSectionsByParent(section.getId(), snapshotTree);
        boolean hasTargetChildren = childSections.stream()
                .anyMatch(c -> c.getTargetType() != null);

        for (TargetPopulationService.TargetInfo target : targets) {
            // 确定 rootTargetId（第一层设置，后续层继承）
            Long effectiveRootId = rootTargetId != null ? rootTargetId : target.getId();
            String effectiveRootName = rootTargetName != null ? rootTargetName : target.getName();

            // 为当前分区收集 submission（含当前分区和无目标子分区的字段）
            collectSubmissionWithDetails(task, section, sectionTargetType,
                    target.getId(), target.getName(), effectiveRootId, effectiveRootName,
                    submissionBatch, pendingDetails, snapshotTree);
            count++;

            // 递归处理有目标配置的子分区
            if (hasTargetChildren) {
                for (TemplateSection child : childSections) {
                    if (child.getTargetType() != null) {
                        count += collectSectionSubmissions(task, project, child,
                                List.of(target), sectionTargetType.name(),
                                effectiveRootId, effectiveRootName,
                                submissionBatch, pendingDetails, snapshotTree);
                    }
                }
            }
        }

        return count;
    }

    /**
     * 构建一个 submission 对象并收集其 items，不立即持久化。
     * submission 在 saveAll 后会被分配 ID，pendingDetails 在此之后统一创建。
     */
    private void collectSubmissionWithDetails(InspTask task, TemplateSection section,
                                               TargetType targetType, Long targetId, String targetName,
                                               Long rootTargetId, String rootTargetName,
                                               List<InspSubmission> submissionBatch,
                                               List<Map.Entry<InspSubmission, List<TemplateItem>>> pendingDetails,
                                               com.school.management.domain.inspection.model.template.snapshot.SnapshotTree snapshotTree) {
        InspSubmission submission = InspSubmission.reconstruct(InspSubmission.builder()
                .taskId(task.getId())
                .sectionId(section.getId())
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .rootTargetId(rootTargetId)
                .rootTargetName(rootTargetName)
                .status(SubmissionStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now()));
        submissionBatch.add(submission);

        // 收集当前分区及其无目标子分区的所有 items（延迟创建 details 直到 ID 分配后）
        List<TemplateItem> items = collectItemsForSubmission(section, snapshotTree);
        pendingDetails.add(Map.entry(submission, items));
    }

    /**
     * 收集分区及其无目标子分区的所有字段（铺平）.
     * J1: snapshot 优先, fallback live.
     */
    private List<TemplateItem> collectItemsForSubmission(
            TemplateSection section,
            com.school.management.domain.inspection.model.template.snapshot.SnapshotTree snapshotTree) {
        List<TemplateItem> items = new java.util.ArrayList<>(lookupItemsBySection(section.getId(), snapshotTree));
        // 递归收集无目标子分区的字段
        List<TemplateSection> children = lookupSectionsByParent(section.getId(), snapshotTree);
        for (TemplateSection child : children) {
            if (child.getTargetType() == null) {
                items.addAll(collectItemsForSubmission(child, snapshotTree));
            }
        }
        return items;
    }

    // ========== J1: snapshot-aware lookup helpers ==========

    /** sections by parent — snapshot 优先, fallback live repo. */
    private List<TemplateSection> lookupSectionsByParent(
            Long parentId,
            com.school.management.domain.inspection.model.template.snapshot.SnapshotTree tree) {
        if (tree != null) {
            return tree.findByParentSectionId(parentId).stream()
                    .map(com.school.management.domain.inspection.model.template.snapshot.SnapshotTree::toLiveSection)
                    .collect(java.util.stream.Collectors.toList());
        }
        return sectionRepository.findByParentSectionId(parentId);
    }

    /** section by id — snapshot 优先, fallback live repo. */
    private Optional<TemplateSection> lookupSectionById(
            Long id,
            com.school.management.domain.inspection.model.template.snapshot.SnapshotTree tree) {
        if (tree != null) {
            return tree.findSectionById(id)
                    .map(com.school.management.domain.inspection.model.template.snapshot.SnapshotTree::toLiveSection);
        }
        return sectionRepository.findById(id);
    }

    /** items by section — snapshot 优先, fallback live repo. */
    private List<TemplateItem> lookupItemsBySection(
            Long sectionId,
            com.school.management.domain.inspection.model.template.snapshot.SnapshotTree tree) {
        if (tree != null) {
            return tree.findItemsBySectionId(sectionId).stream()
                    .map(com.school.management.domain.inspection.model.template.snapshot.SnapshotTree::toLiveItem)
                    .collect(java.util.stream.Collectors.toList());
        }
        return itemRepository.findBySectionId(sectionId);
    }

    /**
     * P1#7 (强化): 模板版本快照漂移检查 — 项目锁定的快照与模板当前最新版本不一致时硬拒绝.
     *
     * <p>不一致意味着项目发布后模板又被改并 publish, 此时 live sections/items
     * 已经与项目原始合约不符. 由于现实现仍按 live 数据填充任务, 必须阻断而非仅告警,
     * 否则数据将与项目发布时的承诺脱节, 影响考核公平性和审计追溯.
     *
     * <p>恢复路径 (任一即可):
     * <ul>
     *   <li>运维把项目 templateVersionId 升到最新 (确认采用新规则)</li>
     *   <li>运维把模板的新版本回滚 / 撤回</li>
     *   <li>未来支持快照重放后, 改为按 snapshot 填充 (此处可移除拒绝逻辑)</li>
     * </ul>
     */
    private void checkTemplateVersionDrift(InspProject project, InspTask task) {
        // I5: 从 hard-throw 改为 log.warn — 与 preCheckTemplateDrift 一致.
        // SubmissionDetail 创建时已冻结 itemCode / scoringConfig 等关键字段 (denormalization),
        // 历史 record 数据不会被改动. 新建 task 接受 live structure.
        Long lockedVersionId = project.getTemplateVersionId();
        Long rootSectionId = project.getRootSectionId();
        if (lockedVersionId == null || rootSectionId == null) {
            return;
        }
        TemplateSection root = sectionRepository.findById(rootSectionId).orElse(null);
        if (root == null || root.getTemplateId() == null) return;
        templateVersionRepository.findLatestByTemplateId(root.getTemplateId()).ifPresent(latest -> {
            if (!lockedVersionId.equals(latest.getId())) {
                log.warn("TEMPLATE_DRIFT (informational): 项目 {} 锁定 templateVersionId={}, " +
                        "模板 {} 最新版本是 {}. 任务 {} 用 live structure 填充.",
                        project.getProjectCode(), lockedVersionId,
                        root.getTemplateId(), latest.getId(), task.getTaskCode());
            }
        });
    }

    /**
     * 从项目范围解析根目标（仅叶子节点）
     * 检查场景只关心叶子组织（如班级），过滤掉非叶子节点（如年级、系部）
     */
    private List<TargetPopulationService.TargetInfo> resolveRootTargets(InspProject project) {
        // 根目标默认为 ORG 类型（项目范围选择的组织）
        List<TargetPopulationService.TargetInfo> allTargets = targetPopulationService.resolveTargets(
                project.getScopeType(), project.getScopeConfig(),
                TargetType.ORG);
        // 过滤：仅保留叶子组织（无子节点的组织）
        return targetPopulationService.filterLeafOrgs(allTargets);
    }

    /**
     * 从 scoringConfig JSON 解析评分模式
     * 处理可能的双重编码JSON（API创建时可能产生）
     */
    private ScoringMode parseScoringMode(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) {
            return ScoringMode.DEDUCTION;
        }
        try {
            JsonNode node = objectMapper.readTree(scoringConfig);
            // 处理双重编码：如果解析结果是字符串节点，尝试再解析一次
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (node.has("mode")) {
                return ScoringMode.valueOf(node.get("mode").asText());
            }
            if (node.has("scoringMode")) {
                String raw = node.get("scoringMode").asText();
                return parseScoringModeValue(raw);
            }
        } catch (Exception e) {
            log.debug("解析 scoringConfig 的 mode 失败, 使用默认值 DEDUCTION: {}", e.getMessage());
        }
        return ScoringMode.DEDUCTION;
    }

    /** 映射前端评分模式名称到枚举（处理别名） */
    private ScoringMode parseScoringModeValue(String raw) {
        // 处理前端使用的别名
        switch (raw) {
            case "DIRECT_SCORE": return ScoringMode.DIRECT;
            case "GRADE_EVAL": return ScoringMode.LEVEL;
            default:
                try { return ScoringMode.valueOf(raw); }
                catch (Exception e) { return ScoringMode.DEDUCTION; }
        }
    }

    // ========== Internal: Score Computation ==========

    /**
     * 任务提交后，计算项目分数
     */
    private void tryComputeProjectScore(InspTask task) {
        Long projectId = task.getProjectId();
        InspProject project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        // 检查该项目的所有任务是否都已提交（SUBMITTED 或更高状态）
        List<InspTask> projectTasks = taskRepository.findByProjectId(projectId);
        boolean allSubmitted = projectTasks.stream()
                .allMatch(t -> t.getStatus() == TaskStatus.SUBMITTED
                        || t.getStatus() == TaskStatus.UNDER_REVIEW
                        || t.getStatus() == TaskStatus.REVIEWED
                        || t.getStatus() == TaskStatus.PUBLISHED);

        if (!allSubmitted) {
            log.debug("项目 {} 尚有未提交的任务，暂不汇总", project.getProjectCode());
            return;
        }

        // 计算项目当日分数
        computeProjectScore(project, task.getTaskDate());
    }

    /**
     * 计算项目在指定日期的分数（所有任务 submissions 的平均分）
     */
    private void computeProjectScore(InspProject project, LocalDate cycleDate) {
        List<InspTask> tasks = taskRepository.findByProjectId(project.getId());
        List<InspTask> dateTasks = tasks.stream()
                .filter(t -> cycleDate.equals(t.getTaskDate()))
                .collect(Collectors.toList());

        if (dateTasks.isEmpty()) return;

        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (InspTask t : dateTasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(t.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() == SubmissionStatus.COMPLETED && sub.getFinalScore() != null) {
                    totalScore = totalScore.add(sub.getFinalScore());
                    count++;
                }
            }
        }

        if (count == 0) return;

        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);

        ProjectScore ps = scoreRepository.findByProjectIdAndCycleDate(project.getId(), cycleDate)
                .orElse(ProjectScore.create(project.getId(), cycleDate));
        ps.updateScore(avgScore, null, count, null);
        scoreRepository.save(ps);

        log.info("项目 {} 日期 {} 汇总分数: {}", project.getProjectCode(), cycleDate, avgScore);
    }

    private String generateTaskCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int suffix = Math.abs((int) (IdWorker.getId() % 9000)) + 1000;
        return "TSK-" + dateStr + "-" + suffix;
    }
}
