package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.model.v7.template.InspTemplate;
import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;
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
    private final TemplateModuleRefRepository moduleRefRepository;
    private final InspTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    // ===== ScoringProfile =====

    @Transactional
    public ScoringProfile createProfile(Long templateId, Long createdBy) {
        profileRepository.findByTemplateId(templateId).ifPresent(existing -> {
            throw new IllegalArgumentException("模板已有评分配置: templateId=" + templateId);
        });
        ScoringProfile profile = ScoringProfile.create(templateId, createdBy);
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Optional<ScoringProfile> getProfile(Long id) {
        return profileRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ScoringProfile> getProfileByTemplateId(Long templateId) {
        return profileRepository.findByTemplateId(templateId);
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
     * 从模板的子模板引用同步 MODULE 类型的评分维度
     * 每个子模板引用生成一个 sourceType=MODULE 的维度，权重取引用的 weight
     */
    @Transactional
    public List<ScoreDimension> syncDimensionsFromModuleRefs(Long scoringProfileId) {
        ScoringProfile profile = profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));

        List<TemplateModuleRef> moduleRefs = moduleRefRepository.findByCompositeTemplateId(profile.getTemplateId());
        List<ScoreDimension> existing = dimensionRepository.findByScoringProfileId(scoringProfileId);

        // Existing MODULE dimensions keyed by moduleTemplateId
        java.util.Map<Long, ScoreDimension> existingModuleDims = new java.util.HashMap<>();
        for (ScoreDimension dim : existing) {
            if ("MODULE".equals(dim.getSourceType()) && dim.getModuleTemplateId() != null) {
                existingModuleDims.put(dim.getModuleTemplateId(), dim);
            }
        }

        // Track which module template IDs are still referenced
        java.util.Set<Long> activeModuleIds = new java.util.HashSet<>();

        for (TemplateModuleRef ref : moduleRefs) {
            activeModuleIds.add(ref.getModuleTemplateId());
            ScoreDimension existingDim = existingModuleDims.get(ref.getModuleTemplateId());

            if (existingDim != null) {
                // Update weight if changed
                existingDim.update(existingDim.getDimensionName(), ref.getWeight(),
                        existingDim.getBaseScore(), existingDim.getPassThreshold());
                dimensionRepository.save(existingDim);
            } else {
                // Create new MODULE dimension
                String moduleName = templateRepository.findById(ref.getModuleTemplateId())
                        .map(InspTemplate::getTemplateName)
                        .orElse("子模板 #" + ref.getModuleTemplateId());
                ScoreDimension newDim = ScoreDimension.reconstruct(ScoreDimension.builder()
                        .scoringProfileId(scoringProfileId)
                        .dimensionCode("MOD_" + ref.getModuleTemplateId())
                        .dimensionName(moduleName)
                        .weight(ref.getWeight())
                        .sourceType("MODULE")
                        .moduleTemplateId(ref.getModuleTemplateId())
                        .sortOrder(ref.getSortOrder()));
                dimensionRepository.save(newDim);
            }
        }

        // Remove MODULE dimensions for removed refs
        for (var entry : existingModuleDims.entrySet()) {
            if (!activeModuleIds.contains(entry.getKey())) {
                dimensionRepository.deleteById(entry.getValue().getId());
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
