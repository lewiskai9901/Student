# Casbin 数据权限重构方案

> 版本: 1.0.0
> 创建日期: 2026-01-02
> 状态: 待实施

## 一、背景与目标

### 1.1 当前问题

现有系统存在两套数据权限机制并存的问题：
- `RoleDataPermission`: 角色级别的粗粒度数据权限
- `UserDataScope`: 用户级别的细粒度数据范围

这导致：
1. 权限判断逻辑分散，难以维护
2. 缺乏统一的权限模型
3. 不支持复杂的矩阵式组织结构

### 1.2 组织结构特点

```
┌─────────────────────────────────────────────────────┐
│                    组织矩阵                          │
├─────────────┬─────────────┬─────────────────────────┤
│             │   24级      │   25级                  │
├─────────────┼─────────────┼─────────────────────────┤
│  汽车系     │  汽车24-X班 │  汽车25-X班             │
├─────────────┼─────────────┼─────────────────────────┤
│  经信系     │  经信24-X班 │  经信25-X班             │
├─────────────┼─────────────┼─────────────────────────┤
│  康养系     │  康养24-X班 │  康养25-X班             │
└─────────────┴─────────────┴─────────────────────────┘
```

- 部门（系部）和年级是独立的维度
- 班级是部门×年级的交叉点
- 需要支持灵活的数据权限配置

### 1.3 重构目标

1. **统一权限模型**: 使用 Casbin 统一管理功能权限和数据权限
2. **角色功能分离**: 角色只定义功能权限，数据范围独立配置
3. **灵活配置**: 支持任意粒度的数据范围分配
4. **高性能**: 利用 Casbin 缓存机制提升权限判断性能
5. **可审计**: 完整的权限变更审计日志

## 二、技术选型

### 2.1 为什么选择 Casbin

| 特性 | Casbin | SpiceDB | OPA | 自研 |
|------|--------|---------|-----|------|
| 学习曲线 | 低 | 高 | 中 | - |
| Java支持 | 优秀 | 需gRPC | 需HTTP | - |
| 性能 | 高(内存缓存) | 高(分布式) | 中 | 取决于实现 |
| 社区活跃 | 高 | 中 | 高 | - |
| 适合规模 | 中小型 | 大型分布式 | 策略复杂场景 | - |
| 部署复杂度 | 低 | 高 | 中 | 低 |

**结论**: Casbin 最适合当前系统规模，学习曲线平缓，Java 生态成熟。

### 2.2 核心依赖

```xml
<!-- pom.xml 添加 -->
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>jcasbin</artifactId>
    <version>1.55.0</version>
</dependency>
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>jdbc-adapter</artifactId>
    <version>2.7.0</version>
</dependency>
```

## 三、权限模型设计

### 3.1 Casbin Model (PERM)

```ini
# backend/src/main/resources/casbin/model.conf

[request_definition]
r = sub, scope, obj, act

[policy_definition]
p = sub, scope, obj, act, eft

[role_definition]
g = _, _
g2 = _, _

[policy_effect]
e = some(where (p.eft == allow)) && !some(where (p.eft == deny))

[matchers]
m = g(r.sub, p.sub) && \
    scopeMatch(r.sub, r.scope, p.scope) && \
    (r.obj == p.obj || p.obj == "*") && \
    (r.act == p.act || p.act == "*")
```

### 3.2 模型说明

| 组件 | 说明 | 示例 |
|------|------|------|
| `r` (Request) | 请求定义 | `user:1, class:101, student, read` |
| `p` (Policy) | 策略定义 | `teacher, scope:self_class, student, read, allow` |
| `g` | 用户-角色关系 | `user:1, teacher` |
| `g2` | 范围层级关系 | `class:101, grade:24` / `user:1, scope:class:101` |
| `e` (Effect) | 策略效果 | allow优先，deny可覆盖 |
| `m` (Matcher) | 匹配规则 | 使用自定义 scopeMatch 函数 |

