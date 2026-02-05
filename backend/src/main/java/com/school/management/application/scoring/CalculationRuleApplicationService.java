package com.school.management.application.scoring;

import com.school.management.application.scoring.dto.*;
import com.school.management.domain.scoring.model.aggregate.CalculationRule;
import com.school.management.domain.scoring.model.valueobject.RuleType;
import com.school.management.domain.scoring.repository.CalculationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 计算规则应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationRuleApplicationService {

    private final CalculationRuleRepository repository;

    /**
     * 获取所有规则（按类型分组）
     */
    public Map<String, List<CalculationRuleDTO>> getAllGroupedByType() {
        List<CalculationRule> rules = repository.findAllEnabledOrderByPriority();
        return rules.stream()
                .map(this::toDTO)
                .collect(Collectors.groupingBy(CalculationRuleDTO::getRuleType));
    }

    /**
     * 获取所有规则
     */
    public List<CalculationRuleDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按类型获取规则
     */
    public List<CalculationRuleDTO> getByRuleType(String ruleType) {
        RuleType type = RuleType.fromCode(ruleType);
        return repository.findByRuleType(type).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取规则
     */
    public CalculationRuleDTO getById(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + id));
    }

    /**
     * 根据代码获取规则
     */
    public CalculationRuleDTO getByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + code));
    }

    /**
     * 创建自定义规则
     */
    @Transactional
    public CalculationRuleDTO create(CreateCalculationRuleCommand command) {
        if (repository.existsByCode(command.getCode())) {
            throw new RuntimeException("规则代码已存在: " + command.getCode());
        }

        CalculationRule rule = CalculationRule.createCustom(
                command.getCode(),
                command.getName(),
                command.getDescription(),
                RuleType.fromCode(command.getRuleType()),
                command.getConditionFormula(),
                command.getActionFormula(),
                command.getPriority() != null ? command.getPriority() : 100,
                command.getStopOnMatch() != null ? command.getStopOnMatch() : false
        );

        rule.setParametersSchema(command.getParametersSchema());
        rule.setDefaultParameters(command.getDefaultParameters());

        repository.save(rule);
        log.info("创建自定义计算规则: {}", rule.getCode());
        return toDTO(rule);
    }

    /**
     * 更新规则
     */
    @Transactional
    public CalculationRuleDTO update(Long id, UpdateCalculationRuleCommand command) {
        CalculationRule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + id));

        if (Boolean.TRUE.equals(rule.getIsSystem())) {
            throw new RuntimeException("系统内置规则不能修改");
        }

        rule.setName(command.getName());
        rule.setDescription(command.getDescription());
        rule.setRuleType(RuleType.fromCode(command.getRuleType()));
        rule.setConditionFormula(command.getConditionFormula());
        rule.setActionFormula(command.getActionFormula());
        rule.setParametersSchema(command.getParametersSchema());
        rule.setDefaultParameters(command.getDefaultParameters());
        rule.setPriority(command.getPriority());
        rule.setStopOnMatch(command.getStopOnMatch());

        repository.save(rule);
        log.info("更新计算规则: {}", rule.getCode());
        return toDTO(rule);
    }

    /**
     * 删除规则
     */
    @Transactional
    public void delete(Long id) {
        CalculationRule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + id));

        if (Boolean.TRUE.equals(rule.getIsSystem())) {
            throw new RuntimeException("系统内置规则不能删除");
        }

        repository.delete(id);
        log.info("删除计算规则: {}", rule.getCode());
    }

    /**
     * 启用/禁用规则
     */
    @Transactional
    public void toggleEnabled(Long id, boolean enabled) {
        CalculationRule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + id));

        if (enabled) {
            rule.enable();
        } else {
            rule.disable();
        }
        repository.save(rule);
    }

    /**
     * 调整规则优先级
     */
    @Transactional
    public void updatePriority(Long id, Integer priority) {
        CalculationRule rule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("计算规则不存在: " + id));

        rule.setPriority(priority);
        repository.save(rule);
    }

    private CalculationRuleDTO toDTO(CalculationRule rule) {
        CalculationRuleDTO dto = new CalculationRuleDTO();
        dto.setId(rule.getId());
        dto.setCode(rule.getCode());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setRuleType(rule.getRuleType() != null ? rule.getRuleType().getCode() : "ceiling");
        dto.setRuleTypeName(rule.getRuleType() != null ? rule.getRuleType().getName() : "封顶规则");
        dto.setConditionFormula(rule.getConditionFormula());
        dto.setActionFormula(rule.getActionFormula());
        dto.setParametersSchema(rule.getParametersSchema());
        dto.setDefaultParameters(rule.getDefaultParameters());
        dto.setPriority(rule.getPriority());
        dto.setStopOnMatch(rule.getStopOnMatch());
        dto.setIsSystem(rule.getIsSystem());
        dto.setIsEnabled(rule.getIsEnabled());
        return dto;
    }
}
