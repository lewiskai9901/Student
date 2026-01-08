# 精密仪器设计系统 (Precision Instruments Design System)

## 设计理念

本设计系统采用**精密仪器美学**,灵感来源于科学实验室设备的精确性和优雅性。

### 核心特征
- 🎯 **深邃学术** - 避开常见的浅蓝紫,使用更有深度的蓝色系
- ⚡ **精密动画** - 粒子效果、光晕、扫描线等科技感元素
- 🔤 **独特字体** - Plus Jakarta Sans + DM Sans + JetBrains Mono
- 💎 **高质感** - 渐变、毛玻璃、发光效果

---

## 快速开始

### 1. 导入组件

```vue
<script setup lang="ts">
import {
  StatCard,
  DataTable,
  FilterBar,
  FormCard,
  GradientButton,
  LoadingState,
  EmptyState
} from '@/components/design-system'
</script>
```

### 2. 使用统计卡片

```vue
<template>
  <StatCard
    title="学生总数"
    :value="1250"
    :icon="Users"
    subtitle="在校学生"
    :trend="12.5"
    :progress="73"
    color="blue"
    clickable
    @click="handleClick"
  />
</template>
```

**Props:**
- `title` - 卡片标题
- `value` - 主数值(自动千分位)
- `icon` - Lucide图标组件
- `subtitle` - 副标题/描述
- `trend` - 趋势百分比(正数↑,负数↓)
- `progress` - 进度0-100
- `color` - 主题色:'blue'|'purple'|'emerald'|'orange'|'cyan'
- `clickable` - 是否可点击

**效果:**
- ✨ 粒子浮动动画
- 💫 背景光晕效果
- 📊 扫描线科技感
- 📈 趋势指示器
- 📉 进度条with发光
- 🌊 点击波纹反馈

---

### 3. 使用渐变按钮

```vue
<template>
  <GradientButton
    variant="primary"
    size="md"
    :icon="Plus"
    icon-position="left"
    :loading="isLoading"
    @click="handleSubmit"
  >
    新增学生
  </GradientButton>
</template>
```

**变体:**
- `primary` - 蓝紫渐变(默认)
- `success` - 绿色渐变
- `danger` - 红色渐变
- `warning` - 橙色渐变
- `outline` - 透明+渐变边框

**效果:**
- 🎨 渐变背景动画
- ✨ 悬停发光
- 🌊 点击波纹
- ⏳ 加载状态

---

### 4. 使用数据表格

```vue
<template>
  <DataTable
    :columns="columns"
    :data="tableData"
    :loading="loading"
    :pagination="pagination"
    :selectable="true"
    row-key="id"
    @row-click="handleRowClick"
    @selection-change="handleSelectionChange"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
  >
    <!-- 自定义列插槽 -->
    <template #status="{ row }">
      <span :class="statusClass(row.status)">
        {{ row.status }}
      </span>
    </template>

    <!-- 操作列 -->
    <template #actions="{ row }">
      <GradientButton size="sm" variant="outline" @click="handleEdit(row)">
        编辑
      </GradientButton>
    </template>
  </DataTable>
</template>

<script setup lang="ts">
const columns = [
  { key: 'id', label: 'ID', width: '80px', sortable: true },
  { key: 'name', label: '姓名', sortable: true },
  { key: 'status', label: '状态', slot: 'status' },
  { key: 'actions', label: '操作', slot: 'actions', width: '120px' }
]
</script>
```

**特点:**
- 📊 内置排序
- ✅ 多选支持
- 📄 分页组件
- 🎯 行悬停效果
- 💀 骨架屏加载
- 🎭 优雅空状态

---

### 5. 使用筛选栏

```vue
<template>
  <FilterBar
    title="筛选条件"
    :collapsible="true"
    :loading="searching"
    :active-filters-count="activeCount"
    @search="handleSearch"
    @reset="handleReset"
  >
    <!-- 筛选字段 -->
    <div>
      <label>📋 姓名</label>
      <input v-model="filters.name" />
    </div>

    <div>
      <label>📅 日期</label>
      <input v-model="filters.date" type="date" />
    </div>

    <div>
      <label>📊 状态</label>
      <select v-model="filters.status">
        <option value="">全部</option>
        <option value="active">激活</option>
      </select>
    </div>
  </FilterBar>
</template>
```

**特点:**
- 🎨 毛玻璃背景
- 📱 响应式布局
- 🔢 激活筛选数量徽章
- 📁 可折叠
- 🎯 Emoji图标

---

### 6. 使用表单卡片

```vue
<template>
  <FormCard
    title="新增学生"
    description="填写学生基本信息"
    :icon="UserPlus"
    :loading="submitting"
    :glassmorphism="false"
  >
    <!-- 表单内容 -->
    <el-form :model="form" label-width="100px">
      <el-form-item label="学号">
        <el-input v-model="form.studentNo" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.name" />
      </el-form-item>
    </el-form>

    <!-- 底部操作按钮 -->
    <template #actions>
      <GradientButton variant="outline" @click="handleCancel">
        取消
      </GradientButton>
      <GradientButton :loading="submitting" @click="handleSubmit">
        提交
      </GradientButton>
    </template>
  </FormCard>
</template>
```

