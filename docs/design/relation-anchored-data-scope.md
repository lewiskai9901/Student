# 数据范围增强：关系锚定 + 类型过滤（闸2 详细设计）

> 状态：设计稿（未实现）。这是 [authorization-hardening.md](./authorization-hardening.md) 里 **P2（数据范围增强）** 的详细展开。
> 动因：用户 a 住在组织 A、管理组织 B 和 C，希望 a 的数据范围 = {B, C}（他管理的组织），而不是 A。现有"本部门/本部门及以下"锚定在用户的**单个主组织**，表达不了"我管理的多个组织"。

## 1. 核心思想：范围由"关系"推导，而非固定归属节点

现状：`本部门 = user.orgUnitId(单个主组织)`。假设"一个用户一个家"——a（住 A、管 B/C）正好打破它。

增强：**数据范围锚定在"用户与组织的关系"上**。
- "我所属的组织" = 用户持 `member` 关系的组织
- **"我管理的组织" = 用户持 `admin`(管理) 关系的组织 = {B, C}** ← a 要的
- 推广：**"我具有 <任意关系> 的组织"**

这与 Salesforce Territory / Google Zanzibar 同理：**权限范围 = 你的关系推导出来的**。且关系/组织是通用概念 → **此能力属 core，不碰去行业化守护**。

## 2. 增强后的范围模型：三段可组合（AND 交集）

```
① 组织锚点 (圈定哪些组织)            ② 含子级?       ③ 类型/特征过滤(可选)
   ALL          无组织边界            ☐ / ☑          NONE        不限
   SELF         仅创建人(creator)     含 org 子树      TYPE_IN     user_type_code ∈ {…}
   PRIMARY_ORG  我所属的主组织                         FEATURE     hasFeature(…)
   RELATION:<r> 我持<r>关系的组织 (admin→{B,C})
   CUSTOM_ORG   管理员手点的固定组织集
```

最终过滤 ≈
```sql
WHERE {orgField} IN ( <锚点解析出的组织集, 可含子树> )   -- ① + ②
  AND {typeField} IN ( <类型/特征过滤> )                -- ③, 可选
```

- **老 5 档是新模型的预设/简写**（平滑共存）：
  - `本部门` = anchor PRIMARY_ORG, subtree=false
  - `本部门及以下` = anchor PRIMARY_ORG, subtree=true
  - `自定义` = anchor CUSTOM_ORG
  - `仅本人` = anchor SELF
  - `全部` = anchor ALL

## 3. 解析逻辑（拦截器）

| 锚点 | 解析出的组织集 |
|---|---|
| ALL | （无 org 条件）|
| SELF | （无 org 条件，改为 `creator = 当前用户`）|
| PRIMARY_ORG | `{ user.orgUnitId }`，subtree 则按 tree_path 展开 |
| **RELATION:r** | `SELECT resource_id FROM access_relations WHERE subject_type='user' AND subject_id=:uid AND relation=:r AND resource_type='org_unit' AND deleted=0`，subtree 则再按 tree_path 展开 |
| CUSTOM_ORG | 配置里的 customOrgIds，subtree 同理 |

- **RELATION:admin** → a 解析出 {B, C}；勾"含子级"再并入 B、C 的子树。
- 类型过滤 `TYPE_IN` → `AND user_type_code IN (...)`；`FEATURE` → `AND user_type_code IN (SELECT type_code FROM entity_type_configs WHERE features 含 :feature)`（或预解析成类型集）。
- **org 集用子查询，不要在应用层物化成大 IN 列表**（性能 + SQL 长度）。

## 4. 存储模型

`role_data_scopes`（按"无数据兼容"原则可直接改表结构）升级为：

| 列 | 含义 |
|---|---|
| role_id, resource_code | 主键维度（不变）|
| anchor_type | ALL / SELF / PRIMARY_ORG / RELATION / CUSTOM_ORG |
| anchor_relation | anchor=RELATION 时的关系码（如 `admin`）|
| include_subtree | 是否含 org 子树 |
| custom_org_ids | anchor=CUSTOM_ORG 时的组织集（JSON）|
| type_filter_mode | NONE / TYPE_IN / FEATURE |
| type_filter_values | 类型码集 或 能力键集（JSON）|

