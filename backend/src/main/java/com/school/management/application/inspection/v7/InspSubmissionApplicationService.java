package com.school.management.application.inspection.v7;

import com.school.management.application.event.EntityEventApplicationService;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private final EntityEventApplicationService entityEventApplicationService;

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

        // 发布 EntityEvent 数据流：INSP_VIOLATION 和 INSP_GRADE
        publishInspectionEvents(saved, details, project);

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
     * 发布检查相关的 EntityEvent：
     * - INSP_VIOLATION: 每条违规记录（VIOLATION_RECORD 类型字段）
     * - INSP_GRADE: 提交整体的评分事件（主体=检查目标）
     */
    private void publishInspectionEvents(InspSubmission submission,
                                          List<SubmissionDetail> details,
                                          InspProject project) {
        String sourceRefType = "INSP_SUBMISSION";
        Long sourceRefId = submission.getId();
        String sourceModule = "INSPECTION_V7";

        // 1. 遍历 details，发布 INSP_VIOLATION 事件
        for (SubmissionDetail detail : details) {
            if (!"VIOLATION_RECORD".equals(detail.getItemType())) continue;
            String responseValue = detail.getResponseValue();
            if (responseValue == null || responseValue.isBlank()) continue;
            try {
                // responseValue 是 JSON 数组，每项包含违规学生信息
                List<Map<String, Object>> records = objectMapper.readValue(
                        responseValue, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> record : records) {
                    Object studentId = record.get("studentId");
                    Object studentName = record.get("studentName");
                    if (studentId == null) continue;
                    Long sid = Long.valueOf(studentId.toString());
                    String sname = studentName != null ? studentName.toString() : "未知";
                    String payload = objectMapper.writeValueAsString(record);
                    entityEventApplicationService.createEvent(
                            "student", sid, sname,
                            "INSP_VIOLATION", "违规记录",
                            payload, sourceModule,
                            sourceRefType, sourceRefId,
                            "inspection,violation", null, null
                    );
                }
            } catch (Exception e) {
                log.warn("解析 VIOLATION_RECORD responseValue 失败，submissionId={}, detailId={}: {}",
                        submission.getId(), detail.getId(), e.getMessage());
            }
        }

        // 2. 发布 INSP_GRADE 事件（主体=检查目标）
        try {
            String targetType = submission.getTargetType() != null
                    ? submission.getTargetType().name().toLowerCase() : "unknown";
            Long targetId = submission.getTargetId();
            String targetName = submission.getTargetName() != null ? submission.getTargetName() : "";
            Map<String, Object> gradePayload = Map.of(
                    "projectId", project.getId() != null ? project.getId() : 0,
                    "projectName", project.getProjectName() != null ? project.getProjectName() : "",
                    "score", submission.getFinalScore() != null ? submission.getFinalScore() : BigDecimal.ZERO,
                    "grade", submission.getGrade() != null ? submission.getGrade() : "",
                    "passed", Boolean.TRUE.equals(submission.getPassed())
            );
            String gradePayloadJson = objectMapper.writeValueAsString(gradePayload);
            entityEventApplicationService.createEvent(
                    targetType, targetId, targetName,
                    "INSP_GRADE", "检查评分",
                    gradePayloadJson, sourceModule,
                    sourceRefType, sourceRefId,
                    "inspection,grade", null, null
            );
        } catch (Exception e) {
            log.warn("发布 INSP_GRADE 事件失败，submissionId={}: {}", submission.getId(), e.getMessage());
        }
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
