# 简化数据权限架构 - 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 废弃用户级数据权限配置，统一使用角色级配置，简化权限架构

**Architecture:**
- 删除 `user_data_scopes` 表及相关代码
- 删除 `User.managedClassId` 冗余字段
- 修改 `DataPermissionServiceImpl` 仅使用角色配置
- 修复多班级班主任只取第一个班的问题

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Vue 3, TypeScript

---

## 变更概览

### 需要删除的文件

| 层级 | 文件 |
|------|------|
| Controller | `UserDataScopeController.java` |
| Service | `UserDataScopeService.java`, `UserDataScopeServiceImpl.java` |
| Mapper | `UserDataScopeMapper.java`, `UserDataScopeMapper.xml` |
| Entity | `UserDataScope.java` |
| DTO | `UserDataScopeDTO.java`, `UserDataScopeRequest.java` |
| Enum | `ScopeType.java` |
| Frontend | `UserDataScopeManager.vue`, 相关 API |

### 需要修改的文件

| 文件 | 修改内容 |
|------|----------|
| `DataPermissionServiceImpl.java` | 移除对 UserDataScopeService 的依赖 |
| `User.java` | 删除 `managedClassId` 字段 |
| `CustomUserDetails.java` | 删除 `managedClassId`, `managedClassIds` |
| `StudentServiceImpl.java` | 修复多班级班主任逻辑 |
| `UsersView.vue` | 移除数据范围配置UI |

---

## Task 1: 创建数据库迁移脚本

**Files:**
- Create: `database/migrations/V20260111__deprecate_user_data_scopes.sql`

**Step 1: 创建迁移脚本**

```sql
-- V20260111__deprecate_user_data_scopes.sql
-- 废弃用户级数据权限配置，统一使用角色级配置

-- 1. 备份 user_data_scopes 表数据（如果有）
CREATE TABLE IF NOT EXISTS user_data_scopes_backup_20260111 AS
SELECT * FROM user_data_scopes WHERE deleted = 0;

-- 2. 记录迁移日志
INSERT INTO migration_log (migration_name, executed_at, description) VALUES
('V20260111__deprecate_user_data_scopes', NOW(), '废弃用户级数据权限配置，数据已备份到 user_data_scopes_backup_20260111');

-- 3. 软删除所有用户数据范围记录
UPDATE user_data_scopes SET deleted = 1, updated_at = NOW() WHERE deleted = 0;

-- 4. 删除 users 表的 managed_class_id 字段（如果存在）
-- 注意：先检查字段是否存在
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'users'
               AND COLUMN_NAME = 'managed_class_id');

SET @query := IF(@exist > 0,
    'ALTER TABLE users DROP COLUMN managed_class_id',
    'SELECT "Column managed_class_id does not exist"');

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 添加注释说明表已废弃
ALTER TABLE user_data_scopes COMMENT = '[DEPRECATED] 用户数据范围表 - 已废弃，使用角色级配置 role_data_permissions';
```

**Step 2: 验证迁移脚本语法**

```bash
cd backend && mysql -u root -p123456 student_management -e "SOURCE ../database/migrations/V20260111__deprecate_user_data_scopes.sql" --verbose
```

**Step 3: Commit**

```bash
git add database/migrations/V20260111__deprecate_user_data_scopes.sql
git commit -m "$(cat <<'EOF'
chore(db): add migration to deprecate user_data_scopes

- Backup existing user_data_scopes data
- Soft delete all records
- Remove users.managed_class_id column
- Mark table as deprecated

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: 修改 DataPermissionServiceImpl - 移除用户级依赖

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/impl/DataPermissionServiceImpl.java`

**Step 1: 修改 DataPermissionServiceImpl**

移除对 `UserDataScopeService` 的依赖，改为使用角色配置：

