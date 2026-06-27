# 访问控制术语表 (scope / relation 消歧)

> 2026-06-27 · 配合插件平台可懂性改造。本文固化两组"高密度近名"概念的口径,
> 供阅读代码/配置数据权限时消歧。**纯文档,零行为。**

数据权限引擎里 "scope" 和 "relation" 两个词各自指代多个不同构件。多数无法低成本重命名
(枚举值持久化进 DB、跨表引用),故用本表锁定指称。已做的命名收敛见末尾。

---

## 一、"scope" 是三条不同的轴 (勿混)

| 轴 | 类型 | 取值 | 管什么 | 持久化 |
|---|---|---|---|---|
| **① 数据范围** (data scope) | `OrgAnchor`(存储真相) + `ScopePreset`(UI 预设糖衣) | OrgAnchor: ALL/SELF/PRIMARY_ORG/RELATION/CUSTOM_ORG/PLUGIN_DIM | 一个角色对某资源**能看/改哪些行** | `role_data_scopes.org_anchor` 等 |
| ①附 grant 内 subject 范围 | `SubjectScope` | SELF/MY_ORG/RELATION/CUSTOM/PLUGIN_DIM/ALL | 一条 `relation_grant` 里 subject 的范围 | `role_data_scopes.relation_grants` JSON |
| **② 角色授予作用域** | `RoleAssignmentScope` (原 `ScopeType`, 已改名) | ALL / ORG_UNIT | 一次"把角色授予用户"作用于全局还是某组织 | `user_roles.scope_type` |
| **③ 权限可见性/语境** | `PermissionScope` | PUBLIC / SELF / MANAGEMENT | 一个权限点属于公开/个人空间/管理面 (与数据范围无关) | `permissions.scope` |

**易错点**: `SELF` / `ALL` / `CUSTOM` 在轴①②③多处出现, 含义不同 —— 说"这个角色 scope 是 SELF"
必须先说清是哪条轴。`DataScope` 旧扁平枚举已删 (purge), 由 `ScopePreset` 取代。

## 二、"relation" 是五层不同构件

| 层 | 表 / 载体 | 含义 | UI 叫法 |
|---|---|---|---|
| 类型字典 | `relation_types` | 主体↔主体的**关系类型** (member/admin/family_of…) | 插件平台「主体关系」|
| 实例图 | `access_relations` | 上面类型的**实例** (谁是某组织 member) | 关系绑定 |
| 资源锚定声明 | `resource_relations` | 某**数据资源**经哪个 列/关系/解析器 **锚定到主体** (owner_org/creator/inspected…) | 插件平台「数据关系」|
| 记录实例 | `record_relations` | 记录↔主体的逐条关系实例 (R4 地基, 当前休眠零消费) | — |
| 角色授予 | `role_data_scopes.relation_grants` | 角色数据范围里的"关系授予" `[{relation, subject…}]` | 数据权限配置·多锚点 |

**最易错点**: `relation_grants[].relation` 的值 (creator/owner_org) 指向的是
**`resource_relations.relation_code`** (数据锚定关系), **不是** `relation_types` 的码 (member/admin)。
即同一个 "relation" 字段, 在 grant 上下文 = 数据锚定关系, 在 access_relations 上下文 = 主体关系。

## 三、已做的命名收敛 (2026-06-27)
- `ScopeType`(access, 角色授予作用域) → **`RoleAssignmentScope`** —— 消除与
  `inspection.model.execution.ScopeType` (检查范围 ORG/PLACE/USER) 的字面撞名 + 移出 scope 堆。
- `DataScope` 旧扁平枚举 → **删除** (ScopePreset 已取代)。
- 其余 (OrgAnchor↔SubjectScope 收敛 / PermissionScope 改名 / resource_relations 改名) 经评估
  为高 churn 低 ROI, **不改名**, 以本术语表消歧即可。
