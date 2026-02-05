package com.school.management.application.organization;

import com.school.management.domain.organization.model.entity.OrgUnitTypeEntity;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织类型应用服务
 */
@Service
public class OrgUnitTypeApplicationService {

    private final OrgUnitTypeRepository orgUnitTypeRepository;

    public OrgUnitTypeApplicationService(OrgUnitTypeRepository orgUnitTypeRepository) {
        this.orgUnitTypeRepository = orgUnitTypeRepository;
    }

    /**
     * 创建组织类型
     */
    @Transactional
    public OrgUnitTypeEntity createOrgUnitType(CreateOrgUnitTypeCommand command) {
        // 检查编码唯一性
        if (orgUnitTypeRepository.existsByTypeCode(command.getTypeCode())) {
            throw new IllegalArgumentException("类型编码已存在: " + command.getTypeCode());
        }

        // 验证父类型存在
        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            orgUnitTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        OrgUnitTypeEntity orgUnitType = OrgUnitTypeEntity.builder()
                .typeCode(command.getTypeCode())
                .typeName(command.getTypeName())
                .parentTypeCode(command.getParentTypeCode())
                .levelOrder(command.getLevelOrder())
                .icon(command.getIcon())
                .color(command.getColor())
                .description(command.getDescription())
                .isAcademic(command.isAcademic())
                .canBeInspected(command.isCanBeInspected())
                .canHaveChildren(command.isCanHaveChildren())
                .maxDepth(command.getMaxDepth())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        return orgUnitTypeRepository.save(orgUnitType);
    }

    /**
     * 更新组织类型
     */
    @Transactional
    public OrgUnitTypeEntity updateOrgUnitType(Long id, UpdateOrgUnitTypeCommand command) {
        OrgUnitTypeEntity orgUnitType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        orgUnitType.update(
                command.getTypeName(),
                command.getDescription(),
                command.getIcon(),
                command.getColor()
        );

        orgUnitType.updateFeatures(
                command.isAcademic(),
                command.isCanBeInspected(),
                command.isCanHaveChildren(),
                command.getMaxDepth()
        );

        if (command.getSortOrder() != null) {
            orgUnitType.updateSortOrder(command.getSortOrder());
        }

        return orgUnitTypeRepository.save(orgUnitType);
    }

    /**
     * 删除组织类型
     */
    @Transactional
    public void deleteOrgUnitType(Long id) {
        OrgUnitTypeEntity orgUnitType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        // 系统预置类型不能删除
        if (orgUnitType.isSystem()) {
            throw new IllegalArgumentException("系统预置类型不能删除");
        }

        // 检查是否被使用
        if (orgUnitTypeRepository.isTypeInUse(orgUnitType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被组织单元使用，无法删除");
        }

        // 检查是否有子类型
        List<OrgUnitTypeEntity> children = orgUnitTypeRepository.findByParentTypeCode(orgUnitType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        orgUnitTypeRepository.deleteById(id);
    }

    /**
     * 启用/禁用组织类型
     */
    @Transactional
    public OrgUnitTypeEntity toggleStatus(Long id, boolean enabled) {
        OrgUnitTypeEntity orgUnitType = orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        if (enabled) {
            orgUnitType.enable();
        } else {
            orgUnitType.disable();
        }

        return orgUnitTypeRepository.save(orgUnitType);
    }

    /**
     * 获取所有组织类型
     */
    public List<OrgUnitTypeEntity> getAllOrgUnitTypes() {
        return orgUnitTypeRepository.findAll();
    }

    /**
     * 获取所有启用的组织类型
     */
    public List<OrgUnitTypeEntity> getEnabledOrgUnitTypes() {
        return orgUnitTypeRepository.findAllEnabled();
    }

    /**
     * 获取组织类型树
     */
    public List<OrgUnitTypeTreeNode> getOrgUnitTypeTree() {
        List<OrgUnitTypeEntity> allTypes = orgUnitTypeRepository.findAll();

        // 按父类型分组
        Map<String, List<OrgUnitTypeEntity>> groupedByParent = allTypes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "" : t.getParentTypeCode()
                ));

        // 构建树
        return buildTree(groupedByParent, "");
    }

