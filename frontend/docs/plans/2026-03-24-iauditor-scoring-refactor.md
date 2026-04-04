# iAuditor 风格评分界面重构

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 TaskExecutionView.vue 从双模式（按目标/按分区）重构为 iAuditor 单目标线性流：左侧目标列表 → 右侧逐项打分 → 完成自动跳下一个。

**Architecture:** 删除 section mode 全部代码（~40% 的模板和逻辑），保留并精炼 target mode。左侧导航只有目标列表（搜索+智能筛选+状态过滤），右侧是选中目标的所有评分项按分区分组显示，底栏显示实时得分和"完成并下一个"。

**Tech Stack:** Vue 3 Composition API, Element Plus, TypeScript, Pinia store (`inspExecutionStore`)

**Source:** `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue` (当前 2167 行)
**Mockup:** `frontend/docs/mockups/style-1-iauditor.html`

---

### Task 1: 删除 section mode 相关代码

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: 删除 script 中 section mode 专用状态和函数**

删除以下代码块（保留被两个模式共用的部分）：

- `viewMode` ref 及其 watcher（lines ~66, ~950-969）
- `activeSectionId` ref（line ~49）
- `sectionTargetSearch`, `sectionTargetFilter`, `filteredSectionTargets`（section mode target filtering）
- `rootSection`, `firstLevelSections` computed（保留 `allSections` 因为 target mode 也用）
- `getSectionSubmissions`, `getSectionProgress`, `getSectionIcon` 函数
- `sectionSubmissions` computed（section mode 的 submissions 列表）
- `currentTargetIndex`, `hasPrev`, `hasNext`, `goToPrev`, `goToNext`（section mode navigation）
- `onTargetSelectChange` 函数
- `completedCount`, `progressPercent` computed
- `handleComplete()` — section mode 完成单个 submission
- `handleStartFilling`, `handleLock`, `handleSkip` — section mode submission lifecycle
- `selectedSubmission` ref 及 `selectSubmission` 函数（section mode 选择单个 submission）
- `regularItems`, `personScoreItems`, `violationItems` computed splits
- section mode 相关的 `scoreSummary` 部分（保留 target mode 的 score 计算）

**不删除的（target mode 需要）：**
- `selectedTargetId`, `targetSearch`, `targetFilter`, `targetContextFilter`
- `uniqueTargets`, `filteredTargets`, `targetMetaMap`, `filterDimensions`
- `loadTargetFilterContext()`
- `selectTarget()`, `goToNextTarget()`, `goToPrevTarget()`
- `targetSectionGroups`, `TargetSectionGroup` interface
- `handleCompleteTarget()`
- `isGroupEditable()`
- 所有 scoring handler 函数（`handleDeductionSelect` 等）
- `loadData()`, `reloadAll()`
- `handleStartTask()`, `handleSubmitTask()`

**Step 2: 删除 template 中 section mode 相关部分**

- 删除 mode-toggle 按钮组（`<div class="mode-toggle">`）
- 删除 `<template v-else>` 下的整个 section mode 左侧导航（section selector + target list）
- 删除右侧 `<!-- ============ SECTION MODE ============ -->` 下的所有模板代码（target selector bar, form area, score bar）
- 删除 `v-if="viewMode === 'target'"` 条件（因为现在只有一个模式，不需要条件）

**Step 3: 删除 CSS 中 section mode 专用样式**

- `.mode-toggle` 及其子元素样式
- `.nav-section-select` 及其子元素样式
- `.section-mode-header`, `.section-mode-target`, `.section-mode-meta`
- `.target-chip-bar`, `.chip-scroll`, `.target-chip` 等 chip 相关样式（如果还存在）
- `.target-row`, `.target-select`, `.target-option` 等旧下拉框样式（如果还存在）
- `.nav-prog--done`
- section mode score bar 内不需要的样式

