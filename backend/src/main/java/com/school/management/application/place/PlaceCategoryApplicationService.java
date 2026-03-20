package com.school.management.application.place;

import com.school.management.application.place.command.CreatePlaceCategoryCommand;
import com.school.management.application.place.command.UpdatePlaceCategoryCommand;
import com.school.management.application.place.query.PlaceCategoryDTO;
import com.school.management.domain.place.model.entity.PlaceCategory;
import com.school.management.domain.place.model.valueobject.PlaceLevel;
import com.school.management.domain.place.repository.PlaceCategoryRepository;
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
public class PlaceCategoryApplicationService {

    private final PlaceCategoryRepository categoryRepository;

    /**
     * 创建空间分类
     */
    @Transactional
    public PlaceCategoryDTO createCategory(CreatePlaceCategoryCommand command, Long operatorId) {
        // 验证编码唯一性
        if (categoryRepository.existsByCode(command.getCategoryCode())) {
            throw new BusinessException("分类编码已存在: " + command.getCategoryCode());
        }

        PlaceLevel level = PlaceLevel.valueOf(command.getApplyToLevel());
        PlaceCategory category;

        if (level == PlaceLevel.BUILDING) {
            category = PlaceCategory.createBuildingCategory(
                command.getCategoryCode(),
                command.getCategoryName(),
                command.getDescription()
            );
        } else {
            category = PlaceCategory.createRoomCategory(
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

        log.info("Created place category: {} by user {}", category.getCategoryCode(), operatorId);
        return PlaceCategoryDTO.fromDomain(category);
    }

    /**
     * 更新空间分类
     */
    @Transactional
    public PlaceCategoryDTO updateCategory(UpdatePlaceCategoryCommand command, Long operatorId) {
        PlaceCategory category = categoryRepository.findById(command.getId())
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

        log.info("Updated place category: {} by user {}", category.getId(), operatorId);
        return PlaceCategoryDTO.fromDomain(category);
    }

    /**
     * 启用分类
     */
    @Transactional
    public void enableCategory(Long id, Long operatorId) {
        PlaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        category.enable();
        category.setUpdatedBy(operatorId);
        categoryRepository.save(category);

        log.info("Enabled place category: {} by user {}", id, operatorId);
    }

    /**
     * 停用分类
     */
    @Transactional
    public void disableCategory(Long id, Long operatorId) {
        PlaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        if (category.isSystem()) {
            throw new BusinessException("系统预置分类不允许停用");
        }

        category.disable();
        category.setUpdatedBy(operatorId);
        categoryRepository.save(category);

        log.info("Disabled place category: {} by user {}", id, operatorId);
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Long id, Long operatorId) {
        PlaceCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在"));

        if (category.isSystem()) {
            throw new BusinessException("系统预置分类不允许删除");
        }

        // TODO: 检查是否有空间使用此分类

        categoryRepository.deleteById(id);
        log.info("Deleted place category: {} by user {}", id, operatorId);
    }

    /**
     * 获取单个分类
     */
    public PlaceCategoryDTO getCategory(Long id) {
        return categoryRepository.findById(id)
            .map(PlaceCategoryDTO::fromDomain)
            .orElseThrow(() -> new BusinessException("分类不存在"));
    }

    /**
     * 获取所有分类
     */
    public List<PlaceCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(PlaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的分类
     */
    public List<PlaceCategoryDTO> getEnabledCategories() {
        return categoryRepository.findAllEnabled().stream()
            .map(PlaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 按层级获取分类
     */
    public List<PlaceCategoryDTO> getCategoriesByLevel(String level) {
        PlaceLevel placeLevel = PlaceLevel.valueOf(level);
        return categoryRepository.findByLevel(placeLevel).stream()
            .map(PlaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 按层级获取启用的分类
     */
    public List<PlaceCategoryDTO> getEnabledCategoriesByLevel(String level) {
        PlaceLevel placeLevel = PlaceLevel.valueOf(level);
        return categoryRepository.findEnabledByLevel(placeLevel).stream()
            .map(PlaceCategoryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 获取楼栋分类
     */
    public List<PlaceCategoryDTO> getBuildingCategories() {
        return getCategoriesByLevel("BUILDING");
    }

    /**
     * 获取房间分类
     */
    public List<PlaceCategoryDTO> getRoomCategories() {
        return getCategoriesByLevel("ROOM");
    }
}
