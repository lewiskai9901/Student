# V2 前端迁移计划

## 概述

本计划旨在将前端从 V1 API 迁移到 V2 DDD 架构 API，同时保持 UI 功能与 V1 一致。

## 迁移策略

1. **后端**: V2 使用新的 DDD 风格字段名
2. **前端**: 适配 V2 新字段名，UI 功能保持与 V1 一致
3. **导航**: 所有 V2 页面在侧边栏显示并带 "V2" 标签
4. **测试**: 测试通过后完全采用 V2 页面

## 字段映射表

### 学生模块 (Student)

| V1 字段名 | V2 字段名 | 说明 |
|-----------|-----------|------|
| realName | name | 姓名 |
| studentStatus | status | 学籍状态 |
| studentStatusName | statusText | 状态文本 |
| admissionDate | enrollmentDate | 入学日期 |
| genderName | genderText | 性别文本 |

### 组织模块 (Organization)

| V1 字段名 | V2 字段名 | 说明 |
|-----------|-----------|------|
| departmentName | name | 部门名称 |
| parentDepartmentId | parentId | 父级ID |

### 权限模块 (Access)

| V1 字段名 | V2 字段名 | 说明 |
|-----------|-----------|------|
| permissionCode | code | 权限编码 |
| roleName | name | 角色名称 |

## 迁移清单

### 1. 学生管理模块 (Student)

#### 后端 (已完成)
- [x] `StudentPO.java` - 添加 userId 字段，支持 JOIN 查询
- [x] `DddStudentMapper.java` - 添加 users 表 JOIN 查询
- [x] `StudentRepositoryImpl.java` - 使用新 mapper 方法
- [x] `StudentDTO.java` - 添加兼容性 getter 方法

#### 前端 (已完成)
- [x] `types/v2/student.ts` - 更新类型定义使用 V2 字段名
- [x] `api/v2/student.ts` - 确认 API 路径正确
- [x] `stores/v2/student.ts` - 更新 store 使用 V2 字段名
- [x] `views/v2/student/StudentListView.vue` - 适配 V2 字段名，实现查看详情、分配宿舍、批量删除、导出功能

### 2. 组织管理模块 (Organization)

#### 后端 (已完成)
- [x] `OrgUnitController.java` - `/api/v2/org-units`
- [x] `SchoolClassController.java` - `/api/v2/organization/classes`

#### 前端 (已完成)
- [x] `types/v2/organization.ts` - 更新类型定义
- [x] `api/v2/organization.ts` - 确认 API 路径
- [x] `views/v2/organization/OrgUnitsView.vue` - 适配基于 V1 视图
- [x] `views/v2/organization/SchoolClassesView.vue` - 适配基于 V1 视图

### 3. 权限管理模块 (Access)

#### 后端 (已完成)
- [x] `PermissionController.java` - `/api/v2/permissions`
- [x] `RoleController.java` - `/api/v2/roles`
- [x] `UserRoleController.java` - `/api/v2/user-roles`

#### 前端 (已完成)
- [x] `types/v2/access.ts` - 更新类型定义
- [x] `api/v2/access.ts` - 确认 API 路径
- [x] `views/v2/access/RoleListView.vue` - 适配基于 V1 视图
- [x] `views/v2/access/PermissionListView.vue` - 适配基于 V1 视图，实现启用/禁用权限功能

### 4. 量化检查模块 (Inspection)

#### 后端 (已完成)
- [x] `InspectionTemplateController.java` - `/api/v2/inspection-templates`
- [x] `InspectionRecordController.java` - `/api/v2/inspection-records`
- [x] `AppealController.java` - `/api/v2/appeals`

#### 前端 (已完成)
- [x] `types/v2/inspection.ts` - 更新类型定义
- [x] `api/v2/inspection.ts` - 确认 API 路径
- [x] `views/v2/inspection/TemplateListView.vue` - 适配基于 V1 视图，实现模板编辑功能
- [x] `views/v2/inspection/RecordListView.vue` - 适配基于 V1 视图
- [x] `views/v2/inspection/AppealListView.vue` - 适配基于 V1 视图

### 5. 宿舍管理模块 (Dormitory)

#### 后端 (已完成)
- [x] `DormitoryController.java` - `/api/v2/dormitories`

#### 前端 (已完成)
- [x] `types/v2/dormitory.ts` - 更新类型定义
- [x] `api/v2/dormitory.ts` - 确认 API 路径
- [x] `views/v2/dormitory/DormitoryListView.vue` - 适配基于 V1 视图

### 6. 任务管理模块 (Task)

#### 后端 (已完成)
- [x] `TaskController.java` - `/api/v2/tasks`
- [x] `TaskEventHandler.java` - 任务领域事件处理器（创建、接受、提交、审批通过/拒绝）

