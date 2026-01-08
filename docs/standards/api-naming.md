# API 命名规范

**版本**: v1.0
**生效日期**: 2026-01-06

---

## 一、URL路径规范

### 1.1 基本规则

| 规则 | 正确示例 | 错误示例 |
|------|---------|---------|
| 使用小写字母 | `/api/v2/users` | `/api/v2/Users` |
| 使用连字符分隔 | `/api/v2/org-units` | `/api/v2/org_units` |
| 使用复数名词 | `/api/v2/students` | `/api/v2/student` |
| 避免动词 | `POST /api/v2/tasks` | `POST /api/v2/create-task` |
| 版本号前缀 | `/api/v2/...` | `/api/...` |

### 1.2 资源层级

```
/api/v2/{domain}/{resource}
/api/v2/{domain}/{resource}/{id}
/api/v2/{domain}/{resource}/{id}/{sub-resource}
```

**示例**:
```
GET  /api/v2/organization/classes           # 班级列表
GET  /api/v2/organization/classes/123       # 单个班级
GET  /api/v2/organization/classes/123/students  # 班级下的学生
POST /api/v2/inspection/records/123/submit  # 提交检查记录
```

### 1.3 领域分组

| 领域 | 路径前缀 | 包含资源 |
|------|---------|---------|
| organization | `/api/v2/organization/` | org-units, grades, classes, majors |
| access | `/api/v2/access/` | roles, permissions, user-roles |
| inspection | `/api/v2/inspection/` | templates, records, appeals |
| student | `/api/v2/student/` | students, enrollments |
| asset | `/api/v2/asset/` | buildings, dormitories |
| task | `/api/v2/task/` | tasks, workflows, approvals |

---

## 二、HTTP方法规范

### 2.1 标准CRUD

| 操作 | HTTP方法 | URL模式 | 示例 |
|------|---------|---------|------|
| 创建 | POST | `/{resource}` | `POST /classes` |
| 查询列表 | GET | `/{resource}` | `GET /classes` |
| 查询单个 | GET | `/{resource}/{id}` | `GET /classes/123` |
| 全量更新 | PUT | `/{resource}/{id}` | `PUT /classes/123` |
| 部分更新 | PATCH | `/{resource}/{id}` | `PATCH /classes/123` |
| 删除 | DELETE | `/{resource}/{id}` | `DELETE /classes/123` |

### 2.2 业务操作

对于非CRUD操作，使用POST + 动词：

```
POST /api/v2/inspection/records/{id}/submit     # 提交
POST /api/v2/inspection/records/{id}/approve    # 审批通过
POST /api/v2/inspection/records/{id}/reject     # 驳回
POST /api/v2/inspection/records/{id}/publish    # 发布
POST /api/v2/task/tasks/{id}/assign             # 分配
POST /api/v2/organization/classes/{id}/activate # 激活
POST /api/v2/organization/classes/{id}/graduate # 毕业
```

---

## 三、请求参数规范

### 3.1 查询参数

| 参数类型 | 命名规范 | 示例 |
|---------|---------|------|
| 分页 | `page`, `pageSize` | `?page=1&pageSize=20` |
| 排序 | `sortBy`, `sortOrder` | `?sortBy=createdAt&sortOrder=desc` |
| 过滤 | `{field}` | `?status=active&gradeId=123` |
| 搜索 | `keyword` | `?keyword=计算机` |
| 时间范围 | `{field}Start`, `{field}End` | `?createdAtStart=2026-01-01` |

### 3.2 路径参数

```
# 正确：使用ID
GET /api/v2/organization/classes/123

# 正确：使用唯一编码（明确标注）
GET /api/v2/organization/classes/code/{classCode}

# 错误：路径参数不明确
GET /api/v2/organization/classes/CS2024-01  # 是ID还是编码？
```

### 3.3 请求体

使用 camelCase 命名：
```json
{
  "className": "计算机科学与技术1班",
  "orgUnitId": 123,
  "enrollmentYear": 2024,
  "majorDirectionId": 456
}
```

---

## 四、响应格式规范

### 4.1 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-01-06T10:30:00Z"
}
```

### 4.2 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [ ... ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

### 4.3 错误响应

```json
{
  "code": 400,
  "message": "参数验证失败",
  "errors": [
    {
      "field": "className",
      "message": "班级名称不能为空"
    }
  ],
  "timestamp": "2026-01-06T10:30:00Z"
}
```

### 4.4 HTTP状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 | OK | 成功（查询、更新） |
| 201 | Created | 创建成功 |
| 204 | No Content | 删除成功 |
| 400 | Bad Request | 参数错误 |
| 401 | Unauthorized | 未登录 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 业务冲突（如编码重复） |
| 500 | Internal Error | 服务器错误 |

---

## 五、字段命名规范

### 5.1 通用字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 主键ID |
| `code` | String | 业务编码 |
| `name` | String | 名称 |
| `status` | String/Enum | 状态 |
| `enabled` | Boolean | 是否启用 |
| `sortOrder` | Integer | 排序号 |
| `remark` | String | 备注 |
| `createdAt` | DateTime | 创建时间 |
| `createdBy` | Long | 创建人ID |
| `updatedAt` | DateTime | 更新时间 |
| `updatedBy` | Long | 更新人ID |

### 5.2 关联字段

```
# 正确：使用 {entity}Id
orgUnitId, gradeId, classId, teacherId

# 错误：不一致的命名
org_unit_id, grade, class_id
```

### 5.3 布尔字段

```
# 正确：is前缀
isPublished, isEnabled, isPinned

# 或：无前缀（简洁）
published, enabled, pinned

# 选择一种风格并保持一致
```

---

## 六、版本管理

### 6.1 URL版本

```
/api/v1/...  # 旧版本（已废弃）
/api/v2/...  # 当前版本
/api/v3/...  # 未来版本（如需）
```

### 6.2 废弃策略

1. 新版本发布后，旧版本保留90天
2. 旧版本添加 `@Deprecated` 注解
3. 旧版本响应头添加 `Deprecation: true`
4. 文档明确标注迁移路径

---

## 七、示例API

### 7.1 班级管理

```
# 列表查询
GET /api/v2/organization/classes?page=1&pageSize=20&orgUnitId=123&status=ACTIVE

# 创建
POST /api/v2/organization/classes
Body: { "classCode": "CS2024-01", "className": "...", ... }

# 查询详情
GET /api/v2/organization/classes/123

# 更新
PUT /api/v2/organization/classes/123
Body: { "className": "...", "standardSize": 50 }

# 激活
POST /api/v2/organization/classes/123/activate

# 分配班主任
POST /api/v2/organization/classes/123/assign-teacher
Body: { "teacherId": 456, "role": "HEAD_TEACHER" }

# 删除
DELETE /api/v2/organization/classes/123
```

### 7.2 量化检查

```
# 创建检查记录
POST /api/v2/inspection/records
Body: { "templateId": 1, "inspectionDate": "2026-01-06", ... }

# 录入扣分
POST /api/v2/inspection/records/123/deductions
Body: { "classId": 456, "itemId": 789, "count": 2, ... }

# 提交审核
POST /api/v2/inspection/records/123/submit

# 审批通过
POST /api/v2/inspection/records/123/approve

# 发布
POST /api/v2/inspection/records/123/publish

# 申诉
POST /api/v2/inspection/appeals
Body: { "recordId": 123, "classId": 456, "reason": "..." }
```

---

**文档结束**
