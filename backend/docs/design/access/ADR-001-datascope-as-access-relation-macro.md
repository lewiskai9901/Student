# ADR-001: DataScope as AccessRelation macro

**Status**: Accepted (2026-05-09)
**Phase**: 关系管理 A+ Phase 2

## Context

The codebase has two seemingly competing authorization data structures:

1. **`DataScope` enum** (`domain/access/model/DataScope.java`) — RBAC-style:
   `ALL` / `DEPARTMENT_AND_BELOW` / `DEPARTMENT` / `CUSTOM` / `SELF` (5 hardcoded values).
   Used by `@DataPermission` annotation + `DataPermissionInterceptor` to inject WHERE clauses on Mapper queries.

   > Note: 旧 CLAUDE.md 文档里曾列出过 `GRADE` / `CLASS`,但当前 `DataScope.java` 实际只剩 5 个 hardcoded 值
   > (V3 通用化时已去掉教育术语)。年级/班级粒度走插件维度 (`PluginDataScopeRouter` + `data_scope_dims`),
   > 不再 hardcode 在 enum 里。

2. **`access_relations` table + `AccessRelationService`** — ReBAC-style (Zanzibar Simplified):
   Five-tuple `(subject, relation, resource)` with implied derivation, time-bounded validity, etc.

新人常问"哪个是 source of truth?"以及"是不是要保持二者同步?"。本 ADR 给出明确答复。

## Decision

**DataScope 是 AccessRelation 的粗粒度 MACRO,不是与之竞争的独立体系。**

| DataScope | 等价 AccessRelation 表达 |
|---|---|
| `ALL` | (绕过 interceptor — 不映射到任何 AccessRelation) |
| `SELF` | `WHERE created_by = me` (退化为 creator filter,等价 subject_id=me 的 access_relation) |
| `DEPARTMENT` | `lookup(user=me, member, org_unit) → orgIds[]` 然后 `WHERE org_unit_id IN (orgIds)` |
| `DEPARTMENT_AND_BELOW` | DEPARTMENT 基础 + `tree_path LIKE '<myOrgPath>%'` 递归子树 |
| `CUSTOM` | `lookup(user=me, "*", org_unit)` — 走 `role_custom_scope` / access_relations 自定义授权 |
| (插件维度,如 `BY_MAJOR`) | `PluginDataScopeRouter.resolve(dimCode, userId, resourceType)` → ID 列表 |

**`DataPermissionInterceptor` 是桥接层:** 它读 role → `MergedDataScope`,然后展开为下面之一:

- 直接 WHERE 子句 (静态 scope: SELF / DEPARTMENT / DEPARTMENT_AND_BELOW)
- access_relations 子查询 (`buildAccessRelationCondition`,当 module 配置了 `resourceType` 时,
  FIXED 2026-04-15 commit 324f515c)
- 插件维度路由 (`buildPluginDimCondition` → `PluginDataScopeRouter`)

参考: `DataPermissionInterceptor.buildSingleRoleCondition` switch 分支。

## Why we don't unify (yet)

完整统一方案需要把每个 DataScope 都编译成 `AccessRelationService.findAccessibleResourceIds(...)`
然后用 `WHERE id IN (...)` 注入。这意味着:

- 38 个 mapper 受影响,每个都需要回归测试
- 任何一个 mapper 的 WHERE 注入坏掉都是高 blast radius
- ROI 边际 — 当前双轨制工作正常 (FIXED 2026-04-15) 且性能可接受

**已选权衡:** 保留双轨,文档讲清楚,加 5 个最常见 scope→relation 翻译的等价测试。

## Future direction

如果未来要重写 interceptor (例如为了缓存或分布式追踪),那时再统一。在那之前:

- `DataScope` 是 **面向角色配置 UI 的粗粒度公共 API**
- `AccessRelation` 是 **粗粒度 API 表达不了的细粒度场景** 的关系模型
  (跨组织委派、时段授权、资源级而非组织级 scope 等)

## Related

- `domain/access/model/DataScope.java` (RBAC enum)
- `domain/access/model/entity/AccessRelation.java` (ReBAC entity)
- `infrastructure/access/DataPermissionInterceptor.java` (桥接器)
- `application/access/AccessRelationService.java` (Zanzibar 6 API)
- `infrastructure/access/PluginDataScopeRouter.java` (插件维度路由)
