# ADR-002: 双轨审批引擎(INLINE 默认 / FLOWABLE 可选)

**Status**: Accepted (2026-05-10)
**Phase**: 工作流引擎 Phase 6

## Context

Phase 1-5 引入 Flowable 引擎,但既有 access 域已用 raw `pending_relation_approvals` 表 + `RelationApprovalService`(P5 W5.2 / P7 W7.1)做了简化版审批。

直接删旧路径迁 Flowable 风险大:
- 既有数据迁移
- 测试覆盖断裂
- 部署需要 Flowable 启动(dev/test profile 当前是 exclude)

## Decision

**双轨并存,通过 application.yml `access.approval.engine` 切换:**

| 模式 | 路径 | 何时用 |
|---|---|---|
| `INLINE`(默认) | RelationApprovalService → pending_relation_approvals 表 | dev/test/不需要复杂流程的生产环境 |
| `FLOWABLE` | RelationApprovalWorkflowService → BPMN 流程 | 需要审批链路、超时、撤回、可视化追踪的生产环境 |

由于 `RelationApprovalWorkflowService` 标 `@ConditionalOnBean(RuntimeService.class)`,**dev profile 不存在该 bean,自然降级到 INLINE**,无需手动改配置。

## 后续迁移路径

按业务节奏:
1. 确认 prod profile 启动 Flowable 表正常创建(Phase 9 真启动验证)
2. 设 `access.approval.engine: FLOWABLE` 切到流程引擎
3. 渐进迁移其他业务(inspection 申诉 / 教务调课 ...) — 各自 PR

## 风险

- INLINE 与 FLOWABLE 行为有微差(INLINE 直接落库;FLOWABLE 多个流程节点)— 业务方需理解
- 切换 engine 模式后,运行中的 INLINE pending 不会自动迁到 Flowable(需手动转移或保留双轨观察期)

## 兜底

`AccessRelationService.grant()` 在 FLOWABLE 模式下也会判 `relationApprovalWorkflowService != null`,bean 不存在时降级 INLINE。永远不抛 NPE。
