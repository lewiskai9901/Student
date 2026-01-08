# V2 API Reference

V2 API基于DDD六边形架构实现，提供更清晰的领域边界和更好的可维护性。

## 概述

### 基础URL
```
http://localhost:8080/api/v2
```

### 认证
所有V2 API需要JWT认证（除登录接口外）。在请求头中添加：
```
Authorization: Bearer {access_token}
```

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  }
}
```

---

## 组织管理 API

### 组织单元 (OrgUnit)

#### 获取组织树
```http
GET /v2/org-units/tree
```

**权限**: `system:department:view`

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "code": "ROOT",
      "name": "学校",
      "type": "SCHOOL",
      "parentId": null,
      "children": [
        {
          "id": 2,
          "code": "DEPT001",
          "name": "信息工程系",
          "type": "DEPARTMENT",
          "parentId": 1,
          "children": []
        }
      ]
    }
  ]
}
```

#### 获取组织单元列表
```http
GET /v2/org-units
```

**权限**: `system:department:view`

#### 获取单个组织单元
```http
GET /v2/org-units/{id}
```

**权限**: `system:department:view`

#### 创建组织单元
```http
POST /v2/org-units
```

**权限**: `system:department:create`

**请求体**:
```json
{
  "code": "DEPT002",
  "name": "计算机系",
  "type": "DEPARTMENT",
  "parentId": 1
}
```

#### 更新组织单元
```http
PUT /v2/org-units/{id}
```

**权限**: `system:department:update`

#### 删除组织单元
```http
DELETE /v2/org-units/{id}
```

**权限**: `system:department:delete`

#### 启用/禁用组织单元
```http
PUT /v2/org-units/{id}/enable
PUT /v2/org-units/{id}/disable
```

**权限**: `system:department:update`

#### 分配负责人
```http
PUT /v2/org-units/{id}/leader
```

**权限**: `system:department:update`

**请求体**:
```json
{
  "leaderId": 123
}
```

---

### 班级 (SchoolClass)

#### 获取班级列表（分页）
```http
GET /v2/organization/classes
```

**权限**: `system:department:view`

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码（默认1） |
| size | int | 每页数量（默认10） |
| orgUnitId | long | 所属组织单元ID |
| status | string | 状态: PREPARING/ACTIVE/GRADUATED/DISSOLVED |

#### 获取班级详情
```http
GET /v2/organization/classes/{id}
```

**权限**: `system:department:view`

#### 根据编码获取班级
```http
GET /v2/organization/classes/code/{classCode}
```

**权限**: `system:department:view`

#### 创建班级
```http
POST /v2/organization/classes
```

**权限**: `system:department:create`

**请求体**:
```json
{
  "classCode": "2024CS01",
  "className": "2024级计算机1班",
  "orgUnitId": 2,
  "gradeLevel": 1,
  "enrollmentYear": 2024,
  "expectedGradYear": 2027,
  "maxStudents": 50
}
```

#### 更新班级
```http
PUT /v2/organization/classes/{id}
```

**权限**: `system:department:update`

#### 删除班级
```http
DELETE /v2/organization/classes/{id}
```

**权限**: `system:department:delete`

#### 班级状态操作
```http
POST /v2/organization/classes/{id}/activate   # 激活班级
POST /v2/organization/classes/{id}/graduate   # 班级毕业
POST /v2/organization/classes/{id}/dissolve   # 撤销班级
```

**权限**: `system:department:update`

#### 分配班主任
```http
POST /v2/organization/classes/{id}/head-teacher
```

**权限**: `system:department:update`

**请求体**:
```json
{
  "teacherId": 456,
  "startDate": "2024-09-01"
}
```

#### 分配副班主任
```http
POST /v2/organization/classes/{id}/deputy-head-teacher
```

**权限**: `system:department:update`

#### 结束教师任职
```http
POST /v2/organization/classes/{classId}/teachers/{teacherId}/end?role=HEAD_TEACHER
```

**权限**: `system:department:update`

---

## 权限管理 API

### 权限 (Permission)

#### 获取权限列表
```http
GET /v2/permissions
```

**权限**: `system:permission:view`

#### 获取权限树
```http
GET /v2/permissions/tree
```

**权限**: `system:permission:view`

#### 创建权限
```http
POST /v2/permissions
```

**权限**: `system:permission:create`

#### 更新权限
```http
PUT /v2/permissions/{id}
```

**权限**: `system:permission:update`

#### 删除权限
```http
DELETE /v2/permissions/{id}
```

**权限**: `system:permission:delete`

---

### 角色 (Role)

#### 获取角色列表
```http
GET /v2/roles
```

**权限**: `system:role:view`

#### 获取角色详情
```http
GET /v2/roles/{id}
```

**权限**: `system:role:view`

#### 创建角色
```http
POST /v2/roles
```

**权限**: `system:role:create`

#### 更新角色
```http
PUT /v2/roles/{id}
```

**权限**: `system:role:update`

#### 删除角色
```http
DELETE /v2/roles/{id}
```

**权限**: `system:role:delete`

#### 分配角色权限
```http
POST /v2/roles/{id}/permissions
```

**权限**: `system:role:update`

**请求体**:
```json
{
  "permissionIds": [1, 2, 3, 4]
}
```

---

## 量化检查 API

### 检查模板 (InspectionTemplate)

#### 获取模板列表
```http
GET /v2/inspection-templates
```

**权限**: `quantification:template:view`

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码 |
| size | int | 每页数量 |
| keyword | string | 关键词搜索 |

#### 获取模板详情
```http
GET /v2/inspection-templates/{id}
```

**权限**: `quantification:template:view`

#### 创建模板
```http
POST /v2/inspection-templates
```

**权限**: `quantification:template:create`

#### 更新模板
```http
PUT /v2/inspection-templates/{id}
```

**权限**: `quantification:template:update`

#### 删除模板
```http
DELETE /v2/inspection-templates/{id}
```

**权限**: `quantification:template:delete`

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 422 | 业务验证失败 |
| 500 | 服务器内部错误 |

---

## 与V1 API对比

| 功能 | V1 API | V2 API |
|------|--------|--------|
| 部门管理 | `/api/departments` | `/api/v2/org-units` |
| 班级管理 | `/api/classes` | `/api/v2/organization/classes` |
| 权限管理 | `/api/permissions` | `/api/v2/permissions` |
| 角色管理 | `/api/roles` | `/api/v2/roles` |
| 检查模板 | `/api/check-templates` | `/api/v2/inspection-templates` |

V2 API特点：
- 基于DDD领域模型设计
- 更清晰的领域边界
- 统一的错误处理
- 更完善的验证机制

---

*文档更新时间: 2026年1月2日*
