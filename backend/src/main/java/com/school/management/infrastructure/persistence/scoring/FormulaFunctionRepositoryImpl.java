package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.scoring.model.entity.FormulaFunction;
import com.school.management.domain.scoring.repository.FormulaFunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 内置函数仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FormulaFunctionRepositoryImpl implements FormulaFunctionRepository {

    private final FormulaFunctionMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<FormulaFunction> findByName(String name) {
        FormulaFunctionPO po = mapper.selectByName(name);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<FormulaFunction> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormulaFunction> findByCategory(String category) {
        return mapper.selectByCategory(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormulaFunction> findAll() {
        LambdaQueryWrapper<FormulaFunctionPO> wrapper = new LambdaQueryWrapper<>();
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private FormulaFunction toDomain(FormulaFunctionPO po) {
        return FormulaFunction.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .category(po.getCategory())
                .parametersDef(fromJson(po.getParametersDef(), new TypeReference<List<Map<String, Object>>>() {}))
                .returnType(po.getReturnType())
                .implementation(po.getImplementation())
                .examples(fromJson(po.getExamples(), new TypeReference<List<Map<String, Object>>>() {}))
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .build();
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
