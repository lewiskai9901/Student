package com.school.management.application.organization;

import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.repository.OrgTypeRepository;
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
public class OrgTypeApplicationService {

    private final OrgTypeRepository orgTypeRepository;

    public OrgTypeApplicationService(OrgTypeRepository orgTypeRepository) {
        this.orgTypeRepository = orgTypeRepository;
    }

    /**
     * 创建组织类型
     */
    @Transactional
    public OrgType createOrgType(CreateOrgTypeCommand command) {
        // 检查编码唯一性
        if (orgTypeRepository.existsByTypeCode(command.getTypeCode())) {
            throw new IllegalArgumentException("类型编码已存在: " + command.getTypeCode());
        }

        // 验证父类型存在
        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            orgTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        OrgType orgType = OrgType.builder()
                .typeCode(command.getTypeCode())
                .typeName(command.getTypeName())
                .parentTypeCode(command.getParentTypeCode())
                .levelOrder(command.getLevelOrder())
                .icon(command.getIcon())
                .color(command.getColor())
                .description(command.getDescription())
                .canHaveClasses(command.isCanHaveClasses())
                .canHaveStudents(command.isCanHaveStudents())
                .canBeInspected(command.isCanBeInspected())
                .canHaveLeader(command.isCanHaveLeader())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        return orgTypeRepository.save(orgType);
    }

    /**
     * 更新组织类型
     */
    @Transactional
    public OrgType updateOrgType(Long id, UpdateOrgTypeCommand command) {
        OrgType orgType = orgTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        orgType.update(
                command.getTypeName(),
                command.getDescription(),
                command.getIcon(),
                command.getColor()
        );

        orgType.updateFeatures(
                command.isCanHaveClasses(),
                command.isCanHaveStudents(),
                command.isCanBeInspected(),
                command.isCanHaveLeader()
        );

        if (command.getSortOrder() != null) {
            orgType.updateSortOrder(command.getSortOrder());
        }

        return orgTypeRepository.save(orgType);
    }

    /**
     * 删除组织类型
     */
    @Transactional
    public void deleteOrgType(Long id) {
        OrgType orgType = orgTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        // 系统预置类型不能删除
        if (orgType.isSystem()) {
            throw new IllegalArgumentException("系统预置类型不能删除");
        }

        // 检查是否被使用
        if (orgTypeRepository.isTypeInUse(orgType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被组织单元使用，无法删除");
        }

        // 检查是否有子类型
        List<OrgType> children = orgTypeRepository.findByParentTypeCode(orgType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        orgTypeRepository.deleteById(id);
    }

    /**
     * 启用/禁用组织类型
     */
    @Transactional
    public OrgType toggleStatus(Long id, boolean enabled) {
        OrgType orgType = orgTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));

        if (enabled) {
            orgType.enable();
        } else {
            orgType.disable();
        }

        return orgTypeRepository.save(orgType);
    }

    /**
     * 获取所有组织类型
     */
    public List<OrgType> getAllOrgTypes() {
        return orgTypeRepository.findAll();
    }

    /**
     * 获取所有启用的组织类型
     */
    public List<OrgType> getEnabledOrgTypes() {
        return orgTypeRepository.findAllEnabled();
    }

    /**
     * 获取组织类型树
     */
    public List<OrgTypeTreeNode> getOrgTypeTree() {
        List<OrgType> allTypes = orgTypeRepository.findAll();

        // 按父类型分组
        Map<String, List<OrgType>> groupedByParent = allTypes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "" : t.getParentTypeCode()
                ));

        // 构建树
        return buildTree(groupedByParent, "");
    }

    private List<OrgTypeTreeNode> buildTree(Map<String, List<OrgType>> groupedByParent, String parentCode) {
        List<OrgType> children = groupedByParent.getOrDefault(parentCode, new ArrayList<>());
        return children.stream()
                .map(type -> {
                    OrgTypeTreeNode node = new OrgTypeTreeNode(type);
                    node.setChildren(buildTree(groupedByParent, type.getTypeCode()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取组织类型
     */
    public OrgType getOrgTypeById(Long id) {
        return orgTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + id));
    }

    /**
     * 根据编码获取组织类型
     */
    public OrgType getOrgTypeByCode(String typeCode) {
        return orgTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("组织类型不存在: " + typeCode));
    }

    // ==================== 命令对象 ====================

    public static class CreateOrgTypeCommand {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses;
        private boolean canHaveStudents;
        private boolean canBeInspected = true;
        private boolean canHaveLeader = true;
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
        public boolean isCanHaveClasses() { return canHaveClasses; }
        public void setCanHaveClasses(boolean canHaveClasses) { this.canHaveClasses = canHaveClasses; }
        public boolean isCanHaveStudents() { return canHaveStudents; }
        public void setCanHaveStudents(boolean canHaveStudents) { this.canHaveStudents = canHaveStudents; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveLeader() { return canHaveLeader; }
        public void setCanHaveLeader(boolean canHaveLeader) { this.canHaveLeader = canHaveLeader; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateOrgTypeCommand {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses;
        private boolean canHaveStudents;
        private boolean canBeInspected;
        private boolean canHaveLeader;
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
        public boolean isCanHaveClasses() { return canHaveClasses; }
        public void setCanHaveClasses(boolean canHaveClasses) { this.canHaveClasses = canHaveClasses; }
        public boolean isCanHaveStudents() { return canHaveStudents; }
        public void setCanHaveStudents(boolean canHaveStudents) { this.canHaveStudents = canHaveStudents; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanHaveLeader() { return canHaveLeader; }
        public void setCanHaveLeader(boolean canHaveLeader) { this.canHaveLeader = canHaveLeader; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class OrgTypeTreeNode {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder;
        private String icon;
        private String color;
        private String description;
        private boolean canHaveClasses;
        private boolean canHaveStudents;
        private boolean canBeInspected;
        private boolean canHaveLeader;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;
        private List<OrgTypeTreeNode> children;

        public OrgTypeTreeNode(OrgType orgType) {
            this.id = orgType.getId();
            this.typeCode = orgType.getTypeCode();
            this.typeName = orgType.getTypeName();
            this.parentTypeCode = orgType.getParentTypeCode();
            this.levelOrder = orgType.getLevelOrder();
            this.icon = orgType.getIcon();
            this.color = orgType.getColor();
            this.description = orgType.getDescription();
            this.canHaveClasses = orgType.isCanHaveClasses();
            this.canHaveStudents = orgType.isCanHaveStudents();
            this.canBeInspected = orgType.isCanBeInspected();
            this.canHaveLeader = orgType.isCanHaveLeader();
            this.isSystem = orgType.isSystem();
            this.isEnabled = orgType.isEnabled();
            this.sortOrder = orgType.getSortOrder();
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
        public boolean isCanHaveClasses() { return canHaveClasses; }
        public boolean isCanHaveStudents() { return canHaveStudents; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public boolean isCanHaveLeader() { return canHaveLeader; }
        public boolean isSystem() { return isSystem; }
        public boolean isEnabled() { return isEnabled; }
        public Integer getSortOrder() { return sortOrder; }
        public List<OrgTypeTreeNode> getChildren() { return children; }
        public void setChildren(List<OrgTypeTreeNode> children) { this.children = children; }
    }
}
