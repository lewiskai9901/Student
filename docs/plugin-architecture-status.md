# 插件架构推进状态

> **最近更新**: 2026-04-19
> **当前架构等级**: **A** (B+ → A, 距 A+ 还差 3 个 Phase)
> **A+ 判定进度**: 12/15 (80%)

---

## 已完成 (12 Phase, 47 任务)

| Phase | 内容 |
|---|---|
| **-1** 紧急止血 | 冷启动可用 / ArchUnit 全启用 / Legacy 合并 / tmp 清理 |
| **0** 抽象 Registrar | `AbstractPluginRegistrar<P,D>` 基类, 6 Registrar 继承 |
| **1** origin 归一 | 8 张表加 `origin` 字段 (PLUGIN:CORE@1.0.0 / TENANT:CUSTOM#1) |
| **3** 事务+冲突+Casbin | @Transactional + 跨插件冲突 fail-fast + Casbin 自动 reload 事件 |
| **5** 多租户 | `tenant_plugin_enablement` 表 + Service + enable/disable/config API |
| **6** 治理 API | enable/disable/uninstall/health/dependency-graph + UI 按钮 |
| **7** create-plugin CLI | `scripts/create-plugin.sh` (实测生成 HEALTH) |
| **8** SemVer 兼容矩阵 | `SemVer.java` + getDependsOnWithVersion + 7 单元测试 |
| **9** Metrics + 矩阵测试 | 启动耗时 + `/metrics` + 6 矩阵集成测试 |
| **10** 开发者文档 | `docs/plugin-developer-guide.md` 完整 |

### 架构测试 (37/37 全绿)

```
ArchUnitPluginArchitectureTest       12
ArchUnitDashboardEndpointTest         1
ArchUnitMyEndpointTest                2
DddLayerTest                          4
NoMagicTriggerStringTest              1
PluginDeclarationCoverageTest         4
PluginMatrixIntegrationTest           6
SemVerTest                            7
```

### 启动日志(每次冷启动应看到)

```
[PluginPackageRegistrar]   版本兼容: EDU (v1.0.0) ⇒ 依赖 CORE@>=1.0.0 <2.0.0 满足
[PluginPackageRegistrar] 已加载 2 个行业包 - 启动顺序: [CORE, EDU]
[PluginRegistrar]              扫描 32 插件, 32 声明   耗时 ~500ms
[RelationTypePluginRegistrar]  扫描  2 插件, 13 声明   耗时 ~270ms
[MessagingRegistrar]           13 插件: 触发点 17 / 事件 53 / 默认触发 6
[PermissionRegistrar]          扫描  2 插件, 310 声明   耗时 ~1000ms
[Casbin] 收到 PermissionsRefreshedEvent ⇒ 重载策略 322 p / 574 g
[RolePresetRegistrar]          扫描  2 插件,  15 声明
[DataScopeRegistrar]           扫描  1 插件,   3 声明
[MenuRegistrar]                2 插件, 11 顶级菜单
[RelationTypeRegistry]         加载 13 关系 (via ApplicationReadyEvent)
```

---

## 待完成 (3 Phase, 剩余约 8-10 周)

### Phase 2 — 统一 SPI 顶层协议 (2 周)

**目标**: 把当前 8 个 SPI 接口 (`PluginManifest`/`EntityTypePlugin`/`RelationTypePlugin`/`MessagingDomainPlugin`/`PermissionProvider`/`RolePresetPlugin`/`DataScopePlugin`/`MenuContributionPlugin`) 收敛为**一个** `PluginPackage` + `sealed Contribution` 契约。

**入口文件**: `backend/.../extension/AbstractPluginRegistrar.java`

**核心设计**:
```java
interface PluginPackage {
    PluginMetadata metadata();
    Stream<Contribution> contribute();
}

sealed interface Contribution permits
    EntityTypeContribution,
    RelationTypeContribution,
    EventDomainContribution,
    PermissionContribution,
    RoleContribution,
    MenuContribution,
    DataScopeContribution,
    DomainContribution {
    String uniqueKey();
}
```

**约束**:
- 旧 8 SPI **必须保留**向下兼容 (打 `@Deprecated`)
- 新 SPI 作为组合层,内部分发到对应 Registrar
- 37 架构测试必须继续全绿

**验证**: 写一个 `UnifiedPluginPackageTest` 证明新旧 SPI 同时工作

---

### Phase 3.5 — DDD 物理包重组 (3 周, 最重)

**目标**: 把教育特定的领域聚合从 `domain/` 迁到 `plugins/education/domain/`

**当前状态** (问题):
```
backend/src/main/java/com/school/management/domain/
├── access/          ← 真通用 ✓
├── organization/    ← 真通用 ✓
├── place/           ← 真通用 ✓
├── shared/          ← 真通用 ✓
├── student/         ← ❌ 教育特定,应该在 plugins/education/
├── academic/        ← ❌ 教育特定
├── teaching/        ← ❌ 教育特定
└── calendar/        ← ❌ 教育特定
```

**目标状态**:
```
├── domain/                               ← 纯通用 4 个包
│   ├── access/ organization/ place/ shared/
├── infrastructure/extension/plugins/education/
│   ├── domain/                           ← ✅ 迁入
│   │   ├── student/ academic/ teaching/ calendar/
│   ├── application/                      ← 同步迁
│   ├── infrastructure/                   ← 同步迁
│   └── interfaces/                       ← 同步迁
```

**约束**:
- 100+ 文件 import 改动
- **必须用 IDE (IntelliJ Refactor > Move Package)**, 禁止 sed 批量替换
- 每迁一个包 commit 一次, 便于回滚
- ArchUnit 新增规则: `domain/` 包只允许 access/organization/place/shared

**验证**:
- 架构测试继续全绿
- 端到端业务测试 (学生 CRUD / 成绩录入 / 考勤) 通过
- 冷启动正常

---

### Phase 4 — 前端 Module Federation (3 周)

**目标**: 启用/禁用 EDU → 前端**自动**加载/卸载路由和组件,无需前端改代码

**当前状态**:
- `frontend/src/router/index.ts` 1394 行硬编码
- 79 处教育业务路由静态 `import`
- 后端 `/api/menus/my` 只是 allow-list 过滤

**目标状态**:
```
frontend/
├── src/
│   ├── router/core-routes.ts         ← 仅通用路由
│   └── boot/loadPlugins.ts           ← 运行时动态装载
plugins/education-frontend/          ← 独立 Vite 子项目
├── vite.config.ts                    ← federation exposes './routes' + './menus'
├── src/views/{Student/Academic/...}.vue
└── dist/remoteEntry.js               ← 构建产物
```

**启动时**:
```ts
const plugins = await fetch('/api/plugins/enabled')
for (const p of plugins) {
  await loadRemoteEntry(p.remoteUrl)
  const routes = await loadRemoteModule('@plugin/edu', './routes')
  router.addRoute(routes.default)
}
```

**约束**:
- 先做 POC 分支验证 Vite federation 稳定性
- Element Plus / Pinia 需要 shared
- 保留 SPA 热重载可用

**验证**:
- 禁用 EDU 后刷新前端 → `/student/list` 路由消失 (404)
- 重新启用 → 路由恢复

---

## 每个新会话的启动提示

### Phase 2 提示词(复制粘贴)
```
请继续插件架构 Phase 2 的工作。

前置状态:
- 已完成 Phase -1 至 Phase 10, 12/15 A+ 指标达成
- 详见 docs/plugin-architecture-status.md
- 37 架构测试全绿, 冷启动正常

本会话目标: Phase 2 — 统一 SPI 顶层协议 (把 8 SPI 收敛为 1 PluginPackage + Contribution 契约)

约束:
- 旧 8 SPI 必须保留 @Deprecated 向下兼容
- 一次完成, 不要拖到下会话
- 架构测试必须继续 37+ 全绿
- 冷启动 mvn spring-boot:run 必须成功
- 数据无回归 (310 perms / 15 roles / 32 types)

开始前: git log --oneline | head -10 了解最近提交
```

### Phase 3.5 提示词
```
请继续插件架构 Phase 3.5 — DDD 物理包重组。

前置状态:
- Phase 2 已完成
- 详见 docs/plugin-architecture-status.md

本会话目标: 把 domain/student, domain/academic, domain/teaching, domain/calendar 迁到 plugins/education/domain/

约束:
- 必须用 IDE 的 Refactor Move Package (如 IntelliJ), 不要 sed 批量替换
- 每迁一个包 commit 一次, 便于 revert
- 加 ArchUnit 规则: domain/ 只能有 access/organization/place/shared 4 个子包
- 架构测试全绿 + 端到端业务测试通过

第一步: 列出所有需要迁移的文件, 估计工作量
```

### Phase 4 提示词
```
请继续插件架构 Phase 4 — 前端 Module Federation。

前置状态:
- Phase 2 + 3.5 已完成
- 详见 docs/plugin-architecture-status.md

本会话目标: 让启用/禁用 EDU 能自动切换前端菜单和路由

约束:
- 先做 POC 分支 (`git checkout -b poc/module-federation`), 验证 Vite federation 稳定性
- 保留核心路由静态 (login / dashboard / system 等不动)
- Element Plus / Pinia 必须 shared, 否则会重复加载
- 验证: 禁用 EDU 后 /student/list 返回 404

第一步: 评估 @originjs/vite-plugin-federation 在 Vue 3 + Vite 5 的兼容性
```

---

## 每个 Phase 验证门 (3 道)

每次结束前必须全过:

```bash
# Gate 1: 冷启动
cmd //c "taskkill /IM java.exe /F"
cd /d/学生管理系统/backend
DB_PASSWORD=123456 JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
# 期望: "Started StudentManagementApplication in XX seconds"

# Gate 2: 架构测试
mvn test -Dtest="ArchUnit*,PluginDeclarationCoverageTest,NoMagicTriggerStringTest,DddLayerTest,PluginMatrixIntegrationTest,SemVerTest"
# 期望: Tests run: 37+, Failures: 0, Errors: 0

# Gate 3: API 回归
TOKEN=$(curl -sX POST localhost:8080/api/auth/login -H Content-Type:application/json -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)
curl -s localhost:8080/api/plugin-platform/overview -H "Authorization: Bearer $TOKEN" | jq .data.industries
# 期望: CORE + EDU, 数据 310 perms / 15 roles / 32 types
```

**任何一道不过 → 任务状态保持 in_progress, 绝不标完成**。

---

## 会话节奏建议

- **本周**: 完成 commit (已完成) + 观察部署 3-5 天
- **下周**: 启动 **Phase 2 独立会话** (2 周)
- **再下周**: 部署验证 1 周
- **第 4 周起**: 启动 **Phase 3.5 独立会话** (3 周, 最重)
- **第 8 周起**: 启动 **Phase 4 独立会话** (3 周)

**禁止叠加**: 不要在 Phase 2 没稳定前启动 Phase 3.5, bug 会指数级难定位。

---

## 紧急回滚

如某 Phase 做到一半发现路线错了:

```bash
git log --oneline | head -20           # 找到 Phase 起点 hash
git reset --hard <hash-before-phase>   # 硬重置
```

Phase -1 ~ 10 的成果已 commit, 回滚不会丢失它们。
