# V2 前端重构分析与计划文档

**文档版本**: 1.1
**创建日期**: 2026-01-03
**更新日期**: 2026-01-03
**状态**: Phase 1 已完成

---

## 一、重构问题总结

### 1.1 本次重构存在的严重问题

| 问题类别 | 问题描述 | 影响程度 | 状态 |
|---------|---------|---------|------|
| **API路径错误** | 前端使用 `/v2/access/roles` 但后端是 `/v2/roles` | 🔴 严重 | ✅ 已修复 |
| **响应处理错误** | Store使用 `response.success` 但实际直接返回 `data` | 🔴 严重 | ✅ 已修复 |
| **方法名不匹配** | Store调用的API方法名与实际API不一致 | 🔴 严重 | ✅ 已修复 |
| **API方法缺失** | Store调用 `orgUnitApi.getByType()` 但API中不存在 | 🔴 严重 | ✅ 已修复 |
| **类型定义不匹配** | 视图访问 `role.name` 但类型定义为 `roleName` | 🟡 中等 | ✅ 已验证一致 |
| **Store方法缺失** | enable/disable 权限/角色 方法未实现 | 🟡 中等 | ✅ 已修复 |
| **前端迁移不完整** | 109+ V1视图只迁移了7个到V2 | 🔴 严重 | ❌ 未修复 |
| **组件未拆分** | V2视图是单文件，未遵循组件化原则 | 🟡 中等 | ❌ 未修复 |
| **Composables缺失** | 无可复用的业务逻辑组合函数 | 🟡 中等 | ❌ 未修复 |

### 1.2 重构完成度评估

```
后端 V2 重构: ████████████████████░ 95% (DDD架构完成)
前端 API层:   █████████████████░░░ 85% (已修复: 路径、缺失方法)
前端 Store层: █████████████████░░░ 85% (已修复: 响应处理、缺失方法)
前端 Types层: ██████████████░░░░░░ 70% (已验证后端匹配)
前端 Views层: ██░░░░░░░░░░░░░░░░░░ 6%  (7/109+ 视图)
前端 组件化:  ░░░░░░░░░░░░░░░░░░░░ 0%  (未开始)
前端 Composables: ░░░░░░░░░░░░░░░░░░░░ 0%  (未开始)

总体前端重构完成度: 约 40%
```

### 1.3 Phase 1 修复记录 (2026-01-03)

#### 修复内容:
1. **Organization API** (`frontend/src/api/v2/organization.ts`)
   - 添加 `getOrgUnitsByType(type)` 方法
   - 添加 `getOrgUnitChildren(id)` 方法
   - 更新 `orgUnitApi` 对象导出新方法

2. **Organization Store** (`frontend/src/stores/v2/organization.ts`)
   - 修复所有方法的响应处理模式 (`response.success` → 直接使用 `data`)
   - 添加 `loadOrgUnits()` 方法
   - 添加 `enableOrgUnit(id)` / `disableOrgUnit(id)` 方法
   - 添加 `dissolveClass(id)` 方法
   - 修复 `orgUnitsByType` 计算属性 (`u.type` → `u.unitType`)
   - 更新导出列表

3. **Access Store** (`frontend/src/stores/v2/access.ts`)
   - 添加 `enablePermission(id)` / `disablePermission(id)` 方法
   - 添加 `enableRole(id)` / `disableRole(id)` 方法
   - 更新导出列表

4. **Access API** (`frontend/src/api/v2/access.ts`)
   - 修复 `roleApi.setPermissions` 直接引用函数而非包装

5. **Inspection Store** (`frontend/src/stores/v2/inspection.ts`)
   - 修复所有方法的响应处理模式
   - 添加错误处理和日志

#### 验证:
- ✅ `npm run build` 构建成功 (1m 42s)

---

## 二、后端 V2 API 完整清单

### 2.1 API 总览

