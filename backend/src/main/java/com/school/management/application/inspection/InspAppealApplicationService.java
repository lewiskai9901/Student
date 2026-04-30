package com.school.management.application.inspection;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.model.appeal.AppealStatus;
import com.school.management.domain.inspection.model.appeal.InspAppeal;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.repository.InspAppealRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 申诉应用服务 (P1#8) — 让 inspection:appeal:* 三个权限点真正落到工作流上.
 *
 * <p>流程:
 * <ol>
 *   <li>当事人对一条 {@link SubmissionDetail} 不服 → 调 {@link #submitAppeal} 创建申诉 (PENDING)</li>
 *   <li>审核员看待审清单 → 调 {@link #approve} 通过 (APPROVED) 或 {@link #reject} 驳回 (REJECTED)</li>
 *   <li>提交人可主动 {@link #withdraw} 撤回 (WITHDRAWN, 仅 PENDING 时)</li>
 * </ol>
 *
 * <p>事件: AppealSubmittedEvent / AppealApprovedEvent / AppealRejectedEvent
 * 后续可让监听器根据 APPROVED 自动调整 SubmissionDetail.score 并重算 ProjectScore.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspAppealApplicationService {

    private final InspAppealRepository appealRepository;
    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final InspectionAuditLogger auditLogger;

    /** review #F: 系统默认申诉时效 (天) — 项目可通过 InspProject.appealWindowDays 覆盖 */
    private static final int DEFAULT_APPEAL_WINDOW_DAYS = 7;

    @Autowired(required = false)
    private TriggerService triggerService;

    @Transactional
    public InspAppeal submitAppeal(Long submissionDetailId, Long submitterUserId, String submitterName,
                                    String reason, String attachments, BigDecimal expectedAdjustment) {
        SubmissionDetail detail = detailRepository.findById(submissionDetailId)
                .orElseThrow(() -> new IllegalArgumentException("扣分明细不存在: " + submissionDetailId));

        // 同一明细不允许重复提交 PENDING 申诉
        boolean hasPending = appealRepository.findBySubmissionDetailId(submissionDetailId).stream()
                .anyMatch(a -> a.getStatus() == AppealStatus.PENDING);
        if (hasPending) {
            throw new IllegalStateException("该扣分明细已存在待审核的申诉, 请勿重复提交");
        }

        // 兜底链: 从 detail → submission → task 解析 projectId / taskId / subjectType / subjectId
        Long submissionId = detail.getSubmissionId();
        Long taskId = null;
        Long projectId = null;
        String subjectType = null;
        Long subjectId = null;
        InspTask task = null;
        if (submissionId != null) {
            Optional<InspSubmission> subOpt = submissionRepository.findById(submissionId);
            if (subOpt.isPresent()) {
                InspSubmission sub = subOpt.get();
                taskId = sub.getTaskId();
                TargetType tt = sub.getTargetType();
                if (tt != null) {
                    subjectType = (tt == TargetType.ORG ? "ORG_UNIT" : tt.name());
                }
                subjectId = sub.getTargetId();
            }
        }
        if (taskId != null) {
            Optional<InspTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                task = taskOpt.get();
                projectId = task.getProjectId();
            }
        }

        // review #4: 双层校验 — 任务必须 PUBLISHED 且在时效窗内
        if (task != null) {
            if (task.getPublishedAt() == null) {
                throw new IllegalStateException(
                        "任务尚未发布, 扣分判定未正式生效, 暂不能申诉. 请等待任务发布后再申诉.");
            }
            Integer windowDays = projectId != null
                    ? projectRepository.findById(projectId)
                            .map(InspProject::getAppealWindowDays).orElse(null)
                    : null;
            int effectiveWindow = windowDays != null && windowDays > 0
                    ? windowDays : DEFAULT_APPEAL_WINDOW_DAYS;
            LocalDateTime cutoff = task.getPublishedAt().plusDays(effectiveWindow);
            if (LocalDateTime.now().isAfter(cutoff)) {
                // 友好格式化日期: 只显示日期不显示毫秒
                String publishedDate = task.getPublishedAt().toLocalDate().toString();
                throw new IllegalStateException(
                        "申诉时效已过 — 任务发布后 " + effectiveWindow + " 天内方可申诉, " +
                        "任务于 " + publishedDate + " 发布.");
            }
        }

        String appealCode = generateAppealCode();
        InspAppeal appeal = InspAppeal.submit(
                appealCode, submissionDetailId, submissionId, taskId, projectId,
                subjectType, subjectId, submitterUserId, submitterName,
                reason, attachments, expectedAdjustment);
        InspAppeal saved;
        try {
            saved = appealRepository.save(appeal);
        } catch (DuplicateKeyException dup) {
            // A: pending_lock_key 唯一约束命中 — 并发提交场景, 应用层 anyMatch 校验和 DB 之间被穿透
            throw new IllegalStateException("该扣分明细已存在待审核的申诉, 请勿重复提交", dup);
        }
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        // P1#9: 触发当事人通知点 — 申诉人确认收到 + 审核员有待审
        fireTrigger(InspectionTriggerPoints.INSP_APPEAL_SUBMITTED, ctx -> {
            ctx.put("appealId", saved.getId());
            ctx.put("appealCode", saved.getAppealCode());
            ctx.put("submitterUserId", submitterUserId);
            ctx.put("submitterName", submitterName != null ? submitterName : "");
            ctx.put("reason", reason != null ? reason : "");
            ctx.put("submissionDetailId", submissionDetailId);
            ctx.put("_refType", "inspection_appeal");
            ctx.put("_refId", saved.getId());
        });
        log.info("Appeal submitted: code={}, detailId={}, submitter={}",
                saved.getAppealCode(), submissionDetailId, submitterUserId);
        return saved;
    }

    @Transactional
    public InspAppeal approve(Long id, Long reviewerId, String reviewerName,
                               String comment, BigDecimal finalAdjustment) {
        InspAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + id));
        appeal.approve(reviewerId, reviewerName, comment, finalAdjustment);
        InspAppeal saved = appealRepository.save(appeal);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        // P1#9: 触发申诉人通知 — 通过结果
        fireTrigger(InspectionTriggerPoints.INSP_APPEAL_REVIEWED, ctx -> {
            ctx.put("appealId", saved.getId());
            ctx.put("appealCode", saved.getAppealCode());
            ctx.put("submitterUserId", saved.getSubmitterUserId());
            ctx.put("reviewResult", "APPROVED");
            ctx.put("reviewerId", reviewerId);
            ctx.put("reviewerComment", comment != null ? comment : "");
            ctx.put("finalAdjustment", finalAdjustment != null ? finalAdjustment : BigDecimal.ZERO);
            ctx.put("_refType", "inspection_appeal");
            ctx.put("_refId", saved.getId());
        });
        // C: 审计日志
        auditLogger.log("InspAppeal", saved.getId(), saved.getAppealCode(),
                "APPEAL_APPROVED", comment,
                Map.of("finalAdjustment", finalAdjustment != null ? finalAdjustment : BigDecimal.ZERO,
                        "submitterUserId", saved.getSubmitterUserId() != null ? saved.getSubmitterUserId() : 0L,
                        "submissionDetailId", saved.getSubmissionDetailId() != null ? saved.getSubmissionDetailId() : 0L));
        return saved;
    }

    @Transactional
    public InspAppeal reject(Long id, Long reviewerId, String reviewerName, String comment) {
        InspAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + id));
        appeal.reject(reviewerId, reviewerName, comment);
        InspAppeal saved = appealRepository.save(appeal);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        // P1#9: 触发申诉人通知 — 驳回结果
        fireTrigger(InspectionTriggerPoints.INSP_APPEAL_REVIEWED, ctx -> {
            ctx.put("appealId", saved.getId());
            ctx.put("appealCode", saved.getAppealCode());
            ctx.put("submitterUserId", saved.getSubmitterUserId());
            ctx.put("reviewResult", "REJECTED");
            ctx.put("reviewerId", reviewerId);
            ctx.put("reviewerComment", comment != null ? comment : "");
            ctx.put("finalAdjustment", BigDecimal.ZERO);
            ctx.put("_refType", "inspection_appeal");
            ctx.put("_refId", saved.getId());
        });
        // C: 审计日志
        auditLogger.log("InspAppeal", saved.getId(), saved.getAppealCode(),
                "APPEAL_REJECTED", comment,
                Map.of("submitterUserId", saved.getSubmitterUserId() != null ? saved.getSubmitterUserId() : 0L,
                        "submissionDetailId", saved.getSubmissionDetailId() != null ? saved.getSubmissionDetailId() : 0L));
        return saved;
    }

    /** P1#9: 触发器调用安全包装 — TriggerService 可能不存在或失败, 不影响主业务 */
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
    public InspAppeal withdraw(Long id, Long byUserId) {
        InspAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + id));
        appeal.withdraw(byUserId);
        return appealRepository.save(appeal);
    }

    @Transactional(readOnly = true)
    public Optional<InspAppeal> getAppeal(Long id) {
        return appealRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspAppeal> listMyAppeals(Long submitterUserId) {
        return appealRepository.findBySubmitterUserId(submitterUserId);
    }

    @Transactional(readOnly = true)
    public List<InspAppeal> listPending() {
        return appealRepository.findByStatus(AppealStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<InspAppeal> listByProject(Long projectId) {
        return appealRepository.findByProjectId(projectId);
    }

    private String generateAppealCode() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int suffix = Math.abs((int) (IdWorker.getId() % 9000)) + 1000;
        return "APL-" + dateStr + "-" + suffix;
    }
}
