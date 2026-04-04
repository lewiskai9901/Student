package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import com.school.management.domain.inspection.repository.v7.GradeDefinitionRepository;
import com.school.management.domain.inspection.repository.v7.GradeSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeSchemeApplicationService {

    private final GradeSchemeRepository schemeRepository;
    private final GradeDefinitionRepository definitionRepository;

    public List<GradeScheme> listSchemes(Long tenantId) {
        List<GradeScheme> schemes = schemeRepository.findByTenantIdOrSystem(tenantId);
        schemes.forEach(s -> s.setGrades(definitionRepository.findByGradeSchemeId(s.getId())));
        return schemes;
    }

    public GradeScheme getScheme(Long id) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        scheme.setGrades(definitionRepository.findByGradeSchemeId(id));
        return scheme;
    }

    @Transactional
    public GradeScheme createScheme(Long tenantId, String displayName, String description,
                                     String schemeType, List<GradeDefinition> grades, Long createdBy) {
        GradeScheme scheme = GradeScheme.create(tenantId, displayName, description, schemeType, createdBy);
        scheme = schemeRepository.save(scheme);
        saveGrades(scheme.getId(), grades);
        scheme.setGrades(definitionRepository.findByGradeSchemeId(scheme.getId()));
        return scheme;
    }

    @Transactional
    public GradeScheme cloneFromPreset(Long sourceId, String displayName, Long tenantId, Long createdBy) {
        GradeScheme source = getScheme(sourceId);
        GradeScheme clone = GradeScheme.create(tenantId, displayName, source.getDescription(),
                source.getSchemeType(), createdBy);
        clone = schemeRepository.save(clone);
        for (GradeDefinition def : source.getGrades()) {
            GradeDefinition copy = GradeDefinition.create(
                    clone.getId(), def.getCode(), def.getName(),
                    def.getMinValue(), def.getMaxValue(),
                    def.getColor(), def.getIcon(), def.getSortOrder());
            definitionRepository.save(copy);
        }
        clone.setGrades(definitionRepository.findByGradeSchemeId(clone.getId()));
        return clone;
    }

    @Transactional
    public GradeScheme updateScheme(Long id, String displayName, String description,
                                     List<GradeDefinition> grades) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        scheme.updateInfo(displayName, description);
        schemeRepository.save(scheme);
        if (grades != null) {
            definitionRepository.deleteByGradeSchemeId(id);
            saveGrades(id, grades);
        }
        scheme.setGrades(definitionRepository.findByGradeSchemeId(id));
        return scheme;
    }

    @Transactional
    public void deleteScheme(Long id) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        if (Boolean.TRUE.equals(scheme.getIsSystem())) {
            throw new IllegalStateException("系统预设方案不可删除");
        }
        definitionRepository.deleteByGradeSchemeId(id);
        schemeRepository.deleteById(id);
    }

    private void saveGrades(Long schemeId, List<GradeDefinition> grades) {
        if (grades == null) return;
        int order = 0;
        for (GradeDefinition def : grades) {
            GradeDefinition toSave = GradeDefinition.create(
                    schemeId, def.getCode(), def.getName(),
                    def.getMinValue(), def.getMaxValue(),
                    def.getColor(), def.getIcon(), order++);
            definitionRepository.save(toSave);
        }
    }
}