| Controller | 路径前缀 | 端点数 | 状态 |
|------------|---------|-------|------|
| OrgUnitController | `/v2/org-units` | 9 | ✅ 可用 |
| SchoolClassController | `/v2/organization/classes` | 14 | ✅ 可用 |
| RoleController | `/v2/roles` | 8 | ✅ 可用 |
| PermissionController | `/v2/permissions` | 6 | ✅ 可用 |
| UserRoleController | `/v2/users` | 8 | ✅ 可用 |
| InspectionTemplateController | `/v2/inspection-templates` | 6 | ✅ 可用 |
| InspectionRecordController | `/v2/inspection-records` | 9 | ✅ 可用 |
| AppealController | `/v2/appeals` | 13 | ✅ 可用 |
| ScopeManageController | `/v2/scopes` | 16 | ✅ 可用 |
| **总计** | | **97** | |

### 2.2 详细 API 端点

#### 2.2.1 组织单元 API (`/v2/org-units`)

| 方法 | 端点 | 描述 | 前端已实现 |
|------|------|------|-----------|
| GET | `/v2/org-units` | 获取列表 | ✅ |
| GET | `/v2/org-units/tree` | 获取组织树 | ✅ |
| GET | `/v2/org-units/{id}` | 获取详情 | ✅ |
| GET | `/v2/org-units/by-type/{type}` | 按类型获取 | ✅ (Phase 1修复) |
| GET | `/v2/org-units/{id}/children` | 获取子节点 | ✅ (Phase 1修复) |
| POST | `/v2/org-units` | 创建 | ✅ |
| PUT | `/v2/org-units/{id}` | 更新 | ✅ |
| DELETE | `/v2/org-units/{id}` | 删除 | ✅ |
| PUT | `/v2/org-units/{id}/enable` | 启用 | ✅ |
| PUT | `/v2/org-units/{id}/disable` | 禁用 | ✅ |

#### 2.2.2 角色 API (`/v2/roles`)

| 方法 | 端点 | 描述 | 前端已实现 |
|------|------|------|-----------|
| GET | `/v2/roles` | 获取列表 | ✅ |
| GET | `/v2/roles/{id}` | 获取详情 | ✅ |
| POST | `/v2/roles` | 创建 | ✅ |
| PUT | `/v2/roles/{id}` | 更新 | ✅ |
| DELETE | `/v2/roles/{id}` | 删除 | ✅ |
| PUT | `/v2/roles/{id}/permissions` | 设置权限 | ✅ |
| POST | `/v2/roles/{id}/permissions/{permissionId}` | 添加权限 | ❌ |
| DELETE | `/v2/roles/{id}/permissions/{permissionId}` | 移除权限 | ❌ |

#### 2.2.3 权限 API (`/v2/permissions`)

| 方法 | 端点 | 描述 | 前端已实现 |
|------|------|------|-----------|
| GET | `/v2/permissions` | 获取列表 | ✅ |
| GET | `/v2/permissions/tree` | 获取权限树 | ✅ |
| GET | `/v2/permissions/{id}` | 获取详情 | ✅ |
| POST | `/v2/permissions` | 创建 | ✅ |
| PUT | `/v2/permissions/{id}` | 更新 | ✅ |
| DELETE | `/v2/permissions/{id}` | 删除 | ✅ |

#### 2.2.4 用户角色 API (`/v2/users`)

| 方法 | 端点 | 描述 | 前端已实现 |
|------|------|------|-----------|
| GET | `/v2/users/{userId}/roles` | 获取用户角色 | ✅ |
| GET | `/v2/users/{userId}/permissions` | 获取用户权限 | ❌ |
| POST | `/v2/users/{userId}/roles/{roleId}` | 分配角色 | ❌ |
| POST | `/v2/users/{userId}/roles/{roleId}/scoped` | 分配角色(含范围) | ✅ |
| DELETE | `/v2/users/{userId}/roles/{roleId}` | 移除角色 | ✅ |
| PUT | `/v2/users/{userId}/roles` | 设置用户角色 | ✅ |
| GET | `/v2/users/me/permissions` | 当前用户权限 | ✅ |
| GET | `/v2/users/me/roles` | 当前用户角色 | ✅ |

---

## 三、前端模块完整性分析

### 3.1 权限管理模块 (Access)

**完整性评分: 80%**

#### 问题清单

