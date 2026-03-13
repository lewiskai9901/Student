package com.school.management.infrastructure.persistence.inspection.v7.compliance;

import com.school.management.domain.inspection.model.v7.compliance.ItemComplianceMapping;
import com.school.management.domain.inspection.repository.v7.ItemComplianceMappingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ItemComplianceMappingRepositoryImpl implements ItemComplianceMappingRepository {

    private final ItemComplianceMappingMapper mapper;

    public ItemComplianceMappingRepositoryImpl(ItemComplianceMappingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ItemComplianceMapping save(ItemComplianceMapping mapping) {
        ItemComplianceMappingPO po = toPO(mapping);
        if (mapping.getId() == null) {
            mapper.insert(po);
            mapping.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return mapping;
    }

    @Override
    public Optional<ItemComplianceMapping> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ItemComplianceMapping> findByItemId(Long templateItemId) {
        return mapper.findByItemId(templateItemId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ItemComplianceMapping> findByClauseId(Long clauseId) {
        return mapper.findByClauseId(clauseId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByItemId(Long templateItemId) {
        mapper.softDeleteByItemId(templateItemId);
    }

    private ItemComplianceMappingPO toPO(ItemComplianceMapping d) {
        ItemComplianceMappingPO po = new ItemComplianceMappingPO();
        po.setId(d.getId());
        po.setTemplateItemId(d.getTemplateItemId());
        po.setClauseId(d.getClauseId());
        po.setCoverageLevel(d.getCoverageLevel());
        po.setNotes(d.getNotes());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ItemComplianceMapping toDomain(ItemComplianceMappingPO po) {
        return ItemComplianceMapping.reconstruct(ItemComplianceMapping.builder()
                .id(po.getId())
                .templateItemId(po.getTemplateItemId())
                .clauseId(po.getClauseId())
                .coverageLevel(po.getCoverageLevel())
                .notes(po.getNotes())
                .createdAt(po.getCreatedAt()));
    }
}
