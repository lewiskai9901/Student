# 统一数据归属架构 —— 终版设计（修正版）

> 状态：终版设计稿（2026-06-18）。**取代并删除**同主题两份草稿：`2026-06-17-data-anchoring-unification-design.md`（纯列版）、`2026-06-17-unified-anchoring-registry-design.md`（注册版，存储落点有结构性错误）。
>
> 本稿是在对真实代码做三路审计后的修正版：①`access_relations` 经查证是**纯主体↔主体图**（生成列把主体语义焊死在 schema，传递 BFS 只对主体成立）；②全系统恰好 **4 个主体型资源**（user/org_unit/place/student）的 org 归属本就不在列里；③多锚点 OR 今天**唯一**真实消费者是检查"受检面"，靠 `MyReceivedInspectionsController` 自查 SQL 绕开权限层。
>
> 设计目标取向：**不计重构成本，只取最优**。承接 [[project-composable-data-scope-m1]]、[[project-authorization-hardening]]、[[project-data-permission-ui-redesign]]。

---

## 0. 一句话

**一个逻辑模型（"资源 = 一组命名关系"），两个物理存储按粒度分工，四个主体闭合关系图，引擎数据驱动并能自解释。**

- 主体↔主体关系 → `access_relations`（保持纯净，含传递 BFS，**不碰**）。
- 记录↔主体关系 → **新建一等 `record_relations` 表**（扁平、无传递，专收记录级关系）。
- **`resource_kind`（主体 / 普通记录）先决存储**；普通记录再按基数（单值→列、多值→`record_relations`）。
- 多值 / 图派生的可见性 → **物化倒排索引**（Leopard 做法，本版直接建，不留作逃生口）。
- 注册表 + 一种词汇（关系）贯穿全栈，引擎对每条授权可 `explain`。

---

## 1. 出发点：系统已经对的别动，缺的精准补

审计结论先行——**当前系统不是混乱，是一个正确的两层结构，只缺三件东西**：

**已经对（必须保留）：**
- `access_relations` 是干净的**三主体图**：22 个关系全部 `{user,org_unit,place}` 互连；写入前过 `relation_types` 校验；`membership_lock_key` / `place_belongs_lock_key` / `place_responsible_lock_key` 三个生成列把主体语义**焊死在 schema**。这张图支撑 Zanzibar 6-API + 传递 BFS。
- 普通记录用**列**锚（`org_unit_id` / `created_by` / `target_id`），`ScopeEvaluator` 三互斥路径过滤。
- 主体型资源（user/org_unit/place/student）的 org 归属**不在列里**：org_unit 走 `parent_id`，place 走 `belongs_to`，user/student 走 `member`（`user_student.org_unit_id` 已 V20260531_4 物理删除）。

**缺的（本设计补）：**
1. **统一声明层**：32 资源里多数 `data_resources` 锚点字段为 NULL，靠默认兜底——声明不齐、易漂移。
2. **多锚点能力**：检查记录语义上要"我开的 ∪ 本组织开的 ∪ 针对本组织的"三路 OR，引擎不支持，被迫用旁路 controller 自查。
3. **记录级 M:N 的正规落点**：reviewer / 多被检查对象这类"记录挂多个主体"，今天无处可放。

> **关键纠偏**：缺的 3 件都**不需要**把记录灌进 `access_relations`。把记录级 tuple 塞进主体图 = 污染唯一干净的图 + 让传递 BFS 遍历到记录节点 + 偷偷退回被否决的"全 tuple"模式。**记录级关系另立一等表。**

---

## 2. 核心模型：一个逻辑模型，两个物理存储

系统里每条数据只回答一个问题：**"我和这条数据是什么关系？"** 知道关系 + 角色授予 = 决定可见 / 可写。

