# V5 重构方案 - 权限系统设计

> **版本**: 3.0
> **日期**: 2026-01-31
> **关联文档**: [V5_ARCHITECTURE.md](./V5_ARCHITECTURE.md)

---

## 一、概述

### 1.1 设计原则

- **部门不分配功能权限**：业界主流做法，部门只用于组织归属和数据范围
- **角色 = 功能权限 + 数据权限**：角色同时配置两个维度
- **无硬编码**：所有配置项从数据库读取，支持动态扩展
- **模块级数据权限**：同一角色在不同模块可有不同数据范围

### 1.2 权限判断流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              权限检查流程                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   用户请求: GET /api/inspection/records                                     │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 1: 功能权限检查 (Spring Security)                               │   │
│   │                                                                     │   │
│   │ @PreAuthorize("hasAuthority('inspection:record:view')")             │   │
│   │                                                                     │   │
│   │ 无权限 → 返回 403 Forbidden                                         │   │
│   │ 有权限 → 继续 Step 2                                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 2: 数据权限处理 (MyBatis Interceptor)                           │   │
│   │                                                                     │   │
│   │ @DataPermission(module = "inspection_record")                       │   │
│   │                                                                     │   │
│   │ 1. 获取用户所有角色在该模块的数据权限配置                             │   │
│   │ 2. 多角色取并集                                                     │   │
│   │ 3. 生成数据过滤 SQL 条件                                            │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                     │                                       │
│                                     ▼                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ Step 3: 执行查询 (自动过滤)                                          │   │
│   │                                                                     │   │
│   │ 原SQL:                                                              │   │
│   │   SELECT * FROM target_inspection_records WHERE task_id = ?         │   │
│   │                                                                     │   │
│   │ 过滤后 (DEPARTMENT_AND_BELOW):                                      │   │
│   │   SELECT * FROM target_inspection_records                           │   │
│   │   WHERE task_id = ?                                                 │   │
│   │   AND org_unit_id IN (1, 2, 3, 4)                                   │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、数据范围类型

### 2.1 预定义范围

| 范围代码 | 名称 | 优先级 | 计算方式 | 说明 |
|----------|------|--------|----------|------|
| ALL | 全部数据 | 100 | NONE | 无限制 |
| DEPARTMENT_AND_BELOW | 本部门及下级 | 80 | USER_ORG_TREE | 用户部门及其子部门 |
| DEPARTMENT | 仅本部门 | 60 | USER_ORG | 仅用户所属部门 |
| CUSTOM | 自定义范围 | 40 | CUSTOM_CONFIG | 管理员配置的指定范围 |
| SELF | 仅本人 | 20 | CREATOR | 仅自己创建/负责的数据 |

### 2.2 计算类型说明

| calc_type | 说明 | 计算逻辑 |
|-----------|------|----------|
| NONE | 无限制 | 不添加任何过滤条件 |
| USER_ORG | 用户部门 | `org_unit_id = 用户部门ID` |
| USER_ORG_TREE | 用户部门树 | `org_unit_id IN (用户部门及所有子部门)` |
| CUSTOM_CONFIG | 自定义配置 | 从 `role_data_scope_items` 表读取配置 |
| CREATOR | 创建者 | `created_by = 当前用户ID` |

---

## 三、数据模块配置

### 3.1 模块列表

| 模块代码 | 名称 | 所属领域 | 过滤字段 |
|----------|------|----------|----------|
| student | 学生信息 | organization | org_unit_id, class_id, created_by |
| school_class | 班级管理 | organization | org_unit_id |
| org_unit | 组织架构 | organization | id |
| dormitory_building | 楼栋管理 | space | org_unit_id |
| dormitory_room | 房间管理 | space | org_unit_id, building_id |
| dormitory_checkin | 入住管理 | space | org_unit_id, class_id |
| inspection_template | 检查模板 | inspection | org_unit_id, created_by |
| inspection_project | 检查项目 | inspection | org_unit_id, created_by |
| inspection_task | 检查任务 | inspection | org_unit_id |
| inspection_record | 检查记录 | inspection | org_unit_id, class_id |
| inspection_personal | 个人检查记录 | inspection | target_id (学生专用) |
| inspection_appeal | 申诉管理 | inspection | org_unit_id, class_id, created_by |
| inspection_summary | 检查汇总 | inspection | org_unit_id, class_id |
| inspection_corrective | 整改管理 | inspection | org_unit_id, class_id |
| system_user | 用户管理 | access | org_unit_id |

### 3.2 过滤字段配置说明

`filter_fields` JSON 配置格式：

```json
{
  "org_unit": "org_unit_id",    // 部门过滤字段
  "class": "class_id",          // 班级过滤字段
  "creator": "created_by",      // 创建者过滤字段
  "building": "building_id",    // 楼栋过滤字段
  "student": "target_id"        // 学生过滤字段（个人记录）
}
```

---

## 四、自定义范围配置

### 4.1 范围项类型

| 类型代码 | 名称 | 引用表 | 支持子级 | 适用模块 |
|----------|------|--------|----------|----------|
| ORG_UNIT | 部门 | org_units | ✅ | student, school_class, inspection_* |
| CLASS | 班级 | classes | ❌ | student, inspection_record |
| GRADE | 年级 | grades | ❌ | student, school_class |
| BUILDING | 楼栋 | space | ❌ | dormitory_room, dormitory_checkin |
| MAJOR | 专业 | majors | ❌ | student, school_class |

### 4.2 配置示例

