package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 权限声明 SPI.
 *
 * 每个业务模块通过本接口声明自己需要的功能权限.启动时由 {@link PermissionRegistrar}
 * UPSERT 到 permissions 表,打 plugin_class + industry 标签.
 *
 * 目标:
 *  - 消除 {@code PermissionConstants} 114 个手工常量
 *  - 插件可自带权限,卸载时整包下线
 *  - 新模块加权限只改一个 provider 文件,不用手写 SQL migration
 *
 * 权限码格式建议: {@code module:resource:action}  (如 "system:user:view" / "teaching:schedule:publish")
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.PermissionContribution} 替代. 旧 API 仍被
 *   {@link PermissionRegistrar} 扫描, 运行时等价, 现有实现无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface PermissionProvider {

    /** 模块码(用于分组展示,如 "system" / "education.teaching") */
    String getModuleCode();

    /** 模块显示名 */
    String getModuleName();

    /** 声明本模块所需的所有权限 */
    List<PermissionDef> getPermissions();

    /**
     * 权限定义.
     *
     * @param code        权限码,如 "teaching:schedule:view"
     * @param name        显示名
     * @param description 业务描述
     * @param resourceType 资源类型 (1=菜单 2=按钮 3=API)
     * @param parentCode  父权限码(用于菜单树结构,可为 null)
     */
    record PermissionDef(
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
}
