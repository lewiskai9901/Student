# ADR-001: 选 Flowable 7 作为工作流引擎

**Status**: Accepted (2026-05-09)
**Phase**: 工作流引擎 Phase 1

## Context

系统需要"插件可复用的工作流框架"——任何插件能定义自己的流程(请假、报销、调课审批、检查整改审批),不需重复造轮子。

## Decision

选 **Flowable 7.0.1**:
- BPMN 2.0 标准,业界承认 (vs Activiti 已被 Flowable fork)
- Spring Boot 3 兼容
- 中国团队维护,中文文档丰富 (vs Camunda 7 license 商业不友好,Camunda 8 生态偏国际化)
- LGPL 商用友好
- 30+ ACT_* 表自动管理,无需自写 schema

pom 已加,application.yml 已配,但开发期不启用 (dev profile exclude 14 个 autoconfig).

## 启用策略

| Profile | Flowable 状态 | DB |
|---|---|---|
| dev (默认) | 全 exclude | student_management 主库 (不创 ACT 表) |
| test | 启用 + create-drop | H2 内存库 |
| prod | 启用 (从 application.yml 继承) | student_management 主库 (创 ACT 表) |

## 后续 Phase 路线

- Phase 1 (本期): smoke test 验证引擎工作
- Phase 2: REST 端点 (deploy/instance/task/history)
- Phase 3: bpmn.js 前端设计器
- Phase 4: ApproverFinderPlugin SPI 接通(自动审批人路由)
- Phase 5: WorkflowContribution Plugin SPI(各插件可声明自己的 BPMN 文件)
- Phase 6: 现有审批迁到 Flowable (access RelationApproval / inspection 申诉)
- Phase 7: 统一"我的待办" 中心
- Phase 8: ArchUnit 守护 + 文档收官

## 已知风险

- ACT_* 表多: 30+ 张,数据库观察起来"看上去乱"。文档要清楚标识它们是引擎自管,业务不直接 SELECT
- 流程一旦设计稳定就难改: BPMN 改了流程实例处于运行中的话,需要迁移. Flowable 提供 `runtimeService.activateProcessInstanceById` 等 API
- 团队 BPMN 学习曲线: ~1 周自学,主要是控制流概念 (gateway/parallel/exclusive)
