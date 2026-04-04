package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;
import com.school.management.domain.inspection.repository.v7.GradeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GradeDefinitionRepositoryImpl implements GradeDefinitionRepository {

    private final GradeDefinitionMapper mapper;

    @Override
    public GradeDefinition save(GradeDefinition def) {
        GradeDefinitionPO po = toPO(def);
        if (def.getId() == null) {
            mapper.insert(po);
            def.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return def;
    }

    @Override
    public List<GradeDefinition> findByGradeSchemeId(Long gradeSchemeId) {
        return mapper.findByGradeSchemeId(gradeSchemeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByGradeSchemeId(Long gradeSchemeId) {
        mapper.deleteByGradeSchemeId(gradeSchemeId);
    }

    private GradeDefinitionPO toPO(GradeDefinition domain) {
        GradeDefinitionPO po = new GradeDefinitionPO();
        po.setId(domain.getId());
        po.setGradeSchemeId(domain.getGradeSchemeId());
        po.setCode(domain.getCode());
        po.setName(domain.getName());
        po.setMinValue(domain.getMinValue());
        po.setMaxValue(domain.getMaxValue());
        po.setColor(domain.getColor());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        return po;
    }

    private GradeDefinition toDomain(GradeDefinitionPO po) {
        return GradeDefinition.reconstruct(GradeDefinition.builder()
                .id(po.getId())
                .gradeSchemeId(po.getGradeSchemeId())
                .code(po.getCode())
                .name(po.getName())
                .minValue(po.getMinValue())
                .maxValue(po.getMaxValue())
                .color(po.getColor())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder()));
    }
}
