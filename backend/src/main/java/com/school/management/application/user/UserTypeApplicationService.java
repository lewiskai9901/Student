package com.school.management.application.user;

import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户类型应用服务
 */
@Service
public class UserTypeApplicationService {

    private final UserTypeRepository userTypeRepository;

    public UserTypeApplicationService(UserTypeRepository userTypeRepository) {
        this.userTypeRepository = userTypeRepository;
    }

    /**
     * 创建用户类型
     */
    @Transactional
    public UserType createUserType(CreateUserTypeCommand command) {
        // 检查编码唯一性
        if (userTypeRepository.existsByTypeCode(command.getTypeCode())) {
            throw new IllegalArgumentException("类型编码已存在: " + command.getTypeCode());
        }

        // 验证父类型存在
        if (command.getParentTypeCode() != null && !command.getParentTypeCode().isEmpty()) {
            userTypeRepository.findByTypeCode(command.getParentTypeCode())
                    .orElseThrow(() -> new IllegalArgumentException("父类型不存在: " + command.getParentTypeCode()));
        }

        UserType userType = UserType.builder()
                .typeCode(command.getTypeCode())
                .typeName(command.getTypeName())
                .parentTypeCode(command.getParentTypeCode())
                .levelOrder(command.getLevelOrder())
                .icon(command.getIcon())
                .color(command.getColor())
                .description(command.getDescription())
                .canLogin(command.isCanLogin())
                .canBeInspector(command.isCanBeInspector())
                .canBeInspected(command.isCanBeInspected())
                .canManageOrg(command.isCanManageOrg())
                .canViewReports(command.isCanViewReports())
                .requiresClass(command.isRequiresClass())
                .requiresDormitory(command.isRequiresDormitory())
                .defaultRoleCodes(command.getDefaultRoleCodes())
                .isSystem(false)
                .isEnabled(true)
                .sortOrder(command.getSortOrder())
                .build();

        return userTypeRepository.save(userType);
    }