| # | 问题 | 类型 | 优先级 |
|---|------|------|-------|
| 1 | Store缺失 `enablePermission/disablePermission` | 功能缺失 | 高 |
| 2 | Store缺失 `enableRole/disableRole` | 功能缺失 | 高 |
| 3 | 类型定义 `roleName/roleCode` 与视图访问 `name/code` 不一致 | 类型错误 | 中 |
| 4 | `setRolePermissions` 参数类型不匹配 | 接口错误 | 中 |

#### 需要修复的文件

```
frontend/src/
├── api/v2/access.ts          # 添加缺失API方法
├── stores/v2/access.ts       # 添加enable/disable方法
├── types/v2/access.ts        # 统一属性命名
└── views/v2/access/
    ├── RoleListView.vue      # 更新属性访问
    └── PermissionListView.vue # 更新属性访问
```

### 3.2 组织管理模块 (Organization)

**完整性评分: 65%**

#### 问题清单

| # | 问题 | 类型 | 优先级 |
|---|------|------|-------|
| 1 | API缺失 `getByType(type)` 方法 | **严重缺失** | 🔴 紧急 |
| 2 | API缺失 `getChildren(parentId)` 方法 | **严重缺失** | 🔴 紧急 |
| 3 | 类型定义 `unitType/unitName` 与视图访问不一致 | 类型错误 | 中 |
| 4 | Store调用不存在的API会导致运行时错误 | 运行时错误 | 🔴 紧急 |

#### 需要修复的文件

```
frontend/src/
├── api/v2/organization.ts    # 添加 getByType, getChildren
├── stores/v2/organization.ts # 修复API调用
├── types/v2/organization.ts  # 统一属性命名
└── views/v2/organization/
    ├── OrgUnitsView.vue      # 更新属性访问
    └── SchoolClassesView.vue # 验证功能
```

### 3.3 量化检查模块 (Inspection)

**完整性评分: 90%**

#### 问题清单

| # | 问题 | 类型 | 优先级 |
|---|------|------|-------|
| 1 | 类型定义 `templateName` 与视图访问 `name` 不一致 | 类型错误 | 中 |
| 2 | 缺失 `template.version` 字段定义 | 类型缺失 | 低 |
| 3 | `TemplateScope` 枚举缺少 `GRADE` 值 | 枚举不完整 | 低 |

---

## 四、V1 与 V2 页面对应关系

### 4.1 迁移统计

| 分类 | V1页面数 | 已迁移V2 | 迁移率 |
|------|---------|---------|-------|
| 系统管理 | 8 | 2 | 25% |
| 学生事务 | 3 | 1 | 33% |
| 宿舍管理 | 4 | 0 | 0% |
| 量化检查 | 20+ | 3 | 15% |
| 教学管理 | 4 | 0 | 0% |
| 教学评估 | 10 | 0 | 0% |
| 任务管理 | 5 | 0 | 0% |
| 其他 | 55+ | 1 | <2% |
| **总计** | **109+** | **7** | **6%** |

### 4.2 高优先级迁移清单

#### 第一批 - 核心功能（必须立即完成）

| V1视图 | 目标V2视图 | 依赖 | 工作量 |
|--------|-----------|------|-------|
| system/UsersView.vue | v2/access/UserListView.vue | access模块 | 3天 |
| student/StudentList.vue | v2/student/StudentListView.vue | 新建模块 | 4天 |
| quantification/CheckRecordDetailView.vue | v2/inspection/RecordDetailView.vue | inspection模块 | 2天 |
| quantification/CheckRecordScoring.vue | v2/inspection/ScoringView.vue | inspection模块 | 3天 |

#### 第二批 - 重要功能（一周内完成）

| V1视图 | 目标V2视图 | 依赖 | 工作量 |
|--------|-----------|------|-------|
| dormitory/DormitoryBuildingManagement.vue | v2/dormitory/BuildingView.vue | 新建模块 | 3天 |
| dormitory/DormitoryList.vue | v2/dormitory/RoomListView.vue | dormitory模块 | 2天 |
| task/TaskList.vue | v2/task/TaskListView.vue | 新建模块 | 2天 |
| task/MyTask.vue | v2/task/MyTaskView.vue | task模块 | 2天 |

