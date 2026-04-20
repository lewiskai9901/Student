# 插件扩展点 Catalog

> 写给**新插件作者** (内部 + 未来外部开发者) 看. 目标: 新行业包从 0 到运行,
> 不需要读完 `infrastructure/extension/` 源码.
>
> 当前 9 种 `Contribution` + 2 个 PluginPackage 默认方法 + 1 个 DB migration 约定.
> 对应源码 `backend/src/main/java/com/school/management/infrastructure/extension/`.

## 0. 从哪里起步

每个新行业需要至少这些文件:

```
backend/.../plugins/{industry}/
├── {Industry}Manifest.java            ← 必. 实现 PluginPackage (见 §A)
└── (按需) 下面 8 种 Contribution 对应的 @Component
```

对应前端:
```
frontend/src/router/plugins/{industry}.ts   ← 必, 路由声明
frontend/src/views/{industry}/*.vue         ← 必, 页面组件
frontend/src/router/bootstrap.ts            ← 改, PLUGIN_LOADERS 加一行
```

看现成参考: `plugins/education/` (复杂) / `plugins/healthcare/` (骨架).

---

## A. PluginPackage 元信息 (**必须**)

继承 `PluginManifest` (老基类, 将来可能并), 声明行业码/版本/依赖:

```java
@Component
public class HealthcareManifest implements PluginPackage {
    @Override public String getIndustryCode()  { return "HEALTH"; }
    @Override public String getIndustryName()  { return "医疗行业"; }
    @Override public List<String> getDependsOn() { return List.of("CORE"); }

    @Override
    public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.healthcare");
    }
}
```

**陷阱**: `owns()` 必须正确, 否则其他 Registrar 扫到你的类但归不到你的 industry, 数据表 industry 列会是 null.

---

## B. Contribution 9 种

### B1. EntityTypeContribution — 实体子类型 (user/org/place)

**什么**: 给 `entity_type_configs` 表注册一种 user/org/place 的子类型 (如 "医生 DOCTOR", 组织的 "病区 WARD").

**什么时候**: 本插件引入新**身份** / **组织层级** / **场所种类**.

**最小示例** (现阶段走 @Component + EntityTypePlugin, PluginPackage.contribute() 未来也能走):

```java
@Component
@SuppressWarnings("deprecation")
public class PatientPlugin implements EntityTypePlugin {
    @Override public String getEntityType() { return "USER"; }
    @Override public String getTypeCode() { return "PATIENT"; }
    @Override public String getTypeName() { return "病人"; }
    @Override public String getCategory() { return "MEMBER"; }

    @Override public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("medicalRecordNo", "病历号", "text", "基本", true, Map.of()),
            FieldDefinition.of("admittedDate",    "入院日期", "date", "就诊", false, Map.of())
        );
    }

    @Override public Map<String, Boolean> getFeatures() {
        return Map.of("isExternal", true, "hasGuardian", true, "canLogin", false);
    }
}
```

**陷阱**:
- `features` 是业务代码判断用户行为的正确方式. **禁止** `"PATIENT".equals(user.getType())`, 正确写法 `user.hasFeature("isExternal")`
- `getTypeCode()` 大写 snake. 重复 code 启动时 fail-fast

### B2. RelationTypeContribution — 关系类型 (ReBAC)

**什么**: 注册一种可在 `access_relations` 表上建的关系 (如 `guardian_of` / `teaches` / `family_of`).

**什么时候**: 业务需要"A 对 B 有某种权限/关联"且关系类型有限. Zanzibar 语义.

```java
@Component
@SuppressWarnings("deprecation")
public class HealthcareRelationsPlugin implements RelationTypePlugin {
    @Override public String getSourceName() { return "HealthcarePlugin"; }
    @Override public String getTier() { return "DOMAIN"; }

    @Override public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            of("family_of", "user", "user", "家属监护",
               "ASSOCIATION", "家属 → 病人. 病人入院/出院事件时通知家属")
        );
    }
}
```

**陷阱**:
- `category` 取值: OWNERSHIP / MEMBERSHIP / ASSOCIATION / DELEGATION / SUBSCRIPTION
- `relationCode` lowercase snake. 跨插件重复 (如 EDU 已有 `guardian_of`) 启动 fail
- 业务代码引用关系要用**常量**, 不用裸字符串 (`NoMagicTriggerStringTest` 会抓)

### B3. EventDomainContribution — 消息域 (触发点 + 事件类型 + 默认触发器)

