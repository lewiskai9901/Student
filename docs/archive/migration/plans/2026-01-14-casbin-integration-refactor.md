# Casbin 权限系统整合重构设计

## 日期: 2026-01-14

## 目标

删除 DDD 领域层冗余的权限模型，以 Casbin 为唯一权限引擎，简化系统架构。

## 背景

当前系统存在三套权限实现：
1. Casbin 权限系统（`casbin/` 包）
2. DDD 领域模型权限（`domain/access/`）
3. 职能部门权限（`FunctionalDeptModule`）

这导致概念重叠、维护困难。

## 设计决策

### 1. 删除的文件

```
domain/access/model/
├── DataScope.java              ← 删除（用 casbin.enums.ScopeType）
├── RoleCustomScope.java        ← 删除（用 casbin g2 策略）
└── DataModule.java             ← 删除（Casbin 策略中的 resource）

domain/access/repository/
└── RoleCustomScopeRepository.java  ← 删除

domain/organization/model/
├── ScopeType.java              ← 删除（与 casbin.enums.ScopeType 重复）
└── FunctionalDeptModule.java   ← 删除（用 Casbin 策略）

domain/organization/repository/
└── FunctionalDeptModuleRepository.java ← 删除

infrastructure/persistence/access/
├── RoleCustomScopePO.java      ← 删除
└── RoleCustomScopeRepositoryImpl.java ← 删除

infrastructure/persistence/organization/
├── FunctionalDeptModulePO.java ← 删除
├── FunctionalDeptModuleMapper.java ← 删除
├── FunctionalDeptModuleRepositoryImpl.java ← 删除
├── FunctionalDeptScopePO.java  ← 删除
└── FunctionalDeptScopeMapper.java ← 删除

application/organization/
├── FunctionalDeptModuleService.java ← 删除
└── query/FunctionalDeptModuleDTO.java ← 删除

interfaces/rest/organization/
├── FunctionalDeptModuleController.java ← 删除
├── FunctionalDeptModuleResponse.java ← 删除
└── UpdateFunctionalDeptModulesRequest.java ← 删除
```

### 2. 保留的文件

```
domain/access/model/
├── Role.java                   ← 保留（业务规则）
├── Permission.java             ← 保留（业务概念）
├── UserRole.java               ← 保留（业务规则）
├── RoleType.java               ← 保留
└── PermissionType.java         ← 保留

casbin/                         ← 保留并移入 infrastructure/casbin
```

### 3. 架构变更

```
重构前:
├── casbin/                     # 顶层独立包
├── domain/access/model/        # 包含 DataScope、RoleCustomScope
└── domain/organization/model/  # 包含 FunctionalDeptModule

重构后:
├── domain/access/
│   ├── model/                  # 只保留 Role、Permission、UserRole
│   └── port/                   # 新增：PolicyEnforcerPort
└── infrastructure/
    └── casbin/                 # 从顶层移入
```

### 4. Port 接口设计

```java
package com.school.management.domain.access.port;

public interface PolicyEnforcerPort {
    boolean checkPermission(Long userId, String resource, String action);
    Set<Long> getAccessibleClassIds(Long userId);
    Set<Long> getAccessibleDeptIds(Long userId);
    Set<Long> getAccessibleGradeIds(Long userId);
    boolean hasAllDataPermission(Long userId);
}
```

## 数据迁移

无需数据迁移。Casbin 表（`casbin_rule`）已有初始数据，`casbin_init.sql` 包含完整的迁移脚本。

## 配置变更

```yaml
# application-dev.yml
casbin:
  enabled: true  # 从 false 改为 true
```

## 风险评估

- **低风险**：删除的文件在开发环境未被使用（casbin.enabled=false）
- **需验证**：编译通过后需要验证 Casbin 功能正常

## 执行步骤

1. 删除冗余文件
2. 将 casbin 包移入 infrastructure 层
3. 创建 PolicyEnforcerPort 接口
4. 更新依赖的服务和控制器
5. 启用 Casbin 配置
6. 编译验证