**特点:**
- 🎯 图标+标题
- 📝 描述文字
- ⏳ 加载遮罩
- 🌫️ 毛玻璃选项
- 📐 网格背景装饰

---

### 7. 加载和空状态

```vue
<template>
  <!-- 全屏加载 -->
  <LoadingState
    fullscreen
    text="加载中..."
    :progress="uploadProgress"
    show-progress
  />

  <!-- 行内加载 -->
  <LoadingState text="处理中..." />

  <!-- 空状态 -->
  <EmptyState
    :icon="Database"
    title="暂无数据"
    description="当前没有可显示的学生信息"
    action-text="新增学生"
    :action-icon="Plus"
    @action="handleAdd"
  >
    <!-- 自定义操作 -->
    <template #action>
      <GradientButton :icon="Plus">
        立即添加
      </GradientButton>
    </template>
  </EmptyState>
</template>
```

---

## 完整页面示例

### 列表页面模板

```vue
<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div>
      <h1 class="text-2xl font-display font-bold text-gray-900">学生管理</h1>
      <p class="mt-1 text-sm text-gray-600">管理学生基本信息</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        v-for="stat in statistics"
        :key="stat.key"
        v-bind="stat"
      />
    </div>

    <!-- 筛选栏 -->
    <FilterBar
      @search="loadData"
      @reset="resetFilters"
    >
      <!-- 筛选字段 -->
    </FilterBar>

    <!-- 数据表格 -->
    <DataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      @page-change="handlePageChange"
    >
      <!-- 自定义列 -->
    </DataTable>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Users, GraduationCap } from 'lucide-vue-next'
import {
  StatCard,
  DataTable,
  FilterBar,
  GradientButton
} from '@/components/design-system'

// 统计数据
const statistics = ref([
  {
    title: '学生总数',
    value: 1250,
    icon: Users,
    trend: 12.5,
    progress: 73,
    color: 'blue',
    clickable: true
  },
  // ...更多统计
])

// 表格列定义
const columns = [
  { key: 'studentNo', label: '学号', sortable: true },
  { key: 'name', label: '姓名', sortable: true },
  { key: 'class', label: '班级' },
  { key: 'actions', label: '操作', slot: 'actions' }
]

// 数据和方法...
</script>
```

---

## 设计令牌使用

### CSS变量

```css
/* 颜色 */
color: var(--color-primary-600);
background: var(--gradient-primary);

/* 间距 */
padding: var(--spacing-lg);
margin: var(--spacing-xl);

/* 圆角 */
border-radius: var(--radius-xl);

/* 阴影 */
box-shadow: var(--shadow-lg);
box-shadow: var(--shadow-glow-primary);

/* 动画 */
transition: all var(--transition-base);

/* 字体 */
font-family: var(--font-display); /* 标题 */
font-family: var(--font-body);    /* 正文 */
font-family: var(--font-mono);    /* 数字 */
```

### 工具类

```html
<!-- 渐变背景 -->
<div class="gradient-primary"></div>

<!-- 发光效果 -->
<div class="glow-primary"></div>

<!-- 网格背景 -->
<div class="pattern-grid-bg"></div>

<!-- 数字字体 -->
<span class="font-mono">1,234</span>
```

---

## 最佳实践

### ✅ 推荐

1. **统一使用设计系统组件**
   ```vue
   <StatCard ... />  <!-- ✅ -->
   ```

2. **数字使用等宽字体**
   ```vue
   <span class="font-mono">{{ value.toLocaleString() }}</span>
   ```

3. **保持一致的间距**
   ```vue
   <div class="space-y-6">  <!-- 使用 Tailwind 间距 -->
   ```

4. **合理使用动画**
   - 重要交互才用动画
   - 避免过度动画

### ❌ 避免

1. **混用旧样式**
   ```vue
   <div class="old-card">  <!-- ❌ -->
   ```

2. **自定义颜色**
   ```vue
   <div style="color: #123456">  <!-- ❌ 使用设计令牌 -->
   ```

3. **硬编码数值**
   ```vue
   <div style="padding: 24px">  <!-- ❌ 使用 spacing 变量 -->
   ```

---

## 组件清单

### ✅ 已完成
- StatCard - 统计卡片
- GradientButton - 渐变按钮
- DataTable - 数据表格
- FilterBar - 筛选栏
- FormCard - 表单卡片
- LoadingState - 加载状态
- EmptyState - 空状态

### 🚧 规划中
- SearchInput - 搜索输入框
- IconButton - 图标按钮
- Badge - 徽章
- Alert - 警告提示
- Toast - 消息提示
- Modal - 模态框

---

## 设计原则

1. **精密 > 花哨** - 每个动画都有目的
2. **优雅 > 复杂** - 简洁但不简单
3. **一致 > 个性** - 统一的视觉语言
4. **性能 > 特效** - CSS优先,必要时才用JS

---

## 相关资源

- 设计令牌: `src/styles/design-tokens.css`
- 组件源码: `src/components/design-system/`
- 示例页面: `src/views/DashboardView.vue`
- 设计文档: `docs/plans/2025-12-28-ui-design-system-refactor.md`

---

**版本:** 1.0.0
**更新日期:** 2025-12-28
**设计团队:** Claude Code × AI Design System
