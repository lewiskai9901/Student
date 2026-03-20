package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

    // ========== Submission CRUD ==========

    @Transactional
    public InspSubmission createSubmission(Long taskId, TargetType targetType,
                                           Long targetId, String targetName) {
        InspSubmission submission = InspSubmission.create(taskId, targetType, targetId, targetName);
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
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));

        // 获取关联的 task → project
        InspTask task = taskRepository.findById(submission.getTaskId())
                .orElseThrow(() -> new IllegalStateException("任务不存在: " + submission.getTaskId()));
        InspProject project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> new IllegalStateException("项目不存在: " + task.getProjectId()));

        // 读取所有明细，委托给 ScoreAggregationService 计算分数
        List<SubmissionDetail> details = detailRepository.findBySubmissionId(id);
        ScoreAggregationService.ScoreFields fields =
                scoreAggregationService.computeScoreFields(project, details);

        submission.complete(fields.baseScore, fields.finalScore,
                fields.deductionTotal, fields.bonusTotal,
                fields.scoreBreakdown, fields.grade, fields.passed);
        InspSubmission saved = submissionRepository.save(submission);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();

        // 更新 task 的 completedTargets
        updateTaskCompletedCount(task);

        return saved;
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
     * 更新任务的已完成目标计数
     */
    private void updateTaskCompletedCount(InspTask task) {
        List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
        int completed = (int) submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.COMPLETED)
                .count();
        int skipped = (int) submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SKIPPED)
                .count();
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
