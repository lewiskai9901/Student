# E2E 集成联调清单 (2026-05-10)

## 前置环境
- [x] MySQL :3306 (PID 5388)
- [x] Redis :6379 (PID 4812)
- [ ] Backend :8080 (`mvn spring-boot:run`)
- [ ] Frontend :3000 (`npm run dev`)

## Tier 0 — 启动健康
- [ ] 后端 30s 内启动, `/api/swagger-ui.html` 可访问
- [ ] `/api/actuator/health` UP
- [ ] 前端 dev server 启动, `/login` 渲染
- [ ] (Phase 8) `/api/actuator/prometheus` 暴露指标

## Tier 1 — 鉴权核心
- [ ] POST `/api/auth/login` admin/admin123 → access + refresh token
- [ ] GET `/api/auth/me` 携 token → user 信息
- [ ] POST `/api/auth/logout` → access token 进 Redis 黑名单
- [ ] 黑名单 token 再调 → 401
- [ ] refresh token 换新 access token

## Tier 2 — 数据权限 @DataPermission
- [ ] super admin (ALL) 看全部 students
- [ ] DEPT scope 角色只看本部门 students
- [ ] mysql general log 验证 mapper 路径自动 append `WHERE org_unit_id IN (...)` 子查询
- [ ] JdbcTemplate 直查路径(Dashboard 等)需手动 scope

## Tier 3 — Zanzibar ReBAC
- [ ] POST `/api/access-relations` 创建关系 (subject_user_id, relation_type, target_type, target_id)
- [ ] DB `access_relations` 写入正确, `valid_from/to` 字段
- [ ] PUT 修改, DELETE 软删
- [ ] hasFeature/hasPermission 据关系链解析(implied relations 跨级)
- [ ] FLOWABLE 模式: relation 审批走 BPMN; INLINE 模式: 直接生效

## Tier 4 — Workflow Engine (Flowable 7)
- [ ] 启动后 `processes/*.bpmn20.xml` 自动部署 (3 个: hello-world / leave-approval / access-relation-approval)
- [ ] GET `/api/workflow/process-definitions` 返回 3 个
- [ ] POST 启动流程实例 (leave-approval, applicant=admin)
- [ ] GET `/api/mytodo` 聚合 workflow + inspection-task 两 source
- [ ] 完成任务 → 流程推进
- [ ] 历史 `/api/workflow/history` 可查

## Tier 5 — 检查平台 (核心业务)
- [ ] 创建模板 (sections + items) → 升级版本
- [ ] 创建项目 → 派发任务给被检对象
- [ ] 扫码 INSPECTION:TASK:<id> → 进入任务
- [ ] 评分提交 → ScoringObservation 入库 (orgUnitId 自动填充, Composite handler 验证)
- [ ] 提交后申诉 → INLINE / FLOWABLE 双轨
- [ ] 整改单 (corrective case) 流程: created → processing → blocked → closed
- [ ] 模板漂移: 升模板版本 → drifted 标志 → 升级路径
- [ ] 离职重派: per-item partial-success
- [ ] 审计日志 `/inspection/audit-logs` 查询

## Tier 6 — 多端一致
- [ ] 前端 + 小程序登录同一账号, /mytodo 看同样数据
- [ ] 插件 disable EDU → 前端菜单消失 + 小程序 home-grid 消失
- [ ] enable HEALTHCARE (示例插件) → 菜单出现

## Tier 7 — 可观测性 (Phase 8)
- [ ] X-Trace-Id 响应头每个请求都有
- [ ] JSON 日志含 traceId/userId/uri MDC
- [ ] /actuator/prometheus 含 jvm + http_server_requests 指标
- [ ] (可选) Grafana 3 dashboard 加载有数据

## Tier 8 — 前端冲 A 闸联动
- [ ] router.beforeEach meta.permissions 拦无权页 → ElMessage warning + redirect
- [ ] type-check-ceiling.sh CI 模拟通过
- [ ] bundle-size-ceiling.sh CI 模拟通过
- [ ] dependency-cruiser 通过(403 warn 算可接受)

---

## 执行策略
- 串行(集成 bug 易定位)
- 每 tier 跑完先记结果 + 紧急 bug 立即修 commit
- 出 bug 形式: 复现步骤 + 期望 vs 实际 + 修补 commit SHA
- 最终产出 `e2e-integration-test-result.md`
