# 代码废弃路线图

本文档跟踪项目中所有标记为 `@Deprecated` 的代码及其迁移计划。

## 废弃策略

| 阶段 | 版本 | 说明 |
|------|------|------|
| 标记废弃 | 2.0.0 | 添加 `@Deprecated` 注解，保持功能可用 |
| 迁移警告 | 3.0.0 | 日志输出废弃警告，推荐使用新API |
| 移除计划 | 4.0.0 | 设置 `forRemoval = true` |
| 完全移除 | 5.0.0 | 删除废弃代码 |

---

## V1 Controllers (计划移除: 5.0.0)

以下控制器已有V2 DDD版本替代，保留用于向后兼容：

| 废弃控制器 | V2 替代 | 状态 |
|-----------|---------|------|
| `TaskController` | `TaskControllerV2` | ✅ 已迁移 |
| `TaskApprovalController` | `TaskControllerV2` | ✅ 已迁移 |
| `StudentController` | `StudentControllerV2` | 📋 计划中 |
| `RoleController` | `RoleControllerV2` | ✅ 已迁移 |
| `DepartmentController` | `OrgUnitController` | ✅ 已迁移 |
| `ClassController` | `SchoolClassController` | ✅ 已迁移 |
| `PermissionController` | `PermissionControllerV2` | ✅ 已迁移 |
| `CheckTemplateController` | `InspectionTemplateController` | ✅ 已迁移 |
| `GradeController` | `OrgUnitController` | ✅ 已迁移 |
| `DormitoryController` | `DormitoryControllerV2` | 📋 计划中 |
| `CheckRecordController` | `InspectionRecordController` | ✅ 已迁移 |
| `CheckItemAppealController` | `AppealController` | ✅ 已迁移 |

### 迁移说明

```
V1 路径                    →  V2 路径
/api/tasks/*              →  /api/v2/tasks/*
/api/departments/*        →  /api/v2/org-units/*
/api/classes/*            →  /api/v2/organization/classes/*
/api/check-templates/*    →  /api/v2/inspection-templates/*
/api/check-records/*      →  /api/v2/inspection/records/*
/api/appeals/*            →  /api/v2/inspection/appeals/*
```

---

## Entity 字段废弃

### Dormitory.roomType

| 字段 | 废弃原因 | 替代方案 |
|------|----------|----------|
| `roomType` | 已改为使用 `maxOccupancy` 直接指定人数 | 使用 `maxOccupancy` 字段 |

**迁移SQL:**
```sql
-- 迁移数据后可删除 room_type 列
UPDATE dormitory SET max_occupancy =
  CASE room_type
    WHEN 1 THEN 4
    WHEN 2 THEN 6
    WHEN 3 THEN 8
    ELSE 4
  END
WHERE max_occupancy IS NULL;
```

### ClassWeightConfig 字段

| 字段 | 废弃原因 | 替代方案 |
|------|----------|----------|
| `useFixedStandard` | 逻辑已重构 | 使用 `standardMode` 字段 |
| `applyToAll` | 已移除批量应用逻辑 | 逐个配置 |
| `effectiveDate` | 改为使用检查计划日期 | `CheckPlan.startDate` |
| `expireDate` | 改为使用检查计划日期 | `CheckPlan.endDate` |
| `semesterId` | 改为关联检查计划 | `CheckPlan.semesterId` |
| `applyScope` | 已重构为更细粒度控制 | 移除 |

### ClassSizeStandard 方法

| 方法 | 废弃原因 | 替代方案 |
|------|----------|----------|
| `getLocked()` | 命名不清晰 | 使用 `getStandardMode()` |
| `setLocked()` | 命名不清晰 | 使用 `setStandardMode()` |

---

## Service 废弃

### DataPermissionServiceImpl (移除: 4.0.0)

**原因:** 已重构为Casbin权限系统

**替代方案:** 使用 `CasbinPermissionService`

**迁移步骤:**
1. 确保所有权限规则已迁移到Casbin
2. 更新调用方使用新服务
3. 运行测试验证
4. 移除旧实现

### DormitoryServiceImpl 方法

| 方法 | 废弃原因 | 替代方案 |
|------|----------|----------|
| `getMaxOccupancyByRoomType()` | roomType已废弃 | 直接使用 `maxOccupancy` |
| `getDormitoriesBySupervisorId()` | 宿管改为楼级别 | 使用 `BuildingService` |

---

## 前端迁移指南

### API 客户端更新

```typescript
// 旧 (废弃)
import { taskApi } from '@/api/task'
taskApi.list(params)

// 新 (推荐)
import { taskApi } from '@/api/v2/task'
taskApi.list(params)
```

### 路由更新

```typescript
// 旧路由
{ path: '/tasks', component: TaskList }

// 新路由
{ path: '/v2/tasks', component: TaskListV2 }
```

---

## 清理检查清单

### 移除废弃代码前检查

- [ ] 所有调用方已迁移到新API
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 前端已更新
- [ ] 文档已更新
- [ ] 数据库迁移脚本已准备
- [ ] 回滚方案已准备

### 版本发布检查

- [ ] 更新 CHANGELOG
- [ ] 更新 API 文档
- [ ] 通知相关团队
- [ ] 设置过渡期

---

## 时间线

| 版本 | 时间 | 动作 |
|------|------|------|
| 2.0.0 | 2026 Q1 | V2 API发布，V1标记废弃 |
| 3.0.0 | 2026 Q2 | V1 API输出废弃警告 |
| 4.0.0 | 2026 Q3 | V1 API标记forRemoval |
| 5.0.0 | 2026 Q4 | 移除V1 API |

---

## 联系人

废弃代码相关问题请联系:
- 架构师: xxx
- 后端负责人: xxx
