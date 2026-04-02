package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.corrective.*;
import com.school.management.domain.inspection.repository.v7.CorrectiveCaseRepository;
// RcaMethod, EffectivenessStatus used in toDomain
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class CorrectiveCaseRepositoryImpl implements CorrectiveCaseRepository {

    private final CorrectiveCaseMapper mapper;
    private final ObjectMapper objectMapper;

    public CorrectiveCaseRepositoryImpl(CorrectiveCaseMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public CorrectiveCase save(CorrectiveCase c) {
        CorrectiveCasePO po = toPO(c);
        if (c.getId() == null) {
            mapper.insert(po);
            c.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return c;
    }

    @Override
    public Optional<CorrectiveCase> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<CorrectiveCase> findByCaseCode(String caseCode) {
        return Optional.ofNullable(mapper.findByCaseCode(caseCode)).map(this::toDomain);
    }

    @Override
    public List<CorrectiveCase> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findBySubmissionId(Long submissionId) {
        return mapper.findBySubmissionId(submissionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findByAssigneeId(Long assigneeId) {
        return mapper.findByAssigneeId(assigneeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findByStatus(CaseStatus status) {
        return mapper.findByStatus(status.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findByPriority(CasePriority priority) {
        return mapper.findByPriority(priority.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findOverdue(LocalDateTime now) {
        return mapper.findOverdue(now).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveCase> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private CorrectiveCasePO toPO(CorrectiveCase d) {
        CorrectiveCasePO po = new CorrectiveCasePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setCaseCode(d.getCaseCode());
        po.setSubmissionId(d.getSubmissionId());
        po.setDetailId(d.getDetailId());
        po.setProjectId(d.getProjectId());
        po.setTaskId(d.getTaskId());
        po.setTargetType(d.getTargetType());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setIssueDescription(d.getIssueDescription());
        po.setRequiredAction(d.getRequiredAction());
        po.setIssueCategoryId(d.getIssueCategoryId());
        po.setDeficiencyCode(d.getDeficiencyCode());
        po.setRcaMethod(d.getRcaMethod() != null ? d.getRcaMethod().name() : null);
        po.setRcaData(d.getRcaData());
        po.setPreventiveAction(d.getPreventiveAction());
        po.setPriority(d.getPriority() != null ? d.getPriority().name() : null);
        po.setDeadline(d.getDeadline());
        po.setAssigneeId(d.getAssigneeId());
        po.setAssigneeName(d.getAssigneeName());
        po.setEscalationLevel(d.getEscalationLevel());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        po.setCorrectionNote(d.getCorrectionNote());
        po.setCorrectionEvidenceIds(toJson(d.getCorrectionEvidenceIds()));
        po.setCorrectedAt(d.getCorrectedAt());
        po.setVerifierId(d.getVerifierId());
        po.setVerifierName(d.getVerifierName());
        po.setVerifiedAt(d.getVerifiedAt());
        po.setVerificationNote(d.getVerificationNote());
        po.setEffectivenessCheckDate(d.getEffectivenessCheckDate());
        po.setEffectivenessStatus(d.getEffectivenessStatus() != null ? d.getEffectivenessStatus().name() : null);
        po.setEffectivenessNote(d.getEffectivenessNote());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private CorrectiveCase toDomain(CorrectiveCasePO po) {
        return CorrectiveCase.reconstruct(CorrectiveCase.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .caseCode(po.getCaseCode())
                .submissionId(po.getSubmissionId())
                .detailId(po.getDetailId())
                .projectId(po.getProjectId())
                .taskId(po.getTaskId())
                .targetType(po.getTargetType())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .issueDescription(po.getIssueDescription())
                .requiredAction(po.getRequiredAction())
                .issueCategoryId(po.getIssueCategoryId())
                .deficiencyCode(po.getDeficiencyCode())
                .rcaMethod(po.getRcaMethod() != null ? RcaMethod.valueOf(po.getRcaMethod()) : null)
                .rcaData(po.getRcaData())
                .preventiveAction(po.getPreventiveAction())
                .priority(po.getPriority() != null ? CasePriority.valueOf(po.getPriority()) : null)
                .deadline(po.getDeadline())
                .assigneeId(po.getAssigneeId())
                .assigneeName(po.getAssigneeName())
                .escalationLevel(po.getEscalationLevel())
                .status(po.getStatus() != null ? CaseStatus.valueOf(po.getStatus()) : null)
                .correctionNote(po.getCorrectionNote())
                .correctionEvidenceIds(fromJson(po.getCorrectionEvidenceIds()))
                .correctedAt(po.getCorrectedAt())
                .verifierId(po.getVerifierId())
                .verifierName(po.getVerifierName())
                .verifiedAt(po.getVerifiedAt())
                .verificationNote(po.getVerificationNote())
                .effectivenessCheckDate(po.getEffectivenessCheckDate())
                .effectivenessStatus(po.getEffectivenessStatus() != null ? EffectivenessStatus.valueOf(po.getEffectivenessStatus()) : null)
                .effectivenessNote(po.getEffectivenessNote())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<Long> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize correction evidence IDs: {}", json, e);
            return Collections.emptyList();
        }
    }
}
