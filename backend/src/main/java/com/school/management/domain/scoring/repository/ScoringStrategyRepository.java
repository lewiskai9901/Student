package com.school.management.domain.scoring.repository;

import com.school.management.domain.scoring.model.aggregate.ScoringStrategy;
import com.school.management.domain.scoring.model.valueobject.StrategyCategory;

import java.util.List;
import java.util.Optional;

/**
 * 计分策略仓储接口
 */
public interface ScoringStrategyRepository {

    /**
     * 保存策略
     */
    ScoringStrategy save(ScoringStrategy strategy);

    /**
     * 根据ID查询
     */
    Optional<ScoringStrategy> findById(Long id);

    /**
     * 根据代码查询
     */
    Optional<ScoringStrategy> findByCode(String code);

    /**
     * 查询所有启用的策略
     */
    List<ScoringStrategy> findAllEnabled();

    /**
     * 按分类查询
     */
    List<ScoringStrategy> findByCategory(StrategyCategory category);

    /**
     * 查询所有（包括禁用的）
     */
    List<ScoringStrategy> findAll();

    /**
     * 删除策略（仅自定义）
     */
    void delete(Long id);

    /**
     * 检查代码是否存在
     */
    boolean existsByCode(String code);
}
