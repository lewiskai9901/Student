package com.school.management.domain.scoring.repository;

import com.school.management.domain.scoring.model.aggregate.CalculationRule;
import com.school.management.domain.scoring.model.valueobject.RuleType;

import java.util.List;
import java.util.Optional;

/**
 * 计算规则仓储接口
 */
public interface CalculationRuleRepository {

    /**
     * 保存规则
     */
    CalculationRule save(CalculationRule rule);

    /**
     * 根据ID查询
     */
    Optional<CalculationRule> findById(Long id);

    /**
     * 根据代码查询
     */
    Optional<CalculationRule> findByCode(String code);

    /**
     * 查询所有启用的规则（按优先级排序）
     */
    List<CalculationRule> findAllEnabledOrderByPriority();

    /**
     * 按类型查询
     */
    List<CalculationRule> findByRuleType(RuleType ruleType);

    /**
     * 查询所有
     */
    List<CalculationRule> findAll();

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 检查代码是否存在
     */
    boolean existsByCode(String code);
}
