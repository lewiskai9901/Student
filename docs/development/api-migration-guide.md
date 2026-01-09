# V1 到 V2 API 迁移指南

本文档提供从 V1 API 迁移到 V2 (DDD架构) API 的完整指南。

## 概述

V2 API 采用领域驱动设计 (DDD) 架构，具有以下改进：
- 更清晰的领域边界
- 统一的错误处理
- 更好的代码可维护性
- 类型安全的聚合根和值对象

## API 路径映射

### 权限管理 (Access)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/permissions` | `POST /api/v2/permissions` | 创建权限 |
| `GET /api/permissions` | `GET /api/v2/permissions` | 获取权限列表 |
| `GET /api/permissions/{id}` | `GET /api/v2/permissions/{id}` | 获取权限详情 |
| `PUT /api/permissions/{id}` | `PUT /api/v2/permissions/{id}` | 更新权限 |
| `DELETE /api/permissions/{id}` | `DELETE /api/v2/permissions/{id}` | 删除权限 |
| `GET /api/permissions/tree` | `GET /api/v2/permissions/tree` | 获取权限树 |

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/roles` | `POST /api/v2/roles` | 创建角色 |
| `GET /api/roles` | `GET /api/v2/roles` | 获取角色列表 |
| `GET /api/roles/{id}` | `GET /api/v2/roles/{id}` | 获取角色详情 |
| `PUT /api/roles/{id}` | `PUT /api/v2/roles/{id}` | 更新角色 |
| `DELETE /api/roles/{id}` | `DELETE /api/v2/roles/{id}` | 删除角色 |
| `PUT /api/roles/{id}/permissions` | `PUT /api/v2/roles/{id}/permissions` | 设置角色权限 |

### 用户管理 (User)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/users` | `POST /api/v2/users` | 创建用户 |
| `GET /api/users` | `GET /api/v2/users` | 获取用户列表 |
| `GET /api/users/{id}` | `GET /api/v2/users/{id}` | 获取用户详情 |
| `PUT /api/users/{id}` | `PUT /api/v2/users/{id}` | 更新用户 |
| `DELETE /api/users/{id}` | `DELETE /api/v2/users/{id}` | 删除用户 |
| `GET /api/users/{userId}/roles` | `GET /api/v2/users/{userId}/roles` | 获取用户角色 |
| `PUT /api/users/{userId}/roles` | `PUT /api/v2/users/{userId}/roles` | 设置用户角色 |

**DDD 纯领域 API:**
| V2 API | 说明 |
|--------|------|
| `POST /api/v2/domain/users` | 创建用户 (DDD聚合根) |
| `GET /api/v2/domain/users/{id}` | 获取用户详情 |
| `PUT /api/v2/domain/users/{id}` | 更新用户 |
| `PUT /api/v2/domain/users/{id}/status` | 修改用户状态 |
| `POST /api/v2/domain/users/{id}/reset-password` | 重置密码 |

### 组织管理 (Organization)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `GET /api/departments` | `GET /api/v2/org-units/by-type/DEPARTMENT` | 获取部门列表 |
| `POST /api/departments` | `POST /api/v2/org-units` (type=DEPARTMENT) | 创建部门 |
| `GET /api/departments/tree` | `GET /api/v2/org-units/tree` | 获取组织树 |

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/grades` | `POST /api/v2/grades` | 创建年级 |
| `GET /api/grades` | `GET /api/v2/grades` | 获取年级列表 |
| `GET /api/grades/{id}` | `GET /api/v2/grades/{id}` | 获取年级详情 |
| `PUT /api/grades/{id}` | `PUT /api/v2/grades/{id}` | 更新年级 |
| `DELETE /api/grades/{id}` | `DELETE /api/v2/grades/{id}` | 删除年级 |

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/classes` | `POST /api/v2/organization/classes` | 创建班级 |
| `GET /api/classes` | `GET /api/v2/organization/classes` | 获取班级列表 |
| `GET /api/classes/{id}` | `GET /api/v2/organization/classes/{id}` | 获取班级详情 |
| `PUT /api/classes/{id}` | `PUT /api/v2/organization/classes/{id}` | 更新班级 |
| `DELETE /api/classes/{id}` | `DELETE /api/v2/organization/classes/{id}` | 删除班级 |

### 学生管理 (Student)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `POST /api/students` | `POST /api/v2/students` | 创建学生 |
| `GET /api/students` | `GET /api/v2/students` | 获取学生列表 |
| `GET /api/students/{id}` | `GET /api/v2/students/{id}` | 获取学生详情 |
| `PUT /api/students/{id}` | `PUT /api/v2/students/{id}` | 更新学生 |
| `DELETE /api/students/{id}` | `DELETE /api/v2/students/{id}` | 删除学生 |

