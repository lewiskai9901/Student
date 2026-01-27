package com.school.management.application.asset;

import com.school.management.application.asset.command.CreateCategoryCommand;
import com.school.management.application.asset.query.AssetCategoryDTO;
import com.school.management.domain.asset.model.entity.AssetCategory;
import com.school.management.domain.asset.model.valueobject.CategoryType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.asset.repository.AssetCategoryRepository;
import com.school.management.domain.asset.repository.AssetRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产分类服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCategoryService {

    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    /**
     * 创建分类
     */
    @Transactional
    public Long createCategory(CreateCategoryCommand command) {
        // 检查编码是否重复
        if (categoryRepository.existsByCategoryCode(command.getCategoryCode())) {
            throw new BusinessException("分类编码已存在: " + command.getCategoryCode());
        }

        // 检查父分类是否存在
        if (command.getParentId() != null) {
            categoryRepository.findById(command.getParentId())
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
        }

        CategoryType categoryType = command.getCategoryType() != null ?
                CategoryType.fromCode(command.getCategoryType()) : CategoryType.FIXED_ASSET;

        ManagementMode managementMode = command.getDefaultManagementMode() != null ?
                ManagementMode.fromCode(command.getDefaultManagementMode()) : null;

        AssetCategory category = AssetCategory.create(
                command.getCategoryCode(),
                command.getCategoryName(),
                categoryType,
                managementMode
        );
        category.setParentId(command.getParentId());
        category.setDepreciationYears(command.getDepreciationYears());
        category.setUnit(command.getUnit());
        category.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        category.setRemark(command.getRemark());

        categoryRepository.save(category);

        log.info("Created asset category: {} - {}", category.getCategoryCode(), category.getCategoryName());
        return category.getId();
    }

    /**
     * 更新分类
     */
    @Transactional
    public void updateCategory(Long id, CreateCategoryCommand command) {
        AssetCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));

        // 如果编码变更,检查是否重复
        if (!category.getCategoryCode().equals(command.getCategoryCode())) {
            if (categoryRepository.existsByCategoryCode(command.getCategoryCode())) {
                throw new BusinessException("分类编码已存在: " + command.getCategoryCode());
            }
            category.setCategoryCode(command.getCategoryCode());
        }

        category.setCategoryName(command.getCategoryName());
        if (command.getCategoryType() != null) {
            category.setCategoryType(CategoryType.fromCode(command.getCategoryType()));
        }
        if (command.getDefaultManagementMode() != null) {
            category.setDefaultManagementMode(ManagementMode.fromCode(command.getDefaultManagementMode()));
        }
        category.setDepreciationYears(command.getDepreciationYears());
        category.setUnit(command.getUnit());
        if (command.getSortOrder() != null) {
            category.setSortOrder(command.getSortOrder());
        }
        category.setRemark(command.getRemark());

        categoryRepository.save(category);

        log.info("Updated asset category: {}", id);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id) {
        AssetCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在"));

        // 检查是否有子分类
        List<AssetCategory> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }

        // 检查是否有关联资产
        int assetCount = assetRepository.countByCategoryId(id);
        if (assetCount > 0) {
            throw new BusinessException("该分类下存在资产，无法删除");
        }

        categoryRepository.delete(id);
        log.info("Deleted asset category: {}", id);
    }

    /**
     * 获取分类详情
     */
    public AssetCategoryDTO getCategory(Long id) {
        return categoryRepository.findById(id)
                .map(this::toCategoryDTO)
                .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    /**
     * 获取分类树(带资产数量)
     */
    public List<AssetCategoryDTO> getCategoryTree() {
        List<AssetCategory> tree = categoryRepository.findCategoryTreeWithCount();
        return tree.stream()
                .map(this::toCategoryTreeDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有分类(平铺)
     */
    public List<AssetCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryDTO)
                .collect(Collectors.toList());
    }

    // ============ 转换方法 ============

    private AssetCategoryDTO toCategoryDTO(AssetCategory category) {
        ManagementMode effectiveMode = category.getEffectiveManagementMode();
        return AssetCategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryCode(category.getCategoryCode())
                .categoryName(category.getCategoryName())
                .categoryType(category.getCategoryType() != null ? category.getCategoryType().getCode() : null)
                .categoryTypeDesc(category.getCategoryType() != null ? category.getCategoryType().getDescription() : null)
                .defaultManagementMode(effectiveMode != null ? effectiveMode.getCode() : null)
                .defaultManagementModeDesc(effectiveMode != null ? effectiveMode.getDescription() : null)
                .supportsBatchManagement(category.supportsBatchManagement())
                .depreciationYears(category.getDepreciationYears())
                .unit(category.getUnit())
                .sortOrder(category.getSortOrder())
                .remark(category.getRemark())
                .assetCount(category.getAssetCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private AssetCategoryDTO toCategoryTreeDTO(AssetCategory category) {
        AssetCategoryDTO dto = toCategoryDTO(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            dto.setChildren(category.getChildren().stream()
                    .map(this::toCategoryTreeDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
