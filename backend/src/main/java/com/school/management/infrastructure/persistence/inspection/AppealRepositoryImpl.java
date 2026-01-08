package com.school.management.infrastructure.persistence.inspection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.Appeal;
import com.school.management.domain.inspection.model.AppealApproval;
import com.school.management.domain.inspection.model.AppealStatus;
import com.school.management.domain.inspection.repository.AppealRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of AppealRepository.
 */
@Repository
public class AppealRepositoryImpl implements AppealRepository {

    private final AppealMapper appealMapper;
    private final AppealApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;

    public AppealRepositoryImpl(
            AppealMapper appealMapper,
            AppealApprovalMapper approvalMapper,
            ObjectMapper objectMapper) {
        this.appealMapper = appealMapper;
        this.approvalMapper = approvalMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Appeal save(Appeal aggregate) {
        AppealPO po = toPO(aggregate);

        if (aggregate.getId() == null) {
            appealMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            appealMapper.updateById(po);
        }

        // Save approval records
        for (AppealApproval approval : aggregate.getApprovalRecords()) {
            AppealApprovalPO approvalPO = toApprovalPO(approval, aggregate.getId());
            if (approval.getId() == null) {
                approvalMapper.insert(approvalPO);
                approval.setId(approvalPO.getId());
            }
        }

        return aggregate;
    }

    @Override
    public Optional<Appeal> findById(Long id) {
        AppealPO po = appealMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(Appeal aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            appealMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        appealMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return appealMapper.selectById(id) != null;
    }

    @Override
    public Optional<Appeal> findByAppealCode(String appealCode) {
        AppealPO po = appealMapper.findByAppealCode(appealCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<Appeal> findByStatus(AppealStatus status) {
        return appealMapper.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByClassId(Long classId) {
        return appealMapper.findByClassId(classId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByInspectionRecordId(Long recordId) {
        return appealMapper.findByInspectionRecordId(recordId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByApplicantId(Long applicantId) {
        return appealMapper.findByApplicantId(applicantId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findPendingLevel1Review() {
        return appealMapper.findByStatus(AppealStatus.PENDING.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findPendingLevel2Review() {
        return appealMapper.findByStatus(AppealStatus.LEVEL1_APPROVED.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByLevel1ReviewerId(Long reviewerId) {
        return appealMapper.findByLevel1ReviewerId(reviewerId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByAppliedDateRange(LocalDateTime start, LocalDateTime end) {
        return appealMapper.findByAppliedDateRange(start, end).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(AppealStatus status) {
        return appealMapper.countByStatus(status.name());
    }

    @Override
    public boolean existsByDeductionDetailId(Long deductionDetailId) {
        return appealMapper.existsByDeductionDetailId(deductionDetailId);
    }

    @Override
    public List<Appeal> findApprovedNotEffective() {
        return appealMapper.findByStatus(AppealStatus.APPROVED.name()).stream()
            .filter(po -> po.getEffectiveAt() == null)
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private AppealPO toPO(Appeal domain) {
        AppealPO po = new AppealPO();
        po.setId(domain.getId());
        po.setInspectionRecordId(domain.getInspectionRecordId());
        po.setDeductionDetailId(domain.getDeductionDetailId());
        po.setClassId(domain.getClassId());
        po.setAppealCode(domain.getAppealCode());
        po.setReason(domain.getReason());

        // Serialize attachments to JSON
        try {
            po.setAttachments(objectMapper.writeValueAsString(domain.getAttachments()));
        } catch (Exception e) {
            po.setAttachments("[]");
        }

        po.setOriginalDeduction(domain.getOriginalDeduction());
        po.setRequestedDeduction(domain.getRequestedDeduction());
        po.setApprovedDeduction(domain.getApprovedDeduction());
        po.setStatus(domain.getStatus().name());
        po.setApplicantId(domain.getApplicantId());
        po.setAppliedAt(domain.getAppliedAt());
        po.setLevel1ReviewerId(domain.getLevel1ReviewerId());
        po.setLevel1ReviewedAt(domain.getLevel1ReviewedAt());
        po.setLevel1Comment(domain.getLevel1Comment());
        po.setLevel2ReviewerId(domain.getLevel2ReviewerId());
        po.setLevel2ReviewedAt(domain.getLevel2ReviewedAt());
        po.setLevel2Comment(domain.getLevel2Comment());
        po.setEffectiveAt(domain.getEffectiveAt());
        return po;
    }

    private AppealApprovalPO toApprovalPO(AppealApproval domain, Long appealId) {
        AppealApprovalPO po = new AppealApprovalPO();
        po.setId(domain.getId());
        po.setAppealId(appealId);
        po.setReviewerId(domain.getReviewerId());
        po.setReviewLevel(domain.getReviewLevel());
        po.setAction(domain.getAction());
        po.setComment(domain.getComment());
        po.setReviewedAt(domain.getReviewedAt());
        return po;
    }

    private Appeal toDomain(AppealPO po) {
        List<String> attachments = parseAttachments(po.getAttachments());

        return Appeal.builder()
            .id(po.getId())
            .inspectionRecordId(po.getInspectionRecordId())
            .deductionDetailId(po.getDeductionDetailId())
            .classId(po.getClassId())
            .appealCode(po.getAppealCode())
            .reason(po.getReason())
            .attachments(attachments)
            .originalDeduction(po.getOriginalDeduction())
            .requestedDeduction(po.getRequestedDeduction())
            .applicantId(po.getApplicantId())
            .build();
    }

    private List<String> parseAttachments(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
