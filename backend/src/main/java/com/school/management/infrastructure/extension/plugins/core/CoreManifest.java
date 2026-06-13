package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.DataResourceDef;
import com.school.management.infrastructure.extension.MenuItemDef;
import com.school.management.infrastructure.extension.RolePresetDef;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.RelationTypeDef;
import com.school.management.infrastructure.extension.RelationTypeDef.Implied;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import static com.school.management.infrastructure.extension.MenuItemDef.of;

/**
 * 通用核心插件包 — 任何部署都需要,不可卸载.
 *
 * 包含:
 *  - 9 个核心关系类型 (Phase 2 W2.2: 已迁入本 Manifest 的 contribute() 流)
 *  - 通用权限 / 超级管理员角色
 *  - 通用事件类型(USER_CREATED / ORG_MERGED 等)
 *
 * Phase 2 W2.2: CoreRelationsPlugin 已删, 关系定义直接在此声明.
 * 业务代码引用关系码用 {@link CoreRelations} 常量.
 */
@Component
public class CoreManifest implements PluginPackage {

    private static final String SOURCE = "CORE";
    private static final String TIER = "CORE";

    @Override
    public String getIndustryCode() { return "CORE"; }

    @Override
    public String getIndustryName() { return "通用核心"; }

    @Override
    public boolean owns(Class<?> pluginClass) {
        String pkg = pluginClass.getPackageName();
        // core 包 + 未标注 industry 的旧插件(保守落到 CORE)
        return pkg.contains(".plugins.core") || !pkg.contains(".plugins.");
    }

    @Override
    public Stream<Contribution> contribute() {
        Stream<Contribution> base = Stream.of(
            // 成员关系 — 每用户唯一归属 (maxPerSubject=1), forceGrant 强制
            wrap(RelationTypeDef.of(CoreRelations.MEMBER, "user", "org_unit", "成员",
                "MEMBERSHIP", "用户属于某组织").withMaxPerSubject(1)),

            // 管理关系 (organization) — 主管理员单一,副管理员可多人
            // W4 reference demo: admin(org) 派生两条 implied:
            //   1) viewer on user via MEMBERS_OF_ORG — 能管组织就能看组织里的人
            //   2) admin on org_unit via DESCENDANTS_OF_ORG — 管理权沿组织树下沉 (isTransitive 的显式展开)
            wrap(RelationTypeDef.of(CoreRelations.ADMIN, "user", "org_unit", "主管理员",
                "OWNERSHIP", "组织的主负责人(如班主任/部门主管),沿子组织传递")
                .transitive().withMaxPerResource(1)
                .withImplied(List.of(
                    new Implied("user",     "viewer", Implied.MEMBERS_OF_ORG),
                    new Implied("org_unit", "admin",  Implied.DESCENDANTS_OF_ORG)
                ))),

            wrap(RelationTypeDef.of(CoreRelations.DEPUTY, "user", "org_unit", "副管理员",
                "OWNERSHIP", "组织副负责人").transitive()),

            // 管理关系 (place) — 负责人单一,管理者可多人
            wrap(RelationTypeDef.of(CoreRelations.ADMIN, "user", "place", "场所负责人",
                "OWNERSHIP", "场所的主负责人").withMaxPerResource(1)),

            // W4.4 reference demo: 场所管理者 → 自动对场所内所有 occupant(user) 有 viewer 权限
            // 语义: "能管场所" 意味着 "能看到场所里的人",派生规则走 OCCUPANTS_OF_PLACE discovery
            wrap(RelationTypeDef.of(CoreRelations.MANAGES, "user", "place", "场所管理者",
                "OWNERSHIP", "非主责管理者 (保洁/物业等)")
                .withImplied(List.of(
                    new Implied("user", "viewer", Implied.OCCUPANTS_OF_PLACE)
                ))),

            // 归属 — 一个场所只能归属一个主组织 (maxPerSubject=1, subject=place)
            // 真相源: 只存覆盖点, 无关系=沿场所树继承父级; 投影列 places.effective_org_unit_id
            // 由 PlaceOrgProjector 维护。不支持 valid_to 时效(投影器无法感知过期)。
            wrap(RelationTypeDef.of(CoreRelations.BELONGS_TO, "place", "org_unit", "归属",
                "ASSOCIATION", "场所归属某组织").withMaxPerSubject(1)),

            // 占用 — 受场所容量约束
            wrap(RelationTypeDef.of(CoreRelations.OCCUPIES, "user", "place", "占用",
                "MEMBERSHIP", "宿舍入住/工位使用 (含 check_in/check_out 时间)").withCapacityBound()),

            // 委托
            wrap(RelationTypeDef.of(CoreRelations.DELEGATED_TO, "user", "user", "委托",
                "DELEGATION", "权限临时委托给另一用户")),

            // 订阅
            wrap(RelationTypeDef.of(CoreRelations.WATCHES, "user", "org_unit", "关注",
                "SUBSCRIPTION", "用户订阅某组织的动态")),

            // W3.1 viewer — 通用只读访问 (3 条覆盖 user/org/place)
            wrap(RelationTypeDef.of(CoreRelations.VIEWER, "user", "user", "查阅者(用户)",
                "ASSOCIATION", "通用只读访问 — 直接 grant 给某用户对某用户档案的查阅权")),
            wrap(RelationTypeDef.of(CoreRelations.VIEWER, "user", "org_unit", "查阅者(组织)",
                "ASSOCIATION", "通用只读访问 — grant 给某用户对某组织的查阅权")),
            wrap(RelationTypeDef.of(CoreRelations.VIEWER, "user", "place", "查阅者(场所)",
                "ASSOCIATION", "通用只读访问 — grant 给某用户对某场所的查阅权")),

            // W3.1 responsible_for — 通用责任人 (3 条覆盖 user/org/place)
            wrap(RelationTypeDef.of(CoreRelations.RESPONSIBLE_FOR, "user", "user", "责任人(对人)",
                "OWNERSHIP", "通用责任 — 对某用户负责 (如导师对学生,医师对病人)")),
            wrap(RelationTypeDef.of(CoreRelations.RESPONSIBLE_FOR, "user", "org_unit", "责任人(对组织)",
                "OWNERSHIP", "通用责任 — 对某组织负责 (如部门主管,班主任)")),
            // 一场所至多一个责任人 (对齐旧 places.responsible_user_id 单列语义);
            // 与 admin|user|place(场所管理权)职责不同: responsible_for=业务问责, 参与场所树继承解析
            wrap(RelationTypeDef.of(CoreRelations.RESPONSIBLE_FOR, "user", "place", "责任人(对场所)",
                "OWNERSHIP", "通用责任 — 对某场所负责 (如设备责任人,场地负责人)").withMaxPerResource(1)),

            // Phase 5 — sample workflows (BPMN 文件在 classpath:processes/)
            new Contribution.WorkflowContribution(
                getIndustryCode(),
                "processes/leave-approval.bpmn20.xml",
                "请假审批流程示例"),
            new Contribution.WorkflowContribution(
                getIndustryCode(),
                "processes/hello-world.bpmn20.xml",
                "Hello World 测试流程"),

            // Phase 6 (workflow-engine) — access RelationApproval 走 Flowable
            new Contribution.WorkflowContribution(
                getIndustryCode(),
                "processes/access-relation-approval.bpmn20.xml",
                "关系授权审批流程")
        );
        return Stream.of(
                base,
                tenantAdminPermissionBindings(),
                coreDataResources(),
                coreRoles(),
                coreMenus(),
                corePermissions()
            ).flatMap(s -> s);
    }

