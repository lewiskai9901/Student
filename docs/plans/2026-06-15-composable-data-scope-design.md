# 可组合数据范围模型（关系锚点 / 关系过滤 / 类型过滤 + 读写分离）

> 状态：设计稿（2026-06-15，已逐段评审通过）。取代当前"扁平 DataScope 枚举 + 旁挂列"的补丁式模型。
> 动因：闸2 的 2a（我管理的组织）/2b（类型过滤）是以枚举值和旁挂列实现的捷径；继续加"关系过滤"会让枚举笛卡尔积爆炸 + 列继续旁挂。本设计把数据范围重建为**正交可组合的三轴 + 读写分离**，一次建对、不再缝补。

## 0. 诊断

当前 `DataScope` 枚举把若干正交维度压成扁平值：`DEPARTMENT/DEPARTMENT_AND_BELOW/MANAGED_ORGS/MANAGED_ORGS_AND_BELOW` 实质是 `(组织锚点 × 含子树)` 的笛卡尔积，且关系写死为 `admin`；`custom_org_unit_ids`、`type_filter` 是旁挂列。每加一个关系 × 含不含子树就要再炸 2 个枚举值。

涉及的两个不同"关系"概念：
- **A. 关系作组织锚点**：圈哪些组织。"我管理的组织" = 我持 `admin` 关系的组织。推广 = 持任意关系的组织。
- **B. 关系作结果用户过滤**：在圈定组织内按用户与组织的关系筛/排除（如"排除管理者"）。

加上 2b 的"按用户类型过滤"，共三类筛选条件，且天然正交。

## 1. 数据模型（重建 `role_data_scopes`）

无数据兼容包袱 → 直接改建表 SQL，seed 重写。

| 列 | 含义 |
|---|---|
| role_id, resource_code, tenant_id | 维度 |
| **apply_to** ENUM('READ','WRITE','BOTH') 默认 BOTH | 读写分离一等公民 |
| **org_anchor** ENUM('ALL','SELF','PRIMARY_ORG','RELATION','CUSTOM_ORG','PLUGIN_DIM') | 轴①组织锚点 |
| **anchor_param** VARCHAR null | RELATION→关系码(admin/member/…)；PLUGIN_DIM→维度码(BY_CLASS…) |
| **include_subtree** TINYINT 默认 0 | 轴①含子树 |
| **custom_org_ids** JSON null | org_anchor=CUSTOM_ORG |
| **subject_rel_include** JSON null | 轴②只保留对锚定组织持这些关系的用户；null=不限 |
| **subject_rel_exclude** JSON null | 轴②剔除持这些关系的用户（"排除管理者"） |
| **type_filter** JSON null | 轴③类型码集（2b）|
| priority, created_at, updated_at, deleted | — |

唯一键：`(role_id, resource_code, apply_to, tenant_id)` —— 一个角色对一个资源最多两行（READ+WRITE）或一行 BOTH。

旧 `scope_type`/`custom_org_unit_ids` 列废弃；标量轴用列，列表轴用 JSON。插件维度收进 `org_anchor=PLUGIN_DIM`，不丢。

## 2. 内存 ScopeSpec + 老枚举降级为预设

`ScopeSpec` 值对象（字段 = 三轴直映射）：`applyTo / orgAnchor / anchorParam / includeSubtree / customOrgIds / subjectRelInclude / subjectRelExclude / typeFilter`。

老 7 枚举不再是存储真相，而是 UI **预设**（一键填好列）：

| 预设 | orgAnchor | anchorParam | subtree |
|---|---|---|---|
| 全部数据 | ALL | — | — |
| 仅本人 | SELF | — | — |
| 本组织 | PRIMARY_ORG | — | 否 |
| 本组织及以下 | PRIMARY_ORG | — | 是 |
| 我管理的组织 | RELATION | admin | 否 |
| 我管理的组织及以下 | RELATION | admin | 是 |
| 自定义 | CUSTOM_ORG | — | — |
| (插件) 按班级/专业… | PLUGIN_DIM | BY_CLASS… | — |

②③轴是叠加项（在任意预设上勾"排除某关系""只看某类型"）。新增 `OrgAnchor` 枚举为轴①真相；`DataScope` 退化为 `ScopePreset` 目录（仅显示名/排序/→Spec 映射），2a 的 `MANAGED_ORGS*` 收敛成 `RELATION:admin ±subtree` 预设。seed 旧值在 baseline 精确翻译成新列。

## 3. 拦截器重构：单一 compose 管线 × 资源路径

核心：**先解析"组织集"这一共享积木，三种资源路径统一消费**，再叠加 ②③。消除今天散在 `buildSingleRoleCondition`/`buildMembershipCondition` 的重复关系子查询。

```
每角色:  spec = resolveSpec(role, resource, actionClass)
         cond = subjectSelect(spec) AND subjectRelFilter(spec②) AND typeFilter(spec③)
多角色:  OR 合并（不变）
```

`resolveOrgSet(spec)`（唯一真相）：

| orgAnchor | 产出 |
|---|---|
| ALL | ⊤（无组织界）|
| SELF | 特判 → `creatorField = me` |
| PRIMARY_ORG | `{user.orgId}`，subtree 则 tree_path 展开 |
| RELATION:r | `SELECT resource_id FROM access_relations WHERE subject=me AND relation=r AND resource_type='org_unit'…`，subtree 再展开 |
| CUSTOM_ORG | customOrgIds，subtree 同理 |
| PLUGIN_DIM | 走 PluginDataScopeRouter 出资源 id 集 |

`subjectSelect` 按资源路径消费 orgSet：

