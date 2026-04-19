package com.school.management.infrastructure.extension;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 菜单 Registrar — 扫描所有 {@link MenuContributionPlugin},合并成一棵菜单树.
 *
 * <p>特例: 不走 {@link AbstractPluginRegistrar} 基类因为不落 DB (菜单结构代码驱动,
 * DB 存不合理: admin 改菜单可能破坏前端路由约定). 内存缓存由
 * {@link com.school.management.interfaces.rest.system.MenuController} 读出,
 * 提供 {@code /api/menus/my} 查询,按当前用户权限过滤.
 *
 * @Order(700): 在 permission/role 之后 (确保权限码已注册).
 */
@Slf4j
@Component
@Order(700)
@RequiredArgsConstructor
public class MenuRegistrar implements ApplicationRunner {

    private final List<MenuContributionPlugin> plugins;
    private final PluginPackageRegistrar packageRegistrar;

    /** 所有插件贡献的顶级菜单(已合并 + 排序) */
    @Getter
    private volatile List<MenuContributionPlugin.MenuItemDef> allMenus = List.of();

    /** 菜单项 → 来源插件(industry) 映射,用于 UI 展示"此菜单由哪个行业提供" */
    @Getter
    private volatile java.util.Map<String, String> menuIndustryMap = new java.util.HashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        if (plugins.isEmpty()) {
            log.info("[MenuRegistrar] 无菜单贡献插件");
            return;
        }

        List<MenuContributionPlugin.MenuItemDef> merged = new ArrayList<>();
        java.util.Map<String, String> industryMap = new java.util.HashMap<>();
        for (MenuContributionPlugin p : plugins) {
            String industry = packageRegistrar.resolveIndustry(p.getClass());
            for (var menu : p.getMenus()) {
                merged.add(menu);
                industryMap.put(menu.path(), industry == null ? "CORE" : industry);
            }
        }

        // 按 order 排序顶级菜单
        merged.sort(Comparator.comparingInt(MenuContributionPlugin.MenuItemDef::order));
        this.allMenus = List.copyOf(merged);
        this.menuIndustryMap = java.util.Map.copyOf(industryMap);

        log.info("[MenuRegistrar] 加载 {} 个插件,{} 个顶级菜单",
            plugins.size(), allMenus.size());
    }
}