```
                         逻辑模型：资源 = 一组命名关系
                                    │
              ┌─────────────────────┴─────────────────────┐
        主体↔主体关系                                 记录↔主体关系
   (member/admin/belongs_to/...)              (creator/owner_org/inspector/reviewer/...)
              │                                           │
       access_relations                            ┌──────┴──────┐
     (纯净 / 有传递 BFS / 不碰)                  单值→列        多值→record_relations
                                            (org_unit_id/    (扁平 / 无传递 / 一等表)
                                             created_by/...)
                                    │
                          ┌─────────┴─────────┐
                     读：SQL 拦截器        写：闸3 (M2)
                          └─────────┬─────────┘
                          物化倒排索引加速图派生路径
                                    │
                          全栈一种词汇（关系） + explain()
```

**两个物理表是有意分立，不是凑合**——它们语义本就不同：

| | `access_relations` | `record_relations`（新） |
|---|---|---|
| 连接 | 主体 ↔ 主体 | 业务记录 ↔ 主体 |
| 传递性 | 有（admin→members 等，BFS） | **无**（记录关系是扁平事实） |
| 约束 | 主体专属生成列 / 锁键 | 记录维度唯一键 |
| 引擎 | Zanzibar check/expand/lookup | 单跳子查询 / 物化 |

---

## 3. 资源分两类：`resource_kind` 是第一根轴

存储**首先**由"记录本身是不是主体"决定，**不是**由基数决定（基数决存储对 student 就错——它单值却在 `access_relations`）。

| `resource_kind` | 判据 | 例子（审计实数） | 归属如何存 |
|---|---|---|---|
| `SUBJECT` | 记录就是某个主体 | user / org_unit / place / student（=user） | **已在 `access_relations`**，引擎走主体图 join，**不注册列锚** |
| `PLAIN` | 记录不是主体 | 检查记录 / 考勤 / 成绩 / 申诉…（29 个） | 列（单值）/ `record_relations`（多值） |

`resource_kind` 落在 `data_resources` 表（资源级属性），引擎据此分派。**主体型资源的 org 归属永远不当"关系→列"注册**——它本就是主体图里的一条边。

---

## 4. 存储推导（按关系声明 + 一致性校验；R1 实现期修正）

> **R1 修正**：旧稿"`resource_kind` × `cardinality` → `storage` 的总函数 derive"是**过简**——
> 主体型资源的 `creator` 关系仍是普通 `created_by` 列，并非 `SUBJECT_GRAPH`。同一资源的不同关系存储各异，
> 不存在一个吃 `(resource_kind, cardinality)` 吐 `storage` 的全函数。故存储**按关系声明**。

每条关系用工厂声明存储，构造期（`ResourceRelationDef`）校验**基数↔存储自洽**：

| `storage_kind` | 基数约束 | 必填 | 适用 |
|---|---|---|---|
| `COLUMN` | 必须 SINGLE（列装不下集合） | `column_name` | 普通记录的单值关系（creator / owner_org / 单一被检查） |
| `RECORD_RELATION` | 必须 MULTI | `ar_relation` | 普通记录的多值关系（reviewer / 多被检查 / 分享） |
| `SUBJECT_GRAPH` | 任意 | `ar_relation` | 主体型资源的关系（student.owner_org = member）；走 access_relations |
| `MATERIALIZED` | — | — | 不可声明（热路径叠加，§11） |

**`resource_kind` 仍是第一根轴，但作用于资源级守护而非每关系推导**：「**主体型资源的组织归属关系必须 `SUBJECT_GRAPH`，不得声明为 `COLUMN`**」——防重建已删的 `user_student.org_unit_id`。该交叉校验需同时知道资源 kind 与其关系，属**资源级构建期守护（R6/ §10）**，非 Def 内可判。

"分支"收敛进引擎一处 `switch(storage_kind)` + 注册表元数据，业务代码零 if（是"分支收敛进引擎"，非"零分支"——诚实表述）。

---

## 5. 数据库设计

### 5.1 `access_relations` —— 保持，不动

