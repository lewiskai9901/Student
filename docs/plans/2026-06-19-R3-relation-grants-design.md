# R3 设计稿 —— `role_data_scopes` → `relation_grants`(按关系授予 + 多锚点 OR)

> 状态:设计稿(2026-06-19)。承接统一数据归属终版设计 `docs/plans/2026-06-18-unified-data-anchoring-design.md` §7/§8/§9/§15-R3,以及 M1 可组合数据范围 `docs/plans/2026-06-15-composable-data-scope-M1.md`。
>
> 决策(2026-06-19 brainstorming):**采用设计稿 §8 的完整 relation_grants 模型**(非测量式扩展),把 M1 的"1 组织锚点 + 关系过滤"统一为"N 条关系授予 OR"。
>
> 前置已就绪:R1-R2.4 已让 `resource_relations` 注册表成为锚点唯一真相源(org/creator/viaMembership),P3 懒加载/fail-fast 在位。R3 让注册表进一步驱动**整条按关系的可见性**,不止 buildMeta 的单锚点。

---

## 0. 一句话

**每条 `role×resource×apply_to` 配置 = 一组"关系授予"(`{relation, subject, subtree?}`);可见集 = 各授予子条件 OR 叠加;引擎按 `resource_relations` 注册表的 `storage_kind` 数据驱动生成子条件。** M1 三轴被"关系授予"统一表达,旧 `OrgAnchor`/`ScopePreset` 降级为生成 grants 的预设糖。

---

## 1. 出发点:M1 已交付什么,R3 的真增量

M1(`role_data_scopes` 正交三轴)已交付:
- **轴① 组织锚点** `OrgAnchor`(ALL/SELF/PRIMARY_ORG/RELATION/CUSTOM_ORG/PLUGIN_DIM)+ param + subtree + customOrgIds。
- **轴② 主体关系过滤** `subject_rel_include/exclude`(已是关系感知)。
- **轴③ 类型过滤** `type_filter`(正交)。
- 读写分离 `apply_to`(READ/WRITE/BOTH);UI 重设计(默认范围 + 资源例外 + 预览 + 模拟)。

**R3 的真增量 = 多锚点 OR**:M1 每行只有**一个** orgAnchor。inspection 受检面要"我开的 ∨ 本组织开的 ∨ 针对本组织的"= **三条不同关系(creator/owner_org/inspected)各带 subject 范围,OR 叠加** —— M1 表达不了,现靠 `MyReceivedInspectionsController` 旁路 SQL 绕开权限层。R3 把"单锚点"升级为"N 条关系授予",这是 §8 的统一模型,也解锁多锚点。

---

## 2. 模型:一条配置 = 一组关系授予

```
role_data_scopes 一行 (role × resource × apply_to):
  relation_grants = [
    { relation: "creator",   subject: "SELF" },
    { relation: "owner_org", subject: "MY_ORG", subtree: true },
    { relation: "inspected", subject: "MY_ORG", subtree: true }
  ]
  type_filter = ["user_type_code:TEACHER"]   // 轴③ 正交保留
```

- **`relation`**:引用 `resource_relations` 注册表里**该资源**的关系码(creator / owner_org / inspector / inspected / reviewer …)。引擎据其 `storage_kind` 决定子条件形态。
- **`subject`**(统一解析器):
  - `SELF` —— 当前用户(列锚资源 = creator 列 = 我;成员资源 = member-self)。
  - `MY_ORG`(配 `subtree`)—— 我的主属组织(+ 子树),复用 M1 `resolveOrgSet`。
  - `RELATION:<code>` —— 经该关系到达的组织集(如 `RELATION:admin` = 我管理的组织),复用 MANAGED_ORGS / `access_relations` 查询。
  - `CUSTOM`(配 `orgIds`)—— 管理员显式指定组织集。
  - `PLUGIN_DIM:<dim>` —— 插件维度(BY_CLASS…),复用 `PluginDataScopeRouter`。
  - `ALL` —— 无界(该关系不约束)。
- 可见集 = 各 grant 子条件 **OR**;轴③ `type_filter` 对**带类型属性**的子条件 **AND** 叠加(纯 `creator:SELF` 等不波及)。

---

## 3. Schema 变更:`role_data_scopes`

