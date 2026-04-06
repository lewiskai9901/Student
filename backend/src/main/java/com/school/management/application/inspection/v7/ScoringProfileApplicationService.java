package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScoringProfileApplicationService {

    private final ScoringProfileRepository profileRepository;
    private final ScoreDimensionRepository dimensionRepository;
    private final GradeBandRepository gradeBandRepository;
    private final CalculationRuleV7Repository ruleRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;
    private final ScoringProfileVersionRepository versionRepository;
    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    // ===== ScoringProfile =====

    @Transactional
    public ScoringProfile createProfile(Long sectionId, Long createdBy) {
        // 如果已存在则直接返回，不报错（支持幂等调用）
        Optional<ScoringProfile> existing = profileRepository.findBySectionId(sectionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ScoringProfile profile = ScoringProfile.create(sectionId, createdBy);
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Optional<ScoringProfile> getProfile(Long id) {
        return profileRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ScoringProfile> getProfileBySectionId(Long sectionId) {
        return profileRepository.findBySectionId(sectionId);
    }

    @Transactional(readOnly = true)
    public List<ScoringProfile> listProfiles() {
        return profileRepository.findAll();
    }

    @Transactional
    public ScoringProfile updateProfile(Long id, BigDecimal maxScore,
                                         BigDecimal minScore, Integer precisionDigits,
                                         Long updatedBy) {
        ScoringProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + id));
        profile.update(maxScore, minScore, precisionDigits, updatedBy);
        return profileRepository.save(profile);
    }

    @Transactional
    public ScoringProfile updateAdvancedSettings(Long id,
            Boolean trendFactorEnabled, Integer trendLookbackDays,
            BigDecimal trendBonusPerPercent, BigDecimal trendPenaltyPerPercent,
            BigDecimal trendMaxAdjustment,
            Boolean decayEnabled, String decayMode,
            BigDecimal decayRatePerDay, BigDecimal decayFloor,
            String multiRaterMode, String raterWeightBy,
            BigDecimal consensusThreshold,
            Boolean calibrationEnabled, String calibrationMethod,
            Integer calibrationPeriodDays, Integer calibrationMinSamples,
            Long updatedBy) {
        ScoringProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + id));
        profile.updateAdvancedSettings(trendFactorEnabled, trendLookbackDays,
                trendBonusPerPercent, trendPenaltyPerPercent, trendMaxAdjustment,
                decayEnabled, decayMode, decayRatePerDay, decayFloor,
                multiRaterMode, raterWeightBy, consensusThreshold,
                calibrationEnabled, calibrationMethod, calibrationPeriodDays, calibrationMinSamples,
                updatedBy);
        return profileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(Long id) {
        ruleRepository.deleteByScoringProfileId(id);
        gradeBandRepository.deleteByScoringProfileId(id);
        dimensionRepository.deleteByScoringProfileId(id);
        profileRepository.deleteById(id);
    }

    // ===== ScoreDimension =====

    @Transactional
    public ScoreDimension createDimension(Long scoringProfileId, String dimensionCode,
                                           String dimensionName, Integer weight,
                                           BigDecimal baseScore, BigDecimal passThreshold,
                                           Integer sortOrder) {
        profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));
        ScoreDimension dimension = ScoreDimension.reconstruct(ScoreDimension.builder()
                .scoringProfileId(scoringProfileId)
                .dimensionCode(dimensionCode)
                .dimensionName(dimensionName)
                .weight(weight)
                .baseScore(baseScore)
                .passThreshold(passThreshold)
                .sortOrder(sortOrder));
        return dimensionRepository.save(dimension);
    }

    @Transactional(readOnly = true)
    public List<ScoreDimension> listDimensions(Long scoringProfileId) {
        return dimensionRepository.findByScoringProfileId(scoringProfileId);
    }

    @Transactional
    public ScoreDimension updateDimension(Long dimensionId, String dimensionName,
                                           Integer weight, BigDecimal baseScore,
                                           BigDecimal passThreshold) {
        ScoreDimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new IllegalArgumentException("评分维度不存在: " + dimensionId));
        dimension.update(dimensionName, weight, baseScore, passThreshold);
        return dimensionRepository.save(dimension);
    }

    @Transactional
    public void deleteDimension(Long dimensionId) {
        gradeBandRepository.deleteByDimensionId(dimensionId);
        dimensionRepository.deleteById(dimensionId);
    }

    /**
     * 自动同步所有子项权重：从当前分区的直接子分区读取，自动创建/更新/删除维度。
     * 维度完全由子分区列表驱动，不需要手动管理。
     * 新增子项默认权重 = 100（扣多少就是多少）。
     */
    @Transactional
    public List<ScoreDimension> syncAllDimensions(Long scoringProfileId) {
        ScoringProfile profile = profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));

        Long sectionId = profile.getSectionId();
        List<ScoreDimension> existing = dimensionRepository.findByScoringProfileId(scoringProfileId);

        java.util.Map<String, ScoreDimension> existingByCode = new java.util.HashMap<>();
        for (ScoreDimension dim : existing) {
            existingByCode.put(dim.getDimensionCode(), dim);
        }
        java.util.Set<String> activeCodes = new java.util.HashSet<>();
        int sortOrder = 0;

        // 同步子分区 → 维度
        List<TemplateSection> childSections = sectionRepository.findByParentSectionId(sectionId);
        for (TemplateSection sec : childSections) {
            String code = "SEC_" + sec.getId();
            activeCodes.add(code);
            ScoreDimension dim = existingByCode.get(code);
            if (dim == null) {
                dim = ScoreDimension.reconstruct(ScoreDimension.builder()
                        .scoringProfileId(scoringProfileId)
                        .dimensionCode(code)
                        .dimensionName(sec.getSectionName())
                        .weight(100)
                        .baseScore(new BigDecimal("100"))
                        .sourceType("SECTION")
                        .sortOrder(sortOrder));
                dimensionRepository.save(dim);
            } else {
                dim.update(sec.getSectionName(), dim.getWeight(), dim.getBaseScore(), dim.getPassThreshold());
                dimensionRepository.save(dim);
            }
            sortOrder++;
        }

        // 同步直接字段（isScored=true）→ 维度
        List<TemplateItem> directItems = itemRepository.findBySectionId(sectionId);
        for (TemplateItem item : directItems) {
            if (item.getIsScored() == null || !item.getIsScored()) continue;
            String code = "ITEM_" + item.getId();
            activeCodes.add(code);
            ScoreDimension dim = existingByCode.get(code);
            if (dim == null) {
                dim = ScoreDimension.reconstruct(ScoreDimension.builder()
                        .scoringProfileId(scoringProfileId)
                        .dimensionCode(code)
                        .dimensionName(item.getItemName())
                        .weight(item.getItemWeight() != null ? item.getItemWeight().intValue() : 100)
                        .baseScore(new BigDecimal("100"))
                        .sourceType("ITEM")
                        .sortOrder(sortOrder));
                dimensionRepository.save(dim);
            } else {
                dim.update(item.getItemName(), item.getItemWeight() != null ? item.getItemWeight().intValue() : dim.getWeight(), dim.getBaseScore(), dim.getPassThreshold());
                dimensionRepository.save(dim);
            }
            sortOrder++;
        }

        // 删除已不存在的维度
        for (ScoreDimension dim : existing) {
            if (!activeCodes.contains(dim.getDimensionCode())) {
                gradeBandRepository.deleteByDimensionId(dim.getId());
                dimensionRepository.deleteById(dim.getId());
            }
        }

        return dimensionRepository.findByScoringProfileId(scoringProfileId);
    }


    // ===== GradeBand =====

    @Transactional
    public GradeBand createGradeBand(Long scoringProfileId, Long dimensionId,
                                      String gradeCode, String gradeName,
                                      BigDecimal minScore, BigDecimal maxScore,
                                      String color, String icon, Integer sortOrder) {
        profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));
        GradeBand band = GradeBand.reconstruct(GradeBand.builder()
                .scoringProfileId(scoringProfileId)
                .dimensionId(dimensionId)
                .gradeCode(gradeCode)
                .gradeName(gradeName)
                .minScore(minScore)
                .maxScore(maxScore)
                .color(color)
                .icon(icon)
                .sortOrder(sortOrder));
        return gradeBandRepository.save(band);
    }

    @Transactional(readOnly = true)
    public List<GradeBand> listGradeBands(Long scoringProfileId) {
        return gradeBandRepository.findByScoringProfileId(scoringProfileId);
    }

    @Transactional
    public GradeBand updateGradeBand(Long bandId, String gradeName,
                                      BigDecimal minScore, BigDecimal maxScore,
                                      String color, String icon) {
        GradeBand band = gradeBandRepository.findById(bandId)
                .orElseThrow(() -> new IllegalArgumentException("等级区间不存在: " + bandId));
        band.update(gradeName, minScore, maxScore, color, icon);
        return gradeBandRepository.save(band);
    }

    @Transactional
    public void deleteGradeBand(Long bandId) {
        gradeBandRepository.deleteById(bandId);
    }

    // ===== CalculationRule =====

    @Transactional
    public CalculationRuleV7 createRule(Long scoringProfileId, String ruleCode,
                                         String ruleName, Integer priority,
                                         RuleType ruleType, String config,
                                         Boolean isEnabled, String scopeType,
                                         String targetDimensionIds,
                                         String activationCondition, String appliesTo,
                                         LocalDate effectiveFrom, LocalDate effectiveUntil,
                                         String exclusionGroup) {
        profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));
        CalculationRuleV7 rule = CalculationRuleV7.reconstruct(CalculationRuleV7.builder()
                .scoringProfileId(scoringProfileId)
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .priority(priority)
                .ruleType(ruleType)
                .config(config)
                .isEnabled(isEnabled)
                .scopeType(scopeType)
                .targetDimensionIds(targetDimensionIds)
                .activationCondition(activationCondition)
                .appliesTo(appliesTo)
                .effectiveFrom(effectiveFrom)
                .effectiveUntil(effectiveUntil)
                .exclusionGroup(exclusionGroup));
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<CalculationRuleV7> listRules(Long scoringProfileId) {
        return ruleRepository.findByScoringProfileIdOrderByPriority(scoringProfileId);
    }

    @Transactional
    public CalculationRuleV7 updateRule(Long ruleId, String ruleName, Integer priority,
                                         RuleType ruleType, String config, Boolean isEnabled,
                                         String scopeType, String targetDimensionIds,
                                         String activationCondition, String appliesTo,
                                         LocalDate effectiveFrom, LocalDate effectiveUntil,
                                         String exclusionGroup) {
        CalculationRuleV7 rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("计算规则不存在: " + ruleId));
        rule.update(ruleName, priority, ruleType, config, isEnabled,
                scopeType, targetDimensionIds, activationCondition, appliesTo,
                effectiveFrom, effectiveUntil, exclusionGroup);
        return ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    // ===== EscalationPolicy (1.3) =====

    @Transactional
    public EscalationPolicy createEscalationPolicy(Long profileId, String policyName,
            Integer lookupPeriodDays, String escalationMode,
            BigDecimal multiplier, BigDecimal adder, String fixedTable,
            BigDecimal maxEscalationFactor, String matchBy, Boolean isEnabled) {
        profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + profileId));
        EscalationPolicy policy = EscalationPolicy.reconstruct(EscalationPolicy.builder()
                .profileId(profileId)
                .policyName(policyName)
                .lookupPeriodDays(lookupPeriodDays)
                .escalationMode(escalationMode)
                .multiplier(multiplier)
                .adder(adder)
                .fixedTable(fixedTable)
                .maxEscalationFactor(maxEscalationFactor)
                .matchBy(matchBy)
                .isEnabled(isEnabled));
        return escalationPolicyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public List<EscalationPolicy> listEscalationPolicies(Long profileId) {
        return escalationPolicyRepository.findByProfileId(profileId);
    }

    @Transactional
    public EscalationPolicy updateEscalationPolicy(Long policyId, String policyName,
            Integer lookupPeriodDays, String escalationMode,
            BigDecimal multiplier, BigDecimal adder, String fixedTable,
            BigDecimal maxEscalationFactor, String matchBy, Boolean isEnabled) {
        EscalationPolicy policy = escalationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("递增策略不存在: " + policyId));
        policy.update(policyName, lookupPeriodDays, escalationMode, multiplier, adder,
                fixedTable, maxEscalationFactor, matchBy, isEnabled);
        return escalationPolicyRepository.save(policy);
    }

    @Transactional
    public void deleteEscalationPolicy(Long policyId) {
        escalationPolicyRepository.deleteById(policyId);
    }

    // ===== Profile Versioning (1.7) =====

    @Transactional
    public ScoringProfileVersion publishVersion(Long profileId, String changeSummary, Long publishedBy) {
        ScoringProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + profileId));

        // Collect all sub-resources into a snapshot
        List<ScoreDimension> dims = dimensionRepository.findByScoringProfileId(profileId);
        List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(profileId);
        List<CalculationRuleV7> ruleList = ruleRepository.findByScoringProfileIdOrderByPriority(profileId);
        List<EscalationPolicy> policies = escalationPolicyRepository.findByProfileId(profileId);

        String snapshot;
        try {
            var snapshotMap = new java.util.LinkedHashMap<String, Object>();
            snapshotMap.put("profile", profile);
            snapshotMap.put("dimensions", dims);
            snapshotMap.put("gradeBands", bands);
            snapshotMap.put("rules", ruleList);
            snapshotMap.put("escalationPolicies", policies);
            snapshot = objectMapper.writeValueAsString(snapshotMap);
        } catch (Exception e) {
            throw new RuntimeException("评分配置快照序列化失败", e);
        }

        profile.incrementVersion();
        profileRepository.save(profile);

        ScoringProfileVersion version = ScoringProfileVersion.reconstruct(ScoringProfileVersion.builder()
                .profileId(profileId)
                .tenantId(profile.getTenantId())
                .version(profile.getCurrentVersion())
                .snapshot(snapshot)
                .publishedBy(publishedBy)
                .changeSummary(changeSummary));
        return versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<ScoringProfileVersion> listVersions(Long profileId) {
        return versionRepository.findByProfileId(profileId);
    }

    @Transactional(readOnly = true)
    public Optional<ScoringProfileVersion> getVersion(Long profileId, Integer version) {
        return versionRepository.findByProfileIdAndVersion(profileId, version);
    }
}
