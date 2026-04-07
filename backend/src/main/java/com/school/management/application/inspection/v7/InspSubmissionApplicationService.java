package com.school.management.application.inspection.v7;

import com.school.management.application.event.EntityEventApplicationService;
import com.school.management.application.event.TriggerService;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.domain.inspection.service.ObservationContext;
import com.school.management.domain.inspection.service.ObservationExtractor;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspSubmissionApplicationService {

    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;
    private final InspEvidenceRepository evidenceRepository;
    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final ScoreAggregationService scoreAggregationService;
    private final SpringDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final EntityEventApplicationService entityEventApplicationService;

    @Autowired(required = false)
    private TriggerService triggerService;
    @Autowired(required = false)
    private List<ObservationExtractor> observationExtractors;
    @Autowired(required = false)
    private SubmissionObservationRepository observationRepository;
    @Autowired(required = false)
    private TemplateItemRepository templateItemRepository;

    // ========== Submission CRUD ==========

    @Transactional
    public InspSubmission createSubmission(Long taskId, TargetType targetType,
                                           Long targetId, String targetName) {
        InspSubmission submission = InspSubmission.create(taskId, targetType, targetId, targetName);
        // Auto-set sectionId from project's rootSectionId if not already set
        if (submission.getSectionId() == null) {
            taskRepository.findById(taskId).ifPresent(task ->
                projectRepository.findById(task.getProjectId()).ifPresent(project -> {
                    if (project.getRootSectionId() != null) {
                        submission.setSectionId(project.getRootSectionId());
                    }
                })
            );
        }
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public Optional<InspSubmission> getSubmission(Long id) {
        return submissionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<InspSubmission> listSubmissionsByTask(Long taskId) {
        return submissionRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public List<InspSubmission> listSubmissionsByTarget(Long targetId) {
        return submissionRepository.findByTargetId(targetId);
    }

    // ========== Submission Lifecycle ==========

    @Transactional
    public InspSubmission lockSubmission(Long id) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.lock();
        return submissionRepository.save(submission);
    }

    @Transactional
    public InspSubmission unlockSubmission(Long id) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.unlock();
        return submissionRepository.save(submission);
    }

    @Transactional
    public InspSubmission startFilling(Long id) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.startFilling();
        return submissionRepository.save(submission);
    }

    @Transactional
    public InspSubmission saveFormData(Long id, String formData) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.saveFormData(formData);
        return submissionRepository.save(submission);
    }

    /**
     * 完成提交 — 后端计算分数
     * 读取所有 details 的 responseValue，通过 ScoreAggregationService 计算分数，
     * 将结果写入 submission，并更新 task 的 completedTargets 计数。
     */
    @Transactional
    public InspSubmission completeSubmission(Long id) {
        log.info("completeSubmission start: id={}", id);
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        log.info("submission found: taskId={}, sectionId={}", submission.getTaskId(), submission.getSectionId());

        // 获取关联的 task → project
        InspTask task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalStateException("任务不存在: " + submission.getTaskId()));
        InspProject project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new IllegalStateException("项目不存在: " + task.getProjectId()));
        log.info("project found: id={}, name={}", project.getId(), project.getProjectName());

        // Auto-start if still in PENDING or LOCKED status
        if (submission.getStatus() == SubmissionStatus.PENDING
                || submission.getStatus() == SubmissionStatus.LOCKED) {
            log.info("Auto-transitioning submission {} from {} to IN_PROGRESS",
                    id, submission.getStatus());
            submission.startFilling();
        }

        // 读取所有明细，委托给 ScoreAggregationService 计算分数
        List<SubmissionDetail> details = detailRepository.findBySubmissionId(id);
        log.info("details count: {}", details.size());
        try {
            ScoreAggregationService.ScoreFields fields =
                    scoreAggregationService.computeScoreFields(project, details, submission.getSectionId());
            log.info("score computed: base={}, final={}", fields.baseScore, fields.finalScore);

            submission.complete(fields.baseScore, fields.finalScore,
                    fields.deductionTotal, fields.bonusTotal,
                    fields.scoreBreakdown, fields.grade, fields.passed);
            InspSubmission saved = submissionRepository.save(submission);
            log.info("submission saved with status: {}", saved.getStatus());
            eventPublisher.publishAll(saved.getDomainEvents());
            saved.clearDomainEvents();

            // 发布 EntityEvent 数据流：INSP_VIOLATION 和 INSP_GRADE
            publishInspectionEvents(saved, details, project);

            // 更新 task 的 completedTargets
            updateTaskCompletedCount(task);

            return saved;
        } catch (Exception e) {
            log.error("completeSubmission failed: id={}, error={}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 兼容旧签名（忽略前端传入分数，改为后端计算）
     */
    @Transactional
    public InspSubmission completeSubmission(Long id, BigDecimal baseScore,
                                              BigDecimal finalScore, BigDecimal deductionTotal,
                                              BigDecimal bonusTotal, String scoreBreakdown,
                                              String grade, Boolean passed) {
        return completeSubmission(id);
    }

    /**
     * 发布检查相关的 EntityEvent：
     * - INSP_VIOLATION: 每条违规记录（VIOLATION_RECORD 类型字段）
     * - INSP_GRADE: 提交整体的评分事件（主体=检查目标）
     */
    private void publishInspectionEvents(InspSubmission submission,
                                          List<SubmissionDetail> details,
                                          InspProject project) {
        if (triggerService == null || observationExtractors == null) {
            log.debug("TriggerService 或 ObservationExtractors 未注入，跳过事件发布");
            return;
        }

        try {
            InspTask task = taskRepository.findById(submission.getTaskId()).orElse(null);

            // 构建 ObservationContext（推断 classId）
            Map<Long, String> itemEventTypeMap = buildItemEventTypeMap(details);
            Long orgUnitId = null;
            String className = null;
            if (submission.getTargetType() == TargetType.CLASS) {
                orgUnitId = submission.getTargetId();
                className = submission.getTargetName();
            }
            // TODO: 若 targetType 是 STUDENT，可通过 student.orgUnitId 查询班级

            ObservationContext ctx = ObservationContext.builder()
                    .submissionId(submission.getId())
                    .projectId(project.getId())
                    .taskId(submission.getTaskId())
                    .projectName(project.getProjectName())
                    .targetType(submission.getTargetType() != null ? submission.getTargetType().name() : null)
                    .targetId(submission.getTargetId())
                    .targetName(submission.getTargetName())
                    .orgUnitId(orgUnitId)
                    .className(className)
                    .observedAt(LocalDateTime.now())
                    .itemEventTypeMap(itemEventTypeMap)
                    .build();

            // 1. 提取所有观察（Strategy 模式，零 if/else）
            List<ScoringObservation> allObservations = new ArrayList<>();
            for (SubmissionDetail detail : details) {
                ObservationExtractor extractor = findExtractor(detail.getItemType());
                if (extractor == null) continue;
                allObservations.addAll(extractor.extract(detail, ctx));
            }

            // 2. 批量写入 submission_observations 表
            if (observationRepository != null && !allObservations.isEmpty()) {
                observationRepository.batchInsert(allObservations);
            }

            // 3. 负面观察 → 触发 INSP_ITEM_RESULT 事件
            for (ScoringObservation obs : allObservations) {
                if (!obs.isNegative()) continue;
                try {
                    Map<String, Object> context = obs.toContextMap();
                    context.put("_refType", "inspection_submission");
                    context.put("_refId", submission.getId());
                    context.put("projectName", project.getProjectName() != null ? project.getProjectName() : "");
                    triggerService.fire("INSP_ITEM_RESULT", context);
                } catch (Exception e) {
                    log.warn("触发 INSP_ITEM_RESULT 失败, obs={}: {}", obs.getItemName(), e.getMessage());
                }
            }

            // 4. 等级事件 → 触发 INSP_GRADE_RESULT
            if (submission.getGrade() != null && submission.getTargetId() != null) {
                try {
                    boolean passed = Boolean.TRUE.equals(submission.getPassed());
                    Map<String, Object> gradeCtx = new HashMap<>();
                    gradeCtx.put("isNegative", !passed);
                    gradeCtx.put("severity", passed ? "LOW" : "HIGH");
                    gradeCtx.put("targetId", submission.getTargetId());
                    gradeCtx.put("targetName", submission.getTargetName() != null ? submission.getTargetName() : "");
                    gradeCtx.put("targetType", ctx.getTargetType());
                    gradeCtx.put("score", submission.getFinalScore() != null ? submission.getFinalScore() : BigDecimal.ZERO);
                    gradeCtx.put("grade", submission.getGrade());
                    gradeCtx.put("passed", passed);
                    gradeCtx.put("projectName", project.getProjectName() != null ? project.getProjectName() : "");
                    gradeCtx.put("_refType", "inspection_submission");
                    gradeCtx.put("_refId", submission.getId());
                    triggerService.fire("INSP_GRADE_RESULT", gradeCtx);
                } catch (Exception e) {
                    log.warn("触发 INSP_GRADE_RESULT 失败: {}", e.getMessage());
                }
            }

            // 5. 完成事件 → 触发 INSP_RECORD_COMPLETE
            try {
                String inspectorName = "";
                try { inspectorName = SecurityUtils.getCurrentUsername(); } catch (Exception ignored) {}
                Map<String, Object> completeCtx = new HashMap<>();
                completeCtx.put("isNegative", false);
                completeCtx.put("taskId", submission.getTaskId());
                completeCtx.put("targetId", submission.getTargetId());
                completeCtx.put("targetName", submission.getTargetName() != null ? submission.getTargetName() : "");
                completeCtx.put("score", submission.getFinalScore() != null ? submission.getFinalScore() : BigDecimal.ZERO);
                completeCtx.put("inspectorName", inspectorName);
                completeCtx.put("_refType", "inspection_submission");
                completeCtx.put("_refId", submission.getId());
                triggerService.fire("INSP_RECORD_COMPLETE", completeCtx);
            } catch (Exception e) {
                log.warn("触发 INSP_RECORD_COMPLETE 失败: {}", e.getMessage());
            }

            log.info("检查事件发布完成: submissionId={}, 观察数={}, 负面={}",
                    submission.getId(), allObservations.size(),
                    allObservations.stream().filter(ScoringObservation::isNegative).count());

        } catch (Exception e) {
            log.error("发布检查事件异常, submissionId={}: {}", submission.getId(), e.getMessage());
        }
    }

    private ObservationExtractor findExtractor(String itemType) {
        if (observationExtractors != null) {
            for (ObservationExtractor ext : observationExtractors) {
                if (ext.supports(itemType) && !(ext instanceof com.school.management.domain.inspection.service.DefaultObservationExtractor)) {
                    return ext;
                }
            }
        }
        // 兜底用 default
        return observationExtractors != null
                ? observationExtractors.stream()
                    .filter(e -> e instanceof com.school.management.domain.inspection.service.DefaultObservationExtractor)
                    .findFirst().orElse(observationExtractors.get(0))
                : null;
    }

    /**
     * 构建 templateItemId → linkedEventTypeCode 映射
     */
    private Map<Long, String> buildItemEventTypeMap(List<SubmissionDetail> details) {
        Map<Long, String> map = new HashMap<>();
        if (templateItemRepository == null) return map;
        Set<Long> itemIds = details.stream()
                .map(SubmissionDetail::getTemplateItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Long itemId : itemIds) {
            try {
                Optional<TemplateItem> item = templateItemRepository.findById(itemId);
                item.ifPresent(ti -> {
                    if (ti.getLinkedEventTypeCode() != null && !ti.getLinkedEventTypeCode().isBlank()) {
                        map.put(itemId, ti.getLinkedEventTypeCode());
                    }
                });
            } catch (Exception ignored) {}
        }
        return map;
    }

    /**
     * 更新任务的已完成目标计数
     */
    private void updateTaskCompletedCount(InspTask task) {
        List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
        int completed = 0;
        int skipped = 0;
        for (InspSubmission s : submissions) {
            if (s.getStatus() == SubmissionStatus.COMPLETED) completed++;
            else if (s.getStatus() == SubmissionStatus.SKIPPED) skipped++;
        }
        task.updateTargetCounts(submissions.size(), completed, skipped);
        taskRepository.save(task);
    }

    // ========== Cascade Score Recalculation ==========

    /**
     * 级联重算：从某个 submission 开始，重算整条链。
     * 调用场景：修改 SubmissionDetail 分数后、申诉通过后，以及管理员手动触发。
     *
     * 链路：SubmissionDetail → Submission 总分 → Task completedTargets 计数 → ProjectScore
     *
     * 实际计算逻辑委托给 ScoreAggregationService。
     */
    @Transactional
    public InspSubmission recalculateFromSubmission(Long submissionId) {
        return scoreAggregationService.recalculateFromSubmission(submissionId);
    }

    @Transactional
    public InspSubmission skipSubmission(Long id) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.skip();
        return submissionRepository.save(submission);
    }

    // ========== Submission Details ==========

    @Transactional
    public SubmissionDetail createDetail(Long submissionId, Long templateItemId,
                                         String itemCode, String itemName, String itemType) {
        SubmissionDetail detail = SubmissionDetail.create(submissionId, templateItemId,
                itemCode, itemName, itemType);
        return detailRepository.save(detail);
    }

    @Transactional
    public SubmissionDetail createDetail(Long submissionId, Long templateItemId,
                                         String itemCode, String itemName, String itemType,
                                         Long sectionId, String sectionName, ScoringMode scoringMode) {
        SubmissionDetail detail = SubmissionDetail.create(submissionId, templateItemId,
                itemCode, itemName, itemType, sectionId, sectionName, scoringMode);
        return detailRepository.save(detail);
    }

    @Transactional
    public SubmissionDetail createDetail(Long submissionId, Long templateItemId,
                                         String itemCode, String itemName, String itemType,
                                         Long sectionId, String sectionName, ScoringMode scoringMode,
                                         String scoringConfig, String validationRules, String conditionLogic) {
        SubmissionDetail detail = SubmissionDetail.create(submissionId, templateItemId,
                itemCode, itemName, itemType, sectionId, sectionName, scoringMode,
                scoringConfig, validationRules, conditionLogic);
        return detailRepository.save(detail);
    }

    @Transactional(readOnly = true)
    public Optional<SubmissionDetail> getDetail(Long id) {
        return detailRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDetail> listDetailsBySubmission(Long submissionId) {
        return detailRepository.findBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDetail> listFlaggedDetails(Long submissionId) {
        return detailRepository.findFlaggedBySubmissionId(submissionId);
    }

    /**
     * 更新明细的作答（responseValue、score、dimensions）。
     * 如果所属 submission 已处于 COMPLETED 状态，自动触发级联重算。
     */
    @Transactional
    public SubmissionDetail updateDetailResponse(Long detailId, String responseValue,
                                                  ScoringMode scoringMode, BigDecimal score,
                                                  String dimensions) {
        SubmissionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
        detail.updateResponse(responseValue, scoringMode, score, dimensions);
        detailRepository.save(detail);

        // 如果所属 submission 已完成，自动触发级联重算
        InspSubmission submission = submissionRepository.findById(detail.getSubmissionId())
                .orElse(null);
        if (submission != null && submission.getStatus() == SubmissionStatus.COMPLETED) {
            scoreAggregationService.recalculateFromSubmission(submission.getId());
        }

        // 重新查询返回最新状态
        return detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
    }

    @Transactional
    public SubmissionDetail updateDetailRemark(Long detailId, String remark) {
        SubmissionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
        detail.updateRemark(remark);
        return detailRepository.save(detail);
    }

    @Transactional
    public SubmissionDetail flagDetail(Long detailId, String reason) {
        SubmissionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
        detail.flag(reason);
        return detailRepository.save(detail);
    }

    @Transactional
    public SubmissionDetail unflagDetail(Long detailId) {
        SubmissionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
        detail.unflag();
        return detailRepository.save(detail);
    }

    @Transactional
    public void deleteDetail(Long detailId) {
        detailRepository.deleteById(detailId);
    }

    // ========== Evidence ==========

    @Transactional
    public InspEvidence addEvidence(Long submissionId, Long detailId,
                                    EvidenceType evidenceType, String fileName,
                                    String fileUrl) {
        InspEvidence evidence = InspEvidence.create(submissionId, evidenceType, fileName, fileUrl);
        return evidenceRepository.save(evidence);
    }

    @Transactional(readOnly = true)
    public List<InspEvidence> listEvidenceBySubmission(Long submissionId) {
        return evidenceRepository.findBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public List<InspEvidence> listEvidenceByDetail(Long detailId) {
        return evidenceRepository.findByDetailId(detailId);
    }

    @Transactional
    public void deleteEvidence(Long evidenceId) {
        evidenceRepository.deleteById(evidenceId);
    }
}