引述其约束以明确边界：主体专属生成列（`membership_lock_key` 等）+ `relation_types` 校验保证它只装主体边。**本设计不向其写入任何记录级行。**

### 5.2 新增 `record_relations` —— 记录↔主体一等表

```sql
CREATE TABLE `record_relations` (
  `id`            bigint NOT NULL AUTO_INCREMENT,
  `resource_code` varchar(50) NOT NULL COMMENT '哪个资源, FK -> data_resources.resource_code',
  `record_id`     bigint      NOT NULL COMMENT '业务记录 id (非主体)',
  `relation_code` varchar(50) NOT NULL COMMENT '关系码: reviewer / inspected / shared_with ...',
  `subject_type`  varchar(20) NOT NULL COMMENT '主体类型: USER/ORG_UNIT/PLACE/ASSET',
  `subject_id`    bigint      NOT NULL COMMENT '主体 id',
  `access_level`  varchar(20) NOT NULL DEFAULT 'READ_ONLY',
  `valid_from`    datetime DEFAULT CURRENT_TIMESTAMP,
  `valid_to`      datetime DEFAULT NULL,
  `metadata`      json DEFAULT NULL,
  `created_by`    bigint DEFAULT NULL,
  `created_at`    datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       tinyint(1) NOT NULL DEFAULT 0,
  `tenant_id`     bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_relation` (`resource_code`,`record_id`,`relation_code`,`subject_type`,`subject_id`,`tenant_id`,`deleted`),
  KEY `idx_by_subject` (`subject_type`,`subject_id`,`relation_code`,`resource_code`,`deleted`),  -- 主体→可见记录
  KEY `idx_by_record`  (`resource_code`,`record_id`,`relation_code`,`deleted`)                   -- 记录→相关主体
) COMMENT='记录↔主体关系表 (扁平, 无传递; 与 access_relations 主体图分立)';
```

与 `access_relations` 的差异即设计要点：**无主体锁键生成列、无传递 BFS、`record_id` 是业务记录**。

### 5.3 注册表 `resource_relations` —— 词汇 + 存储映射唯一真相

```sql
CREATE TABLE `resource_relations` (
  `id`             bigint NOT NULL AUTO_INCREMENT,
  `resource_code`  varchar(50) NOT NULL,
  `relation_code`  varchar(50) NOT NULL COMMENT 'creator/owner_org/inspector/inspected/reviewer',
  `relation_name`  varchar(50) NOT NULL COMMENT '人话显示名 (UI)',
  `subject_type`   varchar(20) NOT NULL COMMENT 'USER/ORG_UNIT/PLACE/ASSET/ANY(多态)',
  `cardinality`    enum('SINGLE','MULTI') NOT NULL,
  `storage_kind`   enum('SUBJECT_GRAPH','COLUMN','RECORD_RELATION','MATERIALIZED') NOT NULL
                   COMMENT '由 resource_kind × cardinality 推导, 登记校验',
  `column_name`    varchar(50) DEFAULT NULL COMMENT 'COLUMN: 业务表列名',
  `type_column`    varchar(50) DEFAULT NULL COMMENT '多态主体的类型列, 配 column_name',
  `ar_relation`    varchar(30) DEFAULT NULL COMMENT 'SUBJECT_GRAPH/RECORD_RELATION: relation 取值',
  `auto_fill`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '写入自动填(creator/owner_org=1)',
  `grants_by_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '无显式授予时默认参与可见性',
  `industry`       varchar(20) DEFAULT NULL,
  `enabled`        tinyint(1) NOT NULL DEFAULT 1,
  `tenant_id`      bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_relation` (`resource_code`,`relation_code`,`tenant_id`),
  KEY `idx_resource` (`resource_code`,`enabled`)
) COMMENT='资源关系注册表 —— 统一锚定模型词汇/存储映射唯一真相';
```

`data_resources` 加一列 `resource_kind enum('SUBJECT','PLAIN')`；旧单锚点字段（`org_unit_field`/`creator_field`/`access_resource_type`/`type_field`）**彻底删列**，内容迁进 `resource_relations`。

### 5.4 ASSET 升为第四主体（闭合关系图）

审计发现 `inspected` 多态取值含 **ASSET**，而 ASSET 今天不是主体（只在打分里存在）。为让"关系恒指向主体"无漏：

- `relation_types` 允许 `asset` 作 subject/resource 类型；`access_relations` / `record_relations` 的 `subject_type` 接受 `asset`。
- assets 表登记为受承认的主体源（已有 `AssetApplicationService` 数据基础）。
- 收益：`inspected → ASSET` 不再是模型外特例，四主体（user/org_unit/place/asset）+ 组织树构成闭合图。

### 5.5 物化倒排索引（§11 详述）

```sql
CREATE TABLE `subject_visible_records` (
  `subject_type`  varchar(20) NOT NULL,
  `subject_id`    bigint NOT NULL,
  `resource_code` varchar(50) NOT NULL,
  `record_id`     bigint NOT NULL,
  `via_relation`  varchar(50) NOT NULL COMMENT '经由哪条关系可见 (供 explain)',
  `tenant_id`     bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`subject_type`,`subject_id`,`resource_code`,`record_id`,`via_relation`,`tenant_id`),
  KEY `idx_list` (`subject_type`,`subject_id`,`resource_code`)
) COMMENT='主体→可见记录 倒排索引 (Leopard); 仅物化图派生/多值路径, 列锚不入';
```

---

## 6. 写入：三条落库路径

复用已验证的 `CompositeMetaObjectHandler` + `FieldFillerStrategy`（inspection 填充器是样板）：

- **`COLUMN` + auto_fill=1**（creator/owner_org）：泛化 `RelationColumnFiller` 落库自动填（creator←当前用户，owner_org←上游反查 / 当前用户组织）。业务零感知。UPDATE 阶段不动归属列（防越权改边界，沿用现有约束）。
- **`COLUMN` + auto_fill=0**（inspected[单]）：业务层显式 set（查谁是业务输入）。
- **`RECORD_RELATION`**（reviewer / inspected[多]）：建立关系时应用服务写 `record_relations` tuple（按注册表 `relation_code` + `subject_type`）。
- **`SUBJECT_GRAPH`**：主体↔主体，仍由 MembershipResolver / PlaceOrgResolver 等写 `access_relations`，本层不介入。

写后发领域事件 → 物化索引增量维护（§11）。

---

## 7. 引擎：数据驱动 + 多锚点 OR + 可解释

`ScopeEvaluator` 升级为纯数据驱动：输入 = 角色对资源的关系授予（§8）+ 注册表元数据；输出 = WHERE。对每条被授予关系按 `storage_kind` 生成子条件：

```
COLUMN  单值 user(inspector):    alias.column = :me
COLUMN  单值 org (owner_org):    alias.column IN (:myOrgSet)
COLUMN  多态(inspected[单]):     (alias.type_column = 'ORG_UNIT' AND alias.column IN (:myOrgSet))
RECORD_RELATION 多值(reviewer):  id IN (SELECT record_id FROM record_relations
                                        WHERE resource_code=:rc AND relation_code=:rel
                                          AND subject_type=:st AND subject_id=:me
                                          AND deleted=0 AND (valid_to IS NULL OR valid_to>NOW()))
