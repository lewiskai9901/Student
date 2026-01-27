package com.school.management.domain.asset.model.entity;

import com.school.management.domain.asset.model.valueobject.CategoryType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产分类实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategory extends Entity<Long> {

    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private CategoryType categoryType;
    private ManagementMode defaultManagementMode;
    private Integer depreciationYears;
    private String unit;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 子分类列表(非持久化字段)
    @Builder.Default
    private List<AssetCategory> children = new ArrayList<>();

    // 资产数量(非持久化字段,用于统计)
    private Integer assetCount;

    /**
     * 创建新分类
     */
    public static AssetCategory create(String categoryCode, String categoryName, CategoryType type) {
        return create(categoryCode, categoryName, type, null);
    }

    /**
     * 创建新分类（指定管理模式）
     */
    public static AssetCategory create(String categoryCode, String categoryName,
                                       CategoryType type, ManagementMode managementMode) {
        AssetCategory category = new AssetCategory();
        category.setCategoryCode(categoryCode);
        category.setCategoryName(categoryName);
        category.setCategoryType(type);
        // 如果未指定管理模式，则使用分类类型的默认管理模式
        category.setDefaultManagementMode(
            managementMode != null ? managementMode : type.getDefaultManagementMode()
        );
        category.setSortOrder(0);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    /**
     * 获取有效的管理模式（如果未设置则从分类类型获取）
     */
    public ManagementMode getEffectiveManagementMode() {
        if (defaultManagementMode != null) {
            return defaultManagementMode;
        }
        return categoryType != null ? categoryType.getDefaultManagementMode() : ManagementMode.SINGLE_ITEM;
    }

    /**
     * 是否支持批量管理
     */
    public boolean supportsBatchManagement() {
        return categoryType != null && categoryType.supportsBatchManagement();
    }

    /**
     * 检查是否为顶级分类
     */
    public boolean isRootCategory() {
        return parentId == null;
    }

    /**
     * 添加子分类
     */
    public void addChild(AssetCategory child) {
        child.setParentId(this.getId());
        this.children.add(child);
    }
}