    /** 通用核心功能权限 (双轨收敛: 从 CorePermissionProvider holder 聚合)。 */
    private Stream<Contribution> corePermissions() {
        return CorePermissionProvider.permissions().stream()
            .map(d -> new Contribution.PermissionContribution(
                CorePermissionProvider.MODULE_CODE, CorePermissionProvider.MODULE_NAME, d));
    }

    /** 包装为 CORE 域菜单贡献 */
    private static Contribution.MenuContribution m(MenuItemDef item) {
        return new Contribution.MenuContribution("core", item);
    }

    /**
     * 通用核心菜单 (双轨收敛: 从已删的 CoreMenuPlugin 迁入)。
     * 首页 / 消息与事件 / 访问控制 / 组织 / 场所 / 检查平台 / 资产 / 系统设置。
     */
    private Stream<Contribution> coreMenus() {
        return Stream.of(
            m(of("/dashboard", "首页", "home", 1)),

            m(of("/message", "消息与事件", "bell", 2).children(List.of(
                of("/message/list", "消息通知", "inbox", 1),
                of("/message/event-types", "事件类型", "settings-2", 2)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/triggers", "事件触发器", "zap", 3)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/subscriptions", "订阅与模板", "list-checks", 4)
                    .requiredPermissions(List.of("system:config:view")),
                of("/message/preferences", "消息偏好", "sliders", 5)
            ))),

            m(of("/access", "访问控制", "shield", 6).children(List.of(
                of("/system/users", "用户管理", "users", 1)
                    .requiredPermissions(List.of("system:user:view")),
                of("/system/roles", "角色管理", "user-cog", 2)
                    .requiredPermissions(List.of("system:role:view")),
                of("/system/permissions", "权限管理", "lock", 3)
                    .requiredPermissions(List.of("system:permission:view")),
                of("/access/relations", "关系绑定", "link-2", 4)
                    .requiredPermissions(List.of("access:relation:view")),
                of("/access/relation-types", "关系字典", "book", 5)
                    .requiredPermissions(List.of("access:relation:view")),
                of("/access/data-permissions", "数据权限", "shield-check", 6)
                    .requiredPermissions(List.of("access:data-permission:view"))
            ))),

            m(of("/organization", "组织管理", "building-2", 3).children(List.of(
                of("/organization/units", "组织架构", "network", 1)
            ))),

            m(of("/place", "场所管理", "map-pin", 4).children(List.of(
                of("/place/management", "场所管理", "map", 1)
            ))),

            m(of("/inspection", "检查平台", "clipboard-check", 12).children(List.of(
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
            ))),

            m(of("/asset", "资产管理", "package", 22).children(List.of(
                of("/asset/center",       "资产总览",   "layout-dashboard", 1).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/categories",   "资产类别",   "tags",             2).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/inventory",    "资产清册",   "clipboard-list",   3).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/borrows",      "借用归还",   "hand",             4).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/maintenance",  "维保工单",   "wrench",           5).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/approvals",    "审批流",     "file-check",       6).requiredPermissions(List.of("asset:approval:view")),
                of("/asset/alerts",       "告警中心",   "bell-ring",        7).requiredPermissions(List.of("asset:manage:view")),
                of("/asset/depreciation", "折旧核算",   "trending-down",    8).requiredPermissions(List.of("asset:manage:view"))
            ))),

            m(of("/system", "系统管理", "settings", 90).children(List.of(
                of("/system/entity-types", "类型配置", "layout-grid", 4)
                    .requiredPermissions(List.of("system:config:view")),
                of("/system/plugins", "插件平台", "package", 5)
                    .requiredPermissions(List.of("system:config:view")),
                of("/system/configs", "系统配置", "sliders", 6)
                    .requiredPermissions(List.of("system:config:view")),
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
            )))
        );
    }

    /**
     * 通用核心预置角色 (双轨收敛: 从已删的 CoreRolePresetPlugin 迁入)。平台级、行业无关。
     */
    private Stream<Contribution> coreRoles() {
        return Stream.of(
            new Contribution.RoleContribution(RolePresetDef.of("SUPER_ADMIN", "超级管理员",
                "平台最高权限,系统唯一,不受租户/行业限制", 0)),
            new Contribution.RoleContribution(RolePresetDef.of("TENANT_ADMIN", "租户管理员",
                "单租户下的管理员,管理本租户所有业务", 10)),
            new Contribution.RoleContribution(RolePresetDef.of("GUEST", "访客",
                "只读角色,不能创建/修改任何数据", 90))
        );
    }

    /**
     * 通用核心数据资源 scope 声明 (Phase 1 双轨收敛: 从已删的 CoreDataResourceProvider 迁入)。
     * 组织向走 5 种常规 scope; 个人向只能 SELF。启动期 UPDATE data_resources.allowed_scopes。
     */
    private Stream<Contribution> coreDataResources() {
        return Stream.of(
            // 组织向: 5 种常规 scope
            dr("user",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("org_unit",    "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("role",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("place",       "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("system_role", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("system_user", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            // 个人向: 只能 SELF
            dr("notification", "SELF"),
            dr("dashboard",    "SELF"),
            // 检查平台 (通用核心)
            dr("inspection_project",     "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_task",        "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_record",      "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_corrective",  "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_alert",       "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_observation", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_violation",   "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("inspection_summary",     "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "CUSTOM"),
            dr("inspection_template",    "ALL", "SELF"),
            dr("inspection_appeal",      "SELF"),
            dr("inspection_personal",    "SELF")
        );
    }

    private static Contribution.DataResourceContribution dr(String code, String... scopes) {
        return new Contribution.DataResourceContribution(DataResourceDef.of(code, scopes));
    }

    /**
     * TENANT_ADMIN 默认功能权限 — 让"非超管的管理员"真正可用 (Casbin 缺口修复, 2026-06-12)。
     *
     * <p>之前 14 个非超管角色 role_permissions 全 0 行, 整个系统只有超管 bypass 在工作。
     * TENANT_ADMIN 作为核心声明的租户级管理员, 拿到系统管理面 (用户/组织/角色/配置/审计)
     * + 权限管理 — 这是它与行业管理角色 (如 SCHOOL_ADMIN, 只有业务面) 的边界。
     * 仍然不授 :delete 与 tenant:* (建租户/删数据留给超管或 UI 显式授予)。
     *
     * <p>由 RolePermissionBindingRegistrar (@Order 600) INSERT IGNORE 落库,
     * admin 在 UI 的调整不被覆盖。码值守护: PluginDeclarationCoverageTest Test 5。
     */
    private Stream<Contribution> tenantAdminPermissionBindings() {
        return Contribution.RolePermissionBindingContribution.bindAll("TENANT_ADMIN",
            "admin:access", "dashboard:view",
            "system:user:view", "system:user:edit", "system:user:add",
            "system:org:view", "system:org:edit", "system:org:create", "system:org:update",
            "system:role:view", "system:role:edit", "system:role:add",
            "system:permission:view", "system:permission:tree",
            "system:config:view", "system:config:edit",
            "system:audit:view", "system:message:manage",
            "access:relation:view", "access:data-permission:view",
            "plugin-platform:view", "entity-type-config:view",
            "workflow:definition:view", "workflow:instance:view", "workflow:history:view",
            "calendar:view"
        ).stream().map(c -> (Contribution) c);
    }

    private static Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
