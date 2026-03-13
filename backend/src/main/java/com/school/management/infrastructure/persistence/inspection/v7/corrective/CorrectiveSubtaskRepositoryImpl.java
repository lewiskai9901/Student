package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.school.management.domain.inspection.model.v7.corrective.CorrectiveSubtask;
import com.school.management.domain.inspection.repository.v7.CorrectiveSubtaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CorrectiveSubtaskRepositoryImpl implements CorrectiveSubtaskRepository {

    private final CorrectiveSubtaskMapper mapper;

    public CorrectiveSubtaskRepositoryImpl(CorrectiveSubtaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CorrectiveSubtask save(CorrectiveSubtask subtask) {
        CorrectiveSubtaskPO po = toPO(subtask);
        if (subtask.getId() == null) {
            mapper.insert(po);
            subtask.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return subtask;
    }

    @Override
    public Optional<CorrectiveSubtask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<CorrectiveSubtask> findByCaseId(Long caseId) {
        return mapper.findByCaseId(caseId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public int countByCaseIdAndStatus(Long caseId, String status) {
        return mapper.countByCaseIdAndStatus(caseId, status);
    }

    private CorrectiveSubtaskPO toPO(CorrectiveSubtask d) {
        CorrectiveSubtaskPO po = new CorrectiveSubtaskPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setCaseId(d.getCaseId());
        po.setSubtaskName(d.getSubtaskName());
        po.setDescription(d.getDescription());
        po.setAssigneeId(d.getAssigneeId());
        po.setStatus(d.getStatus());
        po.setPriority(d.getPriority());
        po.setDueDate(d.getDueDate());
        po.setCompletedAt(d.getCompletedAt());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private CorrectiveSubtask toDomain(CorrectiveSubtaskPO po) {
        return CorrectiveSubtask.reconstruct(CorrectiveSubtask.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .caseId(po.getCaseId())
                .subtaskName(po.getSubtaskName())
                .description(po.getDescription())
                .assigneeId(po.getAssigneeId())
                .status(po.getStatus())
                .priority(po.getPriority())
                .dueDate(po.getDueDate())
                .completedAt(po.getCompletedAt())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
