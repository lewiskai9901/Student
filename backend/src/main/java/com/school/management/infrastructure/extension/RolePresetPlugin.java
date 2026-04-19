package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Set;

/**
 * 预置角色插件 SPI.
 *
 * 行业插件通过本接口声明开箱即用的角色(如教育的"班主任"/医疗的"主治医师"),
 * 避免在 SQL migration 里硬编码. 启动时由 {@link RolePresetRegistrar} UPSERT 到 roles 表,
 * 并打 is_plugin_preset=1 + plugin_class 标签.
 *
 * 管理员在 UI 可以:
 *   - 启用/禁用(改 status)
 *   - 修改 role_name/role_desc(不建议但允许)
 *   - 但不能删除 plugin 声明的角色
 *
 * 权限关联留给 {@link PermissionProvider} + 单独的角色-权限配置,避免职责耦合.
 */
public interface RolePresetPlugin {

    /** 返回本插件声明的预置角色列表 */
    List<RolePresetDef> getPresets();

    /**
     * 预置角色定义.
     *
     * @param roleCode         唯一码,如 "CLASS_TEACHER"
     * @param roleName         显示名 "班主任"
     * @param roleType         类型,如 "SYSTEM" / "CUSTOM" / "PRESET"
     * @param description      业务描述
     * @param level            层级 (数字越小权限越大,0=系统,10=管理员,20=老师,30=学生...)
     * @param permissionCodes  关联的权限码(将来由 PermissionProvider 保证存在)
     */
    record RolePresetDef(
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
}
