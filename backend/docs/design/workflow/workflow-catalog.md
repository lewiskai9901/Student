# 工作流引擎 — 部署文档

> 工作流引擎 Phase 1-8 收官产物
> 日期:2026-05-10
> 引擎:Flowable 7.0.1 (BPMN 2.0)

## 当前部署的流程

| 流程 ID | 文件 | 来源 | 描述 |
|---|---|---|---|
| `hello_world` | `processes/hello-world.bpmn20.xml` | CoreManifest | 集成 smoke test |
| `leave_approval` | `processes/leave-approval.bpmn20.xml` | CoreManifest | 请假审批示例 |
| `access_relation_approval` | `processes/access-relation-approval.bpmn20.xml` | CoreManifest | **关系授权审批(Phase 6 接通)** |

新增流程方式:
1. 创建 `.bpmn20.xml` 在某 plugin 的 `resources/processes/` 下
2. 在 plugin 的 `Manifest.contribute()` 加 `WorkflowContribution(industryCode, resourcePath, description)`
3. 重启 — `WorkflowContributionDeployer` 在 `ApplicationReadyEvent` 自动部署

## REST 端点(Phase 2)

```
/workflow/process-definitions
  POST /deploy             — 上传 BPMN 部署
  GET  /                   — 列流程定义
  GET  /{id}               — 单个详情
  DEL  /deployments/{id}   — 删除部署
  POST /{id}/suspend       — 挂起
  POST /{id}/activate      — 激活

/workflow/process-instances
  POST /start              — 启动实例
  GET  /                   — 列活跃实例
  GET  /{id}               — 单个详情
  POST /{id}/cancel        — 取消

/workflow/tasks
  GET  /mine               — 我的待办
  GET  /{id}               — 单个详情
  POST /{id}/claim         — 认领
  POST /{id}/unclaim       — 取消认领
  POST /{id}/complete      — 完成
  POST /{id}/delegate      — 转办

/workflow/history
  GET /instances           — 历史实例
  GET /instances/{id}/tasks — 实例的任务历史
  GET /my-tasks            — 我经手的任务

/my-todos                  — 跨源待办聚合(Phase 7)
  GET /                    — 我的全部待办(可 ?source= 过滤)
  GET /counts              — 各源数量
```

## SPI 扩展点(Phase 4-7)

| SPI | 用途 | 默认实现 |
|---|---|---|
| `WorkflowContribution` (sealed permit) | 插件声明 BPMN 文件 | CoreManifest 3 个 |
| `WorkflowApproverHelper` (`workflowApprover` bean) | BPMN 表达式取审批人 | 调 ApproverResolver |
| `MyTodoSourcePlugin` (interface) | 注册新待办源 | `MyWorkflowTodoSource` + `MyInspectionTaskTodoSource` |

## 配置(application.yml)

```yaml
flowable:
  database-schema-update: true     # auto create/migrate ACT_* 表
  async-executor-activate: true    # 异步任务执行器
  history-level: audit             # 历史级别 (none/activity/audit/full)
  process-definition-location-prefix: classpath*:/processes/

# Phase 6 — access 关系审批引擎切换
access:
  approval:
    engine: INLINE   # INLINE (默认 pending 表) | FLOWABLE (走流程引擎)
```

## Profile 行为

| Profile | Flowable 状态 | 影响 |
|---|---|---|
| dev | exclude 14 个 autoconfig | controllers 加载但 services NoSuchBeanDefinitionException - **不应跑 mvn spring-boot:run** |
| test | autoconfig 启用 + H2 内存库(application-test.yml) | smoke test 在 sliced context 下跑 |
| prod | 默认从 application.yml | 30+ ACT_* 表创建到主库,生产 ready |

## 现存遗留(可选 Phase 9+)

1. **prod profile 真启动验证未做** — 需要 mvn package + 实跑后端 + 测端点
2. **Inspection 申诉迁到 Flowable** — Phase 6 单点接通了 access 审批,inspection 还走旧 saga(待迁)
3. **bpmn-js 设计器前端 build 验证** — Phase 3 代码 well-formed 但 npm install 因 worktree 副本 node_modules 状态没跑
4. **SLA / timeout 升级** — BPMN timer event 已支持,业务侧未声明
5. **流程版本管理 UI** — 后端 API 完整, 前端只有简单列表

## ADR 索引

- ADR-001: 选 Flowable 7 作为引擎(选型理由)
- ADR-002: 双轨审批引擎(INLINE / FLOWABLE)— 见下文
