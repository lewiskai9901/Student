package com.school.management.application.space;

import com.school.management.domain.space.model.entity.UniversalSpaceType;
import com.school.management.domain.space.repository.UniversalSpaceTypeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用空间类型应用服务
 */
@Service
@RequiredArgsConstructor
public class UniversalSpaceTypeApplicationService {

    private final UniversalSpaceTypeRepository spaceTypeRepository;

    /**
     * 获取所有空间类型
     */
    public List<UniversalSpaceType> getAllSpaceTypes() {
        return spaceTypeRepository.findAll();
    }

    /**
     * 获取所有启用的空间类型
     */
    public List<UniversalSpaceType> getEnabledSpaceTypes() {
        return spaceTypeRepository.findAllEnabled();
    }

    /**
     * 获取所有根类型
     */
    public List<UniversalSpaceType> getRootTypes() {
        return spaceTypeRepository.findAllRootTypes();
    }

    /**
     * 获取允许的子类型
     */
    public List<UniversalSpaceType> getAllowedChildTypes(String parentTypeCode) {
        return spaceTypeRepository.findAllowedChildTypes(parentTypeCode);
    }

    /**
     * 根据ID获取空间类型
     */
    public UniversalSpaceType getSpaceTypeById(Long id) {
        return spaceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("空间类型不存在: " + id));
    }

    /**
     * 根据编码获取空间类型
     */
    public UniversalSpaceType getSpaceTypeByCode(String typeCode) {
        return spaceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("空间类型不存在: " + typeCode));
    }

    /**
     * 创建空间类型
     */
    @Transactional
    public UniversalSpaceType createSpaceType(CreateSpaceTypeCommand command) {
        // 生成类型编码
        String typeCode = generateTypeCode(command.getTypeName());

        // 检查编码是否已存在
        if (spaceTypeRepository.existsByTypeCode(typeCode)) {
            typeCode = typeCode + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }

        UniversalSpaceType spaceType = UniversalSpaceType.builder()
                .typeCode(typeCode)
                .typeName(command.getTypeName())
                .icon(command.getIcon())
                .description(command.getDescription())
                .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
                .isSystem(false)
                .isEnabled(true)
                .isRootType(command.isRootType())
                .allowedChildTypes(command.getAllowedChildTypes() != null ? command.getAllowedChildTypes() : new ArrayList<>())
                .hasCapacity(command.isHasCapacity())
                .bookable(command.isBookable())
                .assignable(command.isAssignable())
                .occupiable(command.isOccupiable())
                .capacityUnit(command.getCapacityUnit())
                .defaultCapacity(command.getDefaultCapacity())
                .build();

        spaceType.validate();
        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 更新空间类型
     */
    @Transactional
    public UniversalSpaceType updateSpaceType(Long id, UpdateSpaceTypeCommand command) {
        UniversalSpaceType spaceType = getSpaceTypeById(id);

        if (spaceType.isSystem()) {
            throw new IllegalStateException("系统预置类型不可修改");
        }

        if (command.getTypeName() != null) {
            spaceType.setTypeName(command.getTypeName());
        }
        if (command.getIcon() != null) {
            spaceType.setIcon(command.getIcon());
        }
        if (command.getDescription() != null) {
            spaceType.setDescription(command.getDescription());
        }
        if (command.getSortOrder() != null) {
            spaceType.setSortOrder(command.getSortOrder());
        }
        if (command.getAllowedChildTypes() != null) {
            spaceType.setAllowedChildTypes(command.getAllowedChildTypes());
        }
        if (command.getHasCapacity() != null) {
            spaceType.setHasCapacity(command.getHasCapacity());
        }
        if (command.getBookable() != null) {
            spaceType.setBookable(command.getBookable());
        }
        if (command.getAssignable() != null) {
            spaceType.setAssignable(command.getAssignable());
        }
        if (command.getOccupiable() != null) {
            spaceType.setOccupiable(command.getOccupiable());
        }
        if (command.getCapacityUnit() != null) {
            spaceType.setCapacityUnit(command.getCapacityUnit());
        }
        if (command.getDefaultCapacity() != null) {
            spaceType.setDefaultCapacity(command.getDefaultCapacity());
        }

        spaceType.validate();
        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 删除空间类型
     */
    @Transactional
    public void deleteSpaceType(Long id) {
        UniversalSpaceType spaceType = getSpaceTypeById(id);

        if (spaceType.isSystem()) {
            throw new IllegalStateException("系统预置类型不可删除");
        }

        if (spaceTypeRepository.isTypeInUse(spaceType.getTypeCode())) {
            throw new IllegalStateException("类型正在使用中，不可删除");
        }

        spaceTypeRepository.deleteById(id);
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public UniversalSpaceType toggleStatus(Long id, boolean enabled) {
        UniversalSpaceType spaceType = getSpaceTypeById(id);

        if (!enabled && spaceTypeRepository.isTypeInUse(spaceType.getTypeCode())) {
            throw new IllegalStateException("类型正在使用中，不可禁用");
        }

        if (enabled) {
            spaceType.enable();
        } else {
            spaceType.disable();
        }

        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 获取空间类型树
     */
    public List<SpaceTypeTreeNode> getSpaceTypeTree() {
        List<UniversalSpaceType> allTypes = spaceTypeRepository.findAllEnabled();
        Map<String, UniversalSpaceType> typeMap = allTypes.stream()
                .collect(Collectors.toMap(UniversalSpaceType::getTypeCode, t -> t));

        // 找出根类型
        List<SpaceTypeTreeNode> roots = new ArrayList<>();
        for (UniversalSpaceType type : allTypes) {
            if (type.isRootType()) {
                SpaceTypeTreeNode node = buildTreeNode(type, typeMap, new HashSet<>());
                roots.add(node);
            }
        }

        return roots;
    }

    private SpaceTypeTreeNode buildTreeNode(UniversalSpaceType type, Map<String, UniversalSpaceType> typeMap, Set<String> visited) {
        if (visited.contains(type.getTypeCode())) {
            // 防止循环引用
            return null;
        }
        visited.add(type.getTypeCode());

        SpaceTypeTreeNode node = new SpaceTypeTreeNode();
        node.setTypeCode(type.getTypeCode());
        node.setTypeName(type.getTypeName());
        node.setIcon(type.getIcon());
        node.setHasCapacity(type.isHasCapacity());
        node.setBookable(type.isBookable());
        node.setAssignable(type.isAssignable());
        node.setOccupiable(type.isOccupiable());
        node.setLeaf(type.isLeafType());

        List<SpaceTypeTreeNode> children = new ArrayList<>();
        if (type.getAllowedChildTypes() != null) {
            for (String childCode : type.getAllowedChildTypes()) {
                UniversalSpaceType childType = typeMap.get(childCode);
                if (childType != null && childType.isEnabled()) {
                    SpaceTypeTreeNode childNode = buildTreeNode(childType, typeMap, new HashSet<>(visited));
                    if (childNode != null) {
                        children.add(childNode);
                    }
                }
            }
        }
        node.setChildren(children);

        return node;
    }

    private String generateTypeCode(String typeName) {
        // 简单的编码生成：取拼音首字母或使用UUID
        return "TYPE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ==================== 命令对象 ====================

    @Data
    public static class CreateSpaceTypeCommand {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;
        private boolean rootType;
        private List<String> allowedChildTypes;
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private String capacityUnit;
        private Integer defaultCapacity;
    }

    @Data
    public static class UpdateSpaceTypeCommand {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;
        private List<String> allowedChildTypes;
        private Boolean hasCapacity;
        private Boolean bookable;
        private Boolean assignable;
        private Boolean occupiable;
        private String capacityUnit;
        private Integer defaultCapacity;
    }

    @Data
    public static class SpaceTypeTreeNode {
        private String typeCode;
        private String typeName;
        private String icon;
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private boolean leaf;
        private List<SpaceTypeTreeNode> children;
    }
}
