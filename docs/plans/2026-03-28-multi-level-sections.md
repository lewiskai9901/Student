# 多层级分区支持方案

> **日期**: 2026-03-28
> **目标**: 打分界面、检查配置、评价指标全面支持多层级分区树

---

## 1. 问题现状

模板分区树支持任意层级嵌套（后端已实现），但前端三个核心页面只处理了一层：

```
当前支持:  根分区 → 一级分区（卫生、安全、纪律）
实际需求:  根分区 → 中间分区 → 叶子分区 → 检查项

示例:
  全面检查（根）
  ├── 安全检查（中间层，可独立评级）
  │   ├── 教室安全（叶子，包含检查项）
  │   └── 实验室安全（叶子，包含检查项）
  ├── 卫生检查（中间层，可独立评级）
  │   ├── 宿舍卫生（叶子）
  │   └── 教室卫生（叶子）
  └── 纪律检查（叶子，直接包含检查项，无子分区）
```

## 2. 核心概念

| 分区类型 | 特征 | 打分 | 评价 |
|---------|------|------|------|
| **叶子分区** | 无子分区，包含检查项 | 检查员直接打分 | 叶子指标，取原始分 |
| **中间分区** | 有子分区，不直接包含检查项 | 折叠分组标题 | 复合指标，汇总子指标，可独立映射等级 |
| **根分区** | 模板根节点 | 整个任务 | 顶层复合指标 |

判断规则：
- `section.children.length > 0` → 中间分区
- `section.children.length === 0` → 叶子分区
- `section.parentSectionId === null` → 根分区

## 3. 受影响的组件

### 3.1 打分界面（TaskExecutionView.vue）

**当前**：submissions 按 sectionId 扁平分组
```
▼ 卫生检查 ─── 5项 3/5已评
  地面清洁 [通过/不通过]
  桌椅摆放 [扣分]
▼ 纪律检查 ─── 5项 5/5已评
  出勤率 [直接打分]
```

**改为**：递归嵌套渲染
```
▼ 安全检查 ──────────────── 2个子分区
  ▼ 教室安全 ─── 3项 2/3已评
    消防器材 [通过/不通过]
    电路安全 [扣分]
    应急通道 [通过/不通过]
  ▼ 实验室安全 ─── 2项 0/2已评
    化学品存放 [等级评分]
    防护设备 [通过/不通过]
▼ 卫生检查 ──────────────── 2个子分区
  ▼ 宿舍卫生 ─── 3项 1/3已评
    地面清洁 [通过/不通过]
    ...
▼ 纪律检查 ─── 5项 5/5已评（叶子分区，直接显示检查项）
  出勤率 [直接打分]
  ...
```

**实现方式**：
1. 加载分区树（已有 `getSections(rootId)` API 返回扁平列表，前端构建树）
2. submissions 按 sectionId 挂到对应叶子节点
3. 用递归组件渲染：
   - 中间分区 → 可折叠分组标题，显示子分区进度汇总
   - 叶子分区 → 展示检查项和打分控件（现有逻辑不变）

**代码改动**：
- 新增 `buildSectionTree(sections, submissions)` 工具函数
- 修改渲染逻辑为递归（或抽取 `SectionGroup.vue` 递归组件）
- 中间分区标题样式与叶子分区区分（缩进+背景色差异）

### 3.2 检查配置（SectionConfigView.vue）

**当前**：`props.sections` 只传一级分区，扁平卡片列表

**改为**：传完整分区树，缩进展示

```
分区评价配置  6个分区

├─ 安全检查（中间）           [配置评价]
│  ├─ 教室安全               [配置评价]
│  └─ 实验室安全             [配置评价]
├─ 卫生检查（中间）           [配置评价]
│  ├─ 宿舍卫生               [配置评价]
│  └─ 教室卫生               [配置评价]
└─ 纪律检查                  [配置评价]
```

**实现方式**：
1. ProjectDetailView 传 `sectionTree`（树结构）而非 `sectionList`（扁平）
2. SectionConfigView 递归渲染分区卡片，带缩进
3. 中间分区的评价默认为"复合指标"，叶子分区默认为"叶子指标"
4. 配置评价对话框根据分区类型（中间/叶子）显示不同字段：
   - 叶子分区：评估周期、多次合并、等级方案、归一化
   - 中间分区：评估周期、汇总方式（AVG/SUM/MAX/MIN）、缺失策略、等级方案

**代码改动**：
- ProjectDetailView 构建树结构传入
- SectionConfigView 递归渲染
- 配置评价对话框增加"中间分区"模式

### 3.3 检查计划关联

**当前**：计划的 `sectionIds` 只关联一级分区

**改为**：允许关联任意层级分区
- 选中间分区 = 覆盖其所有子分区
- 选叶子分区 = 只覆盖该分区
- UI 显示分区树结构的 checkbox

### 3.4 指标创建逻辑

**当前**：`createLeafIndicator` 只引用一级分区

**改为**：
- 叶子分区 → 创建叶子指标（sourceSectionId = 叶子分区ID）
- 中间分区 → 创建复合指标（parentIndicatorId 链接子指标）
- 保存评价时自动判断分区类型，调对应 API

## 4. 数据流

### 4.1 分区树构建

