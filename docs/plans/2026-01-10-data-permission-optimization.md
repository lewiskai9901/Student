# 数据权限优化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 统一DataScope枚举、对齐DDD模块定义、添加自定义范围选择UI

**Architecture:** 保留DDD层DataScope作为唯一来源，扩展组织层级支持，模块定义与DDD领域对齐

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Vue 3, Element Plus, TypeScript

---

## Task 1: 扩展DDD层DataScope枚举

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/access/model/DataScope.java`

**Step 1: 添加code和description字段**

```java
package com.school.management.domain.access.model;

/**
 * 数据权限范围枚举
 * 定义用户可访问的数据范围级别
 */
public enum DataScope {
    ALL("all", "全部数据"),
    DEPARTMENT_AND_BELOW("department_and_below", "本部门及以下"),
    DEPARTMENT("department", "仅本部门"),
    GRADE("grade", "本年级"),
    CLASS("class_only", "仅本班级"),
    CUSTOM("custom", "自定义范围"),
    SELF("self", "仅本人");

    private final String code;
    private final String description;

    DataScope(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataScope fromCode(String code) {
        for (DataScope scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown DataScope code: " + code);
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/access/model/DataScope.java
git commit -m "feat(access): extend DataScope enum with code and description"
```

---

## Task 2: 创建DDD对齐的DataModule枚举

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/access/model/DataModule.java`

**Step 1: 创建DataModule枚举**

```java
package com.school.management.domain.access.model;

/**
 * 数据模块枚举
 * 与DDD领域对齐的模块定义
 */
public enum DataModule {
    // Organization Domain - 组织管理领域
    ORG_UNIT("org_unit", "组织架构", "organization"),
    STUDENT("student", "学生信息", "organization"),
    DORMITORY("dormitory", "宿舍管理", "organization"),
    CLASSROOM("classroom", "教室管理", "organization"),

    // Inspection Domain - 量化检查领域
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection"),
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection"),
    APPEAL("appeal", "申诉管理", "inspection"),

    // Evaluation Domain - 评价领域
    RATING("rating", "评价管理", "evaluation"),

    // Task Domain - 任务领域
    TASK("task", "任务管理", "task");

    private final String code;
    private final String name;
    private final String domain;

    DataModule(String code, String name, String domain) {
        this.code = code;
        this.name = name;
        this.domain = domain;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public static DataModule fromCode(String code) {
        for (DataModule module : values()) {
            if (module.code.equals(code)) {
                return module;
            }
        }
        throw new IllegalArgumentException("Unknown DataModule code: " + code);
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/access/model/DataModule.java
git commit -m "feat(access): add DataModule enum aligned with DDD domains"
```

---

## Task 3: 删除V1 DataScope枚举并更新引用

**Files:**
- Delete: `backend/src/main/java/com/school/management/enums/DataScope.java`
- Modify: All files referencing V1 DataScope

**Step 1: 查找所有V1 DataScope引用**

```bash
grep -r "com.school.management.enums.DataScope" backend/src --include="*.java"
```

**Step 2: 更新引用到DDD版本**

将所有 `import com.school.management.enums.DataScope` 改为:
`import com.school.management.domain.access.model.DataScope`

**Step 3: 删除V1枚举文件**

```bash
rm backend/src/main/java/com/school/management/enums/DataScope.java
```

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor(access): remove V1 DataScope enum, use DDD version"
```

---

## Task 4: 更新RoleDataPermissionDTO使用新枚举

**Files:**
- Modify: `backend/src/main/java/com/school/management/dto/RoleDataPermissionDTO.java`

**Step 1: 更新DTO使用DataModule和DataScope枚举**

```java
package com.school.management.dto;

import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import lombok.Data;
import java.util.List;

@Data
public class RoleDataPermissionDTO {
    private Long roleId;
    private String roleName;
    private List<ModulePermission> modulePermissions;

    @Data
    public static class ModulePermission {
        private DataModule module;
        private DataScope scope;
        private List<Long> customOrgUnitIds; // 自定义范围的组织单元ID列表
    }
}
```

**Step 2: 删除DTO中的内部Module枚举**

移除 `RoleDataPermissionDTO` 中的 `Module` 内部枚举定义。

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/dto/RoleDataPermissionDTO.java
git commit -m "refactor(access): update RoleDataPermissionDTO to use DDD enums"
```

---

## Task 5: 创建RoleCustomScope实体

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/access/model/RoleCustomScope.java`
- Create: `backend/src/main/java/com/school/management/domain/access/repository/RoleCustomScopeRepository.java`

**Step 1: 创建RoleCustomScope实体**

```java
package com.school.management.domain.access.model;

import com.school.management.domain.shared.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleCustomScope extends Entity<Long> {
    private Long roleId;
    private DataModule module;
    private Long orgUnitId;

    public RoleCustomScope() {}

    public RoleCustomScope(Long roleId, DataModule module, Long orgUnitId) {
        this.roleId = roleId;
        this.module = module;
        this.orgUnitId = orgUnitId;
    }
}
```

**Step 2: 创建Repository接口**

```java
package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.RoleCustomScope;
import java.util.List;

public interface RoleCustomScopeRepository {
    List<RoleCustomScope> findByRoleIdAndModule(Long roleId, DataModule module);
    void saveAll(Long roleId, DataModule module, List<Long> orgUnitIds);
    void deleteByRoleIdAndModule(Long roleId, DataModule module);
}
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/access/model/RoleCustomScope.java
git add backend/src/main/java/com/school/management/domain/access/repository/RoleCustomScopeRepository.java
git commit -m "feat(access): add RoleCustomScope entity and repository"
```

---

## Task 6: 创建数据库表和持久化实现

**Files:**
- Create: `database/migrations/V20260110__add_role_custom_scope.sql`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/access/RoleCustomScopePO.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/access/RoleCustomScopeMapper.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/access/RoleCustomScopeRepositoryImpl.java`

**Step 1: 创建数据库迁移脚本**

```sql
-- 角色自定义数据范围表
CREATE TABLE IF NOT EXISTS role_custom_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    module VARCHAR(50) NOT NULL COMMENT '模块代码',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_module_org (role_id, module, org_unit_id),
    KEY idx_role_id (role_id),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色自定义数据范围';

-- 更新现有role_data_permission表的scope字段值
UPDATE role_data_permission SET scope = 'all' WHERE scope = '1';
UPDATE role_data_permission SET scope = 'department' WHERE scope = '2';
UPDATE role_data_permission SET scope = 'grade' WHERE scope = '3';
UPDATE role_data_permission SET scope = 'class_only' WHERE scope = '4';
UPDATE role_data_permission SET scope = 'self' WHERE scope = '5';
```

**Step 2: 创建PO类**

```java
package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role_custom_scope")
public class RoleCustomScopePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private String module;
    private Long orgUnitId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

**Step 3: 创建Mapper**

```java
package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleCustomScopeMapper extends BaseMapper<RoleCustomScopePO> {
}
```

**Step 4: 创建Repository实现**

```java
package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.RoleCustomScope;
import com.school.management.domain.access.repository.RoleCustomScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RoleCustomScopeRepositoryImpl implements RoleCustomScopeRepository {

    private final RoleCustomScopeMapper mapper;

    @Override
    public List<RoleCustomScope> findByRoleIdAndModule(Long roleId, DataModule module) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId)
               .eq(RoleCustomScopePO::getModule, module.getCode());
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveAll(Long roleId, DataModule module, List<Long> orgUnitIds) {
        deleteByRoleIdAndModule(roleId, module);
        for (Long orgUnitId : orgUnitIds) {
            RoleCustomScopePO po = new RoleCustomScopePO();
            po.setRoleId(roleId);
            po.setModule(module.getCode());
            po.setOrgUnitId(orgUnitId);
            mapper.insert(po);
        }
    }

    @Override
    @Transactional
    public void deleteByRoleIdAndModule(Long roleId, DataModule module) {
        LambdaQueryWrapper<RoleCustomScopePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleCustomScopePO::getRoleId, roleId)
               .eq(RoleCustomScopePO::getModule, module.getCode());
        mapper.delete(wrapper);
    }

    private RoleCustomScope toDomain(RoleCustomScopePO po) {
        RoleCustomScope scope = new RoleCustomScope();
        scope.setId(po.getId());
        scope.setRoleId(po.getRoleId());
        scope.setModule(DataModule.fromCode(po.getModule()));
        scope.setOrgUnitId(po.getOrgUnitId());
        return scope;
    }
}
```

**Step 5: Commit**

```bash
git add database/migrations/V20260110__add_role_custom_scope.sql
git add backend/src/main/java/com/school/management/infrastructure/persistence/access/
git commit -m "feat(access): add RoleCustomScope persistence layer"
```

---

## Task 7: 更新RoleDataPermissionService

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/RoleDataPermissionService.java`

**Step 1: 注入RoleCustomScopeRepository**

**Step 2: 更新保存方法支持自定义范围**

```java
@Transactional
public void saveModulePermission(Long roleId, RoleDataPermissionDTO.ModulePermission permission) {
    // 保存基础权限
    RoleDataPermission entity = new RoleDataPermission();
    entity.setRoleId(roleId);
    entity.setModule(permission.getModule().getCode());
    entity.setScope(permission.getScope().getCode());
    roleDataPermissionMapper.insertOrUpdate(entity);

    // 如果是自定义范围，保存组织单元列表
    if (permission.getScope() == DataScope.CUSTOM && permission.getCustomOrgUnitIds() != null) {
        customScopeRepository.saveAll(roleId, permission.getModule(), permission.getCustomOrgUnitIds());
    }
}
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/service/RoleDataPermissionService.java
git commit -m "feat(access): update RoleDataPermissionService for custom scopes"
```

---

## Task 8: 添加V2 API端点

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/access/RoleDataPermissionController.java`

**Step 1: 创建Controller**

```java
package com.school.management.interfaces.rest.access;

import com.school.management.common.ApiResponse;
import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import com.school.management.dto.RoleDataPermissionDTO;
import com.school.management.service.RoleDataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/roles/{roleId}/data-permissions")
@RequiredArgsConstructor
@Tag(name = "角色数据权限管理", description = "V2 API - 角色数据权限配置")
public class RoleDataPermissionController {

    private final RoleDataPermissionService service;

    @GetMapping
    @Operation(summary = "获取角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public ApiResponse<RoleDataPermissionDTO> getPermissions(@PathVariable Long roleId) {
        return ApiResponse.success(service.getRoleDataPermission(roleId));
    }

    @PutMapping
    @Operation(summary = "更新角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public ApiResponse<Void> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody RoleDataPermissionDTO dto) {
        service.saveRoleDataPermission(roleId, dto);
        return ApiResponse.success(null);
    }

    @GetMapping("/modules")
    @Operation(summary = "获取所有数据模块列表")
    public ApiResponse<List<Map<String, String>>> getModules() {
        List<Map<String, String>> modules = Arrays.stream(DataModule.values())
                .map(m -> Map.of(
                        "code", m.getCode(),
                        "name", m.getName(),
                        "domain", m.getDomain()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(modules);
    }

    @GetMapping("/scopes")
    @Operation(summary = "获取所有数据范围选项")
    public ApiResponse<List<Map<String, String>>> getScopes() {
        List<Map<String, String>> scopes = Arrays.stream(DataScope.values())
                .map(s -> Map.of(
                        "code", s.getCode(),
                        "description", s.getDescription()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(scopes);
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/school/management/interfaces/rest/access/RoleDataPermissionController.java
git commit -m "feat(access): add V2 API for role data permissions"
```

---

## Task 9: 前端API类型定义

**Files:**
- Modify: `frontend/src/api/v2/access.ts`
- Modify: `frontend/src/types/v2/access.ts`

**Step 1: 添加类型定义**

```typescript
// types/v2/access.ts
export interface DataModule {
  code: string
  name: string
  domain: string
}

export interface DataScope {
  code: string
  description: string
}

export interface ModulePermission {
  module: string
  scope: string
  customOrgUnitIds?: number[]
}

export interface RoleDataPermission {
  roleId: number
  roleName: string
  modulePermissions: ModulePermission[]
}
```

**Step 2: 添加API方法**

```typescript
// api/v2/access.ts
export const roleDataPermissionApi = {
  getPermissions: (roleId: number) =>
    request.get<ApiResponse<RoleDataPermission>>(`/v2/roles/${roleId}/data-permissions`),

  updatePermissions: (roleId: number, data: RoleDataPermission) =>
    request.put<ApiResponse<void>>(`/v2/roles/${roleId}/data-permissions`, data),

  getModules: () =>
    request.get<ApiResponse<DataModule[]>>('/v2/roles/0/data-permissions/modules'),

  getScopes: () =>
    request.get<ApiResponse<DataScope[]>>('/v2/roles/0/data-permissions/scopes'),
}
```

**Step 3: Commit**

```bash
git add frontend/src/api/v2/access.ts frontend/src/types/v2/access.ts
git commit -m "feat(frontend): add data permission API types and methods"
```

---

## Task 10: 更新前端RolesView数据权限UI

**Files:**
- Modify: `frontend/src/views/system/RolesView.vue`

**Step 1: 添加自定义范围选择对话框**

在数据权限对话框中添加:
- 按DDD领域分组显示模块
- 当选择"自定义范围"时显示组织树选择器
- 支持多选组织单元

**Step 2: 关键UI组件**

```vue
<template>
  <!-- 数据权限配置对话框 -->
  <el-dialog v-model="dataPermissionDialogVisible" title="配置数据权限" width="700px">
    <div v-for="domain in groupedModules" :key="domain.name" class="mb-6">
      <h4 class="text-sm font-medium text-gray-700 mb-3">{{ domain.label }}</h4>
      <div v-for="module in domain.modules" :key="module.code" class="flex items-center mb-3">
        <span class="w-24 text-sm">{{ module.name }}</span>
        <el-select v-model="modulePermissions[module.code].scope" class="w-40 mr-4">
          <el-option v-for="scope in scopes" :key="scope.code"
                     :label="scope.description" :value="scope.code" />
        </el-select>
        <!-- 自定义范围选择器 -->
        <el-button v-if="modulePermissions[module.code].scope === 'custom'"
                   size="small" @click="openOrgSelector(module.code)">
          选择范围 ({{ modulePermissions[module.code].customOrgUnitIds?.length || 0 }})
        </el-button>
      </div>
    </div>
  </el-dialog>

  <!-- 组织单元选择对话框 -->
  <el-dialog v-model="orgSelectorVisible" title="选择可访问的组织单元" width="500px">
    <el-tree
      ref="orgTreeRef"
      :data="orgTree"
      show-checkbox
      node-key="id"
      :default-checked-keys="currentCustomOrgUnitIds"
      :props="{ label: 'name', children: 'children' }"
    />
    <template #footer>
      <el-button @click="orgSelectorVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmOrgSelection">确定</el-button>
    </template>
  </el-dialog>
</template>
```

**Step 3: Commit**

```bash
git add frontend/src/views/system/RolesView.vue
git commit -m "feat(frontend): add custom scope selector in data permission dialog"
```

---

## Task 11: 编译验证

**Step 1: 后端编译**

```bash
cd backend
mvn compile -DskipTests
```

**Step 2: 前端编译**

```bash
cd frontend
npm run build
```

**Step 3: 修复编译错误（如有）**

**Step 4: Commit修复**

```bash
git add -A
git commit -m "fix: resolve compilation errors"
```

---

## Task 12: 更新文档

**Files:**
- Modify: `CLAUDE.md`

**Step 1: 添加数据权限架构说明**

在CLAUDE.md中添加数据权限相关文档:
- DataScope枚举说明
- DataModule枚举说明
- 自定义范围功能说明
- V2 API端点说明

**Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: add data permission architecture documentation"
```

---

## 完成检查清单

- [ ] Task 1: DataScope枚举扩展
- [ ] Task 2: DataModule枚举创建
- [ ] Task 3: V1 DataScope删除和迁移
- [ ] Task 4: RoleDataPermissionDTO更新
- [ ] Task 5: RoleCustomScope实体
- [ ] Task 6: 数据库和持久化层
- [ ] Task 7: Service层更新
- [ ] Task 8: V2 API端点
- [ ] Task 9: 前端类型和API
- [ ] Task 10: 前端UI更新
- [ ] Task 11: 编译验证
- [ ] Task 12: 文档更新
