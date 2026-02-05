package com.school.management.application.space;

import com.school.management.application.space.command.CreateSpaceCategoryCommand;
import com.school.management.application.space.command.UpdateSpaceCategoryCommand;
import com.school.management.application.space.query.SpaceCategoryDTO;
import com.school.management.domain.space.model.entity.SpaceCategory;
import com.school.management.domain.space.model.valueobject.SpaceLevel;
import com.school.management.domain.space.repository.SpaceCategoryRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间分类应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceCategoryApplicationService {

    private final SpaceCategoryRepository categoryRepository;

    /**
     * 创建空间分类
     */
    @Transactional
    public SpaceCategoryDTO createCategory(CreateSpaceCategoryCommand command, Long operatorId) {
        // 验证编码唯一性
        if (categoryRepository.existsByCode(command.getCategoryCode())) {
            throw new BusinessException("分类编码已存在: " + command.getCategoryCode());
        }

        SpaceLevel level = SpaceLevel.valueOf(command.getApplyToLevel());
        SpaceCategory category;

        if (level == SpaceLevel.BUILDING) {
            category = SpaceCategory.createBuildingCategory(
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getDescription()
            );
        } else {
            category = SpaceCategory.createRoomCategory(
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getDescription(),
                Boolean.TRUE.equals(command.getHasCapacity()),
                command.getCapacityUnit(),
                command.getDefaultCapacity(),
                Boolean.TRUE.equals(command.getBookable()),
                Boolean.TRUE.equals(command.getAssignable()),
                Boolean.TRUE.equals(command.getOccupiable()),
                Boolean.TRUE.equals(command.getHasGender())
            );
        }

        category.setCreatedBy(operatorId);
        category = categoryRepository.save(category);

        log.info("Created space category: {} by user {}", category.getCategoryCode(), operatorId);
        return SpaceCategoryDTO.fromDomain(category);
    }

    /**
     * 更新空间分类
     */
    @Transactional
    public SpaceCategoryDTO updateCategory(UpdateSpaceCategoryCommand command, Long operatorId) {
        SpaceCategory category = categoryRepository.findById(command.getId())
            .orElseThrow(() -> new BusinessException("分类不存在"));

        if (category.isSystem()) {
            throw new BusinessException("系统预置分类不允许修改");
        }

        category.update(
            command.getCategoryName(),
            command.getDescription(),
            command.getIcon(),
            command.getColor(),
            Boolean.TRUE.equals(command.getHasCapacity()),
            command.getCapacityUnit(),
            command.getDefaultCapacity(),
            Boolean.TRUE.equals(command.getBookable()),
            Boolean.TRUE.equals(command.getAssignable()),
            Boolean.TRUE.equals(command.getOccupiable()),
            Boolean.TRUE.equals(command.getHasGender()),
            command.getSortOrder()
        );
        category.setUpdatedBy(operatorId);

        category = categoryRepository.save(category);

        log.info("Updated space category: {} by user {}", category.getId(), operatorId);
        return SpaceCategoryDTO.fromDomain(category);
    }

    /**
     * 启用分类
     */
    @Transactional
    public void enableCategory(Long id, Long operatorId) {
        SpaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        category.enable();
        category.setUpdatedBy(operatorId);
        categoryRepository.save(category);

        log.info("Enabled space category: {} by user {}", id, operatorId);
    }

    /**
     * 停用分类
     */
    @Transactional
    public void disableCategory(Long id, Long operatorId) {
        SpaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        if (category.isSystem()) {
            throw new BusinessException("系统预置分类不允许停用");
        }

        category.disable();
        category.setUpdatedBy(operatorId);
        categoryRepository.save(category);

        log.info("Disabled space category: {} by user {}", id, operatorId);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id, Long operatorId) {
        SpaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        if (category.isSystem()) {
            throw new BusinessException("系统预置分类不允许删除");
        }

        // TODO: 检查是否有空间使用此分类

        categoryRepository.deleteById(id);
        log.info("Deleted space category: {} by user {}", id, operatorId);
    }

    /**
     * 获取单个分类
     */
    public SpaceCategoryDTO getCategory(Long id) {
        return categoryRepository.findById(id)
            .map(SpaceCategoryDTO::fromDomain)
            .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    /**
     * 获取所有分类
     */
    public List<SpaceCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(SpaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的分类
     */
    public List<SpaceCategoryDTO> getEnabledCategories() {
        return categoryRepository.findAllEnabled().stream()
            .map(SpaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 按层级获取分类
     */
    public List<SpaceCategoryDTO> getCategoriesByLevel(String level) {
        SpaceLevel spaceLevel = SpaceLevel.valueOf(level);
        return categoryRepository.findByLevel(spaceLevel).stream()
            .map(SpaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 按层级获取启用的分类
     */
    public List<SpaceCategoryDTO> getEnabledCategoriesByLevel(String level) {
        SpaceLevel spaceLevel = SpaceLevel.valueOf(level);
        return categoryRepository.findEnabledByLevel(spaceLevel).stream()
            .map(SpaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 获取楼栋分类
     */
    public List<SpaceCategoryDTO> getBuildingCategories() {
        return getCategoriesByLevel("BUILDING");
    }

    /**
     * 获取房间分类
     */
    public List<SpaceCategoryDTO> getRoomCategories() {
        return getCategoriesByLevel("ROOM");
    }
}
