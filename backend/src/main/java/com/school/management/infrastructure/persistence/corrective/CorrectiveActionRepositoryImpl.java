package com.school.management.infrastructure.persistence.corrective;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.corrective.model.*;
import com.school.management.domain.corrective.repository.CorrectiveActionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CorrectiveActionRepositoryImpl implements CorrectiveActionRepository {

    private final CorrectiveActionMapper mapper;
    private final ObjectMapper objectMapper;

    public CorrectiveActionRepositoryImpl(CorrectiveActionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public CorrectiveAction save(CorrectiveAction aggregate) {
        CorrectiveActionPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            mapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<CorrectiveAction> findById(Long id) {
        CorrectiveActionPO po = mapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(CorrectiveAction aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            mapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Optional<CorrectiveAction> findByActionCode(String actionCode) {
        CorrectiveActionPO po = mapper.findByActionCode(actionCode);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public List<CorrectiveAction> findByStatus(ActionStatus status) {
        return mapper.findByStatus(status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByClassId(Long classId) {
        return mapper.findByClassId(classId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findByAssigneeId(Long assigneeId) {
        return mapper.findByAssigneeId(assigneeId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveAction> findOverdue() {
        return mapper.findOverdue().stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByStatus(ActionStatus status) {
        return mapper.countByStatus(status.name());
    }

    @Override
    public List<CorrectiveAction> findBySourceAndSourceId(String source, Long sourceId) {
        return mapper.findBySourceAndSourceId(source, sourceId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private CorrectiveActionPO toPO(CorrectiveAction domain) {
        CorrectiveActionPO po = new CorrectiveActionPO();
        po.setId(domain.getId());
        po.setActionCode(domain.getActionCode());
        po.setTitle(domain.getTitle());
        po.setDescription(domain.getDescription());
        po.setSource(domain.getSource().name());
        po.setSourceId(domain.getSourceId());
        po.setSeverity(domain.getSeverity().name());
        po.setCategory(domain.getCategory().name());
        po.setStatus(domain.getStatus().name());
        po.setClassId(domain.getClassId());
        po.setAssigneeId(domain.getAssigneeId());
        po.setDeadline(domain.getDeadline());
        po.setResolutionNote(domain.getResolutionNote());
        try {
            po.setResolutionAttachments(objectMapper.writeValueAsString(domain.getResolutionAttachments()));
        } catch (Exception e) {
            po.setResolutionAttachments("[]");
        }
        po.setResolvedAt(domain.getResolvedAt());
        po.setVerifierId(domain.getVerifierId());
        po.setVerificationResult(domain.getVerificationResult());
        po.setVerificationComment(domain.getVerificationComment());
        po.setVerifiedAt(domain.getVerifiedAt());
        po.setEscalationLevel(domain.getEscalationLevel());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private CorrectiveAction toDomain(CorrectiveActionPO po) {
        List<String> attachments = parseJson(po.getResolutionAttachments());
        return CorrectiveAction.reconstruct()
                .id(po.getId())
                .actionCode(po.getActionCode())
                .title(po.getTitle())
                .description(po.getDescription())
                .source(ActionSource.valueOf(po.getSource()))
                .sourceId(po.getSourceId())
                .severity(ActionSeverity.valueOf(po.getSeverity()))
                .category(ActionCategory.valueOf(po.getCategory()))
                .status(ActionStatus.valueOf(po.getStatus()))
                .classId(po.getClassId())
                .assigneeId(po.getAssigneeId())
                .deadline(po.getDeadline())
                .resolutionNote(po.getResolutionNote())
                .resolutionAttachments(attachments)
                .resolvedAt(po.getResolvedAt())
                .verifierId(po.getVerifierId())
                .verificationResult(po.getVerificationResult())
                .verificationComment(po.getVerificationComment())
                .verifiedAt(po.getVerifiedAt())
                .escalationLevel(po.getEscalationLevel() != null ? po.getEscalationLevel() : 0)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
