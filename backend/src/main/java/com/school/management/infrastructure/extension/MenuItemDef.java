package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 菜单项定义 (双轨收敛: 从已删的 MenuContributionPlugin SPI 提为顶层 record)。
 *
 * <p>行业通过 {@link Contribution.MenuContribution} 声明前端菜单, 启动期 {@code MenuRegistrar}
 * 合并成菜单树缓存 (不落 DB — 菜单结构代码驱动), 前端 {@code GET /api/menus/my} 按权限过滤拉取。
 *
 * @param path                 路由路径
 * @param title                菜单标题
 * @param icon                 图标标识
 * @param order                排序(数值越小越靠前)
 * @param component            Vue 组件路径(null 则作为父菜单)
 * @param requiredPermissions  访问所需权限码集合(任一满足即可)
 * @param requiredFeature      访问所需用户类型 feature 开关(如 'isLearner')
 * @param children             子菜单
 */
public record MenuItemDef(
    String path,
    String title,
    String icon,
    int order,
    String component,
    List<String> requiredPermissions,
    String requiredFeature,
    List<MenuItemDef> children
) {
    public static MenuItemDef of(String path, String title, String icon, int order) {
        return new MenuItemDef(path, title, icon, order, null, List.of(), null, List.of());
    }
    public MenuItemDef withComponent(String component) {
        return new MenuItemDef(path, title, icon, order, component, requiredPermissions, requiredFeature, children);
    }
    public MenuItemDef requiredPermissions(List<String> perms) {
        return new MenuItemDef(path, title, icon, order, component, perms, requiredFeature, children);
    }
    public MenuItemDef requiredFeature(String feature) {
        return new MenuItemDef(path, title, icon, order, component, requiredPermissions, feature, children);
    }
    public MenuItemDef children(List<MenuItemDef> items) {
        return new MenuItemDef(path, title, icon, order, component, requiredPermissions, requiredFeature, items);
    }
}
