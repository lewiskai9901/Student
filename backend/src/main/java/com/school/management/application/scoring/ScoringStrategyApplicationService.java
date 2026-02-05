package com.school.management.application.scoring;

import com.school.management.application.scoring.dto.*;
import com.school.management.domain.scoring.model.aggregate.ScoringStrategy;
import com.school.management.domain.scoring.model.valueobject.StrategyCategory;
import com.school.management.domain.scoring.repository.ScoringStrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 计分策略应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringStrategyApplicationService {

    private final ScoringStrategyRepository repository;

    /**
     * 获取所有策略（按分类分组）
     */
    public Map<String, List<ScoringStrategyDTO>> getAllGroupedByCategory() {
        List<ScoringStrategy> strategies = repository.findAllEnabled();
        return strategies.stream()
                .map(this::toDTO)
                .collect(Collectors.groupingBy(ScoringStrategyDTO::getCategory));
    }

    /**
     * 获取所有策略
     */
    public List<ScoringStrategyDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取策略
     */
    public List<ScoringStrategyDTO> getByCategory(String category) {
        StrategyCategory cat = StrategyCategory.fromCode(category);
        return repository.findByCategory(cat).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取策略
     */
    public ScoringStrategyDTO getById(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("策略不存在: " + id));
    }

    /**
     * 根据代码获取策略
     */
    public ScoringStrategyDTO getByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("策略不存在: " + code));
    }

    /**
     * 创建自定义策略
     */
    @Transactional
    public ScoringStrategyDTO create(CreateScoringStrategyCommand command) {
        // 检查代码唯一性
        if (repository.existsByCode(command.getCode())) {
            throw new RuntimeException("策略代码已存在: " + command.getCode());
        }

        ScoringStrategy strategy = ScoringStrategy.createCustom(
                command.getCode(),
                command.getName(),
                command.getDescription(),
                StrategyCategory.fromCode(command.getCategory()),
                command.getFormulaTemplate(),
                command.getFormulaDescription(),
                command.getSupportedInputTypes()
        );

        strategy.setParametersSchema(command.getParametersSchema());
        strategy.setDefaultParameters(command.getDefaultParameters());

        repository.save(strategy);
        log.info("创建自定义计分策略: {}", strategy.getCode());
        return toDTO(strategy);
    }

    /**
     * 更新策略
     */
    @Transactional
    public ScoringStrategyDTO update(Long id, UpdateScoringStrategyCommand command) {
        ScoringStrategy strategy = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("策略不存在: " + id));

        if (Boolean.TRUE.equals(strategy.getIsSystem())) {
            throw new RuntimeException("系统内置策略不能修改");
        }

        strategy.setName(command.getName());
        strategy.setDescription(command.getDescription());
        strategy.setCategory(StrategyCategory.fromCode(command.getCategory()));
        strategy.updateFormula(command.getFormulaTemplate(), command.getFormulaDescription());
        strategy.setSupportedInputTypes(command.getSupportedInputTypes());
        strategy.setParametersSchema(command.getParametersSchema());
        strategy.setDefaultParameters(command.getDefaultParameters());

        repository.save(strategy);
        log.info("更新计分策略: {}", strategy.getCode());
        return toDTO(strategy);
    }

    /**
     * 删除策略
     */
    @Transactional
    public void delete(Long id) {
        ScoringStrategy strategy = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("策略不存在: " + id));

        if (Boolean.TRUE.equals(strategy.getIsSystem())) {
            throw new RuntimeException("系统内置策略不能删除");
        }

        repository.delete(id);
        log.info("删除计分策略: {}", strategy.getCode());
    }

    /**
     * 启用/禁用策略
     */
    @Transactional
    public void toggleEnabled(Long id, boolean enabled) {
        ScoringStrategy strategy = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("策略不存在: " + id));

        if (enabled) {
            strategy.enable();
        } else {
            strategy.disable();
        }
        repository.save(strategy);
    }

    private ScoringStrategyDTO toDTO(ScoringStrategy strategy) {
        ScoringStrategyDTO dto = new ScoringStrategyDTO();
        dto.setId(strategy.getId());
        dto.setCode(strategy.getCode());
        dto.setName(strategy.getName());
        dto.setDescription(strategy.getDescription());
        dto.setCategory(strategy.getCategory() != null ? strategy.getCategory().getCode() : "basic");
        dto.setCategoryName(strategy.getCategory() != null ? strategy.getCategory().getName() : "基础策略");
        dto.setFormulaTemplate(strategy.getFormulaTemplate());
        dto.setFormulaDescription(strategy.getFormulaDescription());
        dto.setParametersSchema(strategy.getParametersSchema());
        dto.setDefaultParameters(strategy.getDefaultParameters());
        dto.setSupportedInputTypes(strategy.getSupportedInputTypes());
        dto.setSupportedRuleTypes(strategy.getSupportedRuleTypes());
        dto.setIsSystem(strategy.getIsSystem());
        dto.setIsEnabled(strategy.getIsEnabled());
        dto.setSortOrder(strategy.getSortOrder());
        return dto;
    }
}
