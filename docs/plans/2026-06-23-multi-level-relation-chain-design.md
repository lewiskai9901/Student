# 多级关系链数据范围 — 综合设计 (2026-06-23)

> 用户要求: 数据范围 = **多级关系链**, 主体 →[关系]→ 中间实体 →…→ 终端锚点 → 数据。
> 终端必须是**模块数据支持的锚点**。覆盖**三大主体(user/org_unit/place)**, 综合**现有关系系统**
> 与**插件注册的数据关系**。本文档为设计 (不改代码), 供决策是否动工 (= 关系代数引擎, S/S+ 级)。

## 1. 实体全景 (已从活库核实)

### 三大主体
`user` / `org_unit` / `place` —— relation_types 的 from/to 仅这三类。

### 关系图谱 = 链的"边" (relation_types, 现有关系系统; 实例存 access_relations)
| from → to | 关系 (CORE) | 关系 (插件) |
|---|---|---|
| user → org_unit | member 成员 / admin 主管 / deputy 副管 / responsible_for 责任 / viewer 查阅 / watches 关注 | teaches 任课(EDU) |
| user → place | admin 负责 / manages 管理 / occupies 占用 / responsible_for 责任 / viewer 查阅 | — |
| user → user | supervisor_of 上级 / delegated_to 委托 / guardian_of 监护 / family_of 亲属 / responsible_for 责任 / emergency_contact / viewer | mentor_of 导师(EDU) |
| **place → org_unit** | **belongs_to 归属** | — |
| org_unit → * | (无出边, 仅作终点) | — |

观察: **user 是主枢纽**(连 org/place/user); **place→org** 让"经场所"链能继续到组织; org_unit 是叶终点。

### 终端锚点 = 数据"怎么挂主体" (resource_relations, 插件+核心注册; 我 R1-R2.4 建的)
| 存储种类 storage_kind | 含义 | 例 |
|---|---|---|
| COLUMN | 数据表直接列 | owner_org→org_unit_id; creator→created_by |
| SUBJECT_GRAPH | 经 access_relations 成员图 | student/user 的 owner_org (成员关系挂叶子组织) |
| PROVIDER | 插件 resolver bean 算子查询 | inspection_record.inspected; student.taught_by |
| RECORD_RELATION | record_relations 表 | inspection_record.reviewer |

**现状**: ~全部资源 = `creator`(→USER) + `owner_org`(→ORG_UNIT)。**subject_type 只有 USER/ORG_UNIT, 零 PLACE**。

## 2. 两个系统的职责 (综合)
- **relation_types/access_relations = 链的中间边** (任意主体↔主体↔组织/场所 图遍历)。
- **resource_relations = 链的终端锚点** (数据↔三大主体, 带 storage_kind 决定怎么生成可执行 SQL)。
- 链 = `[中间跳: 走 relation_types 图] + [终端: ∈ 该资源 resource_relations]`。**终端∈注册表 = 你要的"链接最后只能是模块数据支持的锚点", 也是可强制执行的边界**。

## 3. 多级链模型 (建议数据结构)
```
ChainScope = {
  hops: [                                  // 中间跳 (0..N, 走 access_relations 图)
    { fromType, relation, toType, direction, subtree? },   // 如 user--manages-->place
    { fromType, relation, toType, ... },                   // 如 place--belongs_to-->org_unit
  ],
  terminal: { anchorRelation },            // ∈ resource_relations[resource] (owner_org/creator/owner_place/PROVIDER/...)
  combine: 'OR' | 'AND',                   // 同级多关系: 并集/交集
  typeFilter?: [...]                       // 类型过滤 (轴③, 已有)
}
```
起点恒为当前用户 (`我`)。每跳查 access_relations 一层; 终端按 storage_kind 落地。

### 三大主体全覆盖示例
- **经组织**: 我 --member--> org --[owner_org]--> 数据
- **经场所**: 我 --manages--> place --belongs_to--> org --[owner_org]--> 数据 (3 级!)
- **经人**: 我 --supervisor_of--> user --member--> org --[owner_org]--> 数据
- **直挂场所**(需先注册 owner_place 锚点): 我 --occupies--> place --[owner_place]--> 数据

## 4. SQL 编译 (每跳一层子查询, 终端按 storage_kind)
```sql
-- 中间跳: 递归/嵌套 access_relations
WITH lvl0 AS (SELECT :me AS id),                                  -- 我
lvl1 AS (SELECT resource_id FROM access_relations
         WHERE subject_id IN (SELECT id FROM lvl0) AND relation='manages' AND resource_type='place'),
lvl2 AS (SELECT resource_id FROM access_relations               -- place--belongs_to-->org
         WHERE subject_type='place' AND subject_id IN (SELECT id FROM lvl1) AND relation='belongs_to')
-- 终端 (storage_kind=COLUMN owner_org): 
... WHERE data.org_unit_id IN (SELECT id FROM lvl2)
-- 终端=SUBJECT_GRAPH: data.id IN (成员子查询 over lvl2)
-- 终端=PROVIDER: resolver(lvl2)
```
AND 同级多关系 = 子查询交集; OR = 并集。

## 4b. 性能架构 (动工前必须定死 — 这是生死线)

### 矛盾
多级链跑在 **DataPermissionInterceptor** 里 → **每张受控表的每次查询都注入**该 WHERE;
分页还 **COUNT + 数据各跑一遍**。若把 §4 的 N 级递归子查询直接塞进每条数据查询 →
热路径上反复跑 N 趟 access_relations 遍历 → 不可接受。