| 资源路径 | 注入 |
|---|---|
| org 字段型 | `alias.orgField IN (orgSet)` |
| 成员型 viaMembership（user）| `alias.id IN (SELECT ar.subject_id … member … resource_id IN orgSet)` |
| access_relation 型 | `alias.id IN (SELECT ar.resource_id … subject=me OR subject∈orgSet)` |
| PLUGIN_DIM | `alias.id IN (resolver ids)` |

`subjectRelFilter`（仅成员型有意义）：include→`AND id IN (持 include 关系子查询)`；exclude→`AND id NOT IN (持 exclude 关系子查询)`。
`typeFilter`：`AND alias.typeField IN (...)`。

读写：actionClass = SELECT→READ∪BOTH，UPDATE/DELETE→WRITE∪BOTH；本阶段 seed 全 BOTH → 行为不变。`1=0`/空集/fail-safe 语义保留；参数按管线顺序（orgSet→②→③）位置绑定。

## 4. 写范围喂给闸3（而非第二条 SQL）

**同一份 ScopeSpec，两张执行面**：

```
ScopeEvaluator
├─ toSqlCondition(spec, ctx) → SQL 片段       拦截器用（READ，集合过滤）
└─ contains(spec, targetId, ctx) → boolean    闸3 用（WRITE，单目标判定）
```

读=集合 SQL 过滤；写=对一个目标判"在不在范围"。一套词汇，两个执行面——读写各在其层，不复制机制。

`contains` 复用解析：① 目标是否为 orgSet 中组织的成员；② 是否持被排除关系（如 admin）→ 是则不在；③ 类型是否在允许集。

接入闸3（最小布线，插进既有 `List<PolicyRule>`）：

```
WithinWriteScopeRule implements PolicyRule
  appliesTo: USER_CREATE/UPDATE/DELETE/DISABLE/RESET_PASSWORD/ASSIGN_ROLES
  evaluate(req):
    spec = subject 对 user 资源的 WRITE∪BOTH ScopeSpec
    若无 spec / orgAnchor=ALL → NOT_APPLICABLE
    若 ScopeEvaluator.contains(spec, req.targetId)==false → DENY("目标超出可写范围")
    否则 NOT_APPLICABLE
```

`UserManagementGuard` 已跑 PolicyEngine（deny-overrides），新规则自动参与，不改调用点。`ProtectSuperAdminRule`/`ProtectOrgManagerRule` 是绝对底线，`WithinWriteScopeRule` 是可配写范围，deny-overrides 叠加。

## 5. 资源 × 轴 能力矩阵（优雅降级）

| 资源路径 | ①锚点 | ①RELATION | ①子树 | ②关系过滤 | ③类型过滤 |
|---|---|---|---|---|---|
| org 字段型 | ✓ | ✓ | ✓ | ✗ | ✓ 若有 type_field |
| 成员型（user）| ✓ | ✓ | ✓ | ✓ | ✓ |
| access_relation 型 | ✓ | ✓ | ✓ | ✗（暂不开）| ✓ |
| 插件维度 | PLUGIN_DIM | — | — | ✗ | ~ |

声明数据驱动：①锚点集合泛化自 `allowed_scopes`；①RELATION/②关系码来自关系字典（同源）；②可用性从 `viaMembership` 提升为 `data_resources` 能力列；③由 `type_field`/`type_entity`。后端返回 `ResourceScopeCapabilities` 给 UI 收窄渲染。拦截器**防御式降级**：②对非成员型资源跳过、③无 type_field 跳过、PLUGIN_DIM 无维度按 fallback —— 配错也不产坏 SQL。

## 6. UI 范围生成器（预设快路 + 高级叠加）

生成器活在**高级模式每资源行**；预设是快路，90% 不展开。紧凑风格（纯文字+下拉）。

```
⚙ 用户   [预设▾ 我管理的组织]  [全部类型▾]   ▸展开
  ① 组织锚点  ○全部 ○仅本人 ○本组织 ●我[关系▾管理]的组织 ○指定组织[树选]  ☑含下级
  ② 关系过滤  ○不限  ○仅[关系▾]  ●排除[关系▾管理者]      ← 仅成员型
  ③ 类型过滤  [全部类型▾ 学生×]                          ← 有 type_field
  ④ 读写      ●读写一致  ○读宽写窄(读[预设▾] 写[预设▾])   ← §4 上线后点亮
```

②③④按 §5 能力矩阵显隐。①②关系下拉来自关系字典。右侧"配置预览"自然语言渲染三轴。场景模板模式保持极简（预设→BOTH+anchor），表达力收在高级模式。

## 7. 分阶段落地 + 验证口径

**M1 — 模型 + READ 全链路（核心，独立可上线）**：重建表 + seed 翻译；OrgAnchor/ScopeSpec/ScopePreset；ScopeEvaluator.toSqlCondition + 拦截器重构（resolveOrgSet 单一真相，删重复子查询）；读写存取新列；ResourceScopeCapabilities；前端生成器 READ 侧。
验证：compose 单测；真库 SQL 逐轴对照；API 往返；浏览器 E2E；**BOTH-seed 角色重构前后表计数逐行零差异**。

**M2 — WRITE 通路（闸3 接线）**：ScopeEvaluator.contains + WithinWriteScopeRule；UI 点亮读宽写窄。
验证：规则单测；E2E（写范围=排除管理者 → GET admin 可、PUT admin=403、PUT 普通成员=200）；护栏共存。

**M3 — 守护 + 收尾**：架构测试封禁"再加扁平枚举值"式扩展；删 DataScope 残留；ADR + memory。

**最大风险**：安全内核，重构 bug 静默放宽/收窄。两道兜底：① 逐轴真库对照证语义；② BOTH-seed 角色快照逐行零差异。

落地方式：git worktree 隔离；每 M 独立提交、独立过验证再合。
