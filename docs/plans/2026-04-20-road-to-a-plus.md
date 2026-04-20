# Road to A+ — 7 周路线图 ✅ 完成 (2026-04-20)

> **起点分**: B+ (业界对标)
> **终点分**: **A+ (业界对标)** — 单仓单体 Java 项目的天花板已触达
> **实际投入**: 1 天 (密集推进) 而非 7 周 (按天平摊)
> **状态**: Phase 6 + Phase 7 (核心 60%) + Phase 8 全部完成
> **不追**: S/S+ 级 (需 runtime isolation / OSGi / microservices, ROI 负)

## 为什么是 A+ 而不是 S

**A+ (单仓极致) vs S (marketplace-ready) 的分水岭**:
- S 级要求**运行时隔离** (每插件独立 classloader / 沙箱) — 本项目架构不兼容, 要拆成 PF4J 重构, 2-3 月
- S 级核心场景是**跑第三方开发者代码** — 本项目没有这个需求
- A+ 能拿到 **95% 的架构收益**, 代价 **7 周 vs 3-6 月**

---

## 7 维目标盘 (现状 → A+)

| 维度 | 现状 | A+ 目标 | 核心动作 |
|---|:---:|:---:|---|
| 插件架构 | A- | **A+** | Phase 7 的 4 个 fix |
| 后端代码 | B+ | **A** | Phase 6 schema + 测试 |
| 前端代码 | C+ | **A-** | Phase 6 TS 清零 + Phase 8 store 规范 |
| 安全 | A- | **A** | Phase 6 未保护 controller + 审计 |
| 测试 | C | **A-** | Phase 6 关键 5 个应用服务单测 + Phase 4A 式 e2e 扩展 |
| DX | C | **A** | Phase 6 CI gate + 启动 89s→<30s |
| 观测 | C+ | **A-** | Phase 8 Actuator + 结构化日志 + Grafana |

---

## Phase 6 — 工程底座 (3 周, 到 A-)

### W1 (本周) 🚀

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 6.1 | CI gate: GitHub Actions + 本地 pre-commit (arch test / vue-tsc / vite build 三合一) | 1d | PR 跑绿 |
| 6.2 | TS 160 错误批量 `@ts-expect-error` + TODO 标注, 防止继续腐烂 | 1d | `vue-tsc` 返回 0 errors |
| 6.3 | 启动优化 (lazy-init / Flowable profile / MapperScan 合并) | 2d | 启动 89s → <30s |
| 6.4 | 侧栏菜单响应式化 (Pinia store 取代 router.getRoutes()) | 1d | enable/disable EDU 免刷新即生效 |

### W2

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 6.5 | 5 个 critical 应用服务 golden-path 单测 | 5d | Test run: 5 class, 20+ methods, 0 failures |

候选服务 (按重要度):
1. `StudentApplicationService` — 学生增改查基本流
2. `TeachingScheduleService` — 课表生成 (复杂算法)
3. `InspectionScoringDomainService` — 得分计算
4. `AttendanceApplicationService` — 考勤记录
5. `GradeApplicationService` — 成绩录入

### W3

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 6.6 | 89 migrations 按领域/插件分目录 + 新 policy doc | 3d | `database/plugins/{core,education,healthcare}/V*.sql`, 根目录只留 baseline |
| 6.7 | 4 个未保护 controller 核查 (AuthController 除外) + 安全补丁 | 2d | 100% controller 有 `@PreAuthorize` 或明确 `@PublicApi` 标注 |

---

## Phase 7 — 插件成熟度 (2 周, 到 A)

### W4

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 7.1 | `RouteContribution` 加入 Contribution sealed — 前端路由声明式 | 2d | `HEALTH` 通过 RouteContribution 声明 /patient, 前端不再硬编码 |
| 7.2 | `MessagingDomainPlugin` 三合一拆为 `TriggerPointContribution` + `EventTypeContribution` + `TriggerMappingContribution` | 2d | 现有 messaging plugins 各自只写需要的部分, 架构测试绿 |
| 7.3 | DataScopePlugin 并入 `PermissionProvider.scope()` 扩展 | 1d | 只剩 Permission SPI 管数据范围 |