**什么**: 一个业务域 (宿舍/成绩/入院) 的消息体系一并声明. Phase 2 的设计是"同域成组", 不单独拆.

**什么时候**: 本插件内出现"XX 事件要通知 YY 的人"的需求.

```java
@Component
@SuppressWarnings("deprecation")
public class PatientAdmissionMessagingPlugin implements MessagingDomainPlugin {
    @Override public String getDomainCode() { return "patient-admission"; }
    @Override public String getDomainName() { return "病人入院"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            TriggerPointDef.of(PATIENT_ADMITTED, "病人入院",
                Map.of("patientId", "Long", "wardId", "Long"))
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(new EventTypeDef(
            "PATIENT_ADMITTED_EVT", "入院登记", "HEALTHCARE", "医疗", "NEUTRAL",
            "HeartPulse", "#dc2626", List.of("USER"), "入院通知家属"
        ));
    }

    @Override
    public List<DefaultTriggerDef> defaultTriggers() {
        return List.of(new DefaultTriggerDef(
            PATIENT_ADMITTED, "PATIENT_ADMITTED_EVT",
            List.of(Map.of("type", "USER", "id", "{{patientId}}"))
        ));
    }
}
```

**陷阱**:
- `pointCode` 必须在本插件 Java 常量里, 不能字面量传 (`NoMagicTriggerStringTest`)
- `triggerService.fire(CODE, ctx)` 的 CODE 必须有对应 `TriggerPointDef`, 否则静默 no-op (`PluginDeclarationCoverageTest` 会抓)

### B4. PermissionContribution — 权限码

**什么**: 往 `permissions` 表注册权限. 配合 `@CasbinAccess` / `@PreAuthorize` 使用.

```java
@Component
public class HealthcarePermissionProvider implements PermissionProvider {
    @Override public String getModuleCode() { return "healthcare"; }
    @Override public String getModuleName() { return "医疗"; }

    @Override public List<PermissionDef> getPermissions() {
        return List.of(
            PermissionDef.of("patient:view",     "查看病人",   "查病人列表/详情"),
            PermissionDef.of("patient:admit",    "办理入院",   "病人登记入院"),
            PermissionDef.of("patient:discharge","办理出院",   "病人出院登记")
        );
    }
}
```

**陷阱**: 业务代码里 `@CasbinAccess(resource="patient", action="view")` 一旦使用, 就**必须**在 PermissionProvider 里声明 `patient:view`. 否则 `PluginDeclarationCoverageTest` 会 fail.

### B5. RoleContribution — 预置角色

```java
@Component
public class HealthcareRolePresetPlugin implements RolePresetPlugin {
    @Override
    public List<RolePresetDef> getPresets() {
        return List.of(
            RolePresetDef.of("DOCTOR",       "医生",       "医护主体", 20)
                .withPermissions(Set.of("patient:view", "patient:admit")),
            RolePresetDef.of("NURSE",        "护士",       "辅助医护", 25)
                .withPermissions(Set.of("patient:view"))
        );
    }
}
```

**陷阱**: 代码里 `@PreAuthorize("hasRole('DOCTOR')")` 和这里声明的 roleCode 必须一致 (`PluginDeclarationCoverageTest`).

### B6. MenuContribution — 前端菜单 (后端声明)

**什么**: 往 `/api/menus/my` 返回的菜单树加一组.

```java
@Component
public class HealthcareMenuPlugin implements MenuContributionPlugin {
    @Override public String getDomainCode() { return "healthcare"; }

    @Override public List<MenuItemDef> getMenus() {
        return List.of(
            MenuItemDef.of("/patient", "病人管理", "heart-pulse", 5)
                .children(List.of(
                    MenuItemDef.of("/patient/list", "病人列表", "list", 1)
                        .requiredPermissions(List.of("patient:view"))
                ))
        );
    }
}
```

**关键**: `path` 必须**和前端 router/plugins/{industry}.ts 里的路径一字不差**. 不一致则 sidebar 过滤掉 (菜单看不见但路由存在).

### B7. DataScopeContribution — 数据范围维度

小众. 只当平台默认 7 种 DataScope (ALL/DEPARTMENT_AND_BELOW/...) 不够用时才写. 教育已用 3 维度 (BY_GRADE / BY_CLASS / BY_SPECIFIC), 医疗暂无.

### B8. RouteContribution (Phase 7.1)