#### 前端 (已完成)
- [x] `types/v2/task.ts` - 更新类型定义
- [x] `api/v2/task.ts` - 确认 API 路径
- [x] `views/v2/task/TaskListView.vue` - 适配基于 V1 视图

## 路由配置

### 启用 V2 路由 (router/v2.ts)

所有 V2 路由需要:
1. 设置 `hidden: false` 使其在侧边栏显示
2. 添加 "(V2)" 后缀到 title 用于区分

```typescript
// 示例
{
  path: '/v2/student/list',
  name: 'V2StudentList',
  component: () => import('@/views/v2/student/StudentListView.vue'),
  meta: {
    title: '学生列表 (V2)',  // 添加 V2 标签
    requiresAuth: true,
    permission: 'student:view',
    order: 1,
    hidden: false  // 改为 false 显示在导航
  }
}
```

## 实施步骤

### Phase 1: 基础设施 (已完成)
1. [x] 创建迁移计划文档
2. [x] 启用所有 V2 路由并添加 "(V2)" 标签
3. [x] 更新类型定义使用 V2 字段名

### Phase 2: 学生模块 (已完成)
1. [x] 更新 `types/v2/student.ts`
2. [x] 更新 `stores/v2/student.ts`
3. [x] 适配 `StudentListView.vue`
4. [x] 测试学生模块 V2

### Phase 3: 组织模块 (已完成)
1. [x] 更新组织相关类型和 API
2. [x] 适配组织管理视图
3. [x] 测试组织模块 V2

### Phase 4: 权限模块 (已完成)
1. [x] 更新权限相关类型和 API
2. [x] 适配权限管理视图
3. [x] 测试权限模块 V2

### Phase 5: 检查模块 (已完成)
1. [x] 更新检查相关类型和 API
2. [x] 适配检查管理视图
3. [x] 测试检查模块 V2

### Phase 6: 宿舍和任务模块 (已完成)
1. [x] 更新宿舍和任务相关类型和 API
2. [x] 适配相关视图
3. [x] 测试所有模块

### Phase 7: 最终测试 (待进行)
1. [ ] 完整功能测试
2. [ ] 确认所有 V2 页面正常工作
3. [ ] 准备正式切换

## 注意事项

1. **保持 V1 功能不变**: V2 视图的功能必须与 V1 完全一致
2. **渐进式迁移**: 可以逐个模块测试和验证
3. **字段名适配**: 前端使用 V2 新字段名，不依赖后端兼容性方法
4. **错误处理**: 确保 API 错误能正确显示给用户

## 验收标准

- [x] 所有 V2 页面在侧边栏可见并带 "(V2)" 标签
- [x] 学生列表显示正确的数据
- [x] CRUD 操作正常工作
- [x] 搜索和筛选功能正常
- [x] 分页功能正常
- [x] 导入导出功能正常（如适用）

## 事件处理器完成状态

### 已创建的事件处理器

1. **StudentEventHandler** - 学生领域事件处理
   - StudentEnrolledEvent - 学生入学通知
   - StudentStatusChangedEvent - 学籍状态变更通知
   - StudentUpdatedEvent - 学生信息更新通知

2. **AssetEventHandler** - 资产领域事件处理
   - BuildingCreatedEvent - 楼栋创建通知
   - BuildingUpdatedEvent - 楼栋更新通知
   - DormitoryCreatedEvent - 宿舍创建通知
   - StudentCheckedInEvent - 学生入住通知
   - StudentCheckedOutEvent - 学生退宿通知

3. **TaskEventHandler** - 任务领域事件处理
   - TaskCreatedEvent - 任务创建批量通知
   - TaskAcceptedEvent - 任务接受通知
   - TaskSubmittedEvent - 任务提交通知
   - TaskApprovedEvent - 任务审批通过通知
   - TaskRejectedEvent - 任务退回通知

## 状态更新 (2026-01-04)

**当前状态**: V2 页面已隐藏，继续使用 V1 页面

**原因分析**:
V2 页面是全新实现，而非在 V1 基础上重构，导致以下问题：
1. 功能缺失：缺少权限检查、年级筛选、导入功能等
2. API 不完整：后端缺少 search、export、import 等端点
3. 字段名不一致：name vs realName, status vs studentStatus

**后端 DDD 架构已完成**:
- 6个核心模块的后端 DDD 迁移
- 13个领域事件处理器的实现
- 通知系统集成

**前端策略调整**:
- V2 路由已隐藏 (`hidden: true`)
- 继续使用稳定的 V1 页面
- 后续如需要，可以渐进式地将 V1 页面迁移到 V2 API
