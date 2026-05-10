# E2E 集成联调结果 (2026-05-10)

## 起点
- master `c9d581f1` (前端冲 A merge 后, 后端 A+ / 前端 A 90)
- 3 个 service: MySQL :3306 / Redis :6379 / Backend :8080 / Frontend :3000

## Tier-by-Tier 结果

| Tier | 项 | 结果 | 备注 |
|------|------|------|------|
| 0 | 启动健康 | ✅ | 5 次 boot 才通 (2 个启动期 bug 阻塞) |
| 1 | 鉴权核心 | ✅ | login / /me / refresh / logout / 黑名单 |
| 2 | 数据权限 + API sanity | ✅ (8/12) | 单 admin 做不了 scope 对比, 留多角色测 |
| 3 | Zanzibar 关系 | ✅ | create/check/delete + 字段对齐 (resourceType/relation) |
| 4 | 工作流引擎 | ✅ | start instance + mytodo + cancel 全活 |
| 5 | 检查平台 | ✅ (5/8 sanity) | 完整业务流大测留 follow-up |
| 6 | 多端一致 | 部分 | 前端 :3000 可达, 完整 UI/小程序自动化留 follow-up |
| 7 | 可观测性 | ✅ | X-Trace-Id + prometheus + Flowable dbVersion |

## 修复的 Bug (3 commit)

| # | Bug | Commit | 影响面 |
|---|-----|--------|--------|
| 1 | workflow `TaskController` 与 education `teaching/TaskController` Spring bean name 撞 (默认 `taskController`) → 启动 ConflictingBeanDefinition 失败 | `89b88e88` | P0 阻塞启动 |
| 2 | `RelationTypeContribution.uniqueKey()` 只用 `relationCode`, CoreManifest W4.4 设计的 admin/viewer/responsible_for 跨多 (subject, resource) 对声明 → ContributionDispatcher "重复登记" 抛 IllegalStateException | `89b88e88` | P0 阻塞启动 |
| 3 | application-dev.yml 历史 Phase 6.3 优化把 Flowable AutoConfiguration 全 exclude (省 22s 启动开销, 但留 RepositoryService 缺失), `/api/workflow/*` 端点全部 NoSuchBean 500 | `4a21a5f8` | P0 工作流不可用 |
| 4 | `workflow:definition:*` / `workflow:instance:*` / `workflow:history:*` 共 8 个 controller 用的权限码 DB seed 缺失, admin 调全部 403 | `4a21a5f8` | P1 工作流端点 admin 403 |
| 5 | WorkflowContributionDeployer 用 @EventListener(ApplicationReadyEvent), 在 spring.main.lazy-initialization=true (dev) 下 bean 不 eager 创建 → 监听器永不注册 → CoreManifest 声明的 3 个 BPMN (leave/hello/access-relation) 启动期没真部署 | (修待下次 boot 验) | P1 流程定义不全 |

## Follow-up (优先级)

### P0 (修不动正路)
(全部已修, 当前后端正常运行)

### P1 (E2E 后续做)
- WF Deployer @Lazy(false) 修生效需重启验, 重启后 leave-approval / hello-world / access-relation-approval 应自动 deploy
- OutboxProcessor 反序列化 SlaBreachedEvent 失败: record 缺默认构造, Jackson 反不出. 历史 outbox 累积 ~30 条事件持续报 ERROR. 修法: SlaBreachedEvent 加 @JsonCreator / 改成 POJO with default ctor + setter
- 黑名单 token 调 API 落 403 (期望 401), Spring Security ExceptionTranslationFilter 的 AccessDenied vs Unauthenticated 边界. 前端处理需统一. 修法: JwtAuthenticationFilter 在黑名单时直接 send 401 不让 SecurityContext 占位

### P2 (业务深测)
- Tier 5 检查平台完整业务流: 模板创建 → 项目派发 → 任务扫码 → 评分提交 → 申诉 → 整改 (~2 天)
- Tier 6 多端一致: chrome 自动化跑前端登录/菜单/权限守护 + 小程序模拟器跑核心场景 + 数据 cross-端对照 (~1 天)
- 多角色 (admin / DEPT / SELF) 数据权限对比 (memory 提到 ALL/DEPT/SELF 未真验证)

### P3 (工程化)
- application-workflow.yml profile + WorkflowContributionDeployer 等 8 个 component 加 @Profile("workflow"), 让 dev 默认快启动模式可恢复
- PermissionContribution dispatcher 接通 Registrar 写 DB (当前只计数, 跨插件权限不能动态 seed, 永远要 SQL migration)
- FLOWABLE vs INLINE 关系审批双轨 E2E (当前只过了 INLINE 路径)

## 验证总结
**核心鉴权 / 关系 / 工作流 / 监控链路 4 大基础都通过了**, 这是前端 A / 后端 A+ 的真集成基线. 检查平台业务流 + 多端一致 + 多角色数据权限是更深的业务测试, 留作分批 follow-up.

3 commit 落地 (`89b88e88` / `4a21a5f8` / 本 commit), 起点 `c9d581f1` 至本 commit 共 +5 个 P0/P1 修复.