**什么**: 声明前端路由元信息. 当前仅登记 + 冲突检测, 尚无 endpoint 下发 (Phase 8+ 兑现).

```java
// 在 PluginPackage.contribute() 里:
Stream.of(
    RouteContribution.of("HEALTH", "/patient", "病人管理"),
    RouteContribution.of("HEALTH", "/patient/list", "病人列表")
)
```

### B9. DomainContribution — 扩展占位

未来非结构化贡献的口子. 当前不用.

---

## B+. Policy / Validator SPI (Track W1)

**用途**: 声明业务规则检查点 — core 变更边界调用时自动执行, 规则可阻断 (BLOCK) 或仅告警 (WARN/INFO).

**适用场景**:
- "宿舍入住人数 < 4 时警告"
- "CLASS 删除前必须无归属学生"
- "创建医院科室时必须关联一位主治医师"

### 契约

`Policy<T>` 接口三方法:
- `code()` — 全局唯一策略码
- `supports(ctx)` — 过滤适用哪些 (entityType, phase) 组合
- `check(ctx)` — 返回 `List<Violation>`, 空=通过

`Violation` 三级:
- `BLOCK` — `PolicyRegistry.enforce()` 会抛 `PolicyViolationException`, 事务回滚
- `WARN` — 落日志/通知, 不阻断
- `INFO` — 审计用

### core 已接入的 hook 点

| entityType | phase | ApplicationService | commit |
|---|---|---|---|
| `place` | BEFORE_CHECKIN / AFTER_CHECKIN | `UniversalPlaceApplicationService.checkIn` | ad7a2327 |
| `place` | BEFORE_CHECKOUT / AFTER_CHECKOUT | `UniversalPlaceApplicationService.checkOut` | ad7a2327 |
| `org_unit` | BEFORE_CREATE / AFTER_CREATE | `OrgUnitApplicationService.createOrgUnit` | 297b73dd |
| `org_unit` | BEFORE_UPDATE / AFTER_UPDATE | `OrgUnitApplicationService.updateOrgUnit` | 297b73dd |
| `org_unit` | BEFORE_DELETE | `OrgUnitApplicationService.deleteOrgUnit` | 297b73dd |
| `org_unit` | BEFORE_ADD_MEMBER / AFTER_ADD_MEMBER | `OrgMemberService.addMember` | 297b73dd |
| `org_unit` | BEFORE_REMOVE_MEMBER / AFTER_REMOVE_MEMBER | `OrgMemberService.removeMember` | 297b73dd |

新 hook 点由 core 扩展, 插件不能自己加, 需 core PR.

### 最小示例

```java
@Component
public class MinOccupantsPolicy implements Policy<Object> {

    private final int threshold;

    public MinOccupantsPolicy(@Value("${policy.place.min-occupants:4}") int threshold) {
        this.threshold = threshold;
    }

    @Override public String code() { return "MIN_OCCUPANTS"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return threshold > 0
            && "place".equals(ctx.entityType())
            && "AFTER_CHECKIN".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        // ... 返回违规列表, 空 = 通过
    }
}
```

参考实现: `infrastructure/extension/plugins/core/policy/MinOccupantsPolicy.java` — core 内置 reference impl, 演示契约.

### 对比其他校验手段

| 手段 | 位置 | 时机 | 失败响应 | 适用 |
|---|---|---|---|---|
| Spring `@Valid` | Controller/DTO | 入参时 | 400 | 请求体字段/格式 |
| JPA `@NotNull` | Entity | save 时 | 数据库异常 | 持久化必填 |
| DDD 聚合 invariant | Aggregate | 状态变更时 | 业务异常 | 聚合内不变量 |
| **Policy SPI** | Application 边界 | BEFORE/AFTER | BLOCK 抛 + WARN 日志 | **跨聚合/跨插件业务规则** |

**陷阱**:
- `supports()` 记得加开关 (阈值为 0 时 return false), 否则禁用也会进 `check()`
- BLOCK 会回滚事务, AFTER 阶段的 BLOCK 意味着"撤销已完成的操作", 慎用
- 业务代码要用常量引用 code, 不要裸字符串

---

## C. PluginConfigSchema (Phase 7.5)

声明插件接受的**运行时配置项**, 管理员 UI 可自动渲染表单.

