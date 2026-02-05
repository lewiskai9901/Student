package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.scoring.model.aggregate.InputType;
import com.school.management.domain.scoring.model.valueobject.ComponentType;
import com.school.management.domain.scoring.model.valueobject.ValueType;
import com.school.management.domain.scoring.repository.InputTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 打分方式仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InputTypeRepositoryImpl implements InputTypeRepository {

    private final InputTypeMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public InputType save(InputType inputType) {
        InputTypePO po = toPO(inputType);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        inputType.setId(po.getId());
        return inputType;
    }

    @Override
    public Optional<InputType> findById(Long id) {
        InputTypePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<InputType> findByCode(String code) {
        InputTypePO po = mapper.selectByCode(code);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InputType> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InputType> findByCategory(String category) {
        return mapper.selectByCategory(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InputType> findAll() {
        LambdaQueryWrapper<InputTypePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(InputTypePO::getSortOrder);
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

    private InputTypePO toPO(InputType domain) {
        InputTypePO po = new InputTypePO();
        po.setId(domain.getId());
        po.setCode(domain.getCode());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setCategory(domain.getCategory());
        po.setComponentType(domain.getComponentType() != null ? domain.getComponentType().getCode() : "number");
        po.setComponentConfig(toJson(domain.getComponentConfig()));
        po.setValueType(domain.getValueType() != null ? domain.getValueType().getCode() : "number");
        po.setValueMapping(toJson(domain.getValueMapping()));
        po.setValidationRules(toJson(domain.getValidationRules()));
        po.setIsSystem(domain.getIsSystem());
        po.setIsEnabled(domain.getIsEnabled());
        po.setSortOrder(domain.getSortOrder());
        return po;
    }

    private InputType toDomain(InputTypePO po) {
        return InputType.builder()
                .id(po.getId())
                .code(po.getCode())
                .name(po.getName())
                .description(po.getDescription())
                .category(po.getCategory())
                .componentType(ComponentType.fromCode(po.getComponentType()))
                .componentConfig(fromJson(po.getComponentConfig(), new TypeReference<Map<String, Object>>() {}))
                .valueType(ValueType.fromCode(po.getValueType()))
                .valueMapping(fromJson(po.getValueMapping(), new TypeReference<Map<String, Object>>() {}))
                .validationRules(fromJson(po.getValidationRules(), new TypeReference<Map<String, Object>>() {}))
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
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