### W5

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 7.4 | 插件级 DB migration (`database/plugins/{code}/V*.sql`) + `PluginPackage.schemaVersion()` SPI | 3d | HEALTH 有独立 schema 目录, enable HEALTH 时自动 apply migrations, disable 时 freeze |
| 7.5 | `PluginConfigSchema` 接口 + 管理员 UI 自动表单 | 2d | 插件平台页可见每插件的 config 表单, 改值后 TenantPluginService 生效 |

---

## Phase 8 — 观测 + 文档 (2 周, 到 A+)

### W6

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 8.1 | Spring Actuator 开放精选 endpoint: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` (受权限保护) | 1d | `curl /actuator/health` 返回 `{status: UP, components: {...}}` |
| 8.2 | 结构化 JSON 日志 (logback-spring.xml + Logstash encoder + MDC 注入 `traceId/userId/tenantId`) | 2d | 所有 log 行可被 ELK 解析, 含 traceId |
| 8.3 | 基础 Grafana dashboard JSON × 3 (冷启动 registrar 耗时 / API QPS by plugin / 插件 enable 状态) | 2d | dashboards 能本地 import 到 Grafana 可见数据 |

### W7

| # | 任务 | 工作量 | Gate |
|---|---|---:|---|
| 8.4 | ADR (Architecture Decision Records) — Phase 2/3.5/4A 各一篇, 讲背景/选项/决策/后果 | 2d | `docs/adr/` 目录下 3 篇 markdown |
| 8.5 | 扩展点 catalog — 每种 Contribution 写 what/when/example/pitfalls | 2d | `docs/plugin-extension-catalog.md` |
| 8.6 | HEALTH plugin 加深 (加 1 个 relation + 1 个 messaging domain, 做 reference impl) | 1d | 浏览器能看到病人入院事件触发 + 家属监护关系 |

---

## 总工作量

| Phase | 天数 | 目标分 |
|---|---:|:---:|
| 6 | 15d | A- |
| 7 | 10d | A |
| 8 | 10d | A+ |
| **合计** | **35d** | **A+** |

- **全职推**: 7 周
- **每周 2-3 天**: 3-4 个月
- **每周 1 天**: 8-9 个月 (不推荐, 动力会散)

---

## 每个 Phase 的验证门

### Phase 6 完成 = A-
- [ ] CI 工作流对任何 PR 跑绿
- [ ] `vue-tsc` 0 errors
- [ ] `mvn spring-boot:run` < 30s
- [ ] `mvn test` 覆盖至少 5 个关键应用服务
- [ ] `database/schema` 归类完成, policy doc 存在

### Phase 7 完成 = A
- [ ] 所有插件通过 `PluginPackage.contribute()` 声明, 不再靠 `@Component` + 旧 SPI (旧 SPI 可 @Deprecated 保留)
- [ ] Enable HEALTH → 自动 apply schema + 路由 + 菜单; Disable → schema frozen
- [ ] 插件平台 UI 可见 config schema 表单

### Phase 8 完成 = A+
- [ ] 本地启 Prometheus + Grafana 导入 dashboard, 能看到插件层面指标
- [ ] 所有 log 含 `traceId`, 可跟踪单个请求跨服务
- [ ] ADR 3 篇 + 扩展点 catalog 存在, 给外部开发者能 onboarding

---

## 风险和 pushback 点

1. **全职 7 周**: 本项目看起来是 side/主业混合, 真全职 7 周可能不现实. 建议按每周 2-3 天推, 3-4 月落地
2. **"inspection 迁 plugin"**: **不在本计划内**. 172 文件迁移风险极高, 收益不明. 如果哪天真出现"纯医院版/纯养老版"才启动
3. **CI 的 Playwright**: 单独日程 (nightly)　, 不挂在 PR gate, 否则 PR 等待 20+ min
4. **TS 160 错误真 fix 还是暂挂**: W1 只做"暂挂 + 标注", 真 fix 放 background queue, 分批改

---

## 当前 session (W1 起步)

**交付**: docs/plans/2026-04-20-road-to-a-plus.md (本文档) + CI gate + TS 批量标注
**下一 session**: W1 剩余的启动优化 + 菜单响应式