```java
@Override
public PluginConfigSchema configSchema() {
    return new PluginConfigSchema(List.of(
        Field.stringField("admissionWardPrefix", "入院病区前缀", "A-", false),
        Field.booleanField("autoAssignBed", "自动床位分配", true),
        Field.enumField("notifyChannel", "通知渠道",
            List.of("NONE","SMS","WECHAT"), "SMS")
    ));
}
```

读配置: `TenantPluginService.getConfig(tenantId, "HEALTH", "admissionWardPrefix")`.

---

## D. DB schema migrations

**新插件 DB 改动走新目录**:

```
database/plugins/{industry}/V{version}__{description}.sql
例: database/plugins/healthcare/V200.0.0__create_patient_admission.sql
```

policy 见 [`database/MIGRATIONS.md`](../database/MIGRATIONS.md).

**Flyway 实际多 locations 布线** 当前未上 (Phase 7.4 skeleton). 目前所有 SQL 仍在 `database/schema/`. 新开发先按新目录放 (下次 Flyway 布线会自动 pick up), 同时保证 version 号不和 legacy 冲突.

---

## E. 前端联动

### E.1 路由文件
```ts
// frontend/src/router/plugins/healthcare.ts
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/patient',
    redirect: '/patient/list',
    meta: { title: '病人管理', icon: 'HeartPulse', requiresAuth: true, order: 5 },
    children: [
      {
        path: '/patient/list',
        component: () => import('@/views/healthcare/PatientList.vue'),
        meta: { title: '病人列表', requiresAuth: true, permission: 'patient:view' }
      }
    ]
  }
]
export default routes
```

### E.2 bootstrap 注册
```ts
// frontend/src/router/bootstrap.ts
const PLUGIN_LOADERS = {
  EDU: () => import('./plugins/edu'),
  HEALTH: () => import('./plugins/healthcare')
}
```

### E.3 Vue 组件
在 `frontend/src/views/healthcare/PatientList.vue` 写页面. 接入 `@/api/patient.ts` 封装 backend.

---

## F. 启动时会看到什么

插件装载顺序 (按 @Order):

```
[PluginPackageRegistrar]       ← 扫 PluginPackage, 拓扑排序, 版本校验
[ContributionDispatcher]       ← 扫 contribute() 流, 冲突检测
[PluginRegistrar]               ← 扫 EntityTypePlugin @Component
[RelationTypePluginRegistrar]   ← 扫 RelationTypePlugin @Component
[MessagingRegistrar]            ← 扫 MessagingDomainPlugin @Component
[PermissionRegistrar]           ← 扫 PermissionProvider, 发 PermissionsRefreshedEvent
[Casbin] 重载策略
[RolePresetRegistrar]           ← 扫 RolePresetPlugin
[DataScopeRegistrar]            ← 扫 DataScopePlugin
[MenuRegistrar]                 ← 扫 MenuContributionPlugin (缓存, 不落表)
[RelationTypeRegistry]          ← ApplicationReadyEvent 后加载
```

插件装载完看 `/api/plugin-platform/overview` 确认数字正确.

---

## G. ArchUnit / Test 守护 (会自动 fail 的规则)

| 规则 | 触发条件 |
|---|---|
| `ArchUnitPluginArchitectureTest.all_*_implementations_must_be_components` | SPI impl 没 `@Component` |
| `...manifest_classes_must_have_Manifest_suffix` | 类名不 Manifest 结尾 |
| `...messaging_domain_plugins_must_have_MessagingPlugin_suffix` | 同 |
| `migrated_education_domains_must_not_return_to_top_level_domain` | 往 `com.school.management.domain.{student,academic,teaching,calendar}` 下加类 |
| `business_code_must_not_depend_on_industry_plugin_impls_directly` | 业务层 import 行业 plugin 实现 (非常量) |
| `PluginDeclarationCoverageTest` | @CasbinAccess / hasRole 引用了未声明的 perm/role |
| `NoMagicTriggerStringTest` | `triggerService.fire("字面量", ...)` — 必须用常量 |
| `ArchUnitControllerAuthTest` | @RestController 没任何 @PreAuthorize/@CasbinAccess |

---

## H. 推荐学习顺序

1. 读 `plugins/healthcare/HealthcareManifest.java` + `PatientPlugin.java` — 最小插件
2. 读 `plugins/education/EducationManifest.java` + `EducationPermissionProvider.java` — 完整插件
3. 跑 `UnifiedPluginPackageTest` 看 15 个契约断言
4. 想加新行业? 复制 `plugins/healthcare/` 改名, 逐步往里填
