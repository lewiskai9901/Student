package com.school.management.infrastructure.extension;

/**
 * 权限定义 (双轨收敛: 从已删的 PermissionProvider SPI 提为顶层 record)。
 *
 * <p>各模块通过 {@link Contribution.PermissionContribution} 声明功能权限, 启动期
 * {@code PermissionRegistrar} UPSERT 到 permissions 表 (CUSTOM 保护管理员自创)。
 *
 * @param code         权限码, 如 "teaching:schedule:view"
 * @param name         显示名
 * @param description  业务描述
 * @param resourceType 资源类型 (1=菜单 2=按钮 3=API)
 * @param parentCode   父权限码(菜单树结构, 可 null)
 */
public record PermissionDef(
    String code,
    String name,
    String description,
    int resourceType,
    String parentCode
) {
    public static PermissionDef of(String code, String name, String description) {
        return new PermissionDef(code, name, description, 2, null);
    }
    public static PermissionDef menu(String code, String name, String description) {
        return new PermissionDef(code, name, description, 1, null);
    }
    public PermissionDef withParent(String parentCode) {
        return new PermissionDef(code, name, description, resourceType, parentCode);
    }
}
