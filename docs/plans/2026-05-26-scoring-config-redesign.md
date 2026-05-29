# 评分配置 重设计 (4 层方案)

**日期**: 2026-05-26
**触发**: 用户 "评分方案配置 UI 不好,优化,并且评分方案为什么支持多个方案?"
**结论**: "多方案" 是认知错觉 — 实为按 template section 切片; 用 4 层重设计消除错觉并解锁 UX

---

## 现状与问题

### 真相 — 不是"多方案",是"按 section 切片"

DB 层 `(projectId, sectionId)` 是事实上的唯一键:

- `ScoringProfileApplicationService.createProfile` 调 `findByProjectIdAndSectionId` 幂等返回 (line 58)
- `CreateProfileRequest` 强制 `projectId` + `sectionId` 都 `@NotNull` (controller line 283-286)
- 编辑器路由 `?templateId={sectionId}&projectId={projectId}` idempotent load-or-create
- `ScoreAggregationService.resolveScoringProfileId(projectId, sectionId)` 是唯一解析路径

**所以**: 一个项目 N 个 section, 每 (project,section) 一套 profile, **不能任意"新建多个"**.

### 用户痛点 4 条

1. **认知错觉**: Tab 标题 "评分方案" + "新建评分方案" 按钮 + "查看全部" 让用户以为可任意创建并列方案. 实际是 section 维度切片
2. **编辑器孤岛**: 编辑一个 section 时, 看不到本项目其他 section 的存在 / 状态 / 配置
3. **信息密度过载**: 单 profile 编辑器 6 个区块 (基础+维度+规则+高级+健康+版本+模拟器), scroll fatigue
4. **缺合并视角**: 概念图说 "→ 高级调整 = 最终分" 但实际后面还有 "跨 section Indicator 聚合 → 项目评级", 用户不知道这层链路

### 跨 section 聚合已存在

`IndicatorEvaluationService` + `Indicator` 模型 (10+ 文件) 已做跨 section 聚合, 走「评级」Tab 的 Indicator 配置 (`ScoreAggregationService.java:84-87` 注释明确说项目级评级由 Indicator 负责). **不需要新建 ScoringMergePolicy** — 该需求已有归宿.

---

## 重设计 4 层

### L1 — 概念重塑 (必做, 0.5 天)

**ProjectDetailView 「评分方案」Tab**:
- 标题 "评分方案" → "**评分配置 · 按章节**"
- 顶部说明句: 「本项目模板有 N 章, 每章独立评分规则; 项目总分汇总走「评级」Tab 的 Indicator 配置」
- 列表 → **section 卡片网格** (2-3 列响应式)
  - 每卡: 章节名 / 题数 / 规则模式摘要 (扣分制/一票否决/分级) / 状态点 ● 已配置 / ○ 未配置 / ⚠ 有问题
- 删除 "新建评分方案" 按钮 (无意义 — section 决定数量), 改为 "未配置的章节会自动创建"
- 删除原有「设置」Tab 入口残留 (已 P2 移除)

**改动**: `ProjectDetailView.vue` 评分方案 Tab 内容块; 新增 `ScoringSectionCard.vue`.

### L2 — 编辑器 section 切换栏 (核心, 1 天)

**ScoringProfileEditor 顶部**:
- 加 section tabs 切换栏 (类似 VS Code editor tabs)
- 列出本项目所有 section, 当前编辑高亮 + 状态点
- 切换不离开编辑器 (内部 router 不变, 仅切换 currentSection)
- dirty 状态切换前提示 "未保存修改, 离开/保存/取消"

**改动**: `ScoringProfileEditor.vue` 顶部新增 `<SectionTabsBar>`; 新增 `loadProjectSections()` API/store action.

### L3 — 编辑器手风琴 + 头部状态摘要 (体验, 1 天)

4 大块默认状态:
- 1 基础设置: 展开 (必填)
- 2 评分维度: 展开 (必填)
- 3 计算规则链: 折叠 (可选)
- 4 高级算法: 折叠 (可选)

