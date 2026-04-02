package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.model.v7.template.TemplateStatus;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateSectionRepositoryImpl implements TemplateSectionRepository {

    private final TemplateSectionMapper mapper;

    public TemplateSectionRepositoryImpl(TemplateSectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateSection save(TemplateSection section) {
        TemplateSectionPO po = toPO(section);
        if (section.getId() == null) {
            mapper.insert(po);
            section.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return section;
    }

    @Override
    public Optional<TemplateSection> findById(Long id) {
        TemplateSectionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateSection> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        mapper.softDeleteByTemplateId(templateId);
    }

    @Override
    public List<TemplateSection> findByParentSectionId(Long parentSectionId) {
        LambdaQueryWrapper<TemplateSectionPO> qw = new LambdaQueryWrapper<>();
        qw.eq(TemplateSectionPO::getParentSectionId, parentSectionId)
          .orderByAsc(TemplateSectionPO::getSortOrder);
        return mapper.selectList(qw).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TemplateSection> findRootSections(Long templateId) {
        LambdaQueryWrapper<TemplateSectionPO> qw = new LambdaQueryWrapper<>();
        qw.eq(TemplateSectionPO::getTemplateId, templateId)
          .isNull(TemplateSectionPO::getParentSectionId)
          .orderByAsc(TemplateSectionPO::getSortOrder);
        return mapper.selectList(qw).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TemplateSection> findAllRootSections() {
        return mapper.findAllRootSections().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateSection> findRootSectionsPaged(int offset, int size, String status, Long catalogId, String keyword) {
        return mapper.findRootSectionsPaged(offset, size, status, catalogId, keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countRootSections(String status, Long catalogId, String keyword) {
        return mapper.countRootSections(status, catalogId, keyword);
    }

    @Override
    public List<TemplateSection> findDescendants(Long rootSectionId) {
        List<TemplateSection> result = new ArrayList<>();
        collectDescendants(rootSectionId, result);
        return result;
    }

    private void collectDescendants(Long parentId, List<TemplateSection> accumulator) {
        List<TemplateSection> children = findByParentSectionId(parentId);
        for (TemplateSection child : children) {
            accumulator.add(child);
            collectDescendants(child.getId(), accumulator);
        }
    }

    private TemplateSectionPO toPO(TemplateSection domain) {
        TemplateSectionPO po = new TemplateSectionPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setTemplateId(domain.getTemplateId());
        po.setParentSectionId(domain.getParentSectionId());
        po.setRefTemplateId(null); // legacy field, no longer used; refSectionId replaces it
        po.setRefSectionId(domain.getRefSectionId());
        po.setScoringConfig(domain.getScoringConfig());
        po.setSectionCode(domain.getSectionCode());
        po.setSectionName(domain.getSectionName());
        po.setTargetType(domain.getTargetType() != null ? domain.getTargetType().name() : null);
        po.setTargetSourceMode(domain.getTargetSourceMode());
        po.setTargetTypeFilter(domain.getTargetTypeFilter());
        po.setDescription(domain.getDescription());
        po.setTags(domain.getTags());
        po.setCatalogId(domain.getCatalogId());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setLatestVersion(domain.getLatestVersion());
        po.setSortOrder(domain.getSortOrder());
        po.setIsRepeatable(domain.getIsRepeatable());
        po.setConditionLogic(domain.getConditionLogic());
        po.setInputMode(domain.getInputMode());
        po.setInspectionMode(domain.getInspectionMode());
        po.setContinuousStart(domain.getContinuousStart());
        po.setContinuousEnd(domain.getContinuousEnd());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private TemplateSection toDomain(TemplateSectionPO po) {
        return TemplateSection.reconstruct(TemplateSection.builder()
                .id(po.getId())
                .templateId(po.getTemplateId())
                .parentSectionId(po.getParentSectionId())
                .refSectionId(po.getRefSectionId())
                .scoringConfig(po.getScoringConfig())
                .sectionCode(po.getSectionCode())
                .sectionName(po.getSectionName())
                .targetType(po.getTargetType() != null ? TargetType.valueOf(po.getTargetType()) : null)
                .targetSourceMode(po.getTargetSourceMode())
                .targetTypeFilter(po.getTargetTypeFilter())
                .description(po.getDescription())
                .tags(po.getTags())
                .catalogId(po.getCatalogId())
                .status(po.getStatus() != null ? TemplateStatus.valueOf(po.getStatus()) : null)
                .latestVersion(po.getLatestVersion())
                .sortOrder(po.getSortOrder())
                .isRepeatable(po.getIsRepeatable())
                .conditionLogic(po.getConditionLogic())
                .inputMode(po.getInputMode())
                .inspectionMode(po.getInspectionMode())
                .continuousStart(po.getContinuousStart())
                .continuousEnd(po.getContinuousEnd())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
