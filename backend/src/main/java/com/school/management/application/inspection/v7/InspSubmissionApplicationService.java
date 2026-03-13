package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.repository.v7.InspEvidenceRepository;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.v7.SubmissionDetailRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
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
    private final SpringDomainEventPublisher eventPublisher;

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

    @Transactional
    public InspSubmission completeSubmission(Long id, BigDecimal baseScore,
                                              BigDecimal finalScore, BigDecimal deductionTotal,
                                              BigDecimal bonusTotal, String scoreBreakdown,
                                              String grade, Boolean passed) {
        InspSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id));
        submission.complete(baseScore, finalScore, deductionTotal, bonusTotal,
                scoreBreakdown, grade, passed);
        InspSubmission saved = submissionRepository.save(submission);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        return saved;
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

    @Transactional
    public SubmissionDetail updateDetailResponse(Long detailId, String responseValue,
                                                  ScoringMode scoringMode, BigDecimal score,
                                                  String dimensions) {
        SubmissionDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在: " + detailId));
        detail.updateResponse(responseValue, scoringMode, score, dimensions);
        return detailRepository.save(detail);
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
