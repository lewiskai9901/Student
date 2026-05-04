# ADR-004: V108 检查任务多类型解耦

**Status**: Accepted
**Date**: 2026-05-04
**Context**: V108 — `database/schema/V108.0.0__inspection_task_type.sql`

## 背景

原有检查任务模型假设了"计划性检查"是唯一形态:

- `Task.task_date` 必填 (调度生成)
- `Task.deadline` 严格 (逾期硬扣检查员 KPI)
- 任务必由 `InspectionPlan` 自动批量生成
- 没有 type 字段区分来源

**业务现实是**至少 6 种检查行为, 各有独立语义:

| 类型 | 触发方式 | deadline 语义 | KPI 影响 |
|---|---|---|---|
| 计划检查 (SCHEDULED) | 调度自动 | 严格逾期 | 计入完成率 |
| 临时抽查 (AD_HOC) | 检查员手动 | 永不逾期 | 主动性指标 |
| 事件触发 (TRIGGERED) | 申诉/告警 listener | 软逾期 (跟随源单 SLA) | 响应时长 |
| 自查 (SELF_CHECK) | 受检主体本人 | 永不逾期 | 参与度 |
| 投诉核查 (COMPLAINT) | 投诉单触发 | 软逾期 | (复用 TRIGGERED) |
| 互查 (CROSS_AUDIT) | 检查员互检 | 严格逾期 | 审计独立性 |

把这 6 种全塞进"计划任务"模型 → 数据语义错位 + KPI 互相污染 + 用户无法表达真实业务流.

## 决策

**三层抽象解耦**:

### 层 1: `Project.inspection_mode` — 项目级运行模型

```
PLANNED       — 仅按计划生成 (现有默认, 100% 向后兼容)
HYBRID        — 计划 + 临时抽查并存 (推荐生产配置)
SPOT_CHECK    — 完全靠抽查, 不生成计划任务
SELF_AUDIT    — 受检主体自评制
EMERGENCY     — 一次性突击专项
```

### 层 2: `Task.task_type` — 任务级来源标签

`SCHEDULED / AD_HOC / TRIGGERED / SELF_CHECK / COMPLAINT / CROSS_AUDIT`

每种类型独立:
- 工厂方法 (`InspTask.createAdHoc`, `createTriggered` ...)
- 默认 `DeadlinePolicy` (`STRICT / RELAXED / NONE`)
- 状态机起点 (AD_HOC 创建即 CLAIMED, SCHEDULED 创建即 PENDING)
- KPI 路由 (只 SCHEDULED 计入计划完成率)

### 层 3: `Task.source` value object — 触发溯源

```java
record TaskSource(
    String sourceType,   // SCHEDULER / MANUAL / EVENT / IMPORT
    Long actorId,        // 触发用户
    String reason,       // 必填说明
    String refType,      // Appeal/Alert/Complaint
    Long refId           // 源单据 ID
)
```

DB 字段: `source_type / source_actor_id / source_reason / source_ref_type / source_ref_id` + `idx_source_ref` 索引.

## 实施 (Day 1-8 共 8 commits)

| Day | 内容 | 测试 |
|---|---|---|
| 1 | DB 迁移 + 领域 enum + AD_HOC 工厂 + REST API | DB+Java 编译 |
| 2 | 任务列表 ⚡抽查 tab + 发起对话框 | 浏览器端到端 |
| 3 | 项目模式配置 UI + GET/PUT API | **7 e2e** |
| 4 | submit() 按 deadlinePolicy 路由 + isOverdue 重构 | **14 单测** |
| 5-6 | TRIGGERED + AppealApprovedEvent listener | 真实事件链 |
| 7 | SELF_CHECK + CROSS_AUDIT 工厂/API | **+3 单测** |
| 8 | 治理面板 KPI 5 维度 + ADR 本文 | 端到端可视化 |

**累计 24+ 测试全过** + 4 类型真实端到端创建验证.

## 替代方案及拒绝原因

### 方案 A (拒绝): 加 `taskType` 字段, 不动模型其余部分

太轻 — 没有 inspection_mode 就允许所有项目都能发抽查, 容易混乱; 没有 source value object 就丢了溯源能力.

### 方案 C (拒绝): SpotCheck 独立聚合根

太重 — Project / Plan / Score / Analysis 全得双套实现, 工程量翻倍且数据双轨.

### 方案 B (采纳): 三层抽象解耦 (本 ADR)

- Project mode 决定能做什么
- Task type 决定是什么
- Task source 决定怎么来的

刚好平衡了: 模型变更最小 (字段加 + 工厂方法加) + 业务表达最完整.

## 不变量 (回归守护)

1. **现有 task 默认 SCHEDULED + STRICT** — 行为不变
2. **AD_HOC/SELF_CHECK 永不逾期** — 14 单测覆盖
3. **TRIGGERED 软逾期** — 不影响检查员 KPI
4. **PLANNED 项目禁止 allow_ad_hoc** — 后端硬约束 (jdbcTemplate update 强制重置)
5. **TRIGGERED 防重** — 同 (refType, refId) 已有 task 则跳过, 避免一个事件被反复触发
6. **CROSS_AUDIT 必填 dueDate** — 互查有 SLA 不能无限拖

## 后续

- Day 8+ 可选: COMPLAINT listener (复用 TRIGGERED 路径, 仅 refType='Complaint')
- 抽查月度配额硬约束 (现 DB 字段已加, 业务校验暂留前端)
- KPI 加权分: `final_score_weighted = score × (1.0 if SCHEDULED else 1.2 if AD_HOC else ...)`

## 关键 commit

- DB+领域: V108.0.0 / TaskType.java / DeadlinePolicy.java / TaskSource.java / InspTask.java
- 监听器: TriggeredTaskAutoCreateListener.java
- 测试: InspTaskOverdueTest.java (17 单测), inspection-task-types.spec.ts (7 e2e)
- ADR: 本文