```yaml
# 班主任管理两个班级
用户: 张老师
角色: 班主任
数据权限:
  student: CUSTOM
  inspection_record: CUSTOM
  自定义范围:
    - type: CLASS, id: 101, name: 2024级软件1班
    - type: CLASS, id: 102, name: 2024级软件2班

# 宿管员管理两栋楼
用户: 李阿姨
角色: 宿管员
数据权限:
  dormitory_room: CUSTOM
  dormitory_checkin: CUSTOM
  自定义范围:
    - type: BUILDING, id: 1, name: 1号楼
    - type: BUILDING, id: 2, name: 2号楼
```

---

## 五、多角色权限合并

### 5.1 合并策略

当用户拥有多个角色时，采用 **并集(UNION)** 策略：

1. **ALL 优先**：任一角色有 ALL 权限，直接返回 ALL
2. **层级合并**：DEPARTMENT_AND_BELOW > DEPARTMENT
3. **自定义合并**：所有 CUSTOM 范围取并集
4. **SELF 合并**：SELF 与其他范围用 OR 连接

### 5.2 示例

```java
// 张老师有两个角色:
// - 班主任: inspection_record → CUSTOM (101, 102班)
// - 检查员: inspection_record → SELF

// 合并后的 SQL:
WHERE (
    class_id IN (101, 102)    -- 班主任的自定义范围
    OR created_by = 12345     -- 检查员的仅本人
)
```

---

## 六、典型角色配置

### 6.1 超级管理员

```yaml
SUPER_ADMIN:
  功能权限: "*"
  数据权限:
    所有模块: ALL
```

### 6.2 系部主任

```yaml
DEPT_DIRECTOR:
  功能权限:
    - student:*
    - school_class:*
    - dormitory:*:view
    - inspection:*
  数据权限:
    student: DEPARTMENT_AND_BELOW
    school_class: DEPARTMENT_AND_BELOW
    inspection_record: DEPARTMENT_AND_BELOW
    inspection_appeal: DEPARTMENT_AND_BELOW
```

### 6.3 班主任

```yaml
CLASS_TEACHER:
  功能权限:
    - student:info:view
    - student:info:update
    - inspection:record:view
    - inspection:appeal:view
    - inspection:appeal:create
    - inspection:corrective:view
    - inspection:corrective:execute
  数据权限:
    student: CUSTOM
    inspection_record: CUSTOM
    inspection_appeal: CUSTOM
    inspection_corrective: CUSTOM
  自定义范围:
    - type: CLASS, id: {管理的班级ID}
```

### 6.4 检查员

```yaml
INSPECTOR:
  功能权限:
    - inspection:task:view
    - inspection:task:execute
    - inspection:record:view
    - inspection:record:create
    - inspection:record:update
  数据权限:
    inspection_task: SELF
    inspection_record: SELF
```

### 6.5 学生

```yaml
STUDENT:
  功能权限:
    - inspection:personal:view
    - inspection:appeal:view
    - inspection:appeal:create
  数据权限:
    inspection_personal: SELF  # 使用 target_id 过滤
    inspection_appeal: SELF
```

---

## 七、缓存策略

### 7.1 缓存结构

```
Redis Key 结构:
├── user:permission:{userId}     # 用户功能权限集合
├── user:data_scope:{userId}:{moduleCode}  # 用户在模块的数据范围
└── permission:cache:invalidate  # 缓存失效通知频道
```

### 7.2 失效策略

- **角色权限变更**：发布消息到 Redis 频道，所有实例清除相关用户缓存
- **用户角色变更**：立即清除该用户的权限缓存
- **缓存过期**：设置合理的 TTL（建议 30 分钟）

---

## 八、API 设计

### 8.1 数据权限配置 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /roles/data-permissions/modules | 获取所有数据模块（按领域分组） |
| GET | /roles/data-permissions/scopes | 获取所有数据范围类型 |
| GET | /roles/data-permissions/scope-item-types | 获取自定义范围项类型 |
| GET | /roles/data-permissions/scope-items | 获取自定义范围可选项 |
| GET | /roles/{roleId}/data-permissions | 获取角色数据权限配置 |
| PUT | /roles/{roleId}/data-permissions | 更新角色数据权限配置 |

### 8.2 请求/响应格式

**获取角色配置响应**：

```json
{
  "roleId": 1,
  "roleName": "班主任",
  "modulePermissions": [
    {
      "moduleCode": "student",
      "scopeCode": "CUSTOM",
      "scopeItems": [
        {
          "itemTypeCode": "CLASS",
          "scopeId": 101,
          "scopeName": "2024级软件1班",
          "includeChildren": false
        }
      ]
    }
  ]
}
```

**保存角色配置请求**：

```json
{
  "roleId": 1,
  "modulePermissions": [
    {
      "moduleCode": "student",
      "scopeCode": "CUSTOM",
      "scopeItems": [
        { "itemTypeCode": "CLASS", "scopeId": 101, "includeChildren": false },
        { "itemTypeCode": "CLASS", "scopeId": 102, "includeChildren": false }
      ]
    },
    {
      "moduleCode": "inspection_record",
      "scopeCode": "DEPARTMENT_AND_BELOW",
      "scopeItems": []
    }
  ]
}
```

---

## 九、前端组件

### 9.1 组件列表

| 组件 | 路径 | 说明 |
|------|------|------|
| RoleDataPermissionDialog | components/permission/ | 角色数据权限配置对话框 |
| CustomScopeItemDialog | components/permission/ | 自定义范围项配置对话框 |

### 9.2 使用方式

```vue
<template>
  <RoleDataPermissionDialog
    v-model="dialogVisible"
    :role="currentRole"
  />
</template>
```

---

**文档版本**: 3.0
**最后更新**: 2026-01-31
