package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.scoring.*;
import com.school.management.domain.inspection.model.template.TemplateItem;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评分配置应用服务.
 *
 * <p>2026-05-23 重构: ScoringProfile 改为项目-owned, 同 sectionId 在不同
 * 项目下各自一套 profile, 不再跨项目共享. 所有 create / list 操作必须
 * 指定 projectId; 跨项目共享通过 {@link #cloneForProject(Long, Long, Long)}
 * 显式克隆.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ScoringProfileApplicationService {

    private final ScoringProfileRepository profileRepository;
    private final ScoreDimensionRepository dimensionRepository;
    private final GradeBandRepository gradeBandRepository;
    private final CalculationRuleRepository ruleRepository;
    private final EscalationPolicyRepository escalationPolicyRepository;
    private final ScoringProfileVersionRepository versionRepository;
    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    // ===== ScoringProfile =====

    /**
     * 项目-owned 创建评分方案. projectId 必传 — 同 sectionId 在不同项目下
     * 各自一套 profile, 调用前需先选定项目.
     */
    @Transactional
    @CacheEvict(value = "ratingConfig", allEntries = true)
    public ScoringProfile createProfile(Long projectId, Long sectionId, Long createdBy) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 必传 — 评分方案归属项目, 不再跨项目共享");
        }
        // 项目-owned: (project, section) 范围内幂等
        Optional<ScoringProfile> existing = profileRepository.findByProjectIdAndSectionId(projectId, sectionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        ScoringProfile profile = ScoringProfile.create(sectionId, projectId, createdBy);
        try {
            return profileRepository.save(profile);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            log.warn("[ScoringProfile] race detected on projectId={} sectionId={}, fallback to refind",
                    projectId, sectionId);
            return profileRepository.findByProjectIdAndSectionId(projectId, sectionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "评分配置创建失败 (并发竞争且复查未命中, projectId=" + projectId
                                    + " sectionId=" + sectionId + ")", dup));
        }
    }

    /** P0-B Redis 缓存. Spring Cache 自动解包 Optional, #result 即为 ScoringProfile (或 null). */
    @Transactional(readOnly = true)
    @Cacheable(value = "ratingConfig", key = "'scoringProfile:' + #id", unless = "#result == null")
    public Optional<ScoringProfile> getProfile(Long id) {
        return profileRepository.findById(id);
    }

    /** 按 (项目, 分区) 唯一定位 — 替代旧"按 sectionId 全局查"语义. */
    @Transactional(readOnly = true)
    @Cacheable(value = "ratingConfig",
               key = "'scoringProfile:project:' + #projectId + ':section:' + #sectionId",
               unless = "#result == null")
    public Optional<ScoringProfile> getProfileByProjectIdAndSectionId(Long projectId, Long sectionId) {
        return profileRepository.findByProjectIdAndSectionId(projectId, sectionId);
    }

    /** 列出某项目下所有评分方案. */
    @Transactional(readOnly = true)
    @Cacheable(value = "ratingConfig", key = "'scoringProfiles:project:' + #projectId")
    public List<ScoringProfile> listByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 必传");
        }
        return profileRepository.findByProjectId(projectId);
    }

    @Transactional
    @CacheEvict(value = "ratingConfig", allEntries = true)
    public ScoringProfile updateProfile(Long id, BigDecimal maxScore,
                                         BigDecimal minScore, Integer precisionDigits,
                                         Long updatedBy) {
        ScoringProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + id));
        profile.update(maxScore, minScore, precisionDigits, updatedBy);
        return profileRepository.save(profile);
    }

    /**
     * 更新评分方案高级参数. 项目-owned 模式下要求调用方传入预期项目 id 做归属校验,
     * 防止误用 profileId 跨项目改写他项目的评分规则.
     *
     * @param expectedProjectId 预期归属项目 id, null 表示跳过校验 (仅供超管运维路径).
     */
    @Transactional
    @CacheEvict(value = "ratingConfig", allEntries = true)
    public ScoringProfile updateAdvancedSettings(Long id,
            Long expectedProjectId,
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
        if (expectedProjectId != null && profile.getProjectId() != null
                && !expectedProjectId.equals(profile.getProjectId())) {
            throw new IllegalArgumentException(
                    "评分方案 " + id + " 不属于项目 " + expectedProjectId
                            + " (实际归属 " + profile.getProjectId() + "), 禁止跨项目修改");
        }
        profile.updateAdvancedSettings(trendFactorEnabled, trendLookbackDays,
                trendBonusPerPercent, trendPenaltyPerPercent, trendMaxAdjustment,
                decayEnabled, decayMode, decayRatePerDay, decayFloor,
                multiRaterMode, raterWeightBy, consensusThreshold,
                calibrationEnabled, calibrationMethod, calibrationPeriodDays, calibrationMinSamples,
                updatedBy);
        return profileRepository.save(profile);
    }

    @Transactional
    @CacheEvict(value = "ratingConfig", allEntries = true)
    public void deleteProfile(Long id) {
        ruleRepository.deleteByScoringProfileId(id);
        gradeBandRepository.deleteByScoringProfileId(id);
        dimensionRepository.deleteByScoringProfileId(id);
        profileRepository.deleteById(id);
    }

    /**
     * 深拷贝 profile 到目标项目, 同步复制关联的 GradeBand / CalculationRule / ScoreDimension.
     * 所有新实体的 id 由仓储层 (MyBatis-Plus IdWorker) 在 insert 时生成,
     * 复制过程不携带源 id, 不带 createdAt/updatedAt (走默认填充).
     *
     * <p>用于 Phase 2 一次性数据迁移以及 Phase 3 项目克隆功能.
     *
     * <p>注意:
     * <ul>
     *   <li>EscalationPolicy 与 ScoringProfileVersion 不复制 — 前者属于运营级策略,
     *       后者是历史版本快照, 克隆新项目应从空白开始.</li>
     *   <li>GradeBand 中的 dimensionId 会按新 dimension id 重映射, 否则会指向源项目维度.</li>
     * </ul>
     *
     * @return 新 profile (含已分配的 id)
     */
    @Transactional
    @CacheEvict(value = "ratingConfig", allEntries = true)
    public ScoringProfile cloneForProject(Long sourceProfileId, Long newProjectId, Long createdBy) {
        if (newProjectId == null) {
            throw new IllegalArgumentException("newProjectId 必传");
        }
        ScoringProfile source = profileRepository.findById(sourceProfileId)
                .orElseThrow(() -> new IllegalArgumentException("源评分方案不存在: " + sourceProfileId));

        // 1. 复制 profile 自身 — id 留空让 IdWorker 生成, projectId 改为目标项目
        ScoringProfile copy = ScoringProfile.reconstruct(ScoringProfile.builder()
                .tenantId(source.getTenantId())
                .sectionId(source.getSectionId())
                .projectId(newProjectId)
                .maxScore(source.getMaxScore())
                .minScore(source.getMinScore())
                .precisionDigits(source.getPrecisionDigits())
                .currentVersion(0)  // 新版本从 0 开始, 不继承源版本号
                .trendFactorEnabled(source.getTrendFactorEnabled())
                .trendLookbackDays(source.getTrendLookbackDays())
                .trendBonusPerPercent(source.getTrendBonusPerPercent())
                .trendPenaltyPerPercent(source.getTrendPenaltyPerPercent())
                .trendMaxAdjustment(source.getTrendMaxAdjustment())
                .decayEnabled(source.getDecayEnabled())
                .decayMode(source.getDecayMode())
                .decayRatePerDay(source.getDecayRatePerDay())
                .decayFloor(source.getDecayFloor())
                .multiRaterMode(source.getMultiRaterMode())
                .raterWeightBy(source.getRaterWeightBy())
                .consensusThreshold(source.getConsensusThreshold())
                .calibrationEnabled(source.getCalibrationEnabled())
                .calibrationMethod(source.getCalibrationMethod())
                .calibrationPeriodDays(source.getCalibrationPeriodDays())
                .calibrationMinSamples(source.getCalibrationMinSamples())
                .createdBy(createdBy != null ? createdBy : source.getCreatedBy()));
        ScoringProfile savedCopy = profileRepository.save(copy);
        Long newProfileId = savedCopy.getId();

        // 2. 复制 ScoreDimension, 建立 旧 id → 新 id 映射
        Map<Long, Long> dimensionIdMap = new HashMap<>();
        List<ScoreDimension> srcDims = dimensionRepository.findByScoringProfileId(sourceProfileId);
        for (ScoreDimension srcDim : srcDims) {
            ScoreDimension dimCopy = ScoreDimension.reconstruct(ScoreDimension.builder()
                    .tenantId(srcDim.getTenantId())
                    .scoringProfileId(newProfileId)
                    .dimensionCode(srcDim.getDimensionCode())
                    .dimensionName(srcDim.getDimensionName())
                    .weight(srcDim.getWeight())
                    .baseScore(srcDim.getBaseScore())
                    .passThreshold(srcDim.getPassThreshold())
                    .sourceType(srcDim.getSourceType())
                    .moduleTemplateId(srcDim.getModuleTemplateId())
                    .sortOrder(srcDim.getSortOrder()));
            ScoreDimension savedDim = dimensionRepository.save(dimCopy);
            dimensionIdMap.put(srcDim.getId(), savedDim.getId());
        }

        // 3. 复制 GradeBand, dimensionId 按映射重写
        List<GradeBand> srcBands = gradeBandRepository.findByScoringProfileId(sourceProfileId);
        for (GradeBand srcBand : srcBands) {
            Long mappedDimId = srcBand.getDimensionId() != null
                    ? dimensionIdMap.get(srcBand.getDimensionId()) : null;
            GradeBand bandCopy = GradeBand.reconstruct(GradeBand.builder()
                    .tenantId(srcBand.getTenantId())
                    .scoringProfileId(newProfileId)
                    .dimensionId(mappedDimId)
                    .gradeCode(srcBand.getGradeCode())
                    .gradeName(srcBand.getGradeName())
                    .minScore(srcBand.getMinScore())
                    .maxScore(srcBand.getMaxScore())
                    .color(srcBand.getColor())
                    .icon(srcBand.getIcon())
                    .sortOrder(srcBand.getSortOrder()));
            gradeBandRepository.save(bandCopy);
        }

        // 4. 复制 CalculationRule
        List<CalculationRule> srcRules = ruleRepository.findByScoringProfileIdOrderByPriority(sourceProfileId);
        for (CalculationRule srcRule : srcRules) {
            CalculationRule ruleCopy = CalculationRule.reconstruct(CalculationRule.builder()
                    .tenantId(srcRule.getTenantId())
                    .scoringProfileId(newProfileId)
                    .ruleCode(srcRule.getRuleCode())
                    .ruleName(srcRule.getRuleName())
                    .priority(srcRule.getPriority())
                    .ruleType(srcRule.getRuleType())
                    .config(srcRule.getConfig())
                    .isEnabled(srcRule.getIsEnabled())
                    .scopeType(srcRule.getScopeType())
                    .targetDimensionIds(srcRule.getTargetDimensionIds())
                    .activationCondition(srcRule.getActivationCondition())
                    .appliesTo(srcRule.getAppliesTo())
                    .effectiveFrom(srcRule.getEffectiveFrom())
                    .effectiveUntil(srcRule.getEffectiveUntil())
                    .exclusionGroup(srcRule.getExclusionGroup()));
            ruleRepository.save(ruleCopy);
        }

        log.info("Cloned ScoringProfile {} -> {} (project {} -> {}): dims={} bands={} rules={}",
                sourceProfileId, newProfileId, source.getProjectId(), newProjectId,
                srcDims.size(), srcBands.size(), srcRules.size());
        return savedCopy;
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
    public CalculationRule createRule(Long scoringProfileId, String ruleCode,
                                         String ruleName, Integer priority,
                                         RuleType ruleType, String config,
                                         Boolean isEnabled, String scopeType,
                                         String targetDimensionIds,
                                         String activationCondition, String appliesTo,
                                         LocalDate effectiveFrom, LocalDate effectiveUntil,
                                         String exclusionGroup) {
        profileRepository.findById(scoringProfileId)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + scoringProfileId));
        CalculationRule rule = CalculationRule.reconstruct(CalculationRule.builder()
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
    public List<CalculationRule> listRules(Long scoringProfileId) {
        return ruleRepository.findByScoringProfileIdOrderByPriority(scoringProfileId);
    }

    @Transactional
    public CalculationRule updateRule(Long ruleId, String ruleName, Integer priority,
                                         RuleType ruleType, String config, Boolean isEnabled,
                                         String scopeType, String targetDimensionIds,
                                         String activationCondition, String appliesTo,
                                         LocalDate effectiveFrom, LocalDate effectiveUntil,
                                         String exclusionGroup) {
        CalculationRule rule = ruleRepository.findById(ruleId)
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
        List<CalculationRule> ruleList = ruleRepository.findByScoringProfileIdOrderByPriority(profileId);
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
