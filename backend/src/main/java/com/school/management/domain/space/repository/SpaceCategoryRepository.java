package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.SpaceCategory;
import com.school.management.domain.space.model.valueobject.SpaceLevel;

import java.util.List;
import java.util.Optional;

/**
 * 空间分类仓储接口
 */
public interface SpaceCategoryRepository {

    /**
     * 保存分类
     */
    SpaceCategory save(SpaceCategory category);

    /**
     * 根据ID查询
     */
    Optional<SpaceCategory> findById(Long id);

    /**
     * 根据编码查询
     */
    Optional<SpaceCategory> findByCode(String categoryCode);

    /**
     * 查询所有分类
     */
    List<SpaceCategory> findAll();

    /**
     * 根据适用层级查询
     */
    List<SpaceCategory> findByLevel(SpaceLevel level);

    /**
     * 查询所有启用的分类
     */
    List<SpaceCategory> findAllEnabled();

    /**
     * 根据层级查询启用的分类
     */
    List<SpaceCategory> findEnabledByLevel(SpaceLevel level);

    /**
     * 删除分类（逻辑删除）
     */
    void deleteById(Long id);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String categoryCode);

    /**
     * 检查编码是否存在（排除指定ID）
     */
    boolean existsByCodeExcludeId(String categoryCode, Long excludeId);
}
