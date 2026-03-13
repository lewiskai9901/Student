package com.school.management.application.space;

import com.school.management.domain.space.model.entity.SpaceType;
import com.school.management.domain.space.repository.SpaceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 场所类型应用服务（简化版）
 */
@Service
public class SpaceTypeApplicationService {

    private final SpaceTypeRepository spaceTypeRepository;

    public SpaceTypeApplicationService(SpaceTypeRepository spaceTypeRepository) {
        this.spaceTypeRepository = spaceTypeRepository;
    }

    /**
     * 生成类型编码
     */
    private String generateTypeCode() {
        return "ST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 创建场所类型
     */
    @Transactional
    public SpaceType createSpaceType(CreateSpaceTypeCommand command) {
        // 自动生成类型编码
        String typeCode = generateTypeCode();

        // 确保编码唯一
        while (spaceTypeRepository.existsByTypeCode(typeCode)) {
            typeCode = generateTypeCode();
        }

        // 验证父类型存在
        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            spaceTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        SpaceType spaceType = SpaceType.builder()
                .typeCode(typeCode)
                .typeName(command.getTypeName())
                .parentTypeCode(command.getParentTypeCode())
                .levelOrder(command.getLevelOrder())
                .icon(command.getIcon())
                .description(command.getDescription())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 更新场所类型
     */
    @Transactional
    public SpaceType updateSpaceType(Long id, UpdateSpaceTypeCommand command) {
        SpaceType spaceType = spaceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + id));

        spaceType.update(
                command.getTypeName(),
                command.getDescription(),
                command.getIcon()
        );

        if (command.getSortOrder() != null) {
            spaceType.updateSortOrder(command.getSortOrder());
        }

        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 删除场所类型
     */
    @Transactional
    public void deleteSpaceType(Long id) {
        SpaceType spaceType = spaceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + id));

        // 系统预置类型不能删除
        if (spaceType.isSystem()) {
            throw new IllegalArgumentException("系统预置类型不能删除");
        }

        // 检查是否被使用
        if (spaceTypeRepository.isTypeInUse(spaceType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被场所使用，无法删除");
        }

        // 检查是否有子类型
        List<SpaceType> children = spaceTypeRepository.findByParentTypeCode(spaceType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        spaceTypeRepository.deleteById(id);
    }

    /**
     * 启用/禁用场所类型
     */
    @Transactional
    public SpaceType toggleStatus(Long id, boolean enabled) {
        SpaceType spaceType = spaceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + id));

        if (enabled) {
            spaceType.enable();
        } else {
            spaceType.disable();
        }

        return spaceTypeRepository.save(spaceType);
    }

    /**
     * 获取所有场所类型
     */
    public List<SpaceType> getAllSpaceTypes() {
        return spaceTypeRepository.findAll();
    }

    /**
     * 获取所有启用的场所类型
     */
    public List<SpaceType> getEnabledSpaceTypes() {
        return spaceTypeRepository.findAllEnabled();
    }

    /**
     * 获取场所类型树
     */
    public List<SpaceTypeTreeNode> getSpaceTypeTree() {
        List<SpaceType> allTypes = spaceTypeRepository.findAll();

        // 按父类型分组
        Map<String, List<SpaceType>> groupedByParent = allTypes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "" : t.getParentTypeCode()
                ));

        // 构建树
        return buildTree(groupedByParent, "");
    }

    private List<SpaceTypeTreeNode> buildTree(Map<String, List<SpaceType>> groupedByParent, String parentCode) {
        List<SpaceType> children = groupedByParent.getOrDefault(parentCode, new ArrayList<>());
        return children.stream()
                .map(type -> {
                    SpaceTypeTreeNode node = new SpaceTypeTreeNode(type);
                    node.setChildren(buildTree(groupedByParent, type.getTypeCode()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取场所类型
     */
    public SpaceType getSpaceTypeById(Long id) {
        return spaceTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + id));
    }

    /**
     * 根据编码获取场所类型
     */
    public SpaceType getSpaceTypeByCode(String typeCode) {
        return spaceTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("场所类型不存在: " + typeCode));
    }

    // ==================== 命令对象（简化版） ====================

    public static class CreateSpaceTypeCommand {
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String description;
        private Integer sortOrder = 0;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public void setLevelOrder(Integer levelOrder) { this.levelOrder = levelOrder; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateSpaceTypeCommand {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class SpaceTypeTreeNode {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder;
        private String icon;
        private String description;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;
        private List<SpaceTypeTreeNode> children;

        public SpaceTypeTreeNode(SpaceType spaceType) {
            this.id = spaceType.getId();
            this.typeCode = spaceType.getTypeCode();
            this.typeName = spaceType.getTypeName();
            this.parentTypeCode = spaceType.getParentTypeCode();
            this.levelOrder = spaceType.getLevelOrder();
            this.icon = spaceType.getIcon();
            this.description = spaceType.getDescription();
            this.isSystem = spaceType.isSystem();
            this.isEnabled = spaceType.isEnabled();
            this.sortOrder = spaceType.getSortOrder();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public String getTypeCode() { return typeCode; }
        public String getTypeName() { return typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
        public boolean isSystem() { return isSystem; }
        public boolean isEnabled() { return isEnabled; }
        public Integer getSortOrder() { return sortOrder; }
        public List<SpaceTypeTreeNode> getChildren() { return children; }
        public void setChildren(List<SpaceTypeTreeNode> children) { this.children = children; }
    }
}
