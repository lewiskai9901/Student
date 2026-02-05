package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.scoring.model.aggregate.ScoringStrategy;
import com.school.management.domain.scoring.model.valueobject.StrategyCategory;
import com.school.management.domain.scoring.repository.ScoringStrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 计分策略仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ScoringStrategyRepositoryImpl implements ScoringStrategyRepository {

    private final ScoringStrategyMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public ScoringStrategy save(ScoringStrategy strategy) {
        ScoringStrategyPO po = toPO(strategy);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        strategy.setId(po.getId());
        return strategy;
    }

    @Override
    public Optional<ScoringStrategy> findById(Long id) {
        ScoringStrategyPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<ScoringStrategy> findByCode(String code) {
        ScoringStrategyPO po = mapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ScoringStrategy> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoringStrategy> findByCategory(StrategyCategory category) {
        return mapper.selectByCategory(category.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoringStrategy> findAll() {
        LambdaQueryWrapper<ScoringStrategyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ScoringStrategyPO::getSortOrder);
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

    private ScoringStrategyPO toPO(ScoringStrategy domain) {
        ScoringStrategyPO po = new ScoringStrategyPO();
        po.setId(domain.getId());
        po.setCode(domain.getCode());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setCategory(domain.getCategory() != null ? domain.getCategory().getCode() : "basic");
        po.setFormulaTemplate(domain.getFormulaTemplate());
        po.setFormulaDescription(domain.getFormulaDescription());
        po.setParametersSchema(toJson(domain.getParametersSchema()));
        po.setDefaultParameters(toJson(domain.getDefaultParameters()));
        po.setSupportedInputTypes(toJson(domain.getSupportedInputTypes()));
        po.setSupportedRuleTypes(toJson(domain.getSupportedRuleTypes()));
        po.setIsSystem(domain.getIsSystem());
        po.setIsEnabled(domain.getIsEnabled());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        return po;
    }

    private ScoringStrategy toDomain(ScoringStrategyPO po) {
        return ScoringStrategy.builder()
                .id(po.getId())
                .code(po.getCode())
                .name(po.getName())
                .description(po.getDescription())
                .category(StrategyCategory.fromCode(po.getCategory()))
                .formulaTemplate(po.getFormulaTemplate())
                .formulaDescription(po.getFormulaDescription())
                .parametersSchema(fromJson(po.getParametersSchema(), new TypeReference<Map<String, Object>>() {}))
                .defaultParameters(fromJson(po.getDefaultParameters(), new TypeReference<Map<String, Object>>() {}))
                .supportedInputTypes(fromJson(po.getSupportedInputTypes(), new TypeReference<List<String>>() {}))
                .supportedRuleTypes(fromJson(po.getSupportedRuleTypes(), new TypeReference<List<String>>() {}))
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
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
