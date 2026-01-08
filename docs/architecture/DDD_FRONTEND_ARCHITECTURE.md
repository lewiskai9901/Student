# DDD 前端架构 V2

## 概述

本项目前端采用基于 DDD（领域驱动设计）的六边形架构，实现了清晰的关注点分离和模块化设计。

## 目录结构

```
frontend/src/
├── api/v2/                 # API 层 - 与后端通信
│   ├── organization.ts     # 组织管理 API
│   ├── inspection.ts       # 检查管理 API
│   ├── access.ts           # 权限管理 API
│   ├── student.ts          # 学生管理 API
│   ├── dormitory.ts        # 宿舍管理 API
│   ├── task.ts             # 任务管理 API
│   └── index.ts            # API 聚合导出
│
├── types/v2/               # 类型定义层 - TypeScript 类型
│   ├── organization.ts     # 组织相关类型
│   ├── inspection.ts       # 检查相关类型
│   ├── access.ts           # 权限相关类型
│   ├── student.ts          # 学生相关类型
│   ├── dormitory.ts        # 宿舍相关类型
│   ├── task.ts             # 任务相关类型
│   └── index.ts            # 类型聚合导出
│
├── stores/v2/              # 状态管理层 - Pinia Stores
│   ├── organization.ts     # 组织状态管理
│   ├── inspection.ts       # 检查状态管理
│   ├── access.ts           # 权限状态管理
│   ├── student.ts          # 学生状态管理
│   ├── dormitory.ts        # 宿舍状态管理
│   ├── task.ts             # 任务状态管理
│   └── index.ts            # Store 聚合导出
│
├── composables/v2/         # 组合式函数 - 可复用逻辑
│   ├── usePagination.ts    # 分页逻辑
│   ├── useLoading.ts       # 加载状态逻辑
│   ├── useConfirm.ts       # 确认对话框逻辑
│   ├── useTable.ts         # 表格数据逻辑
│   ├── useForm.ts          # 表单逻辑
│   ├── useDialog.ts        # 对话框逻辑
│   ├── useSelection.ts     # 选择逻辑
│   ├── useSearch.ts        # 搜索逻辑
│   └── index.ts            # Composables 聚合导出
│
└── components/v2/          # UI 组件层
    ├── PageContainer.vue   # 页面容器
    ├── SearchBar.vue       # 搜索栏
    ├── ActionBar.vue       # 操作栏
    ├── StatusTag.vue       # 状态标签
    ├── ConfirmButton.vue   # 确认按钮
    ├── DetailDrawer.vue    # 详情抽屉
    ├── DescriptionList.vue # 描述列表
    └── index.ts            # 组件聚合导出
```

## 架构层次

### 1. Types 层 (类型定义)

定义与后端 DTO 对应的 TypeScript 接口和枚举。

```typescript
// types/v2/student.ts
export interface Student {
  id: number
  studentNo: string
  name: string
  // ...
}

export enum StudentStatus {
  STUDYING = 1,
  GRADUATED = 2,
  // ...
}
```

### 2. API 层 (数据访问)

封装所有 HTTP 请求，返回 Promise 类型数据。

```typescript
// api/v2/student.ts
export const studentApi = {
  getList: (params) => request.get('/students', { params }),
  getById: (id) => request.get(`/students/${id}`),
  create: (data) => request.post('/students', data),
  // ...
}
```

### 3. Store 层 (状态管理)

使用 Pinia 管理应用状态，调用 API 并维护数据。

```typescript
// stores/v2/student.ts
export const useStudentStore = defineStore('student', () => {
  const students = ref<Student[]>([])

  const loadStudents = async (params) => {
    const result = await studentApi.getList(params)
    students.value = result.records
  }

  return { students, loadStudents }
})
```

### 4. Composables 层 (可复用逻辑)

提取通用的业务逻辑为组合式函数。

```typescript
// composables/v2/useTable.ts
export function useTable<T>(options: UseTableOptions<T>) {
  const data = ref<T[]>([])
  const loading = ref(false)

  const loadData = async () => {
    loading.value = true
    const result = await options.fetchData(params)
    data.value = result.records
    loading.value = false
  }

  return { data, loading, loadData }
}
```

### 5. Components 层 (UI 组件)

提供标准化的 UI 组件。

```vue
<!-- components/v2/PageContainer.vue -->
<template>
  <div class="page-container">
    <header v-if="title">{{ title }}</header>
    <slot />
  </div>
</template>
```