SUBJECT_GRAPH (主体型资源):      走主体图 (membership/belongs_to) —— 复用现有 viaMembership 路径
MATERIALIZED (热路径):           id IN (SELECT record_id FROM subject_visible_records
                                        WHERE subject_type=:st AND subject_id=:me AND resource_code=:rc)
```

所有子条件 **OR 叠加** 成最终 WHERE。`:myOrgSet` 由组织锚点解析（含子树，复用 M1 `resolveOrgSet`）。

**可见性来源选择**：列锚直接进 SQL（列本身就是索引，**不物化**——物化它只会用更弱一致性复制一个已有的快列）；`RECORD_RELATION` 与传递图派生路径**优先走物化索引**，强一致需求时回退实时子查询（双路并存）。

**可解释（10 分特征）**：每个子条件对应一条具名关系；`explain(recordId, userId)` 返回命中的关系码（"你能看到记录 5,因为你是它的 inspector"）。读 SQL 与 explain 共用同一份 `resource_relations` 元数据编译,保证两者口径一致（§10 守护）。

---

## 8. 权限配置：按关系授予（重构 `role_data_scopes`）

授予语义 = **"这个角色，对这个资源，认可哪些关系，每条关系的 subject 范围是什么"**：

```sql
-- role_data_scopes 保留 id/role_id/resource_code/apply_to/type_filter/tenant, 新增:
`relation_grants` json COMMENT '关系授予数组'
-- 例(检查主任, READ):
[
  { "relation": "inspector", "subject": "SELF" },
  { "relation": "owner_org", "subject": "MY_ORG", "subtree": true },
  { "relation": "inspected", "subject": "MY_ORG", "subtree": true }
]
```

`subject` 取值（统一解析器）：`SELF`（user 型关系 subject=我）/ `MY_ORG`(+subtree)（org 型 subject∈我的组织树）/ `RELATION:<code>`（经该关系到达的组织，复用 MANAGED_ORGS）/ `CUSTOM:<ids>` / `ALL`。

**M1 三轴归并**：轴①组织锚点 = org 型关系授予的 subject；轴②结果关系过滤 = 选/排具体关系；轴③类型过滤 `type_filter` = 正交保留。三轴没消失，被"关系授予"统一表达。M1 旧 `org_anchor` 枚举降级为生成 `relation_grants` 的预设糖（迁移映射见 §15-R3，逐枚举值列表）。

---

## 9. 可见性语义（钉死，无歧义）

1. 角色对资源可见集 = `relation_grants` 每条关系子条件的 **OR**。
2. **无任何 grant** → 取注册表 `grants_by_default=1` 的关系（一般 `owner_org`=本组织）；若无默认关系 → **拒绝（看不到）**，**绝不 fail-open**。
3. `relation_grants` 引用的关系**必须在 `resource_relations` 存在且 enabled**（构建期 + 运行期双校验），否则拒绝。
4. `apply_to` 读写分离：同资源可有 READ / WRITE 两套 `relation_grants`（读宽写窄，各一行）。
5. 多态关系（inspected）授 `MY_ORG` 仅匹配 `type_column='ORG_UNIT'` 行；授 `SELF` 仅匹配 `type_column='USER' AND column=:me`。
6. `type_filter`（轴③）仅 AND-约束**带类型属性的**子条件，不波及 `inspector:SELF` 这类纯主体关系（避免"我亲自检查但类型不符"被误隐）。

---

## 10. 读写对称 + 守护

**读写对称**——同一份 `relation_grants` + 注册表，三张执行面共用一个 `evaluator`：
- 读：`DataPermissionInterceptor` → `toSqlCondition()` → AND 进 SELECT。
- 写：闸3 `WithinScopeRule` → `evaluator.matches(record, WRITE)`（M2）。
- 模拟/预览：UI 输入用户 → 跑 `evaluator` 返回可见计数/样本 + `explain`。

**构建期守护**（ArchUnit / 启动校验，仿 `NoIndustryTableInCoreTest`）：
- 每个 `column_name` 在对应业务表/PO 真实存在（防漂移）。
- `resource_kind × cardinality → storage_kind` 三者一致。
- 可授权资源至少注册一条关系（防"裸表无锚点静默全可见"）。
- `explain` 路径与 SQL 路径由同一编译器产出（防读/解释口径漂移）。

**运行期守护**：`relation_grants` 引用未注册/停用关系 → 拒绝 + 告警。
**核心零侵入**：注册项经 `PluginPackage.contribute()`，`NoIndustryTypeLiteralInCoreTest` 继续守 core 无行业关系字面量。

---

## 11. 物化倒排索引（本版直接建，不是逃生口）

不计成本取最优 → 把"主体→可见记录"图派生路径**主动物化**为 `subject_visible_records`（§5.5），即 Zanzibar Leopard 倒排索引。

- **覆盖范围**：仅 `RECORD_RELATION` 与传递主体路径（admin→下级组织成员等）。**列锚不入索引**（列本身即索引，物化它是负收益）。
- **维护**（事件驱动增量）：记录建/锚点变 / `record_relations` 增删 / 组织成员变 / 组织树搬迁 → 对应领域事件触发索引 upsert/失效。复用现有 `OutboxProcessor` + `AccessEventHandler` 事件机制。
- **一致性**：索引最终一致；强一致读回退实时子查询（双路并存，注册表标 `MATERIALIZED` 即优先走索引）。
- **收益**：多值/列表路径从"OR 多个子查询 + 跨列 index-merge"塌缩为**单次索引等值查找**，规模线性可控。

> 这是 §7 里"列锚直接、图派生走物化"区分的兑现——也是该设计敢称 10 分的规模底气。

---

## 12. 插件接入（零碰核心表）

插件开发新模块（如医疗病历）：
1. **自己的表加关系字段**（org_unit_id / created_by / attending_doctor_id…）。
2. **`contribute()` 登记**：`DataResourceDef`（含 `resource_kind`）+ 一组 `ResourceRelationDef`（关系→列/记录关系，含基数）。
3. 多值关系（主治医生管多病人）→ 登记 `cardinality=MULTI` → 自动落 `record_relations` + 进物化索引，**无需写 DataScopeResolver、无需改任何核心表**。

→ 插件资源与核心资源**完全同构**：同注册、同引擎、同 UI、同 explain。`DataScopeResolver` SPI 退役为旧插件过渡（新插件一律走注册）。

---

## 13. 边界：为什么不做完整 Zanzibar（主见）

业界天花板是完整 Google Zanzibar（全关系 tuple + Leopard + 一致性令牌 zookie）。**即便不计成本本设计也不采用**，理由是**正确性 / 适配**而非省钱：

- **1:1 归属"放在行上的列"是比 tuple 更强的不变量**：值在行上、不会漂移、与业务写事务一致；tuple 化反而引入写同步 + 可漂移的弱一致。列不是"便宜的妥协",是"更强的真相"。
- **一致性令牌（zookie）那套机器在单库单租户下零收益**：它解决的是跨服务分布式快照一致，本系统单 MySQL 不存在该问题。
- 故"最好"在本系统 = **混合修正版**（主体图 + 记录关系表 + 列 + 物化索引），**不是把一切塞进图**。把一切塞进图是退步，不是满分。

---

## 14. 端到端（检查记录）

1. **写**：A 提交检查 → `RelationColumnFiller` 填 `created_by=A` + `owner_org`(上游/A 组织)；`inspected[单]` 业务 set；指派 reviewer → 写 `record_relations`；事件 → 物化索引。
2. **声明**：`inspection_record` `resource_kind=PLAIN`，注册 {creator:COLUMN, owner_org:COLUMN, inspector:COLUMN, inspected:COLUMN多态, reviewer:RECORD_RELATION}。
3. **读**：检查主任查列表 → 拦截器取其 `relation_grants` → evaluator 拼 `created_by=我 OR owner_org IN(我组织树) OR (inspected.type='ORG_UNIT' AND ... IN(我组织树))` → 看到三类并集；reviewer 经物化索引命中指派给自己的。
4. **解释**：`explain(record, 主任)` 返回命中关系。
5. **受检面收编**：`MyReceivedInspectionsController` 的自查 SQL 退役,改为 `inspected:MY_ORG` 一条授予走统一引擎。

---

## 15. 落地拆解（按**正确性**排序，非成本；每步真启动 + 表快照零回归）

> 不计成本不等于不排序——顺序是为"先等价后扩展"的安全网（M1 经验），不是省工。

- **R1 注册表 + resource_kind + 元数据**：建 `resource_relations`；`data_resources` 加 `resource_kind`、删旧单锚字段；`ResourceRelationDef` + `contribute()` 接入；core 32 资源内容迁成注册行（标对 SUBJECT/PLAIN）。
- **R2 引擎数据驱动化（最高风险）**：`ScopeEvaluator` 改读注册表；先保证现有单锚点**字节等价**（仿 M1 等价评审），再开多关系 OR。`ResourceScopeMeta` 扩成"关系集"。
- **R3 权限配置重构**：`role_data_scopes` → `relation_grants`；subject-set 解析器；**M1 旧 `org_anchor` 枚举值 → relation_grants 的逐值映射表**（最易错,单列清单评审）；UI 改"按关系勾选 + 预览"。
- **R4 记录关系表 + 写入泛化**：建 `record_relations`；`RelationColumnFiller`（列自动填）；多值关系写 tuple；retire inspection 专用 filler。
- **R5 ASSET 升主体**：`relation_types` / 两关系表接受 `asset`；闭合 `inspected` 多态。
- **R6 物化倒排索引**：建 `subject_visible_records`；事件驱动维护；引擎 `MATERIALIZED` 分支 + 实时回退；初次全量重建任务。
- **R7 可解释 + 守护**：`evaluator.explain()`；构建期（列存在/基数一致/裸表无锚/读-解释同源）+ 运行期（未注册关系拒绝）。
- **R8 写鉴权（=M2）+ 受检面收编 + 插件样板**：闸3 `matches()` 读宽写窄；退役 `MyReceivedInspectionsController` 自查 SQL；HEALTH 病历按注册零碰核心验证。

**顺序**：R1→R2（先等价后扩展）→R3→R4→R5→R6→R7→R8。每步独立 commit。

---

## 16. 验收（真库 + 可解释）

以**检查记录**为黄金样本，真库注入 + API 往返：
1. 检查主任 = {inspector:SELF, owner_org:MY_ORG+树, inspected:MY_ORG+树} → "我做的 ∪ 本组织开的 ∪ 针对本组织的",逐行核对。
2. 指派 reviewer（MULTI）→ 复核员只看到指派给自己的,验证 `record_relations` + 物化索引路径。
3. 一次检查多个被检查组织（inspected MULTI）→ M:N 落 `record_relations` 且过滤正确。
4. 主体型资源（student）配 `MY_ORG` → 走主体图,**确认未重建 `org_unit_id` 列**（防 §3 回归）。
5. `explain(record, user)` 对每条可见记录返回正确命中关系。
6. WRITE grants 收窄 → 同用户可读不可写某记录,证读写对称。
7. 关物化索引强一致回退路径与索引路径**结果一致**(等价验证)。
8. 单测金线 + 真启动 GREEN 无 Unknown column + 表快照零回归 + `access_relations` **未被写入任何记录级行**(粒度纯净守护)。

---

## 17. 风险 / 边界

- **统一是"逻辑模型 + 词汇 + 开发体验"统一，物理三存储（列 / record_relations / access_relations）+ 物化索引按粒度分工** —— 有意最优,非缺陷。
- **`access_relations` 粒度纯净**是硬约束（守护 + 验收第 8 条兜底）。
- **物化索引一致性**：最终一致 + 强一致回退双路；维护逻辑随事件覆盖面增长需守护漏维护。
- **R2/R3 是大改**：等价评审 + 真库验收不可省（M1 经验）。
- **数据兼容**：测试库无存量,直接破坏性改 schema + 删旧列（符合项目原则）,迁移文件仅供开发库追平。