### 3.3 范围类型定义

```java
public enum ScopeType {
    ALL("ALL", "全部数据", "*"),
    DEPT("DEPT", "指定部门", "dept:"),
    GRADE("GRADE", "指定年级", "grade:"),
    DEPT_GRADE("DEPT_GRADE", "部门+年级交叉", "dept_grade:"),
    CLASS("CLASS", "指定班级", "class:"),
    SELF("SELF", "仅本人", "self:");

    private final String code;
    private final String name;
    private final String prefix;
}
```

### 3.4 范围表达式语法

```
# 全部数据
scope:*

# 指定部门
scope:dept:1              # 汽车系
scope:dept:2              # 经信系

# 指定年级
scope:grade:24            # 24级所有班级
scope:grade:25            # 25级所有班级

# 部门+年级交叉
scope:dept_grade:1:24     # 汽车系24级所有班级
scope:dept_grade:2:25     # 经信系25级所有班级

# 指定班级
scope:class:101           # 具体班级
scope:class:102,103       # 多个班级

# 本人相关
scope:self                # 仅本人创建的数据
scope:self_class          # 本人所在班级
scope:self_dept           # 本人所在部门
```

## 四、数据库设计

### 4.1 核心表结构

```sql
-- Casbin 规则表 (由 Casbin 自动管理)
CREATE TABLE casbin_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ptype VARCHAR(100) NOT NULL,
    v0 VARCHAR(100) DEFAULT '',
    v1 VARCHAR(100) DEFAULT '',
    v2 VARCHAR(100) DEFAULT '',
    v3 VARCHAR(100) DEFAULT '',
    v4 VARCHAR(100) DEFAULT '',
    v5 VARCHAR(100) DEFAULT '',
    INDEX idx_ptype (ptype),
    INDEX idx_v0 (v0),
    INDEX idx_v1 (v1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 范围元数据表 (用于显示友好名称)
CREATE TABLE scope_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope_expression VARCHAR(200) NOT NULL UNIQUE COMMENT '范围表达式',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称',
    scope_type VARCHAR(50) NOT NULL COMMENT '范围类型',
    ref_id BIGINT COMMENT '关联ID',
    parent_scope VARCHAR(200) COMMENT '父范围',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scope_type (scope_type),
    INDEX idx_ref_id (ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户范围分配表 (业务视图)
CREATE TABLE user_scope_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scope_expression VARCHAR(200) NOT NULL,
    scope_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(100),
    assigned_by BIGINT,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME COMMENT '过期时间(可选)',
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_scope_type (scope_type),
    UNIQUE KEY uk_user_scope (user_id, scope_expression, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 权限审计日志表
CREATE TABLE permission_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_type VARCHAR(50) NOT NULL COMMENT 'GRANT/REVOKE/MODIFY',
    target_type VARCHAR(50) NOT NULL COMMENT 'ROLE/USER/SCOPE',
    target_id VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(100),
    operation_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    INDEX idx_target (target_type, target_id),
    INDEX idx_operator (operator_id),
    INDEX idx_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色权限模板表 (快速配置)
CREATE TABLE role_permission_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    default_scope_type VARCHAR(50) NOT NULL COMMENT '默认范围类型',
    scope_pattern VARCHAR(200) COMMENT '范围模式',
    permissions JSON COMMENT '权限列表',
    description VARCHAR(500),
    is_default TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 初始数据示例

```sql
-- 范围元数据
INSERT INTO scope_metadata (scope_expression, display_name, scope_type, ref_id) VALUES
('scope:*', '全部数据', 'ALL', NULL),
('scope:dept:1', '汽车系', 'DEPT', 1),
('scope:dept:2', '经信系', 'DEPT', 2),
('scope:dept:3', '康养系', 'DEPT', 3),
('scope:grade:24', '24级', 'GRADE', 24),
('scope:grade:25', '25级', 'GRADE', 25),
('scope:dept_grade:1:24', '汽车系24级', 'DEPT_GRADE', NULL),
('scope:dept_grade:2:24', '经信系24级', 'DEPT_GRADE', NULL);