```sql
-- 保留: id, role_id, resource_code, apply_to, type_filter(轴③), priority, tenant_id, created_at, updated_at, deleted
-- 删(轴①②列):
ALTER TABLE role_data_scopes
  DROP COLUMN org_anchor, DROP COLUMN anchor_param, DROP COLUMN include_subtree,
  DROP COLUMN custom_org_ids, DROP COLUMN subject_rel_include, DROP COLUMN subject_rel_exclude;
-- 新增:
ALTER TABLE role_data_scopes
  ADD COLUMN relation_grants json DEFAULT NULL COMMENT '关系授予数组 [{relation,subject,subtree?,orgIds?}]; NULL=取 registry grants_by_default';
-- UNIQUE 不变: uk_role_res (role_id, resource_code, apply_to, tenant_id)
```

破坏性直接改(无真实数据):同步改 `baseline_v3.sql`(CREATE + INSERT,需重映射现有 105 行配置)+ 加 post-v3 迁移。迁移含**数据转换**(轴列 → relation_grants JSON,见 §6),不只 DDL。

`relation_grants` JSON 元素 schema:`{ "relation": str, "subject": "SELF|MY_ORG|RELATION:<code>|CUSTOM|PLUGIN_DIM:<dim>|ALL", "subtree": bool?, "orgIds": [long]? }`。

---

## 4. 引擎:`ScopeEvaluator` 多 grant OR + 注册表数据驱动

当前:`toSqlCondition(spec, meta, …)` 从单 `ScopeSpec` compose 一条 org 条件(+ creator/membership)。R3 改为:

```
for grant in spec.relationGrants:
    rel  = registry.relationOf(resourceCode, grant.relation)   // storage_kind/column/ar_relation/type_column
    set  = resolveSubject(grant.subject, grant.subtree, grant.orgIds, userCtx)
    sub  = switch rel.storageKind:
        COLUMN  + org     : alias.column IN (:set)
        COLUMN  + user    : alias.column = :me
        COLUMN  + 多态     : (alias.type_column='ORG_UNIT' AND alias.column IN (:set))
        SUBJECT_GRAPH     : 复用现 viaMembership 子查询 (member → org set)
        RECORD_RELATION   : ⛔ R4 才有 record_relations —— R3 不支持, 见 §7
    grantSqls += sub
where = "(" + OR(grantSqls) + ")"        // 多 grant OR
where = where AND typeFilterCond         // 轴③ (带类型属性的关系)
```

**关键扩展 `ResourceRelationRegistry`**:除现有派生单锚点(`forResource` → org/creator/viaMembership),新增**逐关系暴露**(`relationOf(resourceCode, relationCode)` → `{storageKind, columnName, arRelation, typeColumn}`)。注册表已加载全部 `resource_relations` 行,只是当前只派生 owner_org/creator;扩展是 additive。

**拒绝语义(§9.2,钉死)**:
1. 角色对资源可见集 = grants 各子条件 OR。
2. **无 grant** → 取注册表 `grants_by_default=1` 的关系(一般 owner_org=本组织);**无默认关系 → `1 = 0`(拒绝),绝不 fail-open**。
3. grant 引用的 relation 必须在 `resource_relations` 存在且 enabled(构建期 + 运行期双校验),否则拒绝。
4. `apply_to` 读写分离:同资源 READ / WRITE 各一行 grants(读宽写窄)。

**多态 `inspected`**:授 `MY_ORG` 仅匹配 `type_column='ORG_UNIT'` 行;授 `SELF` 仅匹配 `type_column='USER' AND col=:me`。

---

## 5. `ScopeSpec` / `ScopePreset` / `OrgAnchor` 去向

