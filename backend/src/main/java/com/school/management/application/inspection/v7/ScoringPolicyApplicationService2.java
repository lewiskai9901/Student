package com.school.management.application.inspection.v7;

import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.PolicyCalcRule;
import com.school.management.domain.inspection.model.v7.scoring.PolicyGradeBand;
import com.school.management.domain.inspection.model.v7.scoring.ScoringPolicy;
import com.school.management.domain.inspection.repository.v7.PolicyCalcRuleRepository;
import com.school.management.domain.inspection.repository.v7.PolicyGradeBandRepository;
import com.school.management.domain.inspection.repository.v7.ScoringPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 评分方案应用服务 — 管理 ScoringPolicy、PolicyGradeBand、PolicyCalcRule
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringPolicyApplicationService2 {

    private final ScoringPolicyRepository policyRepository;
    private final PolicyGradeBandRepository gradeBandRepository;
    private final PolicyCalcRuleRepository calcRuleRepository;

    // ========== ScoringPolicy ==========

    @Transactional(readOnly = true)
    public List<ScoringPolicy> listPolicies() {
        return policyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ScoringPolicy> getPolicy(Long id) {
        return policyRepository.findById(id);
    }

    @Transactional
    public ScoringPolicy createPolicy(String policyCode, String policyName, String description,
                                      BigDecimal maxScore, BigDecimal minScore,
                                      Integer precisionDigits, Integer sortOrder) {
        Long userId = SecurityUtils.getCurrentUserId();
        ScoringPolicy policy = ScoringPolicy.builder()
                .policyCode(policyCode)
                .policyName(policyName)
                .description(description)
                .maxScore(maxScore)
                .minScore(minScore)
                .precisionDigits(precisionDigits)
                .sortOrder(sortOrder)
                .createdBy(userId)
                .build();
        ScoringPolicy saved = policyRepository.save(policy);
        log.info("创建评分方案: id={}, code={}", saved.getId(), saved.getPolicyCode());
        return saved;
    }

    @Transactional
    public ScoringPolicy updatePolicy(Long id, String policyName, String description,
                                      BigDecimal maxScore, BigDecimal minScore,
                                      Integer precisionDigits, Integer sortOrder) {
        Long userId = SecurityUtils.getCurrentUserId();
        ScoringPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + id));
        policy.update(policyName, description, maxScore, minScore, precisionDigits, sortOrder, userId);
        ScoringPolicy saved = policyRepository.save(policy);
        log.info("更新评分方案: id={}", id);
        return saved;
    }

    @Transactional
    public void deletePolicy(Long id) {
        ScoringPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + id));
        if (Boolean.TRUE.equals(policy.getIsSystem())) {
            throw new IllegalStateException("系统预置评分方案不允许删除");
        }
        calcRuleRepository.deleteByPolicyId(id);
        gradeBandRepository.deleteByPolicyId(id);
        policyRepository.deleteById(id);
        log.info("删除评分方案: id={}", id);
    }

    // ========== PolicyGradeBand ==========

    @Transactional(readOnly = true)
    public List<PolicyGradeBand> listGradeBands(Long policyId) {
        return gradeBandRepository.findByPolicyId(policyId);
    }

    @Transactional
    public PolicyGradeBand createGradeBand(Long policyId, String gradeCode, String gradeName,
                                           BigDecimal minScore, BigDecimal maxScore,
                                           Integer sortOrder) {
        // 验证策略存在
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + policyId));
        PolicyGradeBand band = PolicyGradeBand.builder()
                .policyId(policyId)
                .gradeCode(gradeCode)
                .gradeName(gradeName)
                .minScore(minScore)
                .maxScore(maxScore)
                .sortOrder(sortOrder)
                .build();
        return gradeBandRepository.save(band);
    }

    @Transactional
    public List<PolicyGradeBand> saveGradeBands(Long policyId, List<PolicyGradeBand> bands) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + policyId));
        gradeBandRepository.deleteByPolicyId(policyId);
        return bands.stream()
                .map(b -> {
                    PolicyGradeBand newBand = PolicyGradeBand.builder()
                            .policyId(policyId)
                            .gradeCode(b.getGradeCode())
                            .gradeName(b.getGradeName())
                            .minScore(b.getMinScore())
                            .maxScore(b.getMaxScore())
                            .sortOrder(b.getSortOrder())
                            .build();
                    return gradeBandRepository.save(newBand);
                })
                .toList();
    }

    @Transactional
    public PolicyGradeBand updateGradeBand(Long policyId, Long bandId,
                                           String gradeCode, String gradeName,
                                           BigDecimal minScore, BigDecimal maxScore,
                                           Integer sortOrder) {
        // 获取当前所有 bands，找到对应的 band 重建（因为 PolicyGradeBand 没有 update 方法）
        List<PolicyGradeBand> bands = gradeBandRepository.findByPolicyId(policyId);
        PolicyGradeBand existing = bands.stream()
                .filter(b -> b.getId().equals(bandId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("等级段不存在: " + bandId));

        PolicyGradeBand updated = PolicyGradeBand.reconstruct(
                PolicyGradeBand.builder()
                        .id(existing.getId())
                        .policyId(policyId)
                        .gradeCode(gradeCode)
                        .gradeName(gradeName)
                        .minScore(minScore)
                        .maxScore(maxScore)
                        .sortOrder(sortOrder)
                        .createdAt(existing.getCreatedAt())
        );
        return gradeBandRepository.save(updated);
    }

    @Transactional
    public void deleteGradeBand(Long policyId, Long bandId) {
        List<PolicyGradeBand> bands = gradeBandRepository.findByPolicyId(policyId);
        boolean exists = bands.stream().anyMatch(b -> b.getId().equals(bandId));
        if (!exists) {
            throw new IllegalArgumentException("等级段不存在: " + bandId);
        }
        // PolicyGradeBandRepository 仅提供 deleteByPolicyId，
        // 此处需要先删除全部再重新插入（或在 Infra 层添加 deleteById）
        // 简化实现：重写全部剩余项
        List<PolicyGradeBand> remaining = bands.stream()
                .filter(b -> !b.getId().equals(bandId))
                .toList();
        gradeBandRepository.deleteByPolicyId(policyId);
        remaining.forEach(gradeBandRepository::save);
        log.info("删除等级段: policyId={}, bandId={}", policyId, bandId);
    }

    // ========== PolicyCalcRule ==========

    @Transactional(readOnly = true)
    public List<PolicyCalcRule> listCalcRules(Long policyId) {
        return calcRuleRepository.findByPolicyId(policyId);
    }

    @Transactional
    public PolicyCalcRule createCalcRule(Long policyId, String ruleCode, String ruleName,
                                         String ruleType, Integer priority, String config) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + policyId));
        PolicyCalcRule rule = PolicyCalcRule.builder()
                .policyId(policyId)
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .ruleType(ruleType)
                .priority(priority)
                .config(config)
                .build();
        return calcRuleRepository.save(rule);
    }

    @Transactional
    public PolicyCalcRule updateCalcRule(Long policyId, Long ruleId,
                                         String ruleCode, String ruleName,
                                         String ruleType, Integer priority,
                                         String config, Boolean isEnabled) {
        List<PolicyCalcRule> rules = calcRuleRepository.findByPolicyId(policyId);
        PolicyCalcRule existing = rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("计算规则不存在: " + ruleId));

        PolicyCalcRule updated = PolicyCalcRule.reconstruct(
                PolicyCalcRule.builder()
                        .id(existing.getId())
                        .policyId(policyId)
                        .ruleCode(ruleCode)
                        .ruleName(ruleName)
                        .ruleType(ruleType)
                        .priority(priority)
                        .config(config)
                        .isEnabled(isEnabled)
                        .createdAt(existing.getCreatedAt())
        );
        return calcRuleRepository.save(updated);
    }

    @Transactional
    public void deleteCalcRule(Long policyId, Long ruleId) {
        List<PolicyCalcRule> rules = calcRuleRepository.findByPolicyId(policyId);
        boolean exists = rules.stream().anyMatch(r -> r.getId().equals(ruleId));
        if (!exists) {
            throw new IllegalArgumentException("计算规则不存在: " + ruleId);
        }
        List<PolicyCalcRule> remaining = rules.stream()
                .filter(r -> !r.getId().equals(ruleId))
                .toList();
        calcRuleRepository.deleteByPolicyId(policyId);
        remaining.forEach(calcRuleRepository::save);
        log.info("删除计算规则: policyId={}, ruleId={}", policyId, ruleId);
    }
}
