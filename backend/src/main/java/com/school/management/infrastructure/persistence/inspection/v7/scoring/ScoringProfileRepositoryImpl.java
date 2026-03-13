package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.v7.ScoringProfileRepository;
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
    public Optional<ScoringProfile> findByTemplateId(Long templateId) {
        ScoringProfilePO po = mapper.findByTemplateId(templateId);
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

    private ScoringProfilePO toPO(ScoringProfile d) {
        ScoringProfilePO po = new ScoringProfilePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTemplateId(d.getTemplateId());
        po.setMaxScore(d.getMaxScore());
        po.setMinScore(d.getMinScore());
        po.setPrecisionDigits(d.getPrecisionDigits());
        po.setCurrentVersion(d.getCurrentVersion());
        // 1.9
        po.setTrendFactorEnabled(d.getTrendFactorEnabled());
        po.setTrendLookbackDays(d.getTrendLookbackDays());
        po.setTrendBonusPerPercent(d.getTrendBonusPerPercent());
        po.setTrendPenaltyPerPercent(d.getTrendPenaltyPerPercent());
        po.setTrendMaxAdjustment(d.getTrendMaxAdjustment());
        // 1.10
        po.setDecayEnabled(d.getDecayEnabled());
        po.setDecayMode(d.getDecayMode());
        po.setDecayRatePerDay(d.getDecayRatePerDay());
        po.setDecayFloor(d.getDecayFloor());
        // 1.11
        po.setMultiRaterMode(d.getMultiRaterMode());
        po.setRaterWeightBy(d.getRaterWeightBy());
        po.setConsensusThreshold(d.getConsensusThreshold());
        // 1.12
        po.setCalibrationEnabled(d.getCalibrationEnabled());
        po.setCalibrationMethod(d.getCalibrationMethod());
        po.setCalibrationPeriodDays(d.getCalibrationPeriodDays());
        po.setCalibrationMinSamples(d.getCalibrationMinSamples());

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
                .templateId(po.getTemplateId())
                .maxScore(po.getMaxScore())
                .minScore(po.getMinScore())
                .precisionDigits(po.getPrecisionDigits())
                .currentVersion(po.getCurrentVersion())
                // 1.9
                .trendFactorEnabled(po.getTrendFactorEnabled())
                .trendLookbackDays(po.getTrendLookbackDays())
                .trendBonusPerPercent(po.getTrendBonusPerPercent())
                .trendPenaltyPerPercent(po.getTrendPenaltyPerPercent())
                .trendMaxAdjustment(po.getTrendMaxAdjustment())
                // 1.10
                .decayEnabled(po.getDecayEnabled())
                .decayMode(po.getDecayMode())
                .decayRatePerDay(po.getDecayRatePerDay())
                .decayFloor(po.getDecayFloor())
                // 1.11
                .multiRaterMode(po.getMultiRaterMode())
                .raterWeightBy(po.getRaterWeightBy())
                .consensusThreshold(po.getConsensusThreshold())
                // 1.12
                .calibrationEnabled(po.getCalibrationEnabled())
                .calibrationMethod(po.getCalibrationMethod())
                .calibrationPeriodDays(po.getCalibrationPeriodDays())
                .calibrationMinSamples(po.getCalibrationMinSamples())

                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