- **`ScopeSpec`**:删 `orgAnchor/anchorParam/includeSubtree/customOrgIds/subjectRelInclude/subjectRelExclude`,改持 `List<RelationGrant> relationGrants`;保留 `applyTo` + `typeFilter`。
- **`RelationGrant`**(新值对象):`{ relation, subject(枚举 SubjectScope), subtree, orgIds }`。`SubjectScope` 枚举:`SELF/MY_ORG/RELATION/CUSTOM/PLUGIN_DIM/ALL` + param。
- **`ScopePreset`**:从"映射轴①"改为"**生成 relation_grants 预设糖**"。例:`DEPARTMENT` → `[{owner_org, MY_ORG}]`;`DEPARTMENT_AND_BELOW` → `[{owner_org, MY_ORG, subtree}]`;`MANAGED_ORGS` → `[{owner_org, RELATION:admin}]`;`SELF` → 列锚资源 `[{creator, SELF}]` / 成员资源 `[{owner_org, SELF}]`;`ALL` → `[{owner_org, ALL}]`。UI 仍可用这些命名预设作快捷入口。
- **`OrgAnchor`**:其语义被 `SubjectScope` 吸收(ALL/SELF/PRIMARY_ORG→MY_ORG/RELATION/CUSTOM_ORG→CUSTOM/PLUGIN_DIM)。可保留为内部解析枚举或删除(R3a 决定)。
- **`DataPermissionPolicyService`**:`getScopeSpec`/`mapToScopeSpec`/`saveRolePermission`/`deriveSpecForSave`/`mapToPermission` 全部从轴列读写改为 `relation_grants` JSON 读写。

---

## 6. 迁移:M1 三轴 → relation_grants(§15-R3"最易错",storage_kind 敏感)

逐 `OrgAnchor` × 资源 storage_kind 映射现有 105 行:

| M1 (org_anchor + param + subtree) | 列锚资源 (storage=COLUMN) | 成员资源 (storage=SUBJECT_GRAPH: user/student) |
|---|---|---|
| `ALL` | `[{owner_org, ALL}]` | `[{owner_org, ALL}]` |
| `SELF` | `[{creator, SELF}]` | `[{owner_org, SELF}]` ⚠ member-self 非 creator |
| `PRIMARY_ORG`(DEPARTMENT) | `[{owner_org, MY_ORG}]` | `[{owner_org, MY_ORG}]` |
| `PRIMARY_ORG`+subtree | `[{owner_org, MY_ORG, subtree}]` | 同左 |
| `RELATION:admin`(MANAGED_ORGS) | `[{owner_org, RELATION:admin}]` | 同左 |
| `RELATION:admin`+subtree | `[{owner_org, RELATION:admin, subtree}]` | 同左 |
| `CUSTOM_ORG` | `[{owner_org, CUSTOM, orgIds}]` | 同左 |
| `PLUGIN_DIM:<dim>` | `[{owner_org, PLUGIN_DIM:<dim>}]` | 同左 |
| 轴② `subject_rel_include/exclude` | 追加/排除对应 relation 的 grant | 同左 |
| 轴③ `type_filter` | **保留列不变** | 同左 |

⚠ **最易错点**:
1. `SELF` 对**成员资源**(user/student,storage=SUBJECT_GRAPH)是 member-self(s.user_id=我),**不是 creator**。迁移须按资源 storage_kind 分流。
2. 多角色合并:M1 多角色逐 spec OR 合并的语义,在 R3 变为多角色各自 grants OR(等价或更安全)。
3. 现有 105 行须逐行验证字节等价(见 §7 R3a)。

迁移以**脚本生成 SQL**(读现有轴列 + join `data_resources.resource_kind` / `resource_relations.storage_kind` → 输出 relation_grants),非手写。

---

## 7. 分相(先等价后扩展;每相独立 commit + 真启动 + 表快照零回归)

### R3a —— relation_grants 列 + 读写接线 + 单 grant 字节等价(最高风险)
- 加 `relation_grants` 列;`DataPermissionPolicyService` 读写改 JSON;`ScopeSpec`→`List<RelationGrant>`。
- 迁移现有 105 行 → 等价 relation_grants(§6 脚本)。
- `ScopeEvaluator` 读 relation_grants,**单 grant 路径产与 M1 同一 SQL**。
- 验证:`ScopeEvaluatorEquivalenceTest`(逐 M1 配置 → 同 SQL,仿 `BuildMetaRegistryEquivalenceTest`)+ dpt_ct/CLASS_TEACHER 金标准 + 全 105 行真库逐条 SQL diff。
- 删 M1 轴①②列(baseline + 迁移)。