**Step 4: 验证构建**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`
Expected: `✓ built in Xm Xs`

**Step 5: Commit**

```bash
git add frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue
git commit -m "refactor: remove section mode, keep target-only iAuditor flow"
```

---

### Task 2: 重构左侧导航栏（匹配 iAuditor mockup）

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: 重写左侧导航模板**

左侧导航结构改为（参考 mockup `style-1-iauditor.html`）：

```html
<div class="sidebar">
  <div class="sidebar-header">
    <!-- 搜索 -->
    <div class="search-box">
      <el-input v-model="targetSearch" placeholder="搜索目标..." size="small" clearable>
        <template #prefix><Search :size="13" /></template>
      </el-input>
    </div>
    <!-- 智能筛选（仅当有多个分组时显示） -->
    <div v-if="contextFilterOptions.length > 1" class="filter-row">
      <select v-model="targetContextFilter" class="filter-select">
        <option value="">{{ contextFilterLabel }}: 全部</option>
        <option v-for="opt in contextFilterOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
    </div>
    <!-- 状态过滤 -->
    <div class="status-pills">
      <button class="status-pill" :class="{ active: targetFilter === 'all' }" @click="targetFilter = 'all'">
        全部 <span class="pill-count">{{ uniqueTargets.length }}</span>
      </button>
      <button class="status-pill" :class="{ active: targetFilter === 'pending' }" @click="targetFilter = 'pending'">
        待检 <span class="pill-count">{{ pendingCount }}</span>
      </button>
      <button class="status-pill" :class="{ active: targetFilter === 'completed' }" @click="targetFilter = 'completed'">
        已完成 <span class="pill-count">{{ completedTargetCount }}</span>
      </button>
    </div>
  </div>
  <!-- 目标列表 -->
  <div class="target-list">
    <div v-for="t in filteredTargets" :key="t.targetId"
      class="target-item" :class="{ active: selectedTargetId === t.targetId }"
      @click="selectTarget(t.targetId)">
      <span class="target-dot" :class="getTargetDotClass(t.targetId)" />
      <span class="target-name">{{ t.targetName }}</span>
      <span class="target-score">{{ getTargetProgress(t.targetId).done }}/{{ getTargetProgress(t.targetId).total }}</span>
    </div>
    <div v-if="filteredTargets.length === 0" class="target-empty">无匹配目标</div>
  </div>
</div>
```

**Step 2: 添加 pending/completed 计数 computed**

```typescript
const pendingCount = computed(() =>
  uniqueTargets.value.filter(t => getTargetStatus(t.targetId) !== 'COMPLETED').length
)
const completedTargetCount = computed(() =>
  uniqueTargets.value.filter(t => getTargetStatus(t.targetId) === 'COMPLETED').length
)

function getTargetDotClass(targetId: number): string {
  const s = getTargetStatus(targetId)
  return s === 'COMPLETED' ? 'done' : s === 'IN_PROGRESS' ? 'progress' : 'pending'
}
```

**Step 3: 重写左侧导航 CSS**

参考 mockup 的 `.sidebar`, `.sidebar-header`, `.search-box`, `.filter-row`, `.filter-select`, `.status-pills`, `.status-pill`, `.target-list`, `.target-item`, `.target-dot`, `.target-name`, `.target-score` 样式。删除旧的 `.left-nav`, `.nav-item`, `.nav-dot`, `.nav-label`, `.nav-name`, `.nav-prog`, `.nav-list`, `.nav-search`, `.nav-filters`, `.nav-context-filter`, `.nav-empty` 样式。

关键样式参数（来自 mockup）:
- sidebar 宽度: 220px
- target-item: padding 5px 8px, font-size 11px, border-radius 5px
- target-dot: 7px circle
- active 状态: background var(--blue-light)
- status-pill: flex 1, height 26px, rounded

**Step 4: 验证构建**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`
Expected: `✓ built`

**Step 5: Commit**

```bash
git add frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue
git commit -m "refactor: sidebar matches iAuditor mockup style"
```

---

### Task 3: 重构右侧评分面板（匹配 iAuditor mockup）

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: 重写右侧面板头部**

