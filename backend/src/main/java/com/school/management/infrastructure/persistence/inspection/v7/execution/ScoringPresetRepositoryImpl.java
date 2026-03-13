package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.school.management.domain.inspection.model.v7.execution.ScoringPreset;
import com.school.management.domain.inspection.repository.v7.ScoringPresetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoringPresetRepositoryImpl implements ScoringPresetRepository {

    private final ScoringPresetMapper mapper;

    public ScoringPresetRepositoryImpl(ScoringPresetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScoringPreset save(ScoringPreset preset) {
        ScoringPresetPO po = toPO(preset);
        if (preset.getId() == null) {
            mapper.insert(po);
            preset.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return preset;
    }

    @Override
    public Optional<ScoringPreset> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ScoringPreset> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ScoringPresetPO toPO(ScoringPreset d) {
        ScoringPresetPO po = new ScoringPresetPO();
        po.setId(d.getId());
        po.setTemplateId(d.getTemplateId());
        po.setPresetName(d.getPresetName());
        po.setPresetType(d.getPresetType());
        po.setItemValues(d.getItemValues());
        po.setUsageCount(d.getUsageCount());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private ScoringPreset toDomain(ScoringPresetPO po) {
        return ScoringPreset.reconstruct(ScoringPreset.builder()
                .id(po.getId())
                .templateId(po.getTemplateId())
                .presetName(po.getPresetName())
                .presetType(po.getPresetType())
                .itemValues(po.getItemValues())
                .usageCount(po.getUsageCount())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt()));
    }
}
