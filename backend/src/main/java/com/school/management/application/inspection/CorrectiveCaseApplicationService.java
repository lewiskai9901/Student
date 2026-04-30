package com.school.management.application.inspection;

import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.model.corrective.*;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.repository.CorrectiveCaseRepository;
import com.school.management.domain.inspection.repository.CorrectiveSubtaskRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CorrectiveCaseApplicationService {

    private final CorrectiveCaseRepository caseRepository;
    private final CorrectiveSubtaskRepository subtaskRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final InspectionAuditLogger auditLogger;
    private final InspProjectRepository projectRepository;
    private final InspSubmissionRepository submissionRepository;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private TriggerService triggerService;

    // ========== CRUD ==========

    @Transactional
    public CorrectiveCase createCase(String caseCode, String issueDescription,
                                     CasePriority priority, Long submissionId, Long detailId,
                                     Long projectId, Long taskId,
                                     String targetType, Long targetId, String targetName,
                                     String requiredAction, LocalDateTime deadline,
                                     Long createdBy) {
        CorrectiveCase c = CorrectiveCase.create(caseCode, issueDescription, priority, createdBy);

        // Set additional fields via builder + reconstruct is not possible after create,
        // so we use the builder approach directly
        // review #1: 从 submission 继承 orgUnitId, 否则数据权限会过滤掉本案例
        Long orgUnitId = null;
        if (submissionId != null) {
            orgUnitId = submissionRepository.findById(submissionId)
                    .map(s -> s.getOrgUnitId())
                    .orElse(null);
        }
        // 兜底: 当 targetType=ORG 时直接用 targetId
        if (orgUnitId == null && "ORG".equals(targetType) && targetId != null) {
            orgUnitId = targetId;
        }

        CorrectiveCase fullCase = CorrectiveCase.reconstruct(CorrectiveCase.builder()
                .caseCode(caseCode)
                .issueDescription(issueDescription)
                .priority(priority)
                .submissionId(submissionId)
                .detailId(detailId)
                .projectId(projectId)
                .taskId(taskId)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .orgUnitId(orgUnitId)
                .requiredAction(requiredAction)
                .deadline(deadline)
                .createdBy(createdBy)
                .status(CaseStatus.OPEN)
                .escalationLevel(0));

        CorrectiveCase saved = caseRepository.save(fullCase);
        // Publish the created event with actual ID
        eventPublisher.publish(new com.school.management.domain.inspection.event.CorrectiveCaseCreatedEvent(
                saved.getId(), caseCode, submissionId, detailId, priority != null ? priority.name() : null));
        // P1#9: 通知触发点 — 让当事人感知有新整改要求
        fireTrigger(InspectionTriggerPoints.INSP_CORRECTIVE_CREATED, ctx -> {
            ctx.put("caseId", saved.getId());
            ctx.put("caseCode", caseCode);
            ctx.put("subjectType", targetType);
            ctx.put("subjectId", targetId);
            ctx.put("subjectName", targetName != null ? targetName : "");
            ctx.put("priority", priority != null ? priority.name() : "");
            ctx.put("deadline", deadline != null ? deadline.toLocalDate().toString() : ""); // review #3: 仅日期
            ctx.put("issueDescription", issueDescription != null ? issueDescription : "");
            ctx.put("_refType", "corrective_case");
            ctx.put("_refId", saved.getId());
        });
        log.info("Created corrective case: code={}, priority={}", caseCode, priority);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<CorrectiveCase> getCase(Long id) {
        return caseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CorrectiveCase> getCaseByCaseCode(String caseCode) {
        return caseRepository.findByCaseCode(caseCode);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByProject(Long projectId) {
        return caseRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listBySubmission(Long submissionId) {
        return caseRepository.findBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByAssignee(Long assigneeId) {
        return caseRepository.findByAssigneeId(assigneeId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByStatus(CaseStatus status) {
        return caseRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listByTask(Long taskId) {
        return caseRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listAll() {
        return caseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CorrectiveCase> listOverdue() {
        return caseRepository.findOverdue(LocalDateTime.now());
    }

    @Transactional
    public void deleteCase(Long id) {
        caseRepository.deleteById(id);
    }

    // ========== Lifecycle ==========

    @Transactional
    public CorrectiveCase assignCase(Long id, Long assigneeId, String assigneeName) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.assign(assigneeId, assigneeName);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        // P1#9: 通知触发点 — 通知责任人有新分派
        final CorrectiveCase finalCase = c;
        fireTrigger(InspectionTriggerPoints.INSP_CORRECTIVE_ASSIGNED, ctx -> {
            ctx.put("caseId", finalCase.getId());
            ctx.put("caseCode", finalCase.getCaseCode());
            ctx.put("assigneeId", assigneeId);
            ctx.put("assigneeName", assigneeName != null ? assigneeName : "");
            ctx.put("deadline", finalCase.getDeadline() != null ? finalCase.getDeadline().toLocalDate().toString() : ""); // review #3: 仅日期
            ctx.put("_refType", "corrective_case");
            ctx.put("_refId", finalCase.getId());
        });
        log.info("Assigned corrective case {} to {}", c.getCaseCode(), assigneeName);
        return c;
    }

    @Transactional
    public CorrectiveCase startWork(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.startWork();
        return caseRepository.save(c);
    }

    @Transactional
    public CorrectiveCase submitCorrection(Long id, String correctionNote, List<Long> evidenceIds) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.submitCorrection(correctionNote, evidenceIds);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Correction submitted for case {}", c.getCaseCode());
        return c;
    }

    @Transactional
    public CorrectiveCase verifyCase(Long id, Long verifierId, String verifierName, String note) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.verify(verifierId, verifierName, note);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        // P1#9: 通知触发点 — 通知当事人和责任人验证通过
        final CorrectiveCase finalCase = c;
        fireTrigger(InspectionTriggerPoints.INSP_CORRECTIVE_VERIFIED, ctx -> {
            ctx.put("caseId", finalCase.getId());
            ctx.put("caseCode", finalCase.getCaseCode());
            ctx.put("verificationResult", "PASSED");
            ctx.put("subjectType", finalCase.getTargetType() != null ? finalCase.getTargetType() : "");
            ctx.put("subjectId", finalCase.getTargetId());
            ctx.put("assigneeId", finalCase.getAssigneeId());
            ctx.put("verifierComment", note != null ? note : "");
            ctx.put("_refType", "corrective_case");
            ctx.put("_refId", finalCase.getId());
        });
        log.info("Corrective case {} verified by {}", c.getCaseCode(), verifierName);
        return c;
    }

    @Transactional
    public CorrectiveCase rejectCase(Long id, Long verifierId, String verifierName, String reason) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.reject(verifierId, verifierName, reason);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        // P1#9 + review #B: 触发驳回通知点 — 用独立 INSP_CORRECTIVE_REJECTED 避免与 VERIFIED 共享幂等 key
        final CorrectiveCase finalCase = c;
        fireTrigger(InspectionTriggerPoints.INSP_CORRECTIVE_REJECTED, ctx -> {
            ctx.put("caseId", finalCase.getId());
            ctx.put("caseCode", finalCase.getCaseCode());
            ctx.put("verificationResult", "REJECTED");
            ctx.put("subjectType", finalCase.getTargetType() != null ? finalCase.getTargetType() : "");
            ctx.put("subjectId", finalCase.getTargetId());
            ctx.put("assigneeId", finalCase.getAssigneeId());
            ctx.put("verifierComment", reason != null ? reason : "");
            ctx.put("_refType", "corrective_case");
            ctx.put("_refId", finalCase.getId());
        });
        log.info("Corrective case {} rejected: {}", c.getCaseCode(), reason);
        return c;
    }

    /**
     * P1#9: TriggerService 兼容包装 — TriggerService 在测试 / 早期启动时可能 null,
     * 触发失败也不该影响主业务. ctxBuilder 仅做填充, 调用方不必担心异常.
     */
    private void fireTrigger(String pointCode, java.util.function.Consumer<Map<String, Object>> ctxBuilder) {
        if (triggerService == null) return;
        try {
            Map<String, Object> ctx = new HashMap<>();
            ctxBuilder.accept(ctx);
            triggerService.fire(pointCode, ctx);
        } catch (Exception e) {
            log.warn("触发 {} 失败: {}", pointCode, e.getMessage());
        }
    }

    @Transactional
    public CorrectiveCase closeCase(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        c.close();
        return caseRepository.save(c);
    }

    /**
     * P1#6: 责任人退出自动重派 — 当 userId 因离职/退出项目/调整无法继续负责案例时,
     * 批量解除其所有 ASSIGNED/IN_PROGRESS 案例, 状态回 OPEN, 等待重新分派.
     *
     * <p>可选: 提供 fallbackAssigneeId 直接重派给指定用户 (常用于把所有案例转交给上级).
     * 不传 fallback 则停留在 OPEN 状态, 由管理员通过 UI 逐一分配.
     *
     * @param userId 离职 / 退出的用户 ID
     * @param reason 退出原因 (审计用)
     * @param fallbackAssigneeId 可选 — 自动重派给此用户
     * @param fallbackAssigneeName 可选 — 自动重派的用户名
     * @return 受影响的案例数
     */
    /**
     * review #5 + #11: 每条 case 独立事务 (partial-success 语义), 校验 fallback 用户合法性.
     */
    public int reassignDepartedAssignee(Long userId, String reason,
                                         Long fallbackAssigneeId, String fallbackAssigneeName) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        // review #11: 后端校验 fallbackUserId 是否为合法且未被禁用的用户
        if (fallbackAssigneeId != null) {
            validateUserExists(fallbackAssigneeId, "fallbackAssigneeId");
        }
        List<CorrectiveCase> cases = caseRepository.findByAssigneeId(userId);
        int affected = 0;
        for (CorrectiveCase c : cases) {
            if (c.getStatus() != CaseStatus.ASSIGNED && c.getStatus() != CaseStatus.IN_PROGRESS) {
                continue;
            }
            // review #5: 每条用独立事务, 一条失败不回滚之前已成功的
            Boolean ok = transactionTemplate.execute(status -> {
                try {
                    CorrectiveCase fresh = caseRepository.findById(c.getId()).orElse(null);
                    if (fresh == null) return false;
                    fresh.unassign(reason);
                    if (fallbackAssigneeId != null) {
                        fresh.assign(fallbackAssigneeId, fallbackAssigneeName);
                    }
                    caseRepository.save(fresh);
                    eventPublisher.publishAll(fresh.getDomainEvents());
                    fresh.clearDomainEvents();
                    auditLogger.log("CorrectiveCase", fresh.getId(), fresh.getCaseCode(),
                            "CASE_UNASSIGNED", reason,
                            Map.of("previousAssigneeId", userId,
                                    "fallbackAssigneeId", fallbackAssigneeId != null ? fallbackAssigneeId : 0L));
                    return true;
                } catch (Exception e) {
                    log.error("Reassign case {} failed (rolled back): {}", c.getCaseCode(), e.getMessage());
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.TRUE.equals(ok)) {
                affected++;
                log.info("Case {} reassigned: previousAssignee={} → {}, reason={}",
                        c.getCaseCode(), userId,
                        fallbackAssigneeId != null ? fallbackAssigneeId : "[unassigned]",
                        reason);
            }
        }
        log.info("Departed user {} reassign summary: {} cases affected (reason={})",
                userId, affected, reason);
        return affected;
    }

    /** review #11: 校验用户存在且未被删除/禁用 */
    private void validateUserExists(Long userId, String paramName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND deleted = 0",
                Integer.class, userId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException(paramName + " 对应用户不存在或已删除: " + userId);
        }
    }

    @Transactional
    public CorrectiveCase escalateCase(Long id) {
        CorrectiveCase c = caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id));
        // review #E: 项目级 maxEscalationLevel 优先
        Integer projectMax = c.getProjectId() != null
                ? projectRepository.findById(c.getProjectId())
                        .map(InspProject::getMaxEscalationLevel).orElse(null)
                : null;
        c.escalate(projectMax);
        caseRepository.save(c);
        eventPublisher.publishAll(c.getDomainEvents());
        c.clearDomainEvents();
        log.info("Corrective case {} escalated to level {}", c.getCaseCode(), c.getEscalationLevel());
        return c;
    }

    // ========== Subtask Management ==========

    @Transactional
    public CorrectiveSubtask createSubtask(Long caseId, String subtaskName, String description,
                                           Long assigneeId, Integer priority, LocalDate dueDate,
                                           Long createdBy) {
        // Verify case exists
        caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + caseId));

        CorrectiveSubtask subtask = CorrectiveSubtask.reconstruct(CorrectiveSubtask.builder()
                .caseId(caseId)
                .subtaskName(subtaskName)
                .description(description)
                .assigneeId(assigneeId)
                .priority(priority)
                .dueDate(dueDate)
                .createdBy(createdBy)
                .status("PENDING"));
        CorrectiveSubtask saved = subtaskRepository.save(subtask);
        log.info("Created subtask for case {}: subtaskId={}, name={}", caseId, saved.getId(), subtaskName);
        return saved;
    }

    @Transactional
    public CorrectiveSubtask updateSubtask(Long subtaskId, String subtaskName, String description,
                                           Long assigneeId, Integer priority, LocalDate dueDate) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.updateDetails(subtaskName, description, assigneeId, priority, dueDate);
        subtaskRepository.save(subtask);
        log.info("Updated subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask startSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.start();
        subtaskRepository.save(subtask);
        log.info("Started subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask completeSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.complete();
        subtaskRepository.save(subtask);
        log.info("Completed subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional
    public CorrectiveSubtask blockSubtask(Long subtaskId) {
        CorrectiveSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在: " + subtaskId));
        subtask.block();
        subtaskRepository.save(subtask);
        log.info("Blocked subtask: id={}", subtaskId);
        return subtask;
    }

    @Transactional(readOnly = true)
    public List<CorrectiveSubtask> getSubtasks(Long caseId) {
        return subtaskRepository.findByCaseId(caseId);
    }

    @Transactional
    public void deleteSubtask(Long subtaskId) {
        subtaskRepository.deleteById(subtaskId);
        log.info("Deleted subtask: id={}", subtaskId);
    }
}
