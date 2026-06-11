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

            // ─── 消息与事件 (路径与前端 router 对齐: /message) ───
            of("/message", "消息与事件", "bell", 2).children(List.of(
                of("/message/list", "消息通知", "inbox", 1),
                of("/message/event-types", "事件类型", "settings-2", 2)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/triggers", "事件触发器", "zap", 3)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/subscriptions", "订阅与模板", "list-checks", 4)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/preferences", "消息偏好", "sliders", 5)
            )),

            // ─── 访问控制 (RBAC 用户/角色/权限 + ReBAC 关系 + 数据权限) ───
            of("/access", "访问控制", "shield", 6).children(List.of(
                of("/system/users", "用户管理", "users", 1)
                    .requiredPermissions(List.of("system:user:view")),
                of("/system/roles", "角色管理", "user-cog", 2)
                    .requiredPermissions(List.of("system:role:view")),
                of("/system/permissions", "权限管理", "lock", 3)
                    .requiredPermissions(List.of("system:permission:view")),
                // system:admin 是超管专属合成码 (MenuQueryApplicationService 注入) —
                // 菜单标注须用真实注册码, 否则 TENANT_ADMIN 等拿到了对应权限也看不到菜单
                // (超管走 MenuController 的 contains("system:admin") 全放行, 不受影响)。
                of("/access/relations", "关系绑定", "link-2", 4)
                    .requiredPermissions(List.of("access:relation:view")),
                of("/access/relation-types", "关系字典", "book", 5)
                    .requiredPermissions(List.of("access:relation:view")),
                of("/access/data-permissions", "数据权限", "shield-check", 6)
                    .requiredPermissions(List.of("access:data-permission:view"))
            )),

            // ─── 组织管理 (通用核心) ───
            of("/organization", "组织管理", "building-2", 3).children(List.of(
                of("/organization/units", "组织架构", "network", 1)
            )),

            // ─── 场所管理 (通用核心) ───
            of("/place", "场所管理", "map-pin", 4).children(List.of(
                of("/place/management", "场所管理", "map", 1)
            )),

            // ─── 检查平台 (已通用化, 通用核心能力; P0+P1+P2+P3 重构后 36 → 18 菜单) ───
            // "我的xx/受检"系列是 SELF 语义页面, 全员可见不标注; 管理/分析面按真实
            // @CasbinAccess 码标注 (Casbin 缺口修复 2026-06-12: 非超管菜单对齐授权矩阵)。
            of("/inspection", "检查平台", "clipboard-check", 12).children(List.of(
                of("/inspection/dashboard",        "检查平台总览", "layout-dashboard",  0)
                    .requiredPermissions(List.of("insp:platform:view")),
                of("/inspection/governance",       "治理工作台",   "shield-check",      1)
                    .requiredPermissions(List.of("insp:platform:view")),
                of("/inspection/tasks",            "我的任务",     "list-todo",         5),
                of("/inspection/my-record",        "我的成绩单",   "badge-check",       6),
                of("/inspection/my-corrective",    "我的整改",     "wrench",            7),
                of("/inspection/appeals/my",       "我的申诉",     "scale",             8),
                of("/inspection/received",             "我的受检中心",   "building-2",     10),
                of("/inspection/received/inspections", "我被检查的记录", "clipboard-list", 11),
                of("/inspection/received/trends",      "检查趋势",       "trending-up",    12),
                of("/inspection/received/recurring",   "高频问题",       "alert-triangle", 13),
                of("/inspection/tasks/review-risk","待审风险池",   "list-checks",       9)
                    .requiredPermissions(List.of("insp:task:review")),
                of("/inspection/appeals/review",   "申诉审核",     "gavel",            10)
                    .requiredPermissions(List.of("inspection_appeal:review")),
                of("/inspection/projects",         "检查项目",     "folder-search",    20)
                    .requiredPermissions(List.of("insp:project:view")),
                of("/inspection/config",           "检查配置",     "settings",         21)
                    .requiredPermissions(List.of("insp:platform:manage")),
                of("/inspection/grade-schemes",    "等级方案",     "award",            23)
                    .requiredPermissions(List.of("insp:scoring-profile:view")),
                of("/inspection/issue-categories", "问题分类",     "tags",             24)
                    .requiredPermissions(List.of("insp:platform:manage")),
                of("/inspection/analytics",        "分析报表",     "bar-chart-3",      30)
                    .requiredPermissions(List.of("insp:analytics:view")),
                of("/inspection/corrective",       "整改管理",     "hammer",           31)
                    .requiredPermissions(List.of("insp:corrective:manage")),
                of("/inspection/alerts",           "预警看板",     "bell",             32)
                    .requiredPermissions(List.of("insp:alert:view")),
                of("/inspection/export",           "导出中心",     "download",         33)
                    .requiredPermissions(List.of("insp:analytics:view")),
                of("/inspection/audit-trail",      "审计日志",     "file-search",      40)
                    .requiredPermissions(List.of("insp:audit:view")),
                of("/inspection/admin/reassign-departed", "离职重派", "user-x",        99)
                    .requiredPermissions(List.of("insp:platform:manage"))
            )),

            // ─── 资产管理 (通用核心, 非行业特定) ───
            of("/asset", "资产管理", "package", 22).children(List.of(
                of("/asset/center",       "资产总览",   "layout-dashboard", 1).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/categories",   "资产类别",   "tags",             2).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/inventory",    "资产清册",   "clipboard-list",   3).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/borrows",      "借用归还",   "hand",             4).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/maintenance",  "维保工单",   "wrench",           5).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/approvals",    "审批流",     "file-check",       6).requiredPermissions(List.of("asset:approval:view")),
                of("/asset/alerts",       "告警中心",   "bell-ring",        7).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/depreciation", "折旧核算",   "trending-down",    8).requiredPermissions(List.of("asset:manage:view"))
            )),

            // ─── 系统设置 ───
            of("/system", "系统管理", "settings", 90).children(List.of(
                // 用户/角色/权限(RBAC) 已迁至"访问控制"(/access), 与关系+数据权限统一
                of("/system/entity-types", "类型配置", "layout-grid", 4)
                    .requiredPermissions(List.of("system:config:view")),
                of("/system/plugins", "插件平台", "package", 5)
                    .requiredPermissions(List.of("system:config:view")),
                of("/system/configs", "系统配置", "sliders", 6)
                    .requiredPermissions(List.of("system:config:view")),
                // 教师档案(/system/teachers)/学期管理(/system/semesters) 是教育菜单,
                // 已迁至 EducationMenuPlugin (学术/教务 下), industry 归正为 EDU。
                // 下面 4 项改用真实注册码 (TENANT_ADMIN 矩阵已授); 登录定制保留超管专属
                of("/system/tenants", "租户管理", "building", 9)
                    .requiredPermissions(List.of("tenant:view")),
                of("/system/login-customization", "登录定制", "palette", 10)
                    .requiredPermissions(List.of("system:admin")),
                of("/system/audit", "系统审计", "history", 11)
                    .requiredPermissions(List.of("system:audit:view")),
                of("/system/operation-logs", "操作日志", "scroll-text", 12)
                    .requiredPermissions(List.of("system:audit:view")),
                of("/system/announcements", "系统公告", "megaphone", 13)
                    .requiredPermissions(List.of("system:message:manage"))
            ))
        );
    }
}
