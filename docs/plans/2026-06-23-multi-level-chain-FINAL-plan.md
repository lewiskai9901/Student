# 多级关系链数据范围 — 最终方案 (2026-06-23)

> 决策已定 (用户): **AND 交集 v1 必须** · **读+写一起** · **直接开 P0**。
> 这是把现"1 跳简化锚定"升级为**完整关系图遍历 (Zanzibar userset 重写)**, S/S+ 级工程。
> 综合: 三大主体 (user/org_unit/place) · 现有关系系统 (relation_types/access_relations) ⊕
> 插件注册锚点 (resource_relations) · 终端∈注册表 · 性能解析/过滤分离。
> 前序设计: [多级链综合设计](2026-06-23-multi-level-relation-chain-design.md) (全景/图谱/缺口/性能 §4b)。

---

## 0. 标准约束 (全程)
- **无数据兼容** (用户强确认): 系统无真实数据 → 破坏性改 schema/JSON 形状, 不迁移不回填; 代码与 DB 一致即可。
- **每阶段验收闸**: 真启动 0 ERROR + **dpt_ct 金标准** (班主任 BY_CLASS → 2 本班生) + 字节等价 (R2.2 那套) + 表快照零回归 + (引擎阶段) 真库 SQL 注入验证。
- 中文优先; 仅代码/SQL/类名原文。

## 1. 概念模型

### 1.1 三大主体 + 关系图 (链的"边", 来自 relation_types)
`user` / `org_unit` / `place`。边 (实例存 access_relations):
- user→org: member/admin/deputy/responsible_for/viewer/watches (+teaches EDU)
- user→place: admin/manages/occupies/responsible_for/viewer
- user→user: supervisor_of/delegated_to/guardian_of/family_of/responsible_for/… (+mentor_of EDU)
- place→org: belongs_to

### 1.2 终端锚点 (数据"怎么挂主体", 来自 resource_relations 注册表)
storage_kind ∈ {COLUMN(列), SUBJECT_GRAPH(成员图), PROVIDER(插件resolver), RECORD_RELATION(record_relations)}。
现状: 几乎全资源 = creator(→USER) + owner_org(→ORG); **零 PLACE 锚点 (缺口, 见 §6)**。

### 1.3 链 (ChainScope) — 核心数据结构
```
ScopeSpec.chains : [Chain]                 // 顶层多链 = OR (满足任一链)
Chain = {
  hops     : [Hop],                        // 中间跳 0..N (走 access_relations 图); 起点恒=当前用户
  terminal : Terminal,                     // 数据怎么落到链末实体 (∈ 注册表)
  typeFilter? : [typeCode]                 // 轴③ 类型过滤 (已有)
}
Hop = {
  relations : [relCode], combine : AND|OR, // 同级多关系: 交集/并集 (v1 都支持)
  toType    : user|org_unit|place,         // 这一跳到达的实体类型
  subtree?  : bool                         // org 跳含下级
}
Terminal = {
  anchorRelations : [relCode], combine : AND|OR  // 每个 ∈ resource_relations[resource]
}
```
- **1 跳退化**: 现"我 member 的组织里的数据" = `{hops:[{[member],OR,org}], terminal:{[owner_org]}}`。旧 grant 是本模型子集。
- **用户原例**: 用户管理/角色A = `{hops:[{[responsible_for,admin],AND,org}], terminal:{[member,responsible_for],AND}, typeFilter:[STUDENT]}`。

## 2. 存储
`role_data_scopes.relation_grants` JSON **重定义**为 `List<Chain>` (无数据兼容, 直接改形状)。
1 跳旧形状作废。`subject_rel_include/exclude`(轴②) 暂保留透传 (与 chain 正交)。

## 3. 引擎: 链编译器 (取代现 ScopeEvaluator 1 跳)