```typescript
interface SectionTreeNode {
  id: number
  sectionName: string
  targetType?: string
  parentSectionId: number | null
  children: SectionTreeNode[]
  isLeaf: boolean  // children.length === 0
}

function buildTree(sections: TemplateSection[], rootId: number): SectionTreeNode[] {
  const map = new Map<number, SectionTreeNode>()
  for (const s of sections) {
    map.set(Number(s.id), { ...s, children: [], isLeaf: true })
  }
  const roots: SectionTreeNode[] = []
  for (const node of map.values()) {
    if (Number(node.parentSectionId) === rootId) {
      roots.push(node)
    } else {
      const parent = map.get(Number(node.parentSectionId))
      if (parent) {
        parent.children.push(node)
        parent.isLeaf = false
      }
    }
  }
  return roots.sort((a, b) => a.sortOrder - b.sortOrder)
}
```

### 4.2 打分界面数据结构

```typescript
interface ScoringSection {
  section: SectionTreeNode
  submission?: InspSubmission  // 叶子分区才有
  details?: SubmissionDetail[] // 叶子分区才有
  children: ScoringSection[]   // 中间分区才有
  progress: { scored: number; total: number } // 汇总进度
}
```

## 5. 实现计划

### Phase 1: 基础数据（后端不改）

| 步骤 | 文件 | 改动 |
|------|------|------|
| 1.1 | `ProjectDetailView.vue` | `sectionList` 改为传完整分区树 |
| 1.2 | 新建 `utils/sectionTree.ts` | `buildTree()` / `flattenTree()` 工具函数 |

### Phase 2: 检查配置多层级

| 步骤 | 文件 | 改动 |
|------|------|------|
| 2.1 | `SectionConfigView.vue` | props 接收树结构 |
| 2.2 | `SectionConfigView.vue` | 分区评价列表改为递归渲染（缩进+树线） |
| 2.3 | `SectionConfigView.vue` | 配置评价对话框区分中间/叶子模式 |
| 2.4 | `SectionConfigView.vue` | 调度组分区选择改为树形 checkbox |

### Phase 3: 打分界面多层级

| 步骤 | 文件 | 改动 |
|------|------|------|
| 3.1 | `TaskExecutionView.vue` | 加载分区树 + 构建 ScoringSection 树 |
| 3.2 | 新建 `SectionGroupRenderer.vue` | 递归渲染组件 |
| 3.3 | `TaskExecutionView.vue` | 替换扁平分区渲染为递归组件 |
| 3.4 | 样式 | 中间分区折叠标题 + 缩进层级样式 |

### Phase 4: 指标自动层级

| 步骤 | 文件 | 改动 |
|------|------|------|
| 4.1 | `SectionConfigView.vue` | 保存评价时根据分区层级自动创建叶子/复合指标 |
| 4.2 | `SectionConfigView.vue` | 中间分区创建复合指标时自动关联子指标 |

## 6. UI 设计

### 6.1 检查配置 — 分区评价树形

```
分区评价配置  6个分区

┌─ 安全检查 ─────────────────── 中间分区 ──── [配置评价]
│  ⚠ 暂未配置评价
│  调度: 每日常规巡查
│
│  ┌─ 教室安全 ────────────────────────────── [配置评价]
│  │  ⚠ 暂未配置评价
│  │
│  └─ 实验室安全 ──────────────────────────── [配置评价]
│     ⚠ 暂未配置评价
│
├─ 卫生检查 ─────────────────── 中间分区 ──── [配置评价]
│  📊 流动红旗 · 得分率 ≥90%
│
│  ┌─ 宿舍卫生 ────────────────────────────── [配置评价]
│  │  📊 合格评定 · ≥60%合格
│  │
│  └─ 教室卫生 ────────────────────────────── [配置评价]
│     ⚠ 暂未配置评价
│
└─ 纪律检查 ─────────────────── 叶子分区 ──── [配置评价]
   📊 星级评定 · 排名前20%五星
```

中间分区卡片与叶子分区视觉区分：
- 中间分区：灰色背景，显示"中间分区"标签，子分区缩进在内
- 叶子分区：白色背景，正常显示

### 6.2 打分界面 — 嵌套折叠

```
2024级-计算机应用24-1班  11/16已评 ─── 69%

▼ 安全检查 ──────────────────────── 5/6已评
  ▼ 教室安全 ──── 3项 3/3已评 ━━━━━━━━━━
    ✓ 消防器材    [通过]      10分
    ✓ 电路安全    [良 7分]
    ✓ 应急通道    [通过]      10分
  ▼ 实验室安全 ── 3项 2/3已评 ━━━━━━━░░░
    ✓ 化学品存放  [优 10分]
    ✓ 防护设备    [通过]      10分
      设备标识    [未评]

▼ 卫生检查 ──────────────────────── 3/5已评
  ▼ 宿舍卫生 ──── 3项 2/3已评
    ...
  ▼ 教室卫生 ──── 2项 1/2已评
    ...

▼ 纪律检查 ──── 5项 5/5已评 ━━━━━━━━━━
  ✓ 出勤率      [18分]
  ✓ 迟到人数    [5 人 × 1分 = 5分]
  ...
```

中间分区标题样式：
- 更大的字体、深色背景条
- 子分区进度汇总（5/6已评）
- 可折叠（点击收起/展开子分区）

叶子分区标题样式：
- 正常字体，蓝色左边框（现有样式）
- 进度条

## 7. 不改的部分

- **后端 API**：分区树查询、submission 创建、指标 CRUD 全部已支持多层级
- **InspectionPlanScheduler**：任务生成已按叶子分区创建 submission
- **IndicatorScoreService**：已支持多层复合指标嵌套计算
- **数据库**：`parentSectionId` 已支持任意深度