```java
package com.school.management.service.impl;

import com.school.management.domain.access.model.DataScope;
import com.school.management.entity.RoleDataPermission;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.RoleDataPermissionMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.service.RoleDataPermissionService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 数据权限服务实现 V4.0
 *
 * 简化版本：仅使用角色级数据权限配置
 * - 从 role_data_permissions 获取权限范围
 * - 从 role_custom_scope 获取自定义范围
 * - 不再使用 user_data_scopes（已废弃）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "false", matchIfMissing = true)
public class DataPermissionServiceImpl implements DataPermissionService {

    private final RoleDataPermissionService roleDataPermissionService;
    private final RoleDataPermissionMapper roleDataPermissionMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;

    @Override
    public DataScope getDataScope(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return DataScope.SELF;
        }

        Long userId = userDetails.getUserId();
        List<Long> roleIds = userMapper.findRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return DataScope.SELF;
        }

        // 查询角色对应模块的数据权限配置
        List<RoleDataPermission> permissions = roleDataPermissionMapper.selectByRoleIdsAndModule(roleIds, moduleCode);
        if (permissions == null || permissions.isEmpty()) {
            return DataScope.SELF;
        }

        // 取最高权限（ALL > DEPARTMENT_AND_BELOW > DEPARTMENT > CUSTOM > SELF）
        DataScope highestScope = DataScope.SELF;
        for (RoleDataPermission p : permissions) {
            DataScope scope = DataScope.fromCode(p.getDataScope());
            if (scope == DataScope.ALL) {
                return DataScope.ALL;
            }
            if (compareScopePriority(scope, highestScope) > 0) {
                highestScope = scope;
            }
        }

        return highestScope;
    }

    @Override
    public List<Long> getAccessibleClassIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Long userId = userDetails.getUserId();
        DataScope scope = getDataScope(moduleCode);

        switch (scope) {
            case ALL:
                return null; // null 表示不限制
            case DEPARTMENT_AND_BELOW:
            case DEPARTMENT:
                // 通过用户部门获取班级
                Long deptId = userDetails.getDepartmentId();
                if (deptId != null) {
                    return classMapper.selectIdsByDepartmentId(deptId);
                }
                return Collections.emptyList();
            case CUSTOM:
                // 从角色自定义范围获取
                return getCustomAccessibleClassIds(userId, moduleCode);
            default:
                // SELF: 返回用户所属班级（如果是学生）或管理的班级（如果是班主任）
                return getUserOwnClassIds(userDetails);
        }
    }

    @Override
    public List<Long> getAccessibleDepartmentIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        DataScope scope = getDataScope(moduleCode);

        switch (scope) {
            case ALL:
                return null; // null 表示不限制
            case DEPARTMENT_AND_BELOW:
            case DEPARTMENT:
            case CUSTOM:
                Long deptId = userDetails.getDepartmentId();
                if (deptId != null) {
                    return Collections.singletonList(deptId);
                }
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public boolean canAccessClass(Long classId, String moduleCode) {
        if (classId == null) {
            return true;
        }

        DataScope scope = getDataScope(moduleCode);
        if (scope == DataScope.ALL) {
            return true;
        }

        List<Long> accessibleIds = getAccessibleClassIds(moduleCode);
        if (accessibleIds == null) {
            return true; // null 表示不限制
        }

        return accessibleIds.contains(classId);
    }

    @Override
    public boolean canAccessStudent(Long studentId, String moduleCode) {
        if (studentId == null) {
            return true;
        }

        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        if (hasAllDataPermission(moduleCode)) {
            return true;
        }

        // 获取学生信息
        var student = studentMapper.selectById(studentId);
        if (student == null) {
            return false;
        }

        // 检查是否是学生本人
        if (student.getUserId() != null && student.getUserId().equals(userDetails.getUserId())) {
            return true;
        }

        // 通过班级判断
        return canAccessClass(student.getClassId(), moduleCode);
    }

    @Override
    public boolean hasAllDataPermission(String moduleCode) {
        return getDataScope(moduleCode) == DataScope.ALL;
    }

    // ==================== 私有方法 ====================

    /**
     * 比较两个 DataScope 的优先级
     * 返回正数表示 a 优先级更高
     */
    private int compareScopePriority(DataScope a, DataScope b) {
        return getPriority(a) - getPriority(b);
    }

    private int getPriority(DataScope scope) {
        switch (scope) {
            case ALL: return 100;
            case DEPARTMENT_AND_BELOW: return 80;
            case DEPARTMENT: return 60;
            case CUSTOM: return 40;
            case SELF: return 20;
            default: return 0;
        }
    }

    /**
     * 从角色自定义范围获取可访问的班级ID
     */
    private List<Long> getCustomAccessibleClassIds(Long userId, String moduleCode) {
        List<Long> roleIds = userMapper.findRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> classIds = new HashSet<>();
        for (Long roleId : roleIds) {
            var config = roleDataPermissionService.getRolePermissionConfigV2(roleId);
            if (config != null && config.getModulePermissions() != null) {
                for (var mp : config.getModulePermissions()) {
                    if (moduleCode.equals(mp.getModuleCode()) && mp.getCustomScope() != null) {
                        if (mp.getCustomScope().getClassIds() != null) {
                            classIds.addAll(mp.getCustomScope().getClassIds());
                        }
                    }
                }
            }
        }

        return new ArrayList<>(classIds);
    }

    /**
     * 获取用户自己的班级ID（所属班级或管理的班级）
     */
    private List<Long> getUserOwnClassIds(CustomUserDetails userDetails) {
        Set<Long> classIds = new HashSet<>();

        // 用户所属班级（学生身份）
        if (userDetails.getClassId() != null) {
            classIds.add(userDetails.getClassId());
        }

        // 用户管理的班级（班主任身份）- 通过 Class.teacherId 反查
        Long userId = userDetails.getUserId();
        List<Long> managedClassIds = classMapper.selectIdsByTeacherId(userId);
        if (managedClassIds != null) {
            classIds.addAll(managedClassIds);
        }

        return new ArrayList<>(classIds);
    }
}
```