-- 角色权限模板
INSERT INTO role_permission_templates (role_code, template_name, default_scope_type, scope_pattern, is_default) VALUES
('ADMIN', '系统管理员', 'ALL', 'scope:*', 1),
('DEPT_MANAGER', '系部负责人', 'DEPT', 'scope:dept:{deptId}', 1),
('GRADE_DIRECTOR', '年级主任', 'DEPT_GRADE', 'scope:dept_grade:{deptId}:{gradeId}', 1),
('CLASS_TEACHER', '班主任', 'CLASS', 'scope:class:{classId}', 1),
('TEACHER', '普通教师', 'SELF', 'scope:self', 1);
```

## 五、核心代码实现

### 5.1 目录结构

```
backend/src/main/java/com/school/management/
├── casbin/
│   ├── config/
│   │   └── CasbinConfig.java           # Casbin配置
│   ├── service/
│   │   ├── CasbinEnforcerService.java  # 权限执行服务
│   │   ├── CasbinScopeService.java     # 范围管理服务
│   │   └── ScopeHierarchyService.java  # 范围层级同步
│   ├── function/
│   │   └── ScopeMatchFunction.java     # 自定义匹配函数
│   ├── dto/
│   │   ├── ScopeAssignmentDTO.java
│   │   └── PermissionCheckDTO.java
│   └── controller/
│       └── ScopeManageController.java   # 范围管理API
```

### 5.2 Casbin 配置类

```java
package com.school.management.casbin.config;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.adapter.JDBCAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class CasbinConfig {

    @Bean
    public Adapter casbinAdapter(DataSource dataSource) {
        return new JDBCAdapter(dataSource);
    }

    @Bean
    public Enforcer enforcer(Adapter adapter) {
        String modelPath = "classpath:casbin/model.conf";
        Enforcer enforcer = new Enforcer(modelPath, adapter);

        // 注册自定义函数
        enforcer.addFunction("scopeMatch", new ScopeMatchFunction());

        // 启用自动加载策略
        enforcer.enableAutoSave(true);

        return enforcer;
    }
}
```

### 5.3 自定义范围匹配函数

```java
package com.school.management.casbin.function;

import org.casbin.jcasbin.util.function.CustomFunction;
import java.util.*;

public class ScopeMatchFunction implements CustomFunction {

    @Override
    public Object call(Object... args) {
        if (args.length < 3) return false;

        String userId = String.valueOf(args[0]);      // 用户ID
        String requestScope = String.valueOf(args[1]); // 请求的范围
        String policyScope = String.valueOf(args[2]);  // 策略的范围

        // 全局范围
        if ("scope:*".equals(policyScope) || "*".equals(policyScope)) {
            return true;
        }

        // 精确匹配
        if (requestScope.equals(policyScope)) {
            return true;
        }

        // 层级匹配
        return matchHierarchy(requestScope, policyScope);
    }

    private boolean matchHierarchy(String request, String policy) {
        // scope:class:101 应该匹配 scope:grade:24 (如果101属于24级)
        // scope:class:101 应该匹配 scope:dept:1 (如果101属于汽车系)
        // scope:class:101 应该匹配 scope:dept_grade:1:24

        // 这里需要查询缓存的层级关系
        return ScopeHierarchyCache.isChildOf(request, policy);
    }
}
```

### 5.4 范围层级缓存

```java
package com.school.management.casbin.service;

