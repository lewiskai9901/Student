package com.school.management.infrastructure.persistence.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.InspectionTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of InspectionTemplateRepository.
 * Maps to the existing 'check_templates' table.
 */
@Repository
public class InspectionTemplateRepositoryImpl implements InspectionTemplateRepository {

    private final InspectionTemplateMapper templateMapper;

    public InspectionTemplateRepositoryImpl(InspectionTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public InspectionTemplate save(InspectionTemplate aggregate) {
        InspectionTemplatePO po = toPO(aggregate);

        if (aggregate.getId() == null) {
            templateMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            templateMapper.updateById(po);
        }

        return aggregate;
    }

    @Override
    public Optional<InspectionTemplate> findById(Long id) {
        InspectionTemplatePO po = templateMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(InspectionTemplate aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            templateMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return templateMapper.selectById(id) != null;
    }

    @Override
    public List<InspectionTemplate> findByScope(TemplateScope scope) {
        // check_templates doesn't have scope, return all enabled
        return templateMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTemplate> findAllPublished() {
        // status = 1 means enabled/published
        return templateMapper.findByStatus(1).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<InspectionTemplate> findByTemplateCode(String templateCode) {
        InspectionTemplatePO po = templateMapper.findByTemplateCode(templateCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<InspectionTemplate> findByStatus(TemplateStatus status) {
        Integer dbStatus = toDbStatus(status);
        return templateMapper.findByStatus(dbStatus).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<InspectionTemplate> findDefaultByScope(TemplateScope scope) {
        InspectionTemplatePO po = templateMapper.findDefault();
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<InspectionTemplate> findByApplicableOrgUnitId(Long orgUnitId) {
        // check_templates doesn't have org unit association, return all enabled
        return templateMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTemplateCode(String templateCode) {
        return templateMapper.existsByTemplateCode(templateCode);
    }

    // ==================== Mapping Methods ====================

    private InspectionTemplatePO toPO(InspectionTemplate domain) {
        InspectionTemplatePO po = new InspectionTemplatePO();
        po.setId(domain.getId());
        po.setTemplateCode(domain.getTemplateCode());
        po.setTemplateName(domain.getTemplateName());
        po.setDescription(domain.getDescription());
        po.setTotalRounds(domain.getCurrentVersion()); // Reuse as totalRounds
        po.setIsDefault(domain.isDefault() != null && domain.isDefault() ? 1 : 0);
        po.setStatus(toDbStatus(domain.getStatus()));
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        return po;
    }

    private InspectionTemplate toDomain(InspectionTemplatePO po) {
        return InspectionTemplate.builder()
            .id(po.getId())
            .templateCode(po.getTemplateCode())
            .templateName(po.getTemplateName())
            .description(po.getDescription())
            .scope(TemplateScope.GLOBAL) // Default since not in DB
            .applicableOrgUnitId(null) // Not in DB
            .isDefault(po.getIsDefault() != null && po.getIsDefault() == 1)
            .currentVersion(po.getTotalRounds() != null ? po.getTotalRounds() : 1)
            .status(fromDbStatus(po.getStatus()))
            .createdBy(po.getCreatedBy())
            .categories(new ArrayList<>()) // Categories loaded separately in actual system
            .build();
    }

    /**
     * Convert TemplateStatus to database status: 1=enabled/published, 0=disabled/draft.
     */
    private Integer toDbStatus(TemplateStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case PUBLISHED -> 1;
            case DRAFT, ARCHIVED -> 0;
        };
    }

    /**
     * Convert database status to TemplateStatus: 1=PUBLISHED, 0=DRAFT.
     */
    private TemplateStatus fromDbStatus(Integer status) {
        if (status == null || status == 0) return TemplateStatus.DRAFT;
        return TemplateStatus.PUBLISHED;
    }
}
