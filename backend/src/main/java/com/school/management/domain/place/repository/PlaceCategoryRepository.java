package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.PlaceCategory;
import com.school.management.domain.place.model.valueobject.PlaceLevel;

import java.util.List;
import java.util.Optional;

/**
 * 空间分类仓储接口
 */
public interface PlaceCategoryRepository {

    /**
     * 保存分类
     */
    PlaceCategory save(PlaceCategory category);

    /**
     * 根据ID查询
     */
    Optional<PlaceCategory> findById(Long id);

    /**
     * 根据编码查询
     */
    Optional<PlaceCategory> findByCode(String categoryCode);

    /**
     * 查询所有分类
     */
    List<PlaceCategory> findAll();

    /**
     * 根据适用层级查询
     */
    List<PlaceCategory> findByLevel(PlaceLevel level);

    /**
     * 查询所有启用的分类
     */
    List<PlaceCategory> findAllEnabled();

    /**
     * 根据层级查询启用的分类
     */
    List<PlaceCategory> findEnabledByLevel(PlaceLevel level);

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