import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScopeHierarchyCache {

    private static final Map<String, Set<String>> HIERARCHY = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshHierarchy();
    }

    public void refreshHierarchy() {
        // 从数据库加载班级-年级-部门关系
        // class:101 -> [grade:24, dept:1, dept_grade:1:24]
        // class:102 -> [grade:24, dept:1, dept_grade:1:24]
        // ...
    }

    public static boolean isChildOf(String child, String parent) {
        Set<String> parents = HIERARCHY.get(child);
        return parents != null && parents.contains(parent);
    }

    public static Set<String> getParents(String scope) {
        return HIERARCHY.getOrDefault(scope, Collections.emptySet());
    }
}
```

### 5.5 权限执行服务

```java
package com.school.management.casbin.service;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CasbinEnforcerService {

    private final Enforcer enforcer;

    /**
     * 检查权限
     */
    public boolean checkPermission(String userId, String scope, String resource, String action) {
        String sub = "user:" + userId;
        boolean allowed = enforcer.enforce(sub, scope, resource, action);
        log.debug("Permission check: user={}, scope={}, resource={}, action={}, result={}",
                userId, scope, resource, action, allowed);
        return allowed;
    }

    /**
     * 获取用户可访问的所有范围
     */
    public Set<String> getUserScopes(String userId) {
        String sub = "user:" + userId;
        Set<String> scopes = new HashSet<>();

        // 从 g2 关系中获取用户直接分配的范围
        List<List<String>> g2Rules = enforcer.getFilteredGroupingPolicy(0, sub);
        for (List<String> rule : g2Rules) {
            if (rule.size() >= 2) {
                scopes.add(rule.get(1));
            }
        }

        return scopes;
    }

    /**
     * 获取用户可访问的班级ID列表
     */
    public Set<Long> getAccessibleClassIds(String userId) {
        Set<String> scopes = getUserScopes(userId);
        Set<Long> classIds = new HashSet<>();

        for (String scope : scopes) {
            if (scope.equals("scope:*")) {
                return null; // null 表示全部
            }
            if (scope.startsWith("scope:class:")) {
                String idStr = scope.substring("scope:class:".length());
                Arrays.stream(idStr.split(","))
                      .map(Long::parseLong)
                      .forEach(classIds::add);
            }
            // 处理其他范围类型，需要展开为班级列表
        }

        return classIds;
    }

    /**
     * 分配用户范围
     */
    public void assignUserScope(String userId, String scope) {
        String sub = "user:" + userId;
        enforcer.addGroupingPolicy(sub, scope);
        log.info("Assigned scope {} to user {}", scope, userId);
    }

    /**
     * 移除用户范围
     */
    public void removeUserScope(String userId, String scope) {
        String sub = "user:" + userId;
        enforcer.removeGroupingPolicy(sub, scope);
        log.info("Removed scope {} from user {}", scope, userId);
    }

    /**
     * 分配用户角色
     */
    public void assignUserRole(String userId, String role) {
        String sub = "user:" + userId;
        enforcer.addRoleForUser(sub, role);
        log.info("Assigned role {} to user {}", role, userId);
    }

    /**
     * 添加权限策略
     */
    public void addPolicy(String role, String scope, String resource, String action) {
        enforcer.addPolicy(role, scope, resource, action, "allow");
    }
}
```

### 5.6 范围管理服务

```java
package com.school.management.casbin.service;