**Step 2: 添加 ClassMapper 方法**

在 `ClassMapper.java` 添加新方法：

```java
/**
 * 根据教师ID查询班级ID列表
 */
List<Long> selectIdsByTeacherId(@Param("teacherId") Long teacherId);

/**
 * 根据部门ID查询班级ID列表
 */
List<Long> selectIdsByDepartmentId(@Param("departmentId") Long departmentId);
```

**Step 3: 添加 ClassMapper.xml 实现**

```xml
<select id="selectIdsByTeacherId" resultType="java.lang.Long">
    SELECT id FROM classes
    WHERE teacher_id = #{teacherId}
    AND deleted = 0
</select>

<select id="selectIdsByDepartmentId" resultType="java.lang.Long">
    SELECT id FROM classes
    WHERE department_id = #{departmentId}
    AND deleted = 0
</select>
```

**Step 4: 运行测试验证**

```bash
cd backend && mvn test -Dtest=DataPermissionServiceImplTest -DfailIfNoTests=false
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/service/impl/DataPermissionServiceImpl.java
git add backend/src/main/java/com/school/management/mapper/ClassMapper.java
git add backend/src/main/resources/mapper/ClassMapper.xml
git commit -m "$(cat <<'EOF'
refactor(permission): simplify DataPermissionServiceImpl to use role-only config

- Remove dependency on UserDataScopeService
- Use role_data_permissions for scope resolution
- Add ClassMapper methods for teacher/department class lookup
- Support multi-class teachers via Class.teacherId lookup

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: 修复 StudentServiceImpl 多班级班主任问题

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/impl/StudentServiceImpl.java`

**Step 1: 修改 applyClassTeacherFilter 方法**

将硬编码的 "取第一个班" 改为支持多班级：

```java
/**
 * 应用班主任权限过滤
 * 如果当前用户是班主任,则只返回其管理的所有班级的学生
 */
private void applyClassTeacherFilter(StudentQueryRequest request) {
    if (isClassTeacher()) {
        List<Long> managedClassIds = getManagedClassIds();

        if (managedClassIds.isEmpty()) {
            log.warn("班主任用户 {} 未管理任何班级,将无法查看学生数据", SecurityUtils.getCurrentUserId());
            // 设置一个不可能存在的班级ID,使查询结果为空
            request.setClassId(-1L);
        } else if (managedClassIds.size() == 1) {
            // 只管理一个班级,直接设置classId过滤
            log.info("班主任用户 {} 管理班级: {}", SecurityUtils.getCurrentUserId(), managedClassIds);
            request.setClassId(managedClassIds.get(0));
        } else {
            // 管理多个班级,设置classIds列表
            log.info("班主任用户 {} 管理多个班级: {}", SecurityUtils.getCurrentUserId(), managedClassIds);
            request.setClassIds(managedClassIds);
            request.setClassId(null); // 清除单个classId
        }
    }
}
```

**Step 2: 确保 StudentQueryRequest 支持 classIds**

检查并添加 `classIds` 字段（如果不存在）：

