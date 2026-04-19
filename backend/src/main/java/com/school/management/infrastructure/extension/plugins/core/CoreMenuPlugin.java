package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.MenuContributionPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.MenuContributionPlugin.MenuItemDef.of;

/**
 * 通用核心菜单 — 任何部署都需要的基础菜单.
 *
 * 包含:首页 / 消息与事件 / 关系管理 / 系统设置.
 * 行业特有菜单(教务/学籍等)由 EducationMenuPlugin 等声明.
 */
@Component
public class CoreMenuPlugin implements MenuContributionPlugin {

    @Override public String getDomainCode() { return "core"; }

    @Override
    public List<MenuItemDef> getMenus() {
        return List.of(
            // ─── 首页 ───
            of("/dashboard", "首页", "home", 1),

            // ─── 消息与事件 ───
            of("/messaging", "消息与事件", "bell", 10).children(List.of(
                of("/messaging/list", "我的消息", "inbox", 1),
                of("/messaging/config", "消息配置", "settings-2", 2)
                    .requiredPermissions(List.of("system:admin"))
            )),

            // ─── 关系管理 ───
            of("/access", "权限管理", "shield", 6).children(List.of(
                of("/access/relations", "关系管理", "link-2", 1)
                    .requiredPermissions(List.of("system:admin")),
                of("/access/relation-types", "关系字典", "book", 2)
                    .requiredPermissions(List.of("system:admin"))
            )),

            // ─── 系统设置 ───
            of("/system", "系统管理", "settings", 90).children(List.of(
                of("/system/users", "用户管理", "users", 1)
                    .requiredPermissions(List.of("system:user:view")),
                of("/system/roles", "角色管理", "user-cog", 2)
                    .requiredPermissions(List.of("system:role:view")),
                of("/system/permissions", "权限管理", "lock", 3)
                    .requiredPermissions(List.of("system:permission:view")),
                of("/system/entity-types", "类型配置", "layout-grid", 4)
                    .requiredPermissions(List.of("system:config:view")),
                of("/system/plugins", "插件平台", "package", 5)
                    .requiredPermissions(List.of("system:config:view"))
            ))
        );
    }
}