import com.school.management.casbin.dto.ScopeAssignmentDTO;
import com.school.management.mapper.UserScopeAssignmentMapper;
import com.school.management.mapper.PermissionAuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CasbinScopeService {

    private final CasbinEnforcerService enforcerService;
    private final UserScopeAssignmentMapper assignmentMapper;
    private final PermissionAuditLogMapper auditLogMapper;

    /**
     * 分配用户数据范围
     */
    @Transactional
    public void assignScope(ScopeAssignmentDTO dto) {
        // 1. 保存到业务表
        assignmentMapper.insert(dto.toEntity());

        // 2. 同步到 Casbin
        enforcerService.assignUserScope(
            dto.getUserId().toString(),
            dto.getScopeExpression()
        );

        // 3. 记录审计日志
        auditLogMapper.insertLog("GRANT", "SCOPE",
            dto.getUserId().toString(), null, dto.getScopeExpression());
    }

    /**
     * 批量分配范围
     */
    @Transactional
    public void batchAssignScopes(Long userId, List<String> scopes) {
        for (String scope : scopes) {
            ScopeAssignmentDTO dto = new ScopeAssignmentDTO();
            dto.setUserId(userId);
            dto.setScopeExpression(scope);
            assignScope(dto);
        }
    }

    /**
     * 移除用户数据范围
     */
    @Transactional
    public void removeScope(Long userId, String scopeExpression) {
        // 1. 软删除业务表记录
        assignmentMapper.softDeleteByUserAndScope(userId, scopeExpression);

        // 2. 同步到 Casbin
        enforcerService.removeUserScope(userId.toString(), scopeExpression);

        // 3. 记录审计日志
        auditLogMapper.insertLog("REVOKE", "SCOPE",
            userId.toString(), scopeExpression, null);
    }

    /**
     * 获取用户当前的数据范围
     */
    public List<ScopeAssignmentDTO> getUserScopes(Long userId) {
        return assignmentMapper.selectByUserId(userId);
    }

    /**
     * 根据范围模板快速配置
     */
    @Transactional
    public void applyTemplate(Long userId, String roleCode, Map<String, Object> params) {
        // 根据角色模板生成范围表达式
        // 如: CLASS_TEACHER + {classId: 101} -> scope:class:101
        // 如: DEPT_MANAGER + {deptId: 1} -> scope:dept:1
    }
}
```

## 六、API 设计

### 6.1 范围管理 API

```
POST   /api/v2/scopes/assign           # 分配用户数据范围
DELETE /api/v2/scopes/revoke           # 移除用户数据范围
GET    /api/v2/scopes/user/{userId}    # 获取用户数据范围
POST   /api/v2/scopes/batch-assign     # 批量分配范围
POST   /api/v2/scopes/apply-template   # 应用角色模板
GET    /api/v2/scopes/templates        # 获取范围模板列表
GET    /api/v2/scopes/metadata         # 获取范围元数据(用于下拉选择)
```

### 6.2 请求/响应示例

```json
// POST /api/v2/scopes/assign
{
    "userId": 1001,
    "scopeType": "CLASS",
    "scopeExpression": "scope:class:101",
    "displayName": "汽车24-1班",
    "remark": "班主任分配"
}

// GET /api/v2/scopes/user/1001
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "userId": 1001,
            "scopeType": "CLASS",
            "scopeExpression": "scope:class:101",
            "displayName": "汽车24-1班",
            "assignedBy": 1,
            "assignedAt": "2026-01-02T10:00:00"
        }
    ]
}

// POST /api/v2/scopes/apply-template
{
    "userId": 1001,
    "roleCode": "GRADE_DIRECTOR",
    "params": {
        "deptId": 1,
        "gradeId": 24
    }
}
```

## 七、前端设计

### 7.1 范围配置组件

```vue
<!-- ScopeConfig.vue -->
<template>
  <div class="scope-config">
    <!-- 范围类型选择 -->
    <el-select v-model="scopeType" placeholder="选择范围类型">
      <el-option label="全部数据" value="ALL" />
      <el-option label="指定部门" value="DEPT" />
      <el-option label="指定年级" value="GRADE" />
      <el-option label="部门+年级" value="DEPT_GRADE" />
      <el-option label="指定班级" value="CLASS" />
      <el-option label="仅本人" value="SELF" />
    </el-select>

    <!-- 动态表单 -->
    <template v-if="scopeType === 'DEPT'">
      <el-select v-model="deptId" placeholder="选择部门">
        <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
    </template>

    <template v-if="scopeType === 'GRADE'">
      <el-select v-model="gradeId" placeholder="选择年级">
        <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
    </template>

    <template v-if="scopeType === 'DEPT_GRADE'">
      <el-select v-model="deptId" placeholder="选择部门">
        <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-select v-model="gradeId" placeholder="选择年级">
        <el-option v-for="g in grades" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
    </template>

    <template v-if="scopeType === 'CLASS'">
      <el-cascader
        v-model="classIds"
        :options="classTree"
        :props="{ multiple: true }"
        placeholder="选择班级"
      />
    </template>

    <!-- 已分配范围列表 -->
    <el-table :data="assignedScopes" style="margin-top: 20px">
      <el-table-column prop="displayName" label="范围名称" />
      <el-table-column prop="scopeType" label="类型" />
      <el-table-column prop="assignedAt" label="分配时间" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="removeScope(row)">
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