> 旧 `scope_type` 枚举值在读取时映射到上表（见 §2 预设表），代码与配置都按新模型走。

## 5. 拦截器改造

`DataPermissionInterceptor.buildSingleRoleCondition` 改为：
1. 读该 (role,resource) 的范围规格 → 得 anchor / relation / subtree / typeFilter。
2. **org 条件**：按 §3 解析锚点 → 拼 `orgField IN (子查询/子树)` 或（SELF）`creatorField = ?`。
3. **类型条件**：typeFilter 非 NONE → `AND typeField IN (...)`。
4. 多角色按现有逻辑 OR 合并；本资源最终条件 = (角色1) OR (角色2) …

复用点 & 缺口：
- 已有 access_relations 子查询基础设施 → **扩展它支持按 `relation` 类型过滤**（当前不看 relation 类型，是已知缺口）。
- typeField/orgField 来自 `data_resources` 配置（resource 已声明 orgUnitField；需为支持类型过滤的资源补 `type_field`，如 user→`user_type_code`）。

## 6. 配置 UI：把"5 档下拉"换成"范围生成器"

```
组织锚点:  ○全部  ○我所属的组织  ●我管理的组织  ○我具有[关系▼]的组织  ○指定组织[树选]  ○仅本人
含下级:    ☑ 含下级组织
类型过滤:  ○不限  ●按类型[学生 ×][教师 ×]  ○按能力[可学习 ×]
```
- 每个资源一份；**理想上区分读/写**（读宽写窄，见 hardening 文档 P2 的读写分离）。
- 锚点选 RELATION 时，关系下拉来自关系字典（member/admin/…）。
- 类型/能力多选来自 `entity_type_configs`（按 entity_type=USER 过滤）。

## 7. 典型用例

| 诉求 | 配置 |
|---|---|
| **a 管理 B、C 的数据** | anchor=RELATION:admin（+含子级可选）→ {B,C} |
| 学工只管学生 | anchor=PRIMARY_ORG+子树, typeFilter=FEATURE:isLearner |
| a 管理的组织里、只看学生 | anchor=RELATION:admin, typeFilter=TYPE_IN:[STUDENT] |
| 班主任只看自己班的学生 | （已存在的 BY_CLASS 维度，关系锚定的特例）|
| a-admin：a 只管成员、不碰管理者 | 在 user 资源上 anchor=RELATION:admin（B,C）+ 关系排除 admin（§8）|

## 8. 与"排除某关系/护栏"的衔接
- **排除型**（"除管理者外"）：在关系子查询上加 `AND id NOT IN (admin 关系子查询)`。属本增强的延伸。
- **护栏（闸3）仍要有**：范围是"常规布线"，可能配错；闸3 的"超管/管理者/防提权"护栏是"配错也突破不了的底线"。两者并存（见 hardening 文档）。

## 9. 风险与权衡
- **性能**：每次查询多一个 access_relations 子查询（+可能的 tree_path 子树）。需确保索引：`access_relations(subject_type,subject_id,relation,resource_type)`、`org_units(tree_path)`。建议**按请求缓存**锚点解析结果，避免一次请求内重复解析。
- **多组织 + 子树**：B、C 子树可能重叠/不相交，`IN 子查询`天然去重。
- **UserContext 单 orgUnitId 不够用**：关系锚点直接查 access_relations，不依赖那个单值。
- **配置复杂度上升**：范围生成器要把语义标注清楚（同"检查模块 scope 标注"思路），否则管理员易配错。
- **类型 vs 行业化**：core 优先 `FEATURE`（行业中性）；`TYPE_IN` 含具体类型码（STUDENT…）更适合行业插件维度或明确知道在配教育场景时。

## 10. 闸2 内部建议分步
```
2a 关系锚点 RELATION:<rel> (+含子级)   ← a 管 B/C 的核心诉求, 价值最高
2b 类型/特征过滤 (AND 组合)
2c 关系排除 (NOT IN 某关系)
2d 读写分离 scope (读宽写窄)
2e 配置 UI 范围生成器
```
建议先 2a（直接满足 a 管 B/C）→ 2b → 其余按需。
