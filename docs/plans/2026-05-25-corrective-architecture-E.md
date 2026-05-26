# 整改架构 E — Separation of Concerns 终极重构

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把整改规则从"题目模板属性"提升到"项目业务策略", 实现模板纯粹 (跨项目复用) + 项目级按题型可控 + 个例可覆盖。

**Architecture:** 双表存储 — `insp_project_corrective_rules` (按 scoring_mode) + `insp_project_item_overrides` (按 template_item_id). 引擎查询优先级: 个例 > 按题型 > 兜底. 题目模板字段 `corrective_override` 退化为"创建项目时的预设建议", 不再被引擎直接消费.

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus + MySQL + Vue 3

---

## 数据模型

### 新表 1 — `insp_project_corrective_rules` (按题型批量规则)
```sql
CREATE TABLE insp_project_corrective_rules (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    scoring_mode VARCHAR(50) NOT NULL,  -- PASS_FAIL / RATING_SCALE / ...
    rule_json JSON NOT NULL,            -- {criticality, neverCorrect, baseSeverityMap, singleThreshold, deadlineOverrideDays}
    tenant_id BIGINT DEFAULT 1,
    org_unit_id BIGINT,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME, updated_at DATETIME, created_by BIGINT, updated_by BIGINT,
    UNIQUE KEY uk_project_mode (project_id, scoring_mode, deleted)
);
```

### 新表 2 — `insp_project_item_overrides` (按题目个例覆盖)
```sql
CREATE TABLE insp_project_item_overrides (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    template_item_id BIGINT NOT NULL,
    rule_json JSON NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    org_unit_id BIGINT,
    deleted TINYINT DEFAULT 0,
    created_at DATETIME, updated_at DATETIME, created_by BIGINT, updated_by BIGINT,
    UNIQUE KEY uk_project_item (project_id, template_item_id, deleted)
);
```

### 保留字段 (向下兼容)
- `insp_template_items.corrective_override` — 不再被引擎读取, 但保留作"创建项目时的预设"
- `insp_projects.corrective_strictness` 等老字段 — 保留, 引擎不再读

---

## 引擎逻辑

```
对每道 SubmissionDetail:
  1. 加载项目 enabled? 关 → NONE
  2. 加载题目个例 (insp_project_item_overrides by project + template_item_id)
     - 有 → 用个例规则 → 判定
  3. 加载按题型规则 (insp_project_corrective_rules by project + scoring_mode)
     - 有 → 用按题型规则 → 判定
  4. 兜底: 智能默认 (按 scoring_mode 选 sensible default — 同前端 smart suggestion 逻辑)
  5. autoCreateLevel 决定自动 vs 候选
  6. deadline: 个例 > 按题型 > 项目 default presets
```

---

## API 设计

### 项目按题型规则
```
GET    /inspection/corrective/projects/{projectId}/by-mode     → List of {mode, rule}
PUT    /inspection/corrective/projects/{projectId}/by-mode/{mode}  → 覆盖单个 mode 规则
DELETE /inspection/corrective/projects/{projectId}/by-mode/{mode}  → 清除单个 mode (回到智能默认)
```

### 项目题目覆盖
```
GET    /inspection/corrective/projects/{projectId}/item-overrides     → List of {templateItemId, rule}
PUT    /inspection/corrective/projects/{projectId}/item-overrides/{itemId} → 覆盖
DELETE /inspection/corrective/projects/{projectId}/item-overrides/{itemId} → 清除
```

### Simulate API (已存在)
- 现有 simulate 接口扩展: 接受 projectId + templateItemId, 后端用真实引擎链查询规则.

---

## 前端 UI

### 模板编辑器 (CorrectiveOverrideEditor 删除)
- ItemEditor 不再渲染整改面板
- 模板纯粹关于"题目结构"

### 项目设置 (新建大节"整改策略")
```
整改策略
├─ 总开关 (Switch)
├─ 自动建单门槛 (select: 不自动 / 严重 / 中度 / 轻微)
├─ 默认完成时限 (3 个 input: 严重/中度/轻微 各 N 天)
├─ ─── Tab: 按题型规则 ───
│   └─ 列出本项目用到的 scoring_mode (从模板抽出)
│       每行: 复用现有 mode-specific UI (binary / threshold / discrete / risk matrix)
├─ ─── Tab: 题目覆盖 (可选, 默认折叠) ───
│   └─ 列出本项目所有题
│       每行: [题名] [题型] [当前规则: 走题型默认 / 自定义] [配置]
│       点配置 → 展开题目个例规则编辑器
└─ ─── Tab: 模拟器 ───
    └─ 选题型 + 选某题 + 假设响应 → 引擎判定 + trace
```

---

## 任务分解

### Phase 1 — Backend 数据层
- P1.1 DB migration: 新建 2 张表 (V20260525_1__corrective_arch_e.sql)
- P1.2 PO/Mapper/Repository: 2 个新仓储
- P1.3 引擎重构: CorrectionEngine 的 itemRule 加载链改用新表

### Phase 2 — Backend API
- P2.1 ProjectCorrectiveRulesController: 按题型 CRUD
- P2.2 ProjectItemOverridesController: 题目覆盖 CRUD
- P2.3 删除老 ItemRule 加载路径 (template_items.corrective_override 不再被引擎读)

### Phase 3 — Frontend 项目设置大节
- P3.1 创建 ProjectCorrectiveStrategy.vue (大卡片)
- P3.2 复用 mode-specific 输入 UI (从 CorrectiveOverrideEditor 拆出)
- P3.3 按题型 Tab + 题目覆盖 Tab + 模拟器 Tab

### Phase 4 — Frontend 模板清理
- P4.1 ItemEditor 删除 CorrectiveOverrideEditor 引用
- P4.2 删除 ProjectDetailView 原 sev 阈值卡

### Phase 5 — 验证
- P5.1 Backend compile + Engine 单测
- P5.2 Frontend type-check
- P5.3 浏览器端到端: 创建项目 → 按题型配 → 题目覆盖 → 模拟验证

---

## 数据迁移策略

无破坏性迁移:
- 老数据 (`insp_template_items.corrective_override`) 保留, 不删
- 新引擎: 优先查新表; 新表无数据时不再 fallback 到老 corrective_override (强制项目级配置, 否则走兜底智能默认)
- 老项目首次进入新版整改设置: UI 自动检测"项目尚未配置任何规则", 弹出"是否按模板原整改规则自动初始化?" — 一键导入到 `insp_project_corrective_rules`

---

## 工作量估算

| 阶段 | 工作量 |
|---|---|
| P1 数据层 | 1.5 天 |
| P2 API | 1 天 |
| P3 项目级 UI (核心工作量) | 3 天 |
| P4 清理 | 0.5 天 |
| P5 验证 | 0.5 天 |
| **总** | **~6.5 天** |
