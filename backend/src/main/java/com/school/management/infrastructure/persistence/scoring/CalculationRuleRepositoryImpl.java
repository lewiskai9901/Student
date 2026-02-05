package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.scoring.model.aggregate.CalculationRule;
import com.school.management.domain.scoring.model.valueobject.RuleType;
import com.school.management.domain.scoring.repository.CalculationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 计算规则仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CalculationRuleRepositoryImpl implements CalculationRuleRepository {

    private final CalculationRuleMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public CalculationRule save(CalculationRule rule) {
        CalculationRulePO po = toPO(rule);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        rule.setId(po.getId());
        return rule;
    }

    @Override
    public Optional<CalculationRule> findById(Long id) {
        CalculationRulePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<CalculationRule> findByCode(String code) {
        CalculationRulePO po = mapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<CalculationRule> findAllEnabledOrderByPriority() {
        return mapper.selectAllEnabledOrderByPriority().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalculationRule> findByRuleType(RuleType ruleType) {
        return mapper.selectByRuleType(ruleType.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalculationRule> findAll() {
        LambdaQueryWrapper<CalculationRulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CalculationRulePO::getPriority);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return mapper.countByCode(code) > 0;
    }

    private CalculationRulePO toPO(CalculationRule domain) {
        CalculationRulePO po = new CalculationRulePO();
        po.setId(domain.getId());
        po.setCode(domain.getCode());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setRuleType(domain.getRuleType() != null ? domain.getRuleType().getCode() : "ceiling");
        po.setConditionFormula(domain.getConditionFormula());
        po.setActionFormula(domain.getActionFormula());
        po.setParametersSchema(toJson(domain.getParametersSchema()));
        po.setDefaultParameters(toJson(domain.getDefaultParameters()));
        po.setPriority(domain.getPriority());
        po.setStopOnMatch(domain.getStopOnMatch());
        po.setIsSystem(domain.getIsSystem());
        po.setIsEnabled(domain.getIsEnabled());
        return po;
    }

    private CalculationRule toDomain(CalculationRulePO po) {
        return CalculationRule.builder()
                .id(po.getId())
                .code(po.getCode())
                .name(po.getName())
                .description(po.getDescription())
                .ruleType(RuleType.fromCode(po.getRuleType()))
                .conditionFormula(po.getConditionFormula())
                .actionFormula(po.getActionFormula())
                .parametersSchema(fromJson(po.getParametersSchema(), new TypeReference<Map<String, Object>>() {}))
                .defaultParameters(fromJson(po.getDefaultParameters(), new TypeReference<Map<String, Object>>() {}))
                .priority(po.getPriority())
                .stopOnMatch(po.getStopOnMatch())
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("JSON反序列化失败", e);
            return null;
        }
    }
}
