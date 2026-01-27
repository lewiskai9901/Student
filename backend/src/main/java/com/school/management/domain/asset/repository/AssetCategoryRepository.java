package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.entity.AssetCategory;

import java.util.List;
import java.util.Optional;

/**
 * 资产分类仓储接口
 */
public interface AssetCategoryRepository {

    /**
     * 保存分类
     */
    AssetCategory save(AssetCategory category);

    /**
     * 根据ID查找分类
     */
    Optional<AssetCategory> findById(Long id);

    /**
     * 根据分类编码查找
     */
    Optional<AssetCategory> findByCategoryCode(String categoryCode);

    /**
     * 查找所有分类
     */
    List<AssetCategory> findAll();

    /**
     * 查找顶级分类
     */
    List<AssetCategory> findRootCategories();

    /**
     * 查找子分类
     */
    List<AssetCategory> findByParentId(Long parentId);

    /**
     * 删除分类(逻辑删除)
     */
    void delete(Long id);

    /**
     * 检查分类编码是否存在
     */
    boolean existsByCategoryCode(String categoryCode);

    /**
     * 获取分类树(带资产数量统计)
     */
    List<AssetCategory> findCategoryTreeWithCount();
}
