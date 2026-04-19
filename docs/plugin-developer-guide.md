# 插件开发者手册

> **适用版本**: Core v1.0.0+
> **最近更新**: 2026-04-19

本项目采用**通用核心 + 行业整包**架构。新增行业(如医疗/养老)不需要改核心代码,只需实现 SPI 插件接口并打包。

---

## 快速开始(3 分钟)

### 方式 A: 使用脚手架命令

```bash
./scripts/create-plugin.sh HEALTH "医疗行业" "CORE"
```

生成 `backend/src/main/java/.../plugins/health/` 目录,包含 6 个文件骨架。

### 方式 B: 手动创建

见本文档"8 SPI 详细说明"章节。

### 验证

```bash
mvn test -Dtest=PluginDeclarationCoverageTest  # 验证声明覆盖
mvn spring-boot:run                             # 启动应用, 观察日志
```

启动日志中应看到:
```
[PluginPackageRegistrar] 已加载 3 个行业包 - 启动顺序: [CORE, EDU, HEALTH]
```

---

## 架构层级

### 三层模型

```
┌─────────────────────────────────────────┐
│         业务代码 (application + domain)   │  ← 零耦合行业
└─────────────────────────────────────────┘
                  ↓ 通过 SPI 消费
┌─────────────────────────────────────────┐
│         SPI 接口 (8 个维度)              │
│  PluginManifest / EntityTypePlugin / … │
└─────────────────────────────────────────┘
                  ↑ 实现
┌─────────────────────────────────────────┐
│      行业包 (CORE / EDU / HEALTH / ...)  │  ← 可独立启停
└─────────────────────────────────────────┘
```

### 三种资源来源

| 类别 | 识别特征 | 生命周期 |
|---|---|---|
| **插件声明** | `origin = "PLUGIN:<CODE>@<ver>"` | 随插件包启停 |
| **CUSTOM** (租户自定义) | `origin = "TENANT:CUSTOM#<id>"` | 不受插件生命周期影响 |
| **孤儿** | `origin IS NULL` | 历史遗留, 应清理 |

---

## 8 个 SPI 维度

| SPI | 职责 | 对应 DB 表 | Registrar 顺序 |
|---|---|---|---|
| `PluginManifest` | 行业包元信息(code/name/version/依赖) | `plugin_packages` | @Order(50) |
| `EntityTypePlugin` | 实体类型(USER/ORG/PLACE 下的具体 typeCode) | `entity_type_configs` | @Order(100) |
| `RelationTypePlugin` | 关系类型(member/owns 等) | `relation_types` | @Order(200) |
| `MessagingDomainPlugin` | 触发点 + 事件类型 + 默认触发器 | `trigger_points` / `entity_event_types` / `event_triggers` | @Order(300) |
| `PermissionProvider` | 权限码 (resource:action) | `permissions` | @Order(400) |
| `RolePresetPlugin` | 预置角色 | `roles` | @Order(500) |
| `DataScopePlugin` | 数据范围维度 | `data_scope_dims` | @Order(600) |
| `MenuContributionPlugin` | 菜单树 | (内存,通过 API 下发) | @Order(700) |

### 各 SPI 详细说明

#### 1. PluginManifest — 行业包元信息

```java
@Component
public class HealthManifest implements PluginManifest {
    public String getIndustryCode()  { return "HEALTH"; }
    public String getIndustryName()  { return "医疗行业"; }
    public String getVersion()       { return "1.0.0"; }  // SemVer

    public List<String> getDependsOn() { return List.of("CORE"); }

    // 版本范围依赖 (Phase 8)
    public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    // 决定一个插件类是否归属本行业
    public boolean owns(Class<?> c) {
        return c.getPackageName().contains(".plugins.health");
    }
}
```

#### 2. EntityTypePlugin — 一个类一个类型

```java
@Component
public class DoctorPlugin implements EntityTypePlugin {
    public String getEntityType() { return "USER"; }
    public String getTypeCode()   { return "DOCTOR"; }
    public String getTypeName()   { return "医生"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("licenseNo", "执业证号", "text",
                "职业信息", true, Map.of())
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("canLogin", true, "isStaff", true);
    }
}
```

#### 3. PermissionProvider — 权限码声明