### 核心解法: **解析与过滤分离** (ReBAC 标准性能模式)
关键洞察: **链的中间部分 (我 →…→ 可达实体集 S) 与"在查哪张表的数据"无关** —— 它只依赖
`(用户, 角色, access_relations 当前状态)`。所以:
1. **每请求只解析一次**: 算出可达集 S = {可达 org ids, 可达 place ids, 可达 user ids}, 缓存在请求上下文。
2. **每条数据查询只注入便宜的终端过滤**: `data.owner_org IN (S)` —— 不再是递归, 是一个 IN。
   N 级深度的代价**只付一次**, 不随查询次数/表数量放大。

### 三层缓存/物化 (读多写少, 物化必胜)
- **L1 请求内**: 同请求多次查询 (含 COUNT+数据) 复用同一 S。
- **L2 跨请求 (按 用户×角色)**: S 缓存, 失效挂在**现有关系变更事件** (access_relations 改 / RolePermissionsChanged / PermissionsRefreshed) —— access_relations 改动频率 << 查询频率, 命中率极高。
- **L3 物化闭包表 (深链/大体量)**: 预计算可达闭包 `reachable(user/role → entity_type, entity_id)`, 关系变更时增量刷新。链解析退化为**一次表查**, 零遍历。
  - 复用现成物化: **place.effective_org_unit_id** (A3 投影, "经场所→组织"免跑 belongs_to)、org path/subtree (子树免递归)。

### 注入形态按基数自适应 (避免巨 IN / 巨子查询)
- S 小 (≤ 阈值, 如 500) → **内联 ids** `IN (1,2,3,…)` (最快, 无子查询)。
- S 中 → 子查询 / 临时表。
- S = 全集 (如 root admin 可达所有 org) → **短路成"不限"** (复用现 ALL 短路, 不生成 IN)。
- S 空 → **deny (1=0)**, 不泄露。

### 硬约束
- **限深 ≤ 3** (中间跳), 编译期拒绝更深 → worst case 有界。
- **环防护**: 编译期检测 user--family_of-->user 类环, 限深兜底。
- **access_relations 索引**: `(subject_type, subject_id, relation)` + `(resource_type, resource_id, relation)` 必建。
- **终端 PROVIDER/RECORD**: resolver 自己负责性能 (本就是插件子查询, 已有 fail-closed)。

### 性能验收 (动工时)
- 真库压测: 大 access_relations (万级边) 下, 3 级链分页查询 P95 与现 1 跳对比, 物化命中后应**接近 1 跳**。
- 退化路径: 缓存未命中首次解析耗时上限。
- COUNT 复用 S 验证 (不重复解析)。

## 5. 插件扩展 (综合插件注册)
- **新链边**: 插件在 relation_types 注册新关系 (teaches/mentor_of 已是 EDU) → 自动可选为中间跳。
- **新终端锚点**: 插件在 resource_relations 注册 PROVIDER/RECORD/COLUMN (taught_by/inspected/reviewer 已是) → 自动可选为终端。
- **UI 数据驱动**: 中间跳下拉 = relation_types (按 fromType 过滤); 终端下拉 = resource_relations[当前资源]。**全部来自注册表, 零硬编码** —— 与现有 R1-R2.4 注册表无缝。

## 6. 缺口 / 必做
1. **PLACE 终端锚点**: 现零资源挂 place。设计支持 subject_type=PLACE 的 resource_relations (COLUMN owner_place_id / SUBJECT_GRAPH occupies)。即使现在没有, 引擎+UI 要能处理 (插件将来注册)。
2. **place→org 物化**: 已有 effective_org_unit_id 投影 (A3 PlaceOrgResolver) → "经场所到组织"链可直接用投影列, 不必跑 belongs_to 子查询 (性能优化)。

## 7. 代价 / 风险 (诚实)
1. **引擎重写**: ScopeEvaluator 从 1 跳 → 链编译器。S/S+ 级。
2. **性能**: 见 §4b —— **解析与过滤分离 + 三层缓存/物化** 是可行性前提, 必须从 P1 引擎第一天就内建, 不能后补。
3. **AND 交集 + 多级嵌套** SQL 复杂, 需大量测 + 金标准回归 (dpt_ct + R2.2 字节等价那套)。
4. **存储**: ChainScope 比现 grant 复杂; role_data_scopes.relation_grants JSON 可扩展为 chain, 或新表。
5. **环检测**: 多级链可能成环 (user--family_of-->user...) → 编译期限深防爆。

## 8. 分阶段 (若动工)
- P0 数据模型: ChainScope JSON schema + 注册表终端锚点校验 (终端∈resource_relations)。
- P1 引擎: 链编译器 (中间 access_relations 递归 + 终端 storage_kind 分派), 限深, 环防护。
  **+ §4b 性能架构同步内建**: 解析与过滤分离 (每请求解析一次 S) + L1/L2 缓存 (挂关系变更事件失效) + 基数自适应注入。物化闭包表 (L3) 可 P1 留接口、P2 视压测结果再上。
- P2 PLACE 锚点 + place→org 投影优化。
- P3 AND 交集。
- P4 UI: 链式下拉 (中间跳 +/− + 终端, 数据驱动) + 预览 + 模拟。
- P5 闸引擎(写路径) + 全面金标准回归。
每阶段: 真启动 + dpt_ct 金标准 + 字节等价 + 表快照零回归 (沿用 R2.2 套路)。

## 9. 判定
这是把现"简化版锚定"升级为**完整关系图遍历 (Zanzibar userset 重写)**。ROI 取决于真实业务是否需要"经场所/经上级/多级"这类跨实体链。**建议先确认有真实用例, 再投 P0-P1**; 否则当前 1 跳 (892934d9) + 注册表已覆盖绝大多数场景。
