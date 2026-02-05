package com.school.management.application.scoring;

import com.school.management.application.scoring.dto.*;
import com.school.management.domain.scoring.model.aggregate.InputType;
import com.school.management.domain.scoring.model.valueobject.ComponentType;
import com.school.management.domain.scoring.model.valueobject.ValueType;
import com.school.management.domain.scoring.repository.InputTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 打分方式应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InputTypeApplicationService {

    private final InputTypeRepository repository;

    /**
     * 获取所有打分方式（按分类分组）
     */
    public Map<String, List<InputTypeDTO>> getAllGroupedByCategory() {
        List<InputType> inputTypes = repository.findAllEnabled();
        return inputTypes.stream()
                .map(this::toDTO)
                .collect(Collectors.groupingBy(InputTypeDTO::getCategory));
    }

    /**
     * 获取所有打分方式
     */
    public List<InputTypeDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取打分方式
     */
    public List<InputTypeDTO> getByCategory(String category) {
        return repository.findByCategory(category).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取打分方式
     */
    public InputTypeDTO getById(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("打分方式不存在: " + id));
    }

    /**
     * 根据代码获取打分方式
     */
    public InputTypeDTO getByCode(String code) {
        return repository.findByCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("打分方式不存在: " + code));
    }

    /**
     * 创建自定义打分方式
     */
    @Transactional
    public InputTypeDTO create(CreateInputTypeCommand command) {
        if (repository.existsByCode(command.getCode())) {
            throw new RuntimeException("打分方式代码已存在: " + command.getCode());
        }

        InputType inputType = InputType.createCustom(
                command.getCode(),
                command.getName(),
                command.getDescription(),
                command.getCategory(),
                ComponentType.fromCode(command.getComponentType()),
                command.getComponentConfig(),
                ValueType.fromCode(command.getValueType()),
                command.getValueMapping(),
                command.getValidationRules()
        );

        repository.save(inputType);
        log.info("创建自定义打分方式: {}", inputType.getCode());
        return toDTO(inputType);
    }

    /**
     * 更新打分方式
     */
    @Transactional
    public InputTypeDTO update(Long id, UpdateInputTypeCommand command) {
        InputType inputType = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("打分方式不存在: " + id));

        if (Boolean.TRUE.equals(inputType.getIsSystem())) {
            throw new RuntimeException("系统内置打分方式不能修改");
        }

        inputType.setName(command.getName());
        inputType.setDescription(command.getDescription());
        inputType.setCategory(command.getCategory());
        inputType.setComponentType(ComponentType.fromCode(command.getComponentType()));
        inputType.setComponentConfig(command.getComponentConfig());
        inputType.setValueType(ValueType.fromCode(command.getValueType()));
        inputType.setValueMapping(command.getValueMapping());
        inputType.setValidationRules(command.getValidationRules());

        repository.save(inputType);
        log.info("更新打分方式: {}", inputType.getCode());
        return toDTO(inputType);
    }

    /**
     * 删除打分方式
     */
    @Transactional
    public void delete(Long id) {
        InputType inputType = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("打分方式不存在: " + id));

        if (Boolean.TRUE.equals(inputType.getIsSystem())) {
            throw new RuntimeException("系统内置打分方式不能删除");
        }

        repository.delete(id);
        log.info("删除打分方式: {}", inputType.getCode());
    }

    /**
     * 启用/禁用打分方式
     */
    @Transactional
    public void toggleEnabled(Long id, boolean enabled) {
        InputType inputType = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("打分方式不存在: " + id));

        if (enabled) {
            inputType.enable();
        } else {
            inputType.disable();
        }
        repository.save(inputType);
    }

    private InputTypeDTO toDTO(InputType inputType) {
        InputTypeDTO dto = new InputTypeDTO();
        dto.setId(inputType.getId());
        dto.setCode(inputType.getCode());
        dto.setName(inputType.getName());
        dto.setDescription(inputType.getDescription());
        dto.setCategory(inputType.getCategory());
        dto.setComponentType(inputType.getComponentType() != null ? inputType.getComponentType().getCode() : "number");
        dto.setComponentTypeName(inputType.getComponentType() != null ? inputType.getComponentType().getName() : "数字输入");
        dto.setComponentConfig(inputType.getComponentConfig());
        dto.setValueType(inputType.getValueType() != null ? inputType.getValueType().getCode() : "number");
        dto.setValueMapping(inputType.getValueMapping());
        dto.setValidationRules(inputType.getValidationRules());
        dto.setIsSystem(inputType.getIsSystem());
        dto.setIsEnabled(inputType.getIsEnabled());
        dto.setSortOrder(inputType.getSortOrder());
        return dto;
    }
}
