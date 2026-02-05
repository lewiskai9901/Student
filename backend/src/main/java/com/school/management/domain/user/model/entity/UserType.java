package com.school.management.domain.user.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户类型领域实体
 * 定义用户的类型和权限特性配置
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserType implements Entity<Long> {

    private Long id;

    /**
     * 类型编码（唯一标识）
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 父类型编码
     */
    private String parentTypeCode;

    /**
     * 层级顺序
     */
    private Integer levelOrder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    // ========== 用户特性 ==========

    /**
     * 是否可登录系统
     */
    private boolean canLogin;

    /**
     * 是否可作为检查员
     */
    private boolean canBeInspector;

    /**
     * 是否可被检查
     */
    private boolean canBeInspected;

    /**
     * 是否可管理组织
     */
    private boolean canManageOrg;

    /**
     * 是否可查看报表
     */
    private boolean canViewReports;

    /**
     * 是否需要班级归属
     */
    private boolean requiresClass;

    /**
     * 是否需要宿舍归属
     */
    private boolean requiresDormitory;

    // ========== 默认权限配置 ==========

    /**
     * 默认角色编码（逗号分隔）
     */
    private String defaultRoleCodes;

    // ========== 系统字段 ==========

    /**
     * 是否系统预置
     */
    private boolean isSystem;

    /**
     * 是否启用
     */
    private boolean isEnabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 更新基本信息
     */
    public void update(String typeName, String description, String icon, String color) {
        this.typeName = typeName;
        this.description = description;
        this.icon = icon;
        this.color = color;
    }

    /**
     * 更新用户特性
     */
    public void updateFeatures(boolean canLogin, boolean canBeInspector, boolean canBeInspected,
                               boolean canManageOrg, boolean canViewReports,
                               boolean requiresClass, boolean requiresDormitory) {
        this.canLogin = canLogin;
        this.canBeInspector = canBeInspector;
        this.canBeInspected = canBeInspected;
        this.canManageOrg = canManageOrg;
        this.canViewReports = canViewReports;
        this.requiresClass = requiresClass;
        this.requiresDormitory = requiresDormitory;
    }

    /**
     * 更新默认角色
     */
    public void updateDefaultRoles(String defaultRoleCodes) {
        this.defaultRoleCodes = defaultRoleCodes;
    }

    /**
     * 更新排序号
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 启用
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 是否顶级类型
     */
    public boolean isTopLevel() {
        return parentTypeCode == null || parentTypeCode.isEmpty();
    }

    /**
     * 是否管理员类型
     */
    public boolean isAdminType() {
        return "ADMIN".equals(typeCode) ||
               (parentTypeCode != null && parentTypeCode.equals("ADMIN"));
    }

    /**
     * 是否教职工类型
     */
    public boolean isTeacherType() {
        return "TEACHER".equals(typeCode) ||
               (parentTypeCode != null && parentTypeCode.equals("TEACHER"));
    }

    /**
     * 是否学生类型
     */
    public boolean isStudentType() {
        return "STUDENT".equals(typeCode) ||
               (parentTypeCode != null && parentTypeCode.equals("STUDENT"));
    }

    /**
     * 获取默认角色列表
     */
    public String[] getDefaultRoleCodeArray() {
        if (defaultRoleCodes == null || defaultRoleCodes.isEmpty()) {
            return new String[0];
        }
        return defaultRoleCodes.split(",");
    }
}