## 使用示例

### 在视图中使用

```vue
<script setup lang="ts">
import { useStudentStore } from '@/stores/v2'
import { useTable, useConfirm } from '@/composables/v2'
import { PageContainer, SearchBar, ActionBar } from '@/components/v2'

const studentStore = useStudentStore()
const { confirmDelete, showSuccess } = useConfirm()

const {
  data: students,
  loading,
  pagination,
  search,
  refresh
} = useTable({
  fetchData: studentStore.loadStudents,
  immediate: true
})

const handleDelete = async (student) => {
  const confirmed = await confirmDelete(student.name)
  if (confirmed) {
    await studentStore.deleteStudent(student.id)
    showSuccess('删除成功')
    refresh()
  }
}
</script>

<template>
  <PageContainer title="学生管理">
    <template #toolbar>
      <SearchBar v-model="keyword" @search="search" />
    </template>

    <el-table :data="students" v-loading="loading">
      <!-- 表格内容 -->
    </el-table>
  </PageContainer>
</template>
```

## 模块列表

| 模块 | 描述 | 状态 |
|------|------|------|
| organization | 组织管理（部门、年级、班级等） | ✅ 完成 |
| inspection | 检查管理（量化检查、评分等） | ✅ 完成 |
| access | 权限管理（用户、角色、权限） | ✅ 完成 |
| student | 学生管理 | ✅ 完成 |
| dormitory | 宿舍管理（楼宇、宿舍、床位） | ✅ 完成 |
| task | 任务管理（任务、审批、消息） | ✅ 完成 |

## Composables 列表

| Composable | 描述 |
|------------|------|
| usePagination | 分页状态管理 |
| useLoading | 加载状态管理 |
| useSimpleLoading | 简单加载状态 |
| useConfirm | 确认对话框 |
| useTable | 表格数据管理 |
| useForm | 表单状态管理 |
| useEditForm | 编辑表单管理 |
| useDialog | 对话框状态管理 |
| useEditDialog | 编辑对话框管理 |
| useDetailDialog | 详情对话框管理 |
| useSelection | 多选状态管理 |
| useSingleSelection | 单选状态管理 |
| useSearch | 搜索功能 |
| useAdvancedSearch | 高级搜索功能 |

## 组件列表

| 组件 | 描述 |
|------|------|
| PageContainer | 页面容器布局 |
| SearchBar | 搜索栏组件 |
| ActionBar | 操作栏组件 |
| StatusTag | 状态标签组件 |
| ConfirmButton | 确认按钮组件 |
| DetailDrawer | 详情抽屉组件 |
| DescriptionList | 描述列表组件 |
| StatCard | 统计卡片（从 design-system 导出） |
| FormCard | 表单卡片（从 design-system 导出） |
| DataTable | 数据表格（从 design-system 导出） |
| FilterBar | 筛选栏（从 design-system 导出） |
| LoadingState | 加载状态（从 design-system 导出） |
| EmptyState | 空状态（从 design-system 导出） |
| GradientButton | 渐变按钮（从 design-system 导出） |

## 迁移指南

### 从旧代码迁移

1. **导入路径更新**
   ```typescript
   // 旧
   import { Student } from '@/types/student'
   import { getStudentList } from '@/api/student'

   // 新
   import type { Student } from '@/types/v2'
   import { studentApi } from '@/api/v2'
   ```

2. **使用 Store**
   ```typescript
   // 旧：直接调用 API
   const students = await getStudentList(params)

   // 新：通过 Store
   const studentStore = useStudentStore()
   await studentStore.loadStudents(params)
   const students = studentStore.students
   ```

3. **使用 Composables**
   ```typescript
   // 旧：手动管理状态
   const loading = ref(false)
   const data = ref([])

   // 新：使用 composable
   const { data, loading, loadData } = useTable({ ... })
   ```

## 最佳实践

1. **类型优先**: 先定义类型，再实现 API 和 Store
2. **单一职责**: 每个模块只负责一个领域
3. **可测试性**: Store 和 Composables 易于单元测试
4. **渐进迁移**: 可以逐步将旧代码迁移到新架构
5. **命名规范**: 使用一致的命名约定（useXxx, xxxApi, useXxxStore）

## 版本历史

- **V2.0.0** (2025-01): 完成 DDD 架构重构
  - 新增 6 个领域模块
  - 新增 14 个 Composables
  - 新增 14 个 V2 组件
  - 构建验证通过