### 3.1 编译 (READ — SQL WHERE 注入)
```
compileChain(chain, resource, ctx):
  cur = { (user, :me) }                              // 起点
  for hop in chain.hops:                             // 中间跳: 逐层走 access_relations
    cur = traverse(cur, hop)                          // AND=每关系子集交集; OR=并集
  S = cur                                            // 可达实体集 (org/place/user ids)
  return terminalPredicate(chain.terminal, S, resource)  // 数据↔S, 按 storage_kind
compileSpec: OR( compileChain(c) for c in chains )
```
- **traverse(set, hop)**: `SELECT resource_id FROM access_relations WHERE subject IN set AND relation IN hop.relations …`; AND = 多关系结果 INTERSECT; OR = UNION; org+subtree = 子树展开。
- **terminalPredicate**: 每 anchorRelation 按 storage_kind →
  - COLUMN: `data.{col} IN (S)`
  - SUBJECT_GRAPH: `data.id IN (成员子查询 over S)`
  - PROVIDER: `resolver(S/ctx)`
  - RECORD_RELATION: `data.id IN (record_relations 子查询)`
  - AND = 多锚点谓词 AND 叠加; OR = OR。

### 3.2 写路径 (闸3 — INSERT/UPDATE/DELETE)
- **UPDATE/DELETE**: 复用 READ 的 WHERE 注入 (injectFilterCondition, 已有机制) → 同一 compileSpec 谓词。
- **INSERT**: 前置校验 — 新行的终端锚点值 (owner_org/owner_place/creator) 必须落在 S 内 (enforceInsertAuthz 扩展为链版); fail-safe。
- 读写共用 **同一 compileSpec** → 语义一致, 单一真相。

## 4. 性能架构 (P1 第一天内建, 非后补 — 见设计 §4b)
- **解析与过滤分离**: 链中间 (我→可达集 S) 与查哪张表无关 → **每请求解析一次 S**, 每条数据查询只注入便宜终端过滤。N 级深度代价只付一次。
- **三层缓存**: L1 请求内 (COUNT+数据复用) / L2 按 用户×角色 (失效挂关系变更事件) / L3 物化闭包表 (深链/大体量; 复用 place.effective_org_unit_id + org 子树投影)。
- **基数自适应注入**: 小→内联 ids / 中→子查询 / 全集→短路"不限" / 空→deny。
- **硬约束**: 限深 ≤3 + 环检测 + access_relations 索引 `(subject_type,subject_id,relation)` & `(resource_type,resource_id,relation)`。
- **验收**: 万级边压测, 3 级链分页 P95 物化命中后接近 1 跳。

## 5. 插件扩展 (全数据驱动, 与现注册表无缝)
- 新链边 = 插件在 relation_types 注册 (teaches/mentor_of 已是) → 自动可选中间跳。
- 新终端 = 插件在 resource_relations 注册 PROVIDER/RECORD/COLUMN (taught_by/inspected/reviewer 已是) → 自动可选终端。
- UI 中间跳下拉 ← relation_types(按 fromType); 终端下拉 ← resource_relations[资源]。零硬编码。

## 6. 三主体+场所 缺口补齐
- **PLACE 终端锚点**: resource_relations 支持 subject_type=PLACE (COLUMN owner_place_id / SUBJECT_GRAPH occupies)。引擎/UI 处理 PLACE 终端 (插件将来注册)。
- **place→org 投影**: "经场所→组织"跳直接用 place.effective_org_unit_id (A3 投影), 免跑 belongs_to 子查询。

## 7. UI: 链式编辑器 (data-permissions, 取代纯关系一跳)
```
某资源 显示/操作范围 = 满足任一链:
  链1: 我 ─[成员∨管理 ▾]→ 组织(☑含下级) ─[占用 ▾]→ 场所 ⟶ 终端[owner_org ∧ 复核] · 类型[学生]   [×]
  [+ 加一跳]  [+ 加一条链]   [AND/OR 切换]
```
中间跳/终端/类型全数据驱动 (注册表); 常驻预览 (链→自然语言) + 模拟用户。