    private List<OrgUnitTypeTreeNode> buildTree(Map<String, List<OrgUnitTypeEntity>> groupedByParent, String parentCode) {
        List<OrgUnitTypeEntity> children = groupedByParent.getOrDefault(parentCode, new ArrayList<>());
        return children.stream()
                .map(type -> {
                    OrgUnitTypeTreeNode node = new OrgUnitTypeTreeNode(type);
                    node.setChildren(buildTree(groupedByParent, type.getTypeCode()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取教学单位类型
     */
    public List<OrgUnitTypeEntity> getAcademicTypes() {
        return orgUnitTypeRepository.findAcademicTypes();
    }

    /**
     * 获取职能部门类型
     */
    public List<OrgUnitTypeEntity> getFunctionalTypes() {
        return orgUnitTypeRepository.findFunctionalTypes();
    }

    /**
     * 获取可检查的类型
     */
    public List<OrgUnitTypeEntity> getInspectableTypes() {
        return orgUnitTypeRepository.findInspectableTypes();
    }

    /**
     * 根据ID获取组织类型
     */
    public OrgUnitTypeEntity getOrgUnitTypeById(Long id) {
        return orgUnitTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));
    }

    /**
     * 根据编码获取组织类型
     */
    public OrgUnitTypeEntity getOrgUnitTypeByCode(String typeCode) {
        return orgUnitTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + typeCode));
    }

    // ==================== 命令对象 ====================

    public static class CreateOrgUnitTypeCommand {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic = true;
        private boolean canBeInspected = true;
        private boolean canHaveChildren = true;
        private Integer maxDepth;
        private Integer sortOrder = 0;

        // Getters and Setters
        public String getTypeCode() { return typeCode; }
        public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public void setLevelOrder(Integer levelOrder) { this.levelOrder = levelOrder; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isAcademic() { return isAcademic; }
        public void setAcademic(boolean academic) { isAcademic = academic; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveChildren() { return canHaveChildren; }
        public void setCanHaveChildren(boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgUnitTypeCommand {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic;
        private boolean canBeInspected;
        private boolean canHaveChildren;
        private Integer maxDepth;
        private Integer sortOrder;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isAcademic() { return isAcademic; }
        public void setAcademic(boolean academic) { isAcademic = academic; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveChildren() { return canHaveChildren; }
        public void setCanHaveChildren(boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; }
        public Integer getMaxDepth() { return maxDepth; }
        public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class OrgUnitTypeTreeNode {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder;
        private String icon;
        private String color;
        private String description;
        private boolean isAcademic;
        private boolean canBeInspected;
        private boolean canHaveChildren;
        private Integer maxDepth;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;
        private List<OrgUnitTypeTreeNode> children;

        public OrgUnitTypeTreeNode(OrgUnitTypeEntity entity) {
            this.id = entity.getId();
            this.typeCode = entity.getTypeCode();
            this.typeName = entity.getTypeName();
            this.parentTypeCode = entity.getParentTypeCode();
            this.levelOrder = entity.getLevelOrder();
            this.icon = entity.getIcon();
            this.color = entity.getColor();
            this.description = entity.getDescription();
            this.isAcademic = entity.isAcademic();
            this.canBeInspected = entity.isCanBeInspected();
            this.canHaveChildren = entity.isCanHaveChildren();
            this.maxDepth = entity.getMaxDepth();
            this.isSystem = entity.isSystem();
            this.isEnabled = entity.isEnabled();
            this.sortOrder = entity.getSortOrder();
        }

        // Getters and Setters
        public Long getId() { return id; }
        public String getTypeCode() { return typeCode; }
        public String getTypeName() { return typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
        public String getDescription() { return description; }
        public boolean isAcademic() { return isAcademic; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public boolean isCanHaveChildren() { return canHaveChildren; }
        public Integer getMaxDepth() { return maxDepth; }
        public boolean isSystem() { return isSystem; }
        public boolean isEnabled() { return isEnabled; }
        public Integer getSortOrder() { return sortOrder; }
        public List<OrgUnitTypeTreeNode> getChildren() { return children; }
        public void setChildren(List<OrgUnitTypeTreeNode> children) { this.children = children; }
    }
}
