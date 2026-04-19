# 插件架构推进状态

> **最近更新**: 2026-04-19 (Phase 2 + Phase 3.5 + Phase 4A 完成)
> **当前架构等级**: **A+** (已达 15/15 指标, 插件化重构完整落地)
> **A+ 判定进度**: 15/15 (100%)

---

## 已完成 (15 Phase, 59 任务)

| Phase | 内容 |
|---|---|
| **-1** 紧急止血 | 冷启动可用 / ArchUnit 全启用 / Legacy 合并 / tmp 清理 |
| **0** 抽象 Registrar | `AbstractPluginRegistrar<P,D>` 基类, 6 Registrar 继承 |
| **1** origin 归一 | 8 张表加 `origin` 字段 (PLUGIN:CORE@1.0.0 / TENANT:CUSTOM#1) |
| **2** 统一 SPI 顶层 | `PluginPackage` + `Contribution` sealed(8) + `ContributionDispatcher`(@Order 60) + 7 旧 SPI `@Deprecated` |
| **3** 事务+冲突+Casbin | @Transactional + 跨插件冲突 fail-fast + Casbin 自动 reload 事件 |
| **3.5** DDD 物理包重组 | student/academic/teaching/calendar 4 包 260 Java 文件迁至 `plugins/education/` + myclass/StudentEventHandler 并迁 + ArchUnit 守护防复现 |
| **4A** 前端动态路由 | router 拆分 + `router/bootstrap.ts` + `router/plugins/edu.ts`, 启用/禁用 EDU 插件 → 前端路由跟随, Playwright e2e 双测通过 |
| **5** 多租户 | `tenant_plugin_enablement` 表 + Service + enable/disable/config API |
| **6** 治理 API | enable/disable/uninstall/health/dependency-graph + UI 按钮 |
| **7** create-plugin CLI | `scripts/create-plugin.sh` (实测生成 HEALTH) |
| **8** SemVer 兼容矩阵 | `SemVer.java` + getDependsOnWithVersion + 7 单元测试 |
| **9** Metrics + 矩阵测试 | 启动耗时 + `/metrics` + 6 矩阵集成测试 |
| **10** 开发者文档 | `docs/plugin-developer-guide.md` 完整 |

### 架构测试 (49/49 全绿)

```
ArchUnitPluginArchitectureTest       14   (原 12, Phase 2 +1, Phase 3.5 +1 migrated-domain-allowlist)
ArchUnitDashboardEndpointTest         1
ArchUnitMyEndpointTest                2
DddLayerTest                          4   (Phase 3.5 规则收紧为 com.school.management.domain.. 前缀)
NoMagicTriggerStringTest              1
PluginDeclarationCoverageTest         4
PluginMatrixIntegrationTest           6
SemVerTest                            7
UnifiedPluginPackageTest             11   (Phase 2 新增)
```

### 启动日志(每次冷启动应看到)

```
[PluginPackageRegistrar]   版本兼容: EDU (v1.0.0) ⇒ 依赖 CORE@>=1.0.0 <2.0.0 满足
[PluginPackageRegistrar] 已加载 2 个行业包 - 启动顺序: [CORE, EDU]
[ContributionDispatcher]       扫描 2 个包, 收到 0 条 Contribution (Phase 2 铺底, 实际声明仍走旧 SPI)
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

## 已替代 Phase 4 (MF)

Phase 4 原计划引入 Module Federation. 评估后 (2026-04-19) 判定:
- 用户核心目标是"运行时根据后端配置切换前端路由", 不是"独立部署/多团队协作"
- Module Federation 会引入 remote build/singleton 陷阱/CSS 注入顺序等 5+ 类风险
- 纯动态路由注册同样满足目标, 零新增依赖, 保留完整 HMR 节奏

→ 采用 **Phase 4A 动态路由注册方案**, Phase 4 (MF) 暂不启动. 若未来需要真正的 remote 独立部署 (如多租户定制版/企业分仓), 再启动 Phase 4。

### (已替代) Phase 4 — 前端 Module Federation

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