---

## 五、详细修复计划

### 5.1 紧急修复（立即执行）

#### 5.1.1 修复 Organization API 缺失方法

**文件**: `frontend/src/api/v2/organization.ts`

```typescript
// 添加缺失的方法

/**
 * 按类型获取组织单元
 */
export function getOrgUnitsByType(type: OrgUnitType): Promise<ApiResponse<OrgUnit[]>> {
  return request({
    url: `${ORG_UNIT_URL}/by-type/${type}`,
    method: 'GET'
  })
}

/**
 * 获取子组织单元
 */
export function getOrgUnitChildren(parentId: number): Promise<ApiResponse<OrgUnit[]>> {
  return request({
    url: `${ORG_UNIT_URL}/${parentId}/children`,
    method: 'GET'
  })
}

// 更新 orgUnitApi 对象
export const orgUnitApi = {
  // ... 现有方法
  getByType: getOrgUnitsByType,
  getChildren: getOrgUnitChildren
}
```

#### 5.1.2 修复 Access Store 缺失方法

**文件**: `frontend/src/stores/v2/access.ts`

```typescript
// 添加缺失的方法

/**
 * 启用权限
 */
const enablePermission = async (id: number) => {
  await permissionApi.enable(id)
  await loadPermissions()
}

/**
 * 禁用权限
 */
const disablePermission = async (id: number) => {
  await permissionApi.disable(id)
  await loadPermissions()
}

/**
 * 启用角色
 */
const enableRole = async (id: number) => {
  await roleApi.enable(id)
  await loadRoles()
}

/**
 * 禁用角色
 */
const disableRole = async (id: number) => {
  await roleApi.disable(id)
  await loadRoles()
}

// 在 return 中导出
return {
  // ... 现有导出
  enablePermission,
  disablePermission,
  enableRole,
  disableRole,
}
```

### 5.2 类型定义统一修复

#### 5.2.1 统一 Role 类型

**文件**: `frontend/src/types/v2/access.ts`

```typescript
// 方案A: 修改类型定义匹配后端响应
export interface Role {
  id: number | string
  roleCode: string      // 后端字段名
  roleName: string      // 后端字段名
  description?: string
  roleType: RoleType    // 后端字段名
  level: number
  isSystem: boolean
  isEnabled: boolean
  dataScope: DataScope
  permissionIds?: string[]
  createdAt?: string

  // 添加别名属性（兼容视图访问）
  get code(): string { return this.roleCode }
  get name(): string { return this.roleName }
  get type(): RoleType { return this.roleType }
}

// 方案B: 在API层转换字段名（推荐）
// 在 api/v2/access.ts 中添加响应转换
function transformRoleResponse(data: any): Role {
  return {
    ...data,
    code: data.roleCode,
    name: data.roleName,
    type: data.roleType,
  }
}
```

### 5.3 新建 V2 视图结构

```
frontend/src/views/v2/
├── access/                    # 已存在，需增强
│   ├── RoleListView.vue      ✅ 存在
│   ├── PermissionListView.vue ✅ 存在
│   └── UserListView.vue      ❌ 需创建
│
├── organization/              # 已存在，需增强
│   ├── OrgUnitsView.vue      ✅ 存在
│   └── SchoolClassesView.vue ✅ 存在
│
├── inspection/                # 已存在，需增强
│   ├── TemplateListView.vue  ✅ 存在
│   ├── TemplateDetailView.vue ❌ 需创建
│   ├── RecordListView.vue    ✅ 存在
│   ├── RecordDetailView.vue  ❌ 需创建
│   ├── ScoringView.vue       ❌ 需创建
│   └── AppealListView.vue    ✅ 存在
│
├── student/                   ❌ 需创建整个模块
│   ├── StudentListView.vue
│   └── StudentDetailView.vue
│
├── dormitory/                 ❌ 需创建整个模块
│   ├── BuildingView.vue
│   ├── RoomListView.vue
│   └── OverviewView.vue
│
└── task/                      ❌ 需创建整个模块
    ├── TaskListView.vue
    ├── MyTaskView.vue
    └── ApprovalView.vue
```