```java
@Component
public class HealthPermissionProvider implements PermissionProvider {
    public String getModuleCode() { return "health"; }
    public String getModuleName() { return "医疗行业"; }

    public List<PermissionDef> getPermissions() {
        return List.of(
            of("health:patient:view", "查看患者", ""),
            of("health:patient:edit", "编辑患者", ""),
            of("health:prescription:write", "开处方", "")
        );
    }
}
```

**关键规则**: 代码中使用 `@CasbinAccess(resource="health:patient", action="view")` 时,**必须**在此声明 `"health:patient:view"` 权限码。否则 `PluginDeclarationCoverageTest` 会在 CI 上红掉。

#### 4. RolePresetPlugin — 预置角色

```java
@Component
public class HealthRolePresetPlugin implements RolePresetPlugin {
    public List<RolePresetDef> getPresets() {
        return List.of(
            RolePresetDef.of("DOCTOR",     "医生",     "临床医师", 20),
            RolePresetDef.of("NURSE",      "护士",     "护理人员", 25),
            RolePresetDef.of("DEPT_HEAD",  "科室主任", "科室负责人", 10)
        );
    }
}
```

#### 5. RelationTypePlugin — 关系类型

```java
@Component
public class HealthRelationsPlugin implements RelationTypePlugin {
    public String getSourceName() { return "HealthPlugin"; }
    public String getTier()       { return "DOMAIN"; }

    public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            RelationTypeDef.of("treats", "user", "user", "诊治",
                "ASSOCIATION", "医生诊治患者")
        );
    }
}
```

#### 6. MessagingDomainPlugin — 触发点+事件

```java
@Component
public class HealthMessagingPlugin implements MessagingDomainPlugin {
    public String getDomainCode() { return "health"; }
    public String getDomainName() { return "医疗"; }

    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef("PATIENT_ADMITTED", "患者入院",
                Map.of("patientId","Long","wardId","Long"),
                "患者住院登记时触发")
        );
    }

    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("PATIENT_ADMIT_EVT", "入院登记",
                "PERSONNEL", "人事", "NEUTRAL", "user-plus", "#16a34a",
                List.of("USER"), "新患者入院")
        );
    }

    public List<DefaultTriggerDef> defaultTriggers() {
        return List.of(
            new DefaultTriggerDef("PATIENT_ADMITTED", "PATIENT_ADMIT_EVT",
                List.of(Map.of("type","USER","id","{{patientId}}")))
        );
    }
}
```

**代码使用**:
```java
// 严格禁止: triggerService.fire("PATIENT_ADMITTED", ...) ← 字面量
// 正确: 通过常量引用
import static com.school.management.infrastructure.extension.plugins.health.constants.HealthTriggerPoints.PATIENT_ADMITTED;
triggerService.fire(PATIENT_ADMITTED, Map.of("patientId", 123L, "wardId", 5L));
```

#### 7. DataScopePlugin — 数据范围维度

```java
@Component
public class HealthDataScopePlugin implements DataScopePlugin {
    public String getDomainCode() { return "health"; }

    public List<DimensionDef> getDimensions() {
        return List.of(
            new DimensionDef("BY_DEPARTMENT", "按科室",
                "按用户所在科室过滤数据", "DepartmentResolver")
        );
    }
}
```

#### 8. MenuContributionPlugin — 菜单树

```java
@Component
public class HealthMenuPlugin implements MenuContributionPlugin {
    public String getDomainCode() { return "health"; }

    public List<MenuItemDef> getMenus() {
        return List.of(
            of("/patient", "患者管理", "heart-pulse", 5).children(List.of(
                of("/patient/list", "患者列表", "users", 1)
                    .requiredPermissions(List.of("health:patient:view"))
            ))
        );
    }
}
```

---

## 生命周期 API

```bash
# 健康检查
GET /api/plugin-platform/HEALTH/health

# 启用 / 禁用
POST /api/plugin-platform/HEALTH/enable
POST /api/plugin-platform/HEALTH/disable

# 全局总览
GET /api/plugin-platform/overview

# Registrar 启动 metrics
GET /api/plugin-platform/metrics
```

**注意**: CORE 包不可禁用。

---

## CUSTOM 资源(管理员自定义)

管理员在 UI 里手动创建的类型/角色/权限:

