package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.scoring.model.entity.FormulaVariable;
import com.school.management.domain.scoring.repository.FormulaVariableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 内置变量仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FormulaVariableRepositoryImpl implements FormulaVariableRepository {

    private final FormulaVariableMapper mapper;

    @Override
    public Optional<FormulaVariable> findByName(String name) {
        FormulaVariablePO po = mapper.selectByName(name);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<FormulaVariable> findAllEnabled() {
        return mapper.selectAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormulaVariable> findByCategory(String category) {
        return mapper.selectByCategory(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FormulaVariable> findAll() {
        LambdaQueryWrapper<FormulaVariablePO> wrapper = new LambdaQueryWrapper<>();
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private FormulaVariable toDomain(FormulaVariablePO po) {
        return FormulaVariable.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .category(po.getCategory())
                .valueType(po.getValueType())
                .defaultValue(po.getDefaultValue())
                .sourceDescription(po.getSourceDescription())
                .isSystem(po.getIsSystem())
                .isEnabled(po.getIsEnabled())
                .build();
    }
}