```java
// 在 StudentQueryRequest.java 中
private List<Long> classIds;

public List<Long> getClassIds() { return classIds; }
public void setClassIds(List<Long> classIds) { this.classIds = classIds; }
```

**Step 3: 修改查询逻辑支持 classIds**

在 StudentMapper.xml 中确保查询支持多班级：

```xml
<if test="classIds != null and classIds.size() > 0">
    AND s.class_id IN
    <foreach collection="classIds" item="cid" open="(" separator="," close=")">
        #{cid}
    </foreach>
</if>
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/service/impl/StudentServiceImpl.java
git add backend/src/main/java/com/school/management/dto/request/StudentQueryRequest.java
git add backend/src/main/resources/mapper/StudentMapper.xml
git commit -m "$(cat <<'EOF'
fix(student): support multi-class teacher filtering

- Allow teachers to manage multiple classes
- Add classIds field to StudentQueryRequest
- Update StudentMapper to support IN clause for classIds

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: 删除 User.managedClassId 字段

**Files:**
- Modify: `backend/src/main/java/com/school/management/entity/User.java`
- Modify: `backend/src/main/java/com/school/management/security/CustomUserDetails.java`
- Modify: `backend/src/main/java/com/school/management/domain/user/model/aggregate/User.java`

**Step 1: 删除 Entity 中的字段**

从 `entity/User.java` 删除：

```java
// 删除以下字段
// private Long managedClassId;
```

**Step 2: 删除 CustomUserDetails 中的字段**

从 `security/CustomUserDetails.java` 删除：

```java
// 删除以下字段
// private Long managedClassId;
// private List<Long> managedClassIds;

// 删除构造函数中的相关代码
// this.managedClassId = user.getManagedClassId();
```

**Step 3: 删除 Domain User 中的字段**

从 `domain/user/model/aggregate/User.java` 删除：

```java
// 删除以下字段和方法
// private Long managedClassId;
// public Long getManagedClassId() { ... }
// public void setManagedClassId(Long classId) { ... }
```

**Step 4: 编译验证**

```bash
cd backend && mvn compile -DskipTests
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/entity/User.java
git add backend/src/main/java/com/school/management/security/CustomUserDetails.java
git add backend/src/main/java/com/school/management/domain/user/model/aggregate/User.java
git commit -m "$(cat <<'EOF'
refactor(user): remove redundant managedClassId field

Field is redundant - teacher's classes determined via Class.teacherId lookup

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: 标记用户数据范围相关代码为 Deprecated

**Files:**
- Modify: `backend/src/main/java/com/school/management/controller/UserDataScopeController.java`
- Modify: `backend/src/main/java/com/school/management/service/UserDataScopeService.java`
- Modify: `backend/src/main/java/com/school/management/service/impl/UserDataScopeServiceImpl.java`

**Step 1: 添加 @Deprecated 注解**

为所有用户数据范围相关类添加废弃标记：

```java
/**
 * @deprecated 用户级数据权限已废弃，请使用角色级配置
 * @see com.school.management.service.RoleDataPermissionService
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@RestController
@RequestMapping("/user-data-scopes")
public class UserDataScopeController {
    // ...
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/school/management/controller/UserDataScopeController.java
git add backend/src/main/java/com/school/management/service/UserDataScopeService.java
git add backend/src/main/java/com/school/management/service/impl/UserDataScopeServiceImpl.java
git commit -m "$(cat <<'EOF'
deprecate(permission): mark user-level data scope as deprecated

Use role-level configuration via RoleDataPermissionService instead

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: 移除前端用户数据范围配置UI

**Files:**
- Modify: `frontend/src/views/system/UsersView.vue`
- Delete or deprecate: `frontend/src/components/user/UserDataScopeManager.vue`

**Step 1: 从 UsersView 移除数据范围配置入口**

找到并移除 `<UserDataScopeManager>` 组件的使用。

**Step 2: 标记组件为废弃**

在 `UserDataScopeManager.vue` 添加注释：

```vue
<!--
  @deprecated 此组件已废弃
  用户级数据权限配置已移除，请使用角色管理中的数据权限配置
  计划在下个大版本中删除
-->
```

**Step 3: Commit**

```bash
git add frontend/src/views/system/UsersView.vue
git add frontend/src/components/user/UserDataScopeManager.vue
git commit -m "$(cat <<'EOF'
deprecate(frontend): remove user data scope config from UsersView

