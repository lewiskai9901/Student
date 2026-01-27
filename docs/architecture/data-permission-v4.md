# 数据权限架构 V4.0

> 文档版本: 4.0.0
> 更新日期: 2026-01-11
> 状态: 已实施

## 概述

V4.0 版本简化了数据权限架构，废弃用户级数据权限配置，统一使用角色级配置。这一变更减少了配置复杂性，使权限管理更加清晰和易于审计。

## 设计原则

**单一配置点**：数据权限仅在角色管理中配置，不再支持用户级配置。

**简化优先**：学校管理系统的权限模型相对固定，不需要用户级别的细粒度配置。

**审计友好**：所有权限配置都在角色层面，便于统一管理和审计。

## 数据范围类型

| DataScope | 说明 | 优先级 |
|-----------|------|--------|
| `ALL` | 全部数据 | 100 |
| `DEPARTMENT_AND_BELOW` | 本部门及下属 | 80 |
| `DEPARTMENT` | 仅本部门 | 60 |
| `CUSTOM` | 自定义范围 | 40 |
| `SELF` | 仅本人数据 | 20 |

## 权限解析流程

```
用户请求 → 获取用户角色ID列表
         ↓
    查询角色数据权限配置 (role_data_permissions)
         ↓
    取最高优先级权限 (ALL > DEPT > CUSTOM > SELF)
         ↓
    如果是 CUSTOM，查询 role_custom_scope
         ↓
    应用权限过滤
```

## 数据库表

### 角色数据权限表 (role_data_permissions)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| role_id | bigint | 角色ID |
| module_code | varchar(50) | 模块编码 (如: student, inspection_record) |
| data_scope | varchar(20) | 数据范围代码 |
| created_at | datetime | 创建时间 |

### 角色自定义范围表 (role_custom_scope)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| role_id | bigint | 角色ID |
| module_code | varchar(50) | 模块编码 |
| scope_type | varchar(20) | ORG_UNIT / GRADE / CLASS |
| target_id | bigint | 目标ID |
| created_at | datetime | 创建时间 |

## 班主任多班级支持

V4.0 修复了班主任只能查看一个班级的问题：

- **旧方案**：`User.managedClassId` 单一字段（已废弃）
- **新方案**：通过 `Class.teacherId` 反查，支持一个老师管理多个班级

```java
// 获取班主任管理的所有班级
List<Long> managedClassIds = classMapper.selectManagedClassIds(userId);
```

## 废弃的组件

以下组件已标记为 `@Deprecated(since = "4.0.0", forRemoval = true)`：

### 后端

| 类型 | 文件 |
|------|------|
| Controller | `UserDataScopeController.java` |
| Service | `UserDataScopeService.java` |
| ServiceImpl | `UserDataScopeServiceImpl.java` |
| Entity | `UserDataScope.java` |
| DTO | `UserDataScopeDTO.java`, `UserDataScopeRequest.java` |
| Mapper | `UserDataScopeMapper.java` |

### 前端

| 类型 | 文件 |
|------|------|
| Component | `UserDataScopeManager.vue` (标记废弃) |
| API | user-data-scope 相关 API |

### 数据库

| 表名 | 状态 |
|------|------|
| `user_data_scopes` | 已清空并标记废弃 |
| `users.managed_class_id` | 字段已删除 |

## API 变更

### 已废弃的 API

- `GET /api/user-data-scopes/user/{userId}` - 获取用户数据范围
- `POST /api/user-data-scopes` - 添加用户数据范围
- `DELETE /api/user-data-scopes/{id}` - 删除用户数据范围

### 推荐使用的 API

- `GET /api/v2/roles/{roleId}/data-permissions` - 获取角色数据权限配置
- `PUT /api/v2/roles/{roleId}/data-permissions` - 更新角色数据权限配置

## 迁移指南

### 从 V3 迁移到 V4

1. **运行数据库迁移脚本**
   ```sql
   -- 执行迁移脚本
   source database/migrations/V20260111__deprecate_user_data_scopes.sql
   ```

2. **检查角色数据权限配置**
   - 确保需要的数据权限已在角色级别配置
   - 验证班主任角色能够看到其管理的所有班级

3. **前端更新**
   - 用户管理页面不再显示"数据范围"按钮
   - 数据权限配置移至角色管理页面

## 核心代码

### DataPermissionServiceImpl

```java
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private final RoleDataPermissionService roleDataPermissionService;
    private final RoleDataPermissionMapper roleDataPermissionMapper;
    private final ClassMapper classMapper;

    @Override
    public DataScope getDataScope(String moduleCode) {
        // 仅使用角色配置，不再查询 user_data_scopes
        List<Long> roleIds = userMapper.findRoleIdsByUserId(userId);
        List<RoleDataPermission> permissions =
            roleDataPermissionMapper.selectByRoleIdsAndModule(roleIds, moduleCode);
        // 取最高优先级
        return calculateHighestScope(permissions);
    }

    private List<Long> getUserOwnClassIds(CustomUserDetails userDetails) {
        Set<Long> classIds = new HashSet<>();
        // 用户所属班级
        if (userDetails.getClassId() != null) {
            classIds.add(userDetails.getClassId());
        }
        // 管理的班级 - 通过 Class.teacherId 查询
        List<Long> managedClassIds = classMapper.selectManagedClassIds(userId);
        if (managedClassIds != null) {
            classIds.addAll(managedClassIds);
        }
        return new ArrayList<>(classIds);
    }
}
```

## 测试验证

完成迁移后，验证以下场景：

- [ ] 管理员可以看到所有数据
- [ ] 部门管理员只能看到本部门数据
- [ ] 班主任可以看到其管理的所有班级学生（包括多班级场景）
- [ ] 普通教师只能看到自己的数据
- [ ] 学生只能看到自己的数据
- [ ] 角色数据权限配置页面正常工作
- [ ] 用户管理页面不再显示数据范围配置入口
