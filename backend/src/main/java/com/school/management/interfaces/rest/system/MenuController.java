package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.extension.MenuContributionPlugin;
import com.school.management.infrastructure.extension.MenuRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单 API — 替代前端 router/index.ts 里硬写教育菜单.
 *
 *   GET /api/menus/my    → 当前用户可见的菜单树(按权限过滤)
 *   GET /api/menus/all   → 所有注册菜单(admin 视图)
 */
@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")  // Phase 6.7: 菜单查询必登录; /all 额外限 admin
public class MenuController {

    private final MenuRegistrar menuRegistrar;
    private final JdbcTemplate jdbc;

    /** 当前用户可见菜单 — 按权限 + feature 过滤 */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myMenus() {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> userPermissions = loadUserPermissions(userId);
        List<Map<String, Object>> tree = menuRegistrar.getAllMenus().stream()
            .map(m -> filterAndSerialize(m, userPermissions))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return Result.success(tree);
    }

    /** 全量菜单(admin 查看 / 插件管理页用) */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('system:admin') or hasAuthority('system:config:view')")
    public Result<List<Map<String, Object>>> allMenus() {
        List<Map<String, Object>> tree = menuRegistrar.getAllMenus().stream()
            .map(this::serialize)
            .collect(Collectors.toList());
        return Result.success(tree);
    }

    // ─── 内部 ───

    private Set<String> loadUserPermissions(Long userId) {
        if (userId == null) return Set.of();
        try {
            return new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT p.permission_code FROM permissions p " +
                "JOIN role_permissions rp ON rp.permission_id = p.id " +
                "JOIN user_roles ur ON ur.role_id = rp.role_id " +
                "WHERE ur.user_id = ? AND p.deleted = 0 AND p.status = 1",
                String.class, userId));
        } catch (Exception e) {
            return Set.of();
        }
    }

    /**
     * 过滤 + 序列化:
     *  - requiredPermissions 不满足 → 整个菜单项隐藏
     *  - 有子项时,子项若全被过滤则父项也隐藏
     */
    private Map<String, Object> filterAndSerialize(MenuContributionPlugin.MenuItemDef item,
                                                    Set<String> userPerms) {
        // 检查权限
        if (item.requiredPermissions() != null && !item.requiredPermissions().isEmpty()) {
            boolean hasAny = item.requiredPermissions().stream().anyMatch(userPerms::contains);
            // 有 system:admin 则所有菜单都能看
            if (!hasAny && !userPerms.contains("system:admin")) return null;
        }

        List<Map<String, Object>> visibleChildren = List.of();
        if (item.children() != null && !item.children().isEmpty()) {
            visibleChildren = item.children().stream()
                .map(c -> filterAndSerialize(c, userPerms))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            // 无子项可见且父项本身无 path → 隐藏
            if (visibleChildren.isEmpty() && item.component() == null) return null;
        }
        return serialize(item, visibleChildren);
    }

    private Map<String, Object> serialize(MenuContributionPlugin.MenuItemDef item) {
        List<Map<String, Object>> children = item.children() == null ? List.of()
            : item.children().stream().map(this::serialize).collect(Collectors.toList());
        return serialize(item, children);
    }

    private Map<String, Object> serialize(MenuContributionPlugin.MenuItemDef item,
                                            List<Map<String, Object>> children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", item.path());
        m.put("title", item.title());
        m.put("icon", item.icon());
        m.put("order", item.order());
        if (item.component() != null) m.put("component", item.component());
        if (item.requiredPermissions() != null && !item.requiredPermissions().isEmpty()) {
            m.put("requiredPermissions", item.requiredPermissions());
        }
        if (item.requiredFeature() != null) m.put("requiredFeature", item.requiredFeature());
        String industry = menuRegistrar.getMenuIndustryMap().get(item.path());
        if (industry != null) m.put("industry", industry);
        m.put("children", children);
        return m;
    }
}