### R3b —— 多 grant OR 引擎 + inspection 受检面(= 收编 R2.3)
- `ResourceRelationRegistry.relationOf` 逐关系 storage 暴露;`ScopeEvaluator` OR 多 grant。
- 配 `inspection_record` 受检面角色 = `[{creator,SELF},{owner_org,MY_ORG,subtree},{inspected,MY_ORG,subtree}]`。
- 真查金标准:检查主任查检查列表 → 命中"我开的 ∪ 本组织开的 ∪ 针对本组织的",逐行核对。
- ⛔ **RECORD_RELATION(reviewer / inspected[多])依赖 `record_relations`(R4)** —— R3 多 grant 只覆盖 COLUMN + SUBJECT_GRAPH;`inspected` 取**单值多态**列。reviewer 等 R4。

### R3c —— UI 改"按关系授予 + 预览"
- 复用 M1 UI 脚手架(默认范围 + 资源例外 + 常驻预览 + 模板 + 模拟用户),**只把"三轴编辑器"换成"关系授予编辑器"**(每资源列出可授予关系,勾选 + 选 subject 范围)。预设(本组织/我管理的/…)作快捷入口生成 grants。
- 前端类型重生成(`role_data_scopes` 契约变)。

### bypass 收编
- R3b 证多 grant 等效后,`MyReceivedInspectionsController` 自查 SQL 退役,改走 `inspected:MY_ORG` 统一引擎。可作 R3d 或并入 R8(建议 R3b 验证通过后单独小 commit)。

---

## 8. 验收(真库 + 等价)

1. **字节等价**:全 105 行 M1 配置 → relation_grants → 引擎产**同一 SQL**(逐条 diff)。
2. **多锚点金标准**:inspection 受检面 3-grant → "我开的 ∪ 本组织开的 ∪ 针对本组织的",真库 API 往返逐行核对。
3. **deny 不 fail-open**:无 grant 且无 `grants_by_default` → `1=0`;引用未注册关系 → 拒绝。
4. **读写分离**:WRITE grants 收窄 → 同用户可读不可写某记录(M2 接线时验,R3 先保证 apply_to 读写各一行)。
5. 单测金线 + 真启动 GREEN(registry 加载、无 Unknown column)+ 表快照零回归 + `access_relations` 未被写入记录级行。
6. dpt_ct/CLASS_TEACHER 金标准复跑(student 成员路径不回归)。

---

## 9. 风险 / 边界

- **迁移映射错**(storage_kind / SELF-成员 / 多角色合并)—— 逐行字节等价是安全网,脚本生成 + 单列清单评审(§6)。
- **evaluator OR 的参数绑定** —— 多 grant 多组 `?`,沿用拦截器现有 trailing-LIMIT 位置插入逻辑;每 grant 的 param index 累加(现 `globalParamIdx` 已是此模式)。
- **UI 重做回归** —— 复用 M1 脚手架降面;Playwright e2e 覆盖默认+例外+预览。
- **RECORD_RELATION 不在 R3 承诺** —— reviewer / inspected[多] 明确留 R4;R3 文案/UI 不暴露这些关系为可授予,避免空头。
- **`grants_by_default`** —— `resource_relations` 现有该列(R1 schema),R3a 需为各资源核定默认关系(一般 owner_org),否则无 grant 资源全 deny。
- **M2(WRITE 闸3)** —— R3 只做 READ 引擎 + apply_to 存储;WRITE 实际执行是 M2/R8,R3 不接 WRITE 执行面。

---

## 10. 与统一设计 §8 的关系 + 后续解锁

- R3 落地 §8(按关系授予)+ §7(多锚点 OR 引擎)+ §9(可见性语义)的 **READ 侧 + COLUMN/SUBJECT_GRAPH 部分**。
- **R4**(`record_relations` 表)解锁 RECORD_RELATION grant(reviewer / inspected[多])→ inspection 受检面补全多被检查 + 复核员。
- **R5** ASSET 升主体 → `inspected→ASSET` grant 闭合。
- **R6** 物化倒排索引 → RECORD_RELATION/图派生 grant 走 `subject_visible_records`(`MATERIALIZED` 分支)。
- **R8** 写鉴权(闸3 `matches()` 消费同一 relation_grants)+ 受检面 controller 收编 + 守护。

**顺序**:R3a(字节等价)→ R3b(多 grant + inspection)→ R3c(UI)→ R4 …。每相独立 commit,真启动 + 快照零回归。R3a 同 R2.2 是高风险切换,**宜独立会话专注**。
