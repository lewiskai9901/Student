package com.school.management.infrastructure.extension;

import java.util.Set;

/**
 * 预置角色定义 (Phase 双轨收敛: 从已删的 RolePresetPlugin SPI 提为顶层 record)。
 *
 * <p>行业通过 {@link Contribution.RoleContribution} 声明开箱即用的角色 (如"班主任")。
 * 启动期 {@code RolePresetRegistrar} UPSERT 到 roles 表 (CUSTOM 保护管理员自创)。
 * 权限关联走 {@link Contribution.RolePermissionBindingContribution}, 与角色声明解耦。
 *
 * @param roleCode         唯一码, 如 "CLASS_TEACHER"
 * @param roleName         显示名 "班主任"
 * @param roleType         类型 "SYSTEM" / "CUSTOM" / "PRESET"
 * @param description      业务描述
 * @param level            层级 (数字越小权限越大)
 * @param permissionCodes  关联权限码 (历史字段, 实际授权走 RolePermissionBindingContribution)
 */
public record RolePresetDef(
    String roleCode,
    String roleName,
    String roleType,
    String description,
    int level,
    Set<String> permissionCodes
) {
    public static RolePresetDef of(String code, String name, String desc, int level) {
        return new RolePresetDef(code, name, "PRESET", desc, level, Set.of());
    }
    public RolePresetDef withPermissions(Set<String> perms) {
        return new RolePresetDef(roleCode, roleName, roleType, description, level, perms);
    }
}