### 7.2 用户管理页面集成

在用户管理页面添加"数据范围"Tab页，使用上述组件进行配置。

## 八、迁移计划

### 8.1 阶段一：准备工作 (1-2天)

- [ ] 添加 Casbin 依赖
- [ ] 创建 model.conf 文件
- [ ] 创建数据库表
- [ ] 实现核心配置类

### 8.2 阶段二：核心服务开发 (3-4天)

- [ ] 实现 ScopeMatchFunction
- [ ] 实现 ScopeHierarchyCache
- [ ] 实现 CasbinEnforcerService
- [ ] 实现 CasbinScopeService
- [ ] 编写单元测试

### 8.3 阶段三：数据迁移 (1-2天)

- [ ] 编写迁移脚本
- [ ] 迁移 user_data_scopes 数据
- [ ] 迁移 role_data_permissions 数据
- [ ] 验证数据一致性

### 8.4 阶段四：API 开发 (2-3天)

- [ ] 实现范围管理 Controller
- [ ] 更新现有数据查询服务
- [ ] 集成权限检查
- [ ] API 测试

### 8.5 阶段五：前端开发 (2-3天)

- [ ] 开发 ScopeConfig 组件
- [ ] 集成到用户管理页面
- [ ] 调整角色管理页面
- [ ] 端到端测试

### 8.6 阶段六：切换与清理 (1-2天)

- [ ] 切换到新权限系统
- [ ] 灰度发布验证
- [ ] 废弃旧代码
- [ ] 文档更新

## 九、注意事项

### 9.1 性能优化

1. **缓存策略**: Casbin 默认使用内存缓存，需配置合适的刷新策略
2. **批量操作**: 批量分配/移除范围时使用事务
3. **层级缓存**: 范围层级关系使用本地缓存，定时刷新

### 9.2 兼容性

1. **双写期**: 迁移期间同时写入新旧表
2. **开关控制**: 使用配置开关控制使用新/旧系统
3. **回滚方案**: 准备回滚脚本

### 9.3 安全性

1. **审计日志**: 所有权限变更必须记录
2. **权限验证**: 管理员操作需二次验证
3. **敏感操作**: 批量操作需审批流程

## 十、附录

### 10.1 常见场景示例

| 场景 | 范围表达式 | 说明 |
|------|-----------|------|
| 系统管理员 | `scope:*` | 访问所有数据 |
| 汽车系负责人 | `scope:dept:1` | 访问汽车系所有数据 |
| 24级主任(汽车系) | `scope:dept_grade:1:24` | 访问汽车系24级所有班级 |
| 班主任(101班) | `scope:class:101` | 访问101班数据 |
| 普通教师 | `scope:self` | 仅访问本人相关数据 |
| 跨班教师 | `scope:class:101,102,103` | 访问多个班级数据 |

### 10.2 参考链接

- [Casbin 官方文档](https://casbin.org/docs/zh-CN/overview)
- [jCasbin GitHub](https://github.com/casbin/jcasbin)
- [JDBC Adapter](https://github.com/jcasbin/jdbc-adapter)