### 5.4 组件化重构计划

```
frontend/src/components/v2/
├── common/                    # 通用组件
│   ├── PageHeader.vue        # 页面标题组件
│   ├── SearchFilter.vue      # 搜索筛选组件
│   ├── DataTable.vue         # 数据表格组件
│   ├── CardGrid.vue          # 卡片网格组件
│   ├── EmptyState.vue        # 空状态组件
│   ├── LoadingSkeleton.vue   # 加载骨架屏
│   └── ConfirmDialog.vue     # 确认对话框
│
├── access/                    # 权限管理组件
│   ├── RoleCard.vue          # 角色卡片
│   ├── RoleForm.vue          # 角色表单
│   ├── RoleDetailDialog.vue  # 角色详情弹窗
│   ├── PermissionTree.vue    # 权限树
│   └── PermissionSelector.vue # 权限选择器
│
├── organization/              # 组织管理组件
│   ├── OrgUnitTree.vue       # 组织树
│   ├── OrgUnitForm.vue       # 组织表单
│   ├── ClassCard.vue         # 班级卡片
│   └── ClassForm.vue         # 班级表单
│
└── inspection/                # 量化检查组件
    ├── TemplateCard.vue      # 模板卡片
    ├── RecordCard.vue        # 记录卡片
    ├── AppealCard.vue        # 申诉卡片
    ├── ScoringPanel.vue      # 评分面板
    └── DeductionForm.vue     # 扣分表单
```

### 5.5 Composables 计划

```
frontend/src/composables/v2/
├── useDataTable.ts           # 表格数据管理
├── useSearchFilter.ts        # 搜索筛选逻辑
├── usePagination.ts          # 分页逻辑
├── useFormValidation.ts      # 表单验证
├── useConfirmDialog.ts       # 确认对话框
├── useRoleManagement.ts      # 角色管理业务逻辑
├── usePermissionManagement.ts # 权限管理业务逻辑
├── useOrgUnitTree.ts         # 组织树业务逻辑
├── useInspectionRecord.ts    # 检查记录业务逻辑
└── useAppealWorkflow.ts      # 申诉流程业务逻辑
```

---

## 六、执行计划

### 6.1 阶段一：紧急修复（1天）

- [ ] 修复 Organization API 缺失方法
- [ ] 修复 Access Store 缺失方法
- [ ] 验证所有 V2 页面基本功能正常

### 6.2 阶段二：类型统一（1天）

- [ ] 统一 Role 类型定义
- [ ] 统一 Permission 类型定义
- [ ] 统一 OrgUnit 类型定义
- [ ] 统一 Template 类型定义
- [ ] 更新所有视图的属性访问

### 6.3 阶段三：组件拆分（3天）

- [ ] 创建通用组件
- [ ] 拆分 Access 模块组件
- [ ] 拆分 Organization 模块组件
- [ ] 拆分 Inspection 模块组件

### 6.4 阶段四：新建模块（5天）

- [ ] 创建 Student 模块（视图+API+Store+类型）
- [ ] 创建 Dormitory 模块
- [ ] 创建 Task 模块

### 6.5 阶段五：Composables（2天）

- [ ] 创建通用 Composables
- [ ] 创建业务 Composables
- [ ] 重构视图使用 Composables

---

## 七、验收标准

### 7.1 功能验收

- [ ] 所有 V2 API 都有对应的前端调用
- [ ] 所有 V2 页面都能正常加载和操作
- [ ] 导航菜单无重复项
- [ ] 控制台无 JavaScript 错误

### 7.2 代码质量

- [ ] TypeScript 类型完整，无 `any` 类型
- [ ] 组件单一职责，不超过 300 行
- [ ] 业务逻辑抽取到 Composables
- [ ] 代码通过 ESLint 检查

### 7.3 用户体验

- [ ] 页面有加载状态指示
- [ ] 操作有成功/失败反馈
- [ ] 空状态有友好提示
- [ ] 错误有明确说明

---

*文档结束*