    /**
     * 更新用户类型
     */
    @Transactional
    public UserType updateUserType(Long id, UpdateUserTypeCommand command) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));

        userType.update(
                command.getTypeName(),
                command.getDescription(),
                command.getIcon(),
                command.getColor()
        );

        userType.updateFeatures(
                command.isCanLogin(),
                command.isCanBeInspector(),
                command.isCanBeInspected(),
                command.isCanManageOrg(),
                command.isCanViewReports(),
                command.isRequiresClass(),
                command.isRequiresDormitory()
        );

        userType.updateDefaultRoles(command.getDefaultRoleCodes());

        if (command.getSortOrder() != null) {
            userType.updateSortOrder(command.getSortOrder());
        }

        return userTypeRepository.save(userType);
    }

    /**
     * 删除用户类型
     */
    @Transactional
    public void deleteUserType(Long id) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));

        // 系统预置类型不能删除
        if (userType.isSystem()) {
            throw new IllegalArgumentException("系统预置类型不能删除");
        }

        // 检查是否被使用
        if (userTypeRepository.isTypeInUse(userType.getTypeCode())) {
            throw new IllegalArgumentException("该类型已被用户使用，无法删除");
        }

        // 检查是否有子类型
        List<UserType> children = userTypeRepository.findByParentTypeCode(userType.getTypeCode());
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("该类型下存在子类型，无法删除");
        }

        userTypeRepository.deleteById(id);
    }

    /**
     * 启用/禁用用户类型
     */
    @Transactional
    public UserType toggleStatus(Long id, boolean enabled) {
        UserType userType = userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));

        if (enabled) {
            userType.enable();
        } else {
            userType.disable();
        }

        return userTypeRepository.save(userType);
    }

    /**
     * 获取所有用户类型
     */
    public List<UserType> getAllUserTypes() {
        return userTypeRepository.findAll();
    }

    /**
     * 获取所有启用的用户类型
     */
    public List<UserType> getEnabledUserTypes() {
        return userTypeRepository.findAllEnabled();
    }

    /**
     * 获取用户类型树
     */
    public List<UserTypeTreeNode> getUserTypeTree() {
        List<UserType> allTypes = userTypeRepository.findAll();

        // 按父类型分组
        Map<String, List<UserType>> groupedByParent = allTypes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "" : t.getParentTypeCode()
                ));

        // 构建树
        return buildTree(groupedByParent, "");
    }

    private List<UserTypeTreeNode> buildTree(Map<String, List<UserType>> groupedByParent, String parentCode) {
        List<UserType> children = groupedByParent.getOrDefault(parentCode, new ArrayList<>());
        return children.stream()
                .map(type -> {
                    UserTypeTreeNode node = new UserTypeTreeNode(type);
                    node.setChildren(buildTree(groupedByParent, type.getTypeCode()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取可登录的类型
     */
    public List<UserType> getLoginableTypes() {
        return userTypeRepository.findLoginableTypes();
    }

    /**
     * 获取可作为检查员的类型
     */
    public List<UserType> getInspectorTypes() {
        return userTypeRepository.findInspectorTypes();
    }

    /**
     * 获取可被检查的类型
     */
    public List<UserType> getInspectableTypes() {
        return userTypeRepository.findInspectableTypes();
    }

    /**
     * 根据ID获取用户类型
     */
    public UserType getUserTypeById(Long id) {
        return userTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + id));
    }

    /**
     * 根据编码获取用户类型
     */
    public UserType getUserTypeByCode(String typeCode) {
        return userTypeRepository.findByTypeCode(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("用户类型不存在: " + typeCode));
    }

    // ==================== 命令对象 ====================

    public static class CreateUserTypeCommand {
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String color;
        private String description;
        private boolean canLogin = true;
        private boolean canBeInspector;
        private boolean canBeInspected;
        private boolean canManageOrg;
        private boolean canViewReports;
        private boolean requiresClass;
        private boolean requiresDormitory;
        private String defaultRoleCodes;
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
        public boolean isCanLogin() { return canLogin; }
        public void setCanLogin(boolean canLogin) { this.canLogin = canLogin; }
        public boolean isCanBeInspector() { return canBeInspector; }
        public void setCanBeInspector(boolean canBeInspector) { this.canBeInspector = canBeInspector; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanManageOrg() { return canManageOrg; }
        public void setCanManageOrg(boolean canManageOrg) { this.canManageOrg = canManageOrg; }
        public boolean isCanViewReports() { return canViewReports; }
        public void setCanViewReports(boolean canViewReports) { this.canViewReports = canViewReports; }
        public boolean isRequiresClass() { return requiresClass; }
        public void setRequiresClass(boolean requiresClass) { this.requiresClass = requiresClass; }
        public boolean isRequiresDormitory() { return requiresDormitory; }
        public void setRequiresDormitory(boolean requiresDormitory) { this.requiresDormitory = requiresDormitory; }
        public String getDefaultRoleCodes() { return defaultRoleCodes; }
        public void setDefaultRoleCodes(String defaultRoleCodes) { this.defaultRoleCodes = defaultRoleCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateUserTypeCommand {
        private String typeName;
        private String icon;
        private String color;
        private String description;
        private boolean canLogin;
        private boolean canBeInspector;
        private boolean canBeInspected;
        private boolean canManageOrg;
        private boolean canViewReports;
        private boolean requiresClass;
        private boolean requiresDormitory;
        private String defaultRoleCodes;
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
        public boolean isCanLogin() { return canLogin; }
        public void setCanLogin(boolean canLogin) { this.canLogin = canLogin; }
        public boolean isCanBeInspector() { return canBeInspector; }
        public void setCanBeInspector(boolean canBeInspector) { this.canBeInspector = canBeInspector; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public void setCanBeInspected(boolean canBeInspected) { this.canBeInspected = canBeInspected; }
        public boolean isCanManageOrg() { return canManageOrg; }
        public void setCanManageOrg(boolean canManageOrg) { this.canManageOrg = canManageOrg; }
        public boolean isCanViewReports() { return canViewReports; }
        public void setCanViewReports(boolean canViewReports) { this.canViewReports = canViewReports; }
        public boolean isRequiresClass() { return requiresClass; }
        public void setRequiresClass(boolean requiresClass) { this.requiresClass = requiresClass; }
        public boolean isRequiresDormitory() { return requiresDormitory; }
        public void setRequiresDormitory(boolean requiresDormitory) { this.requiresDormitory = requiresDormitory; }
        public String getDefaultRoleCodes() { return defaultRoleCodes; }
        public void setDefaultRoleCodes(String defaultRoleCodes) { this.defaultRoleCodes = defaultRoleCodes; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UserTypeTreeNode {
        private Long id;
        private String typeCode;
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder;
        private String icon;
        private String color;
        private String description;
        private boolean canLogin;
        private boolean canBeInspector;
        private boolean canBeInspected;
        private boolean canManageOrg;
        private boolean canViewReports;
        private boolean requiresClass;
        private boolean requiresDormitory;
        private String defaultRoleCodes;
        private boolean isSystem;
        private boolean isEnabled;
        private Integer sortOrder;
        private List<UserTypeTreeNode> children;

        public UserTypeTreeNode(UserType userType) {
            this.id = userType.getId();
            this.typeCode = userType.getTypeCode();
            this.typeName = userType.getTypeName();
            this.parentTypeCode = userType.getParentTypeCode();
            this.levelOrder = userType.getLevelOrder();
            this.icon = userType.getIcon();
            this.color = userType.getColor();
            this.description = userType.getDescription();
            this.canLogin = userType.isCanLogin();
            this.canBeInspector = userType.isCanBeInspector();
            this.canBeInspected = userType.isCanBeInspected();
            this.canManageOrg = userType.isCanManageOrg();
            this.canViewReports = userType.isCanViewReports();
            this.requiresClass = userType.isRequiresClass();
            this.requiresDormitory = userType.isRequiresDormitory();
            this.defaultRoleCodes = userType.getDefaultRoleCodes();
            this.isSystem = userType.isSystem();
            this.isEnabled = userType.isEnabled();
            this.sortOrder = userType.getSortOrder();
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
        public boolean isCanLogin() { return canLogin; }
        public boolean isCanBeInspector() { return canBeInspector; }
        public boolean isCanBeInspected() { return canBeInspected; }
        public boolean isCanManageOrg() { return canManageOrg; }
        public boolean isCanViewReports() { return canViewReports; }
        public boolean isRequiresClass() { return requiresClass; }
        public boolean isRequiresDormitory() { return requiresDormitory; }
        public String getDefaultRoleCodes() { return defaultRoleCodes; }
        public boolean isSystem() { return isSystem; }
        public boolean isEnabled() { return isEnabled; }
        public Integer getSortOrder() { return sortOrder; }
        public List<UserTypeTreeNode> getChildren() { return children; }
        public void setChildren(List<UserTypeTreeNode> children) { this.children = children; }
    }
}