```html
<div class="panel-header">
  <div class="panel-header-info">
    <div class="panel-target-name">{{ currentTargetName }}</div>
    <div class="panel-progress">
      <span>{{ targetItemProgress.done }}/{{ targetItemProgress.total }} 已评</span>
      <div class="progress-bar-wrap">
        <div class="progress-bar-fill" :style="{ width: targetProgressPercent + '%' }" />
      </div>
      <span class="progress-pct">{{ targetProgressPercent }}%</span>
    </div>
  </div>
  <button class="nav-btn" :disabled="!hasPrevTarget" @click="goToPrevTarget">
    <ChevronLeft :size="16" />
  </button>
  <button class="nav-btn" :disabled="!hasNextTarget" @click="goToNextTarget">
    <ChevronRight :size="16" />
  </button>
</div>
```

需要添加 computed：

```typescript
const currentTargetName = computed(() => {
  const t = uniqueTargets.value.find(t => t.targetId === selectedTargetId.value)
  return t?.targetName ?? ''
})

const targetItemProgress = computed(() => {
  let total = 0, done = 0
  for (const g of targetSectionGroups.value) {
    total += g.details.length
    done += g.details.filter(d => numberInputs.value[d.id] !== undefined || selectInputs.value[d.id] !== undefined || textInputs.value[d.id] !== undefined).length
  }
  return { done, total }
})

const targetProgressPercent = computed(() => {
  const { done, total } = targetItemProgress.value
  return total > 0 ? Math.round((done / total) * 100) : 0
})
```

**Step 2: 重写评分项区域（section cards + card grid）**

保持现有的 `targetSectionGroups` v-for 结构，但调整样式匹配 mockup：

- Section header: 白色背景卡片，有 chevron + section name + meta (item count + progress mini bar)
- Section body: grid 布局 `repeat(auto-fill, minmax(240px, 1fr))`
- Field card: 白色边框卡片，已评分的左边有蓝色 border-left
  - card-top: name + type tag
  - card-control: 评分控件
  - card-note: 备注（可选）

**Step 3: 重写底部 score bar**

```html
<div class="bottom-bar" v-if="selectedTargetId">
  <div class="bottom-info">
    目标 <strong>{{ currentTargetIdx + 1 }}</strong> / {{ filteredTargets.length }}
    · 分区 <strong>{{ completedSectionCount }}</strong>/{{ targetSectionGroups.length }} 已完成
  </div>
  <div class="bottom-score">
    <div class="score-display">
      <div class="score-label">当前得分</div>
      <div class="score-value">{{ targetScore }}</div>
    </div>
    <el-button type="primary" size="default"
      :disabled="getTargetStatus(selectedTargetId) === 'COMPLETED'"
      @click="handleCompleteTarget">
      完成并下一个 <ChevronRight :size="14" />
    </el-button>
  </div>
</div>
```

**Step 4: 重写右侧面板 CSS**

参考 mockup 样式：
- `.panel-header`: white bg, border-bottom, flex, padding 12px 20px
- `.panel-target-name`: 15px, font-weight 600
- `.panel-progress`: flex, gap 8px, progress-bar-wrap 120px
- `.section`: margin-bottom 16px
- `.section-header`: white bg, border, rounded, clickable, flex with chevron + title + meta
- `.section-body`: grid auto-fill minmax(240px, 1fr), gap 8px
- `.score-card`: white bg, border, rounded, padding 10px 12px, hover shadow
- `.score-card.scored`: border-left 3px solid var(--blue)
- `.bottom-bar`: fixed at bottom of main panel, white bg, border-top