- DB 字段 `industry='CUSTOM'`, `origin='TENANT:CUSTOM#<tenantId>'`
- 插件重启时**永不被覆盖**(Registrar 的 CUSTOM 保护机制)
- 卸载插件时**永不被级联删除**

---

## 核心保证 / ArchUnit 守卫

启动时必过的架构规则(`mvn test -Dtest=ArchUnit*`):

1. 行业插件之间**零依赖**(EDU 不能 import HEALTH)
2. 业务代码(domain/application/interfaces)**不能**直接依赖插件实现类(只能依赖 Constants/TriggerPoints 等契约)
3. 所有 SPI 实现必须标注 `@Component`
4. 禁用 `triggerService.fire("字面量", ...)` — 必须用常量
5. 禁止导入已删除的 `PermissionConstants` 类
6. 每个 `@CasbinAccess(resource, action)` 必须有对应的 PermissionProvider 声明
7. 每个 `triggerService.fire(CONST)` 必须有对应的 `new TriggerPointDef(CONST, ...)`
8. 每个 `hasRole('X')` 必须有对应的 `RolePresetDef.of("X", ...)`

---

## 冲突检测

如果两个插件声明同一个 unique key(如 EDU 和 HEALTH 都声明 `USER/DOCTOR`),启动时**fail-fast**:

```
IllegalStateException: [PluginRegistrar] 插件冲突:
  声明 USER/DOCTOR 同时被 PLUGIN:EDU@1.0.0 和 PLUGIN:HEALTH@1.0.0 声明,启动中止
```

---

## 版本兼容性(SemVer)

`getDependsOnWithVersion()` 返回的 Map 决定兼容范围。不满足 → 启动中止。

**支持的 range 语法**:
- `">=1.0.0"` 至少 1.0.0
- `"<2.0.0"` 低于 2.0.0
- `">=1.0.0 <2.0.0"` 1.x 兼容
- `"*"` 任意

**升级策略**:
- patch 版本(1.0.x): 向下兼容,无需声明
- minor 版本(1.x.0): 向后兼容,`">=1.x.0"` 即可
- major 版本(x.0.0): 可能破坏兼容,显式 range

---

## 测试策略

1. **单插件测试** — 只加载目标插件 + CORE,运行业务测试
2. **覆盖度测试** — `PluginDeclarationCoverageTest` 跑 4 条规则
3. **架构测试** — `ArchUnitPluginArchitectureTest` 12 条隔离规则
4. **冲突测试** — 故意制造同名声明,验证启动 fail-fast
5. **集成测试** — 启动完整 Spring Context,验证 Casbin reload 事件链

---

## 常见反模式(违反会被 ArchUnit / 覆盖度测试拦截)

| 反模式 | 正确做法 |
|---|---|
| `if ("STUDENT".equals(user.getType()))` | `if (user.hasFeature("isLearner"))` |
| `triggerService.fire("GRADE_SUBMITTED", ...)` | `triggerService.fire(GRADE_SUBMITTED, ...)` 用常量 |
| 在 application 层 `import plugins.education.StudentPlugin` | 只依赖 SPI 接口 `EntityTypePlugin` |
| 运行时 `CREATE TABLE IF NOT EXISTS` | 写 Flyway migration |
| `role.roleType == "SUPER_ADMIN"` | `hasRole("SUPER_ADMIN")` |

---

## FAQ

**Q: 我的插件可以访问核心表(如 users)吗?**
A: 可以读,但直接写请用 Core 的 domain service。

**Q: 卸载 HEALTH 包后数据会怎样?**
A: `industry='HEALTH'` 的行被软删(status=0),数据保留;下次重新安装时会自动恢复。CUSTOM 资源不受影响。

**Q: 两个插件都想贡献菜单到 `/patient` 路径?**
A: 路径唯一,第一个声明者胜出。第二个插件应使用不同 path(如 `/patient/dentistry`)。

**Q: 如何给插件加配置?**
A: `application.yml` 中使用 `app.plugins.health.xxx` 前缀 + `@ConfigurationProperties`。

---

## 参考

- SPI 定义: `backend/.../infrastructure/extension/*.java`
- 示例插件: `backend/.../plugins/education/`
- 脚手架: `scripts/create-plugin.sh`
- 架构测试: `backend/src/test/java/.../architecture/`
