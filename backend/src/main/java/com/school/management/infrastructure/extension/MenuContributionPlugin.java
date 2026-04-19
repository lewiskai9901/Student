package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 菜单贡献插件 SPI.
 *
 * 行业插件通过本接口声明前端菜单,启动时由 {@link MenuRegistrar} 扫描并缓存,
 * 前端登录后 `GET /api/menus/my` 拉取当前用户可见的菜单树.
 *
 * 替代前端 `router/index.ts` 硬写教育菜单的旧方案:
 *   - 换行业 = 换插件 = 菜单自动切换
 *   - 菜单项可绑定权限 + feature 开关,前端按用户权限自动过滤
 *
 * 典型实现:
 * <pre>
 *   &#64;Component
 *   public class EducationMenuPlugin implements MenuContributionPlugin {
 *       public List&lt;MenuItemDef&gt; getMenus() {
 *           return List.of(
 *               MenuItemDef.of("/organization", "组织管理", "building-2", 20)
 *                   .children(List.of(
 *                       MenuItemDef.of("/organization/units", "组织架构", "tree", 1)
 *                           .requiredPermissions(List.of("org:view"))
 *                   ))
 *           );
 *       }
 *   }
 * </pre>
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.MenuContribution} 替代. 旧 API 仍被
 *   {@link MenuRegistrar} 扫描, 运行时等价, 现有实现无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface MenuContributionPlugin {

    /** 插件业务域码(用于前端分组/审计) */
    String getDomainCode();

    /** 声明本插件贡献的顶级菜单项列表 */
    List<MenuItemDef> getMenus();

    /**
     * 菜单项定义.
     *
     * @param path                 路由路径
     * @param title                菜单标题
     * @param icon                 图标标识
     * @param order                排序(数值越小越靠前)
     * @param component            Vue 组件路径(null 则作为父菜单)
     * @param requiredPermissions  访问此菜单所需的权限码集合(任一满足即可)
     * @param requiredFeature      访问所需的用户类型 feature 开关(如 'isLearner')
     * @param children             子菜单
     */
    record MenuItemDef(
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
}
