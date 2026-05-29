package com.school.management.infrastructure.persistence.inspection.scoring;

import com.school.management.domain.inspection.model.scoring.NormalizationMode;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoringProfileRepositoryImpl implements ScoringProfileRepository {

    private final ScoringProfileMapper mapper;

    public ScoringProfileRepositoryImpl(ScoringProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ScoringProfile save(ScoringProfile profile) {
        ScoringProfilePO po = toPO(profile);
        if (profile.getId() == null) {
            mapper.insert(po);
            profile.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return profile;
    }

    @Override
    public Optional<ScoringProfile> findById(Long id) {
        ScoringProfilePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Optional<ScoringProfile> findBySectionId(Long sectionId) {
        ScoringProfilePO po = mapper.findBySectionId(sectionId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScoringProfile> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScoringProfile> findByProjectIdAndSectionId(Long projectId, Long sectionId) {
        ScoringProfilePO po = mapper.findByProjectIdAndSectionId(projectId, sectionId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScoringProfile> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public int deleteByProjectId(Long projectId) {
        return mapper.deleteByProjectId(projectId);
    }

    private ScoringProfilePO toPO(ScoringProfile d) {
        ScoringProfilePO po = new ScoringProfilePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setSectionId(d.getSectionId());
        po.setProjectId(d.getProjectId());
        po.setMaxScore(d.getMaxScore());
        po.setMinScore(d.getMinScore());
        po.setPrecisionDigits(d.getPrecisionDigits());
        po.setCurrentVersion(d.getCurrentVersion());
        // 1.13 章节级归一化 (枚举 -> String, null 兜底 NONE)
        po.setNormalizeBy(d.getNormalizeBy() != null ? d.getNormalizeBy().name() : NormalizeBy.NONE.name());
        po.setNormalizationMode(d.getNormalizationMode() != null ? d.getNormalizationMode().name() : NormalizationMode.NONE.name());
        po.setBaselinePopulation(d.getBaselinePopulation() != null ? d.getBaselinePopulation() : 1);
        po.setNormFloor(d.getNormFloor());
        po.setNormCap(d.getNormCap());

        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ScoringProfile toDomain(ScoringProfilePO po) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .sectionId(po.getSectionId())
                .projectId(po.getProjectId())
                .maxScore(po.getMaxScore())
                .minScore(po.getMinScore())
                .precisionDigits(po.getPrecisionDigits())
                .currentVersion(po.getCurrentVersion())
                // 1.13 章节级归一化 (String -> 枚举, null/非法值兜底 NONE)
                .normalizeBy(parseNormalizeBy(po.getNormalizeBy()))
                .normalizationMode(parseNormalizationMode(po.getNormalizationMode()))
                .baselinePopulation(po.getBaselinePopulation() != null ? po.getBaselinePopulation() : 1)
                .normFloor(po.getNormFloor())
                .normCap(po.getNormCap())

                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }

    private static NormalizeBy parseNormalizeBy(String s) {
        if (s == null || s.isBlank()) {
            return NormalizeBy.NONE;
        }
        try {
            return NormalizeBy.valueOf(s.trim());
        } catch (IllegalArgumentException ex) {
            return NormalizeBy.NONE;
        }
    }

    private static NormalizationMode parseNormalizationMode(String s) {
        if (s == null || s.isBlank()) {
            return NormalizationMode.NONE;
        }
        try {
            return NormalizationMode.valueOf(s.trim());
        } catch (IllegalArgumentException ex) {
            return NormalizationMode.NONE;
        }
    }
}
