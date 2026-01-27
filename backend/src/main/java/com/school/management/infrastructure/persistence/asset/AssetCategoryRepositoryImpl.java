package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.entity.AssetCategory;
import com.school.management.domain.asset.model.valueobject.CategoryType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.asset.repository.AssetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产分类仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetCategoryRepositoryImpl implements AssetCategoryRepository {

    private final AssetCategoryMapper categoryMapper;

    @Override
    public AssetCategory save(AssetCategory category) {
        AssetCategoryPO po = toPO(category);
        if (po.getId() == null) {
            categoryMapper.insert(po);
        } else {
            categoryMapper.updateById(po);
        }
        category.setId(po.getId());
        return category;
    }

    @Override
    public Optional<AssetCategory> findById(Long id) {
        AssetCategoryPO po = categoryMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AssetCategory> findByCategoryCode(String categoryCode) {
        LambdaQueryWrapper<AssetCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetCategoryPO::getCategoryCode, categoryCode);
        AssetCategoryPO po = categoryMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AssetCategory> findAll() {
        LambdaQueryWrapper<AssetCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(AssetCategoryPO::getSortOrder);
        return categoryMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetCategory> findRootCategories() {
        return categoryMapper.selectRootCategories().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetCategory> findByParentId(Long parentId) {
        return categoryMapper.selectByParentId(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    @Override
    public boolean existsByCategoryCode(String categoryCode) {
        LambdaQueryWrapper<AssetCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetCategoryPO::getCategoryCode, categoryCode);
        return categoryMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<AssetCategory> findCategoryTreeWithCount() {
        List<AssetCategoryPO> allCategories = categoryMapper.selectAllWithAssetCount();
        List<AssetCategory> categories = allCategories.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        // 构建树结构
        return buildTree(categories);
    }

    private List<AssetCategory> buildTree(List<AssetCategory> categories) {
        // 分组:根节点和子节点
        Map<Long, List<AssetCategory>> childrenMap = categories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(AssetCategory::getParentId));

        List<AssetCategory> roots = categories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        // 递归设置子节点
        for (AssetCategory root : roots) {
            setChildren(root, childrenMap);
        }

        return roots;
    }

    private void setChildren(AssetCategory parent, Map<Long, List<AssetCategory>> childrenMap) {
        List<AssetCategory> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
        parent.setChildren(children);
        for (AssetCategory child : children) {
            setChildren(child, childrenMap);
        }
    }

    // ============ 转换方法 ============

    private AssetCategory toDomain(AssetCategoryPO po) {
        if (po == null) return null;

        CategoryType categoryType = po.getCategoryType() != null ?
                CategoryType.fromCode(po.getCategoryType()) : null;

        AssetCategory category = AssetCategory.builder()
                .parentId(po.getParentId())
                .categoryCode(po.getCategoryCode())
                .categoryName(po.getCategoryName())
                .categoryType(categoryType)
                .defaultManagementMode(po.getDefaultManagementMode() != null ?
                        ManagementMode.fromCode(po.getDefaultManagementMode()) :
                        (categoryType != null ? categoryType.getDefaultManagementMode() : ManagementMode.SINGLE_ITEM))
                .depreciationYears(po.getDepreciationYears())
                .unit(po.getUnit())
                .sortOrder(po.getSortOrder())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .assetCount(po.getAssetCount())
                .build();
        category.setId(po.getId());
        return category;
    }

    private AssetCategoryPO toPO(AssetCategory category) {
        return AssetCategoryPO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .categoryType(category.getCategoryType() != null ?
                        category.getCategoryType().getCode() : null)
                .defaultManagementMode(category.getDefaultManagementMode() != null ?
                        category.getDefaultManagementMode().getCode() : null)
                .depreciationYears(category.getDepreciationYears())
                .unit(category.getUnit())
                .sortOrder(category.getSortOrder())
                .remark(category.getRemark())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