Data permission now configured at role level only

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: 更新文档

**Files:**
- Modify: `CLAUDE.md`
- Create: `docs/architecture/data-permission-v4.md`

**Step 1: 更新 CLAUDE.md 中的数据权限说明**

更新架构说明，反映新的单一配置点设计。

**Step 2: 创建架构文档**

```markdown
# 数据权限架构 V4.0

## 设计原则

**单一配置点**：数据权限仅在角色管理中配置，不再支持用户级配置。

## 权限配置

### 角色数据权限表 (role_data_permissions)

| 字段 | 说明 |
|------|------|
| role_id | 角色ID |
| module_code | 模块编码 |
| scope_code | 数据范围: all, department, custom, self |

### 自定义范围表 (role_custom_scope)

| 字段 | 说明 |
|------|------|
| role_id | 角色ID |
| module_code | 模块编码 |
| scope_type | ORG_UNIT / GRADE / CLASS |
| target_id | 目标ID |

## 废弃的表

- `user_data_scopes` - 已废弃，数据已备份

## 权限解析流程

1. 获取用户所有角色
2. 查询角色数据权限配置
3. 取最高优先级权限（ALL > DEPT > CUSTOM > SELF）
4. 应用权限过滤
```

**Step 3: Commit**

```bash
git add CLAUDE.md
git add docs/architecture/data-permission-v4.md
git commit -m "$(cat <<'EOF'
docs: update data permission architecture to V4

- Document single configuration point design
- Mark user_data_scopes as deprecated
- Update CLAUDE.md with new architecture

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## 验证清单

完成所有任务后，验证以下内容：

- [ ] 后端编译成功：`mvn compile -DskipTests`
- [ ] 后端测试通过：`mvn test`
- [ ] 前端编译成功：`npm run build`
- [ ] 角色数据权限配置正常工作
- [ ] 班主任可以看到管理的所有班级学生
- [ ] 用户管理页面不再显示数据范围配置

---

## 实施状态

> 更新时间: 2026-01-11

| 任务 | 状态 | 备注 |
|------|------|------|
| Task 1: 创建数据库迁移脚本 | ✅ 完成 | `V20260111__deprecate_user_data_scopes.sql` |
| Task 2: 修改 DataPermissionServiceImpl | ✅ 完成 | 移除 UserDataScopeService 依赖 |
| Task 3: 修复多班级班主任问题 | ✅ 完成 | StudentServiceImpl 使用 classIds 列表 |
| Task 4: 删除 User.managedClassId | ✅ 完成 | 从 entity, domain, DTO 中移除 |
| Task 5: 标记用户数据范围为废弃 | ✅ 完成 | 7个类添加 @Deprecated 注解 |
| Task 6: 移除前端数据范围UI | ✅ 完成 | 两个 UsersView.vue 已清理 |
| Task 7: 更新文档 | ✅ 完成 | `docs/architecture/data-permission-v4.md` |

### 修改的文件清单

**后端:**
- `database/migrations/V20260111__deprecate_user_data_scopes.sql` (新建)
- `DataPermissionServiceImpl.java` (重写)
- `StudentServiceImpl.java` (修改 applyClassTeacherFilter)
- `User.java` (entity - 删除 managedClassId)
- `CustomUserDetails.java` (删除 managedClassId)
- `User.java` (domain - 删除 managedClassId)
- `UserRepositoryImpl.java` (更新 toPO/toDomain)
- `UserDomainResponse.java` (删除 managedClassId)
- `UserDataScopeService.java` (添加 @Deprecated)
- `UserDataScopeServiceImpl.java` (添加 @Deprecated)
- `UserDataScopeController.java` (添加 @Deprecated)
- `UserDataScope.java` (添加 @Deprecated)
- `UserDataScopeDTO.java` (添加 @Deprecated)
- `UserDataScopeRequest.java` (添加 @Deprecated)
- `UserDataScopeMapper.java` (添加 @Deprecated)

**前端:**
- `views/system/UsersView.vue` (移除数据范围UI)
- `modules/system/views/UsersView.vue` (移除数据范围UI)
- `components/user/UserDataScopeManager.vue` (添加废弃注释)

**文档:**
- `docs/architecture/data-permission-v4.md` (新建)