**Step 5: 验证构建**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`

**Step 6: Commit**

```bash
git add frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue
git commit -m "refactor: right panel matches iAuditor mockup style"
```

---

### Task 4: 评分控件统一（匹配 mockup 风格）

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: DEDUCTION/ADDITION 使用 stepper 控件**

当前是 `el-input-number`。改为自定义 stepper 匹配 mockup 的 `−/value/+` 样式：

```html
<div v-if="detail.scoringMode === 'DEDUCTION'" class="card-control">
  <div class="stepper">
    <button class="stepper-btn" :disabled="!isGroupEditable(group)"
      @click="handleDeductionStep(detail, -1, group)">−</button>
    <span class="stepper-value" :class="{ negative: (numberInputs[detail.id] ?? 0) < 0 }">
      {{ numberInputs[detail.id] ?? 0 }}
    </span>
    <button class="stepper-btn" :disabled="!isGroupEditable(group)"
      @click="handleDeductionStep(detail, 1, group)">+</button>
  </div>
</div>
```

添加 handler：

```typescript
function handleDeductionStep(detail: SubmissionDetail, direction: number, group: TargetSectionGroup) {
  const range = getDeductionRange(detail)
  const current = numberInputs.value[detail.id] ?? 0
  const next = Math.max(range.min, Math.min(0, current + direction * range.step))
  if (next !== current) handleDeductionSelect(detail, next)
}
```

类似地处理 ADDITION 和 CUMULATIVE。

**Step 2: PASS_FAIL 使用 pill-group 样式**

匹配 mockup 的连体按钮组：

```html
<div v-else-if="detail.scoringMode === 'PASS_FAIL'" class="card-control">
  <div class="pill-group">
    <button class="pill-btn" :class="{ 'active-green': selectInputs[detail.id] === 'PASS' }"
      :disabled="!isGroupEditable(group)" @click="handlePassFail(detail, 'PASS')">✓ 通过</button>
    <button class="pill-btn" :class="{ 'active-red': selectInputs[detail.id] === 'FAIL' }"
      :disabled="!isGroupEditable(group)" @click="handlePassFail(detail, 'FAIL')">✗ 不通过</button>
  </div>
</div>
```

**Step 3: LEVEL/GRADE 使用 pill-group**

```html
<div v-else-if="..." class="card-control">
  <div class="pill-group">
    <button v-for="lv in getGradeLevels(detail)" :key="lv.label"
      class="pill-btn" :class="{ 'active-blue': selectInputs[detail.id] === lv.label }"
      :disabled="!isGroupEditable(group)" @click="handleGradeSelect(detail, lv.label, lv.score)">
      {{ lv.label }}
    </button>
  </div>
</div>
```

**Step 4: 评分控件 CSS**

参考 mockup 的 `.stepper`, `.stepper-btn`, `.stepper-value`, `.pill-group`, `.pill-btn` 等样式。删除旧的 `.score-btn`, `.pf-btn`, `.grade-btn`, `.counter-btn` 等样式。

**Step 5: 验证构建 + Commit**

---

### Task 5: 清理和优化

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: 删除所有未使用的 imports**

- 删除不再使用的 lucide 图标（如 `ListTree`, `Target` 如果不再用）
- 删除 section mode 相关的未使用 import

**Step 2: 删除所有未使用的 CSS**

搜索模板中未引用的 CSS class，全部删除。目标：文件从 ~2167 行压缩到 ~1200-1400 行。

**Step 3: 最终构建验证**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`

**Step 4: Commit**

```bash
git add frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue
git commit -m "refactor: cleanup unused code, iAuditor-style scoring complete"
```

---

## 验收标准

1. **无双模式切换** — 页面只有一种模式：选目标 → 打分 → 下一个
2. **左侧** — 搜索 + 智能筛选(根据目标类型) + 状态过滤 + 垂直目标列表
3. **右侧** — 目标名+进度条 → 分区折叠面板 → 卡片网格评分项
4. **评分控件** — stepper(扣分/加分/累计) + pill-group(通过/等级) + star(评分量表) + input(直接打分/采集)
5. **底栏** — 进度 + 实时得分 + "完成并下一个"按钮
6. **自动跳转** — 完成当前目标后自动选中下一个待检目标
7. **构建通过** — `vite build` 零错误