### 宿舍管理 (Asset/Dormitory)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `GET /api/dormitories` | `GET /api/v2/dormitory/rooms` | 获取宿舍列表 |
| `GET /api/dormitories/{id}` | `GET /api/v2/dormitory/rooms/{id}` | 获取宿舍详情 |
| `POST /api/dormitories` | `POST /api/v2/dormitory/rooms` | 创建宿舍 |

### 量化检查 (Inspection)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `GET /api/check-templates` | `GET /api/v2/inspection-templates` | 获取检查模板 |
| `POST /api/check-templates` | `POST /api/v2/inspection-templates` | 创建检查模板 |
| `PUT /api/check-templates/{id}/publish` | `PUT /api/v2/inspection-templates/{id}/publish` | 发布模板 |

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `GET /api/daily-checks` | `GET /api/v2/inspection-records` | 获取检查记录 |
| `POST /api/daily-checks` | `POST /api/v2/inspection-records` | 创建检查记录 |

| V1 API | V2 API | 说明 |
|--------|--------|------|
| `GET /api/check-item-appeals` | `GET /api/v2/appeals` | 获取申诉列表 |
| `POST /api/check-item-appeals` | `POST /api/v2/appeals` | 创建申诉 |
| `PUT /api/check-item-appeals/{id}/review` | `PUT /api/v2/appeals/{id}/review` | 审核申诉 |

### 学期管理 (Semester)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| N/A | `POST /api/v2/semesters` | 创建学期 |
| N/A | `GET /api/v2/semesters` | 获取学期列表 |
| N/A | `GET /api/v2/semesters/{id}` | 获取学期详情 |
| N/A | `PUT /api/v2/semesters/{id}` | 更新学期 |
| N/A | `PUT /api/v2/semesters/{id}/activate` | 激活学期 |
| N/A | `PUT /api/v2/semesters/{id}/end` | 结束学期 |

**DDD 纯领域 API:**
| V2 API | 说明 |
|--------|------|
| `POST /api/v2/domain/semesters` | 创建学期 (DDD聚合根) |
| `GET /api/v2/domain/semesters/{id}` | 获取学期详情 |
| `PUT /api/v2/domain/semesters/{id}` | 更新学期 |
| `PUT /api/v2/domain/semesters/{id}/activate` | 激活学期 |
| `PUT /api/v2/domain/semesters/{id}/end` | 结束学期 |

### 评级管理 (Rating)

| V1 API | V2 API | 说明 |
|--------|--------|------|
| N/A | `POST /api/v2/ratings/configs` | 创建评级配置 |
| N/A | `GET /api/v2/ratings/configs` | 获取评级配置列表 |
| N/A | `POST /api/v2/ratings/results/calculate` | 计算评级结果 |
| N/A | `GET /api/v2/ratings/results` | 获取评级结果 |

## 响应格式变化

### V1 响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### V2 响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2026-01-09T10:30:00"
}
```

## 请求体变化示例

### 创建用户

**V1:**
```json
{
  "username": "zhangsan",
  "password": "password123",
  "realName": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "userType": 1
}
```

**V2:**
```json
{
  "username": "zhangsan",
  "password": "password123",
  "realName": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "userType": "ADMIN"
}
```

> 注意: V2 使用枚举字符串而非数字代码

### 创建班级

**V1:**
```json
{
  "className": "2024级1班",
  "gradeId": 1,
  "departmentId": 2,
  "headTeacherId": 100
}
```

**V2:**
```json
{
  "classCode": "2024-01",
  "className": "2024级1班",
  "orgUnitId": 2,
  "enrollmentYear": 2024,
  "majorDirectionId": 5
}
```

> 注意: V2 使用 `orgUnitId` 替代 `departmentId`，添加了 `classCode` 和 `enrollmentYear`

## 迁移建议

1. **逐步迁移**: 建议逐个模块进行迁移，而非一次性全部切换
2. **并行运行**: V1 和 V2 API 可以并行运行，方便渐进式迁移
3. **前端适配**: 前端需要更新 API 基础路径从 `/api/` 到 `/api/v2/`
4. **类型映射**: 注意处理枚举类型从数字到字符串的变化
5. **错误处理**: V2 API 返回更详细的错误信息，需要适配前端错误提示

## 前端 API 模块位置

V2 API 模块位于:
- `frontend/src/api/v2/organization.ts` - 组织管理
- `frontend/src/api/v2/inspection.ts` - 量化检查
- `frontend/src/api/v2/access.ts` - 权限管理
- `frontend/src/api/v2/user.ts` - 用户管理
- `frontend/src/api/v2/semester.ts` - 学期管理

## 联系方式

如在迁移过程中遇到问题，请联系开发团队。

---

*最后更新: 2026-01-09*