每块头部带状态摘要 (折叠时也能看):
- `基础设置 [100/0 分 · 精度 2 ✓]`
- `评分维度 [3 子项 · 权重合 100% ✓]`
- `计算规则链 [2 条 · 1 已启用]`
- `高级算法 [全关] / [趋势+衰减开]`

健康检查融入每块头部, 删除右侧 sidebar 单独健康卡.

**改动**: 改 `ScoringProfileEditor.vue` 左列结构; 抽 `<AccordionSection>` 通用组件 (可复用 inspection 其他地方).

### L4 — 概念图升级 + 实时算分预览 (锦上, 1 天)

**概念图升级 (`ConceptDiagram.vue` 改)**:
- 加第 5 步: `→ [章节合并] → [项目评级]`
- 点 "章节合并" 跳到「评级」Tab 的 Indicator 配置, 与 L1 顶部说明句呼应

**右侧 sticky 重构**:
- 删 `ScoreSimulator.vue` (567 行, 已重复)
- 删独立健康检查卡 (融到 L3 手风琴头部)
- 改 `<RealtimePreview>`: 一道虚拟题, 用户输入分数实时算出经过配置的最终分; 调试配置直观
- 保留 `<VersionHistory>`

**改动**: `ConceptDiagram.vue` 加第 5 步; 新建 `<RealtimePreview>`; 删 `ScoreSimulator.vue` 引用.

---

## 影响范围

| 文件 | L1 | L2 | L3 | L4 |
|---|---|---|---|---|
| `ProjectDetailView.vue` | ✓ |  |  |  |
| `ScoringSectionCard.vue` (新) | ✓ |  |  |  |
| `ScoringProfileEditor.vue` |  | ✓ | ✓ | ✓ |
| `SectionTabsBar.vue` (新) |  | ✓ |  |  |
| `AccordionSection.vue` (新, 可复用) |  |  | ✓ |  |
| `ConceptDiagram.vue` |  |  |  | ✓ |
| `RealtimePreview.vue` (新) |  |  |  | ✓ |
| `ScoreSimulator.vue` (删) |  |  |  | ✓ |
| `inspScoringStore.ts` (加 loadProjectSections) |  | ✓ |  |  |
| `scoring.ts` API (`/sections` endpoint?) |  | ✓ |  |  |

**后端**: 无新功能. L2 可能加 `GET /scoring-profiles/project/{pid}/sections` 拿 section 列表 + 各 profile 状态 (但 `TemplateSection` 已有 API, 前端组装即可).

---

## 分期实施序列

按 task ID:
- **L1 (#73)** — 项目详情 Tab 改名 + 卡片网格 + 删按钮误导 . 不依赖其他.
- **L2 (#74)** — 编辑器加 section tabs. 依赖 L1 (用户从卡片点进编辑器需要 currentSection 上下文).
- **L3 (#75)** — 手风琴. 独立, 可在 L2 后任意时机做.
- **L4 (#76)** — 概念图 + 实时预览. 独立, 收尾.

**L5 已撤销** — 跨 section 聚合归属「评级」Tab 的 Indicator, 不在评分配置范围.

---

## 不做的事

- 不改 DB schema (sectionId 仍 nullable, projectId NOT NULL 已对)
- 不改后端 ScoreAggregationService / IndicatorEvaluationService
- 不动「评级」Tab (是另一个领域)
- 不引入"预设三档" (已被用户否决 — P3b 经验)
- 不做"分步引导 wizard" (P3c — 改动业务流程太大)

---

## 关键风险

1. **L2 dirty 状态切换** — 用户在 section A 编辑未保存就点 section B, 必须有清晰提示, 否则丢配置
2. **L4 实时预览** — 必须只在前端纯函数计算, 不打后端 API 否则交互卡顿
3. **章节命名虚弱** — section 是否都有清晰名字? 测一下空 name / 重名场景

---

## Phase 3 待办 (本次不做)

P3b/P3c 永久搁置:
- 预设三档 (用户否决)
- 分步引导 wizard (改动业务流程太大)