## 8. 分阶段实施 (每阶段独立提交 + 验收闸)
- **P0 数据模型 + 注册表终端校验** (本次开始, 安全/加性, 不碰引擎):
  Chain/Hop/Terminal 领域模型 + JSON 编解码 + `ChainValidator` (终端∈resource_relations / 限深≤3 / 环检测) + 单测。**零金标准风险**。
- **P1 READ 引擎**: 链编译器 (中间遍历 + 终端 storage_kind + AND/OR) + §4 性能架构 (解析/过滤分离 + L1/L2 缓存 + 基数自适应) + 接入 DataPermissionInterceptor (SELECT)。退化 1 跳金标准必过。
- **P2 PLACE 终端 + place→org 投影优化**。
- **P3 写路径 (闸3)**: UPDATE/DELETE WHERE 注入 + INSERT 前置校验, 复用 compileSpec。
- **P4 UI 链式编辑器** (数据驱动 + 预览 + 模拟) + 端到端。
- **P5 物化闭包表 (L3) + 压测 + 全面回归**。
- 每阶段: 真启动 + dpt_ct 金标准 + 字节等价 + 表快照零回归 + (引擎) 真库 SQL 验证。

## 9. 风险
| 风险 | 缓解 |
|---|---|
| 引擎重写大 | 退化 1 跳金标准全程兜底; 分阶段独立提交可回退 |
| 性能 | §4 解析/过滤分离 + 物化, P1 内建; 压测 gate |
| AND 交集 SQL 复杂 | 充分单测 + 真库验证; 限深≤3 收敛 |
| 环/深度爆炸 | 编译期环检测 + 限深 |
| 写路径漏判 | 读写共用 compileSpec; INSERT fail-safe |

## 10. P0 立即开工 (本次)
产出: `domain/access/model/chain/` (Chain/Hop/Terminal + ScopeChainSpec) + JSON 编解码 + `ChainValidator`
(终端∈ResourceRelationRegistry / 限深 / 环) + 单测 (含用户原例 + 1 跳退化 + 非法终端拒绝)。不接引擎, 零回归风险。

## 11. 实施完成状态 (2026-06-24) — 全部交付
P0→P4+P2 + 收尾 #1-#6 全部完成 (每步 dpt_ct 金标准 + shadow + 全量回归):
- **P0** `696f7408` 模型+Validator / **P1-S1** `6652144b` 跳解析 / **S2a** `c71efb98` 编译器 /
  **S2b** `d20eb244` 接 live (shadow 抓修 user_id 列/subject NPE) / **S2c** `478c88ee` AND 交集 /
  **P3** `d1ba9b98` 写路径 / **P4** `b609d076` 链式 UI / **P2** `2797635b` 场所投影
- **#1** `8dff7245` ChainValidator 接保存 (非法链 HTTP 400 + 提示)
- **#2** `a2ac6466` 终端 membership 可选 (属于/负责)
- **#3** `39159467` 终端 membership AND (属于且负责 — 原例字面)
- **#4** `a1586475` PLACE 终端 (成员图 resource_type=末跳类型, 场所占用)
- **#5** 双路径有意共存 (链=规范模型, 1 跳=已证等价优化路径, 2b shadow 实证; 不做赌金标准的 composeGrant 重写)
- **#6** access_relations idx_chain_hop 索引 (链遍历性能; L3 物化闭包作 future-when-volume, 零数据量不预建)

**诚实记录**: #2-#4/#6 为平台超前能力 (场所子系统空 / 无非 member 数据↔组织 / 零数据量), 按用户"平台超前建设"
意愿完成并以 seed 数据 shadow 验证; #5 全量统一(重写退役 composeGrant) 经分析为负 ROI(零功能收益+赌金标准),
改以"有意共存+等价已证"收口。多级关系链特性端到端完整可用。
