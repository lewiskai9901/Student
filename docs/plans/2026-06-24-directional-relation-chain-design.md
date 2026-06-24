# 有向关系链 + 可视化配置 设计

> 日期: 2026-06-24 · 分支: feat/unified-data-anchoring · 状态: 设计已确认, 待实施
> 承接: 多级关系链审计修复 (#1 subtree / #3 place→org / #2 creator+hops) 之后的能力增强。

## 背景与动机

数据权限"多级关系链"目前只能**正向**遍历 (`subject→resource`, 即 relation_types 的 `from→to`),
且 UI 用"我 经[X]关系的 组织"这种句子, 方向藏在措辞里 —— `belongs_to`/`responsible_for`
这类词光看名字分不清谁指向谁。

事实基础 (读真表/真码确认):
- 关系**强有向**: `relation_types` 的 `(relation_code, from_type, to_type, tenant_id)` 是主键;
  `belongs_to` = `from=place, to=org_unit` (场所属于组织)。同一码可有多条不同 (from,to)。
- 存储边有向: `access_relations` 每行 `subject → resource`。
- 链遍历**只走正向**: `ChainHopResolver` 恒 `subject_type=上一级 → resource_type=toType`;
  UI `relsFor()` 也只列 `from=上一级, to=toType` 的关系。唯一"反向"是终端 SUBJECT_GRAPH 把
  member 反读 (org→成员), 写死, 非可配跳。

目标: ① 关系方向**说人话** (反向名); ② 链支持**反向走跳** (组织→其成员用户, 再往下);
③ 配置 UI 改**可视化流图** + 实时预览。

## 范围 (经确认)

**纳入**: 反向名 (`reverse_name`) · 反向走跳 (`Hop.direction`) · 可视化流图 + 实时预览端点。

**刻意未纳入 (砍掉 P-E2 对称)**: 双向对称关系 (`family_of`/`emergency_contact` 之类
A-B 存一条即双向)。理由: ROI 最低 (这类 user↔user 关系几乎不进数据权限链) + 对称与上层
AND 组合有去重坑。**连带不加 `is_symmetric` 列** —— 无引擎消费 = 死列, 违背不 build-ahead。
将来若有真实需求再开 (UNION 两端去重 + 限 OR-only, 本文档 §附录 留做法)。

---

## §1 数据模型

**`relation_types` 加一列** (破坏性改 baseline_v3, 无数据兼容包袱):

| 新列 | 含义 | 例 |
|---|---|---|
| `reverse_name VARCHAR(50)` | 该 (code,from,to) 反着读的名字 | `belongs_to` 正向「场所属于组织」/ 反向「组织下辖场所」; `member` 正向「用户属于组织」/ 反向「组织的成员」 |

**`Hop` 领域模型加方向枚举** `direction ∈ {FORWARD, REVERSE}`:
- `FORWARD` (默认, = 现状): 沿 `subject→resource` (关系定义 from→to)。
- `REVERSE`: 沿 `resource→subject` (把关系倒着用)。
- **回兼关键**: 旧 grant JSON 无 `direction` 字段 → 反序列化默认 FORWARD; Hop compact ctor 兜底。
  → 现有全部配置字节等价, 金标准安全。

**种子**: 给现有 22 条 relation_types 逐条补 `reverse_name` (中文人话, 不杜撰)。
apply 含中文迁移**必带** `--default-character-set=utf8mb4` (踩过 mojibake 坑)。

---

## §2 引擎遍历 + 性能

每跳按 `direction` 换边 (`inner` = 上一级集合, level0 为 `:me`):

**① FORWARD (现状不变)** — `subject→resource`:
```sql
SELECT ar.resource_id FROM access_relations ar
WHERE ar.subject_type=:prev AND ar.subject_id IN(inner)
  AND ar.relation IN(...) AND ar.resource_type=:to AND ar.deleted=0
  [GROUP BY resource_id HAVING COUNT(DISTINCT relation)>=N]   -- AND 组合
```
索引: **`idx_chain_hop`(subject_type,subject_id,relation,resource_type,deleted) 全覆盖**。

**② REVERSE (新)** — `resource→subject` (subject/resource 角色对调):
```sql
SELECT ar.subject_id FROM access_relations ar
WHERE ar.resource_type=:prev AND ar.resource_id IN(inner)
  AND ar.relation IN(...) AND ar.subject_type=:to AND ar.deleted=0
  [GROUP BY subject_id HAVING ...]
```
索引: **`idx_expand`(resource_type,resource_id,relation,deleted) 覆盖前三列**; `subject_type` 残余
过滤 (`resource_id IN` 已大幅收窄, 廉价) → **不新增索引 (YAGNI), profiling 有需要再加 `idx_chain_hop_rev`**。
注: reverse 可作 level0 (如 `supervisor_of` 从我反查"我的下属")。

**place 投影也对称**: 正向 `place→org` 走 `effective_org_unit_id` (现状); 反向 `org→place`
走 `SELECT id FROM places WHERE effective_org_unit_id IN(inner)`。

**终端落法按链末实体类型分支** (ChainCompiler):
- 末 = org_unit / place → SUBJECT_GRAPH 走成员/占用子查询 (**现状不变, 字节等价**)。
- 末 = **user** → SUBJECT_GRAPH **退化为 `data.user_id IN (S)` 直接身份命中** (S 已是用户集)。

限深 `MAX_DEPTH=3` 不变 → reverse 同受上界, 行级 worst-case 有界。

---

## §3 校验器方向 + 既有闸适配

**校验器新增"边合法性 + 方向"校验** (注入 relation_types 边查询, 复用 `RelationTypeRegistry`
或薄 infra 缓存, 避免 infrastructure→application 反向依赖):

| 方向 | 要求存在的 relation_type 边 |
|---|---|
| FORWARD | `(code, from=上一级, to=本跳toType)` |
| REVERSE | `(code, from=本跳toType, to=上一级)` (倒着走) |

找不到 → 拒绝 + 提示"该方向可选关系: …"。**顺带关闭审计 #4** (原"关系不验边合法性",
fail-closed 但无友好报错)。

**place 投影跳豁免**: 正向 `place→org` 与反向 `org→place` 不查 relation_types (走投影),
沿用 #3 "关系可空"豁免, 反向同样豁免。

**既有闸全部不变**: 终端∈注册表 · creator+hops (#2) · PROVIDER/RECORD+hops · MAX_DEPTH≤3。

---

## §4 UI 可视化流图 + 实时预览

**流图组件 (替换 ChainConditionEditor 句子布局)** —— **不引图库** (vue-flow/cytoscape 对
3–5 节点是杀鸡用牛刀, 违背紧凑风格), 手写横向 flex「节点 chip + 边 chip」:

```
[我] ─管理→ [组织▾]+ ─下辖场所←(反)─ … ─属于→ 《学生》
 起点(user)  可编辑实体    边: 关系+方向+含下级      终端  数据(=模块,固定)
```
- **节点 chip**: `我`(固定 user) / 中间实体 (org_unit·place·user, 可改类型/增删) / 数据节点 (固定=模块)。
  点击 → el-popover 改类型 / 插入一跳 / 删。
- **边 chip**: 点击 → popover 编辑 关系(多选) · **方向(正向/反向)** · AND/OR · 含下级。
  边文字用**方向感知名**: 正向 `relation_name` / 反向 `reverse_name`; 箭头 `→ / ←` 直接表达方向。
- 末边 = 终端锚点 (owner_org/creator…, 沿用 §3 校验)。
- 多条范围 (OR) = 多张流图,「+ 加一条范围」叠加。

**实时预览端点 (新)**: `POST /api/roles/data-permissions/chain-preview`
- body: `{moduleCode, grant(链), asUserId(模拟用户)}` —— 复用数据权限页已有「模拟用户」。
- 后端复用 `ScopeEvaluator.composeHopChain` 出谓词 → `SELECT COUNT(*) + 样例 FROM <资源表>
  WHERE <谓词>`, **以模拟用户身份**求值。`@CasbinAccess` 同保存权限, fail-closed。
- 返回 `{count, sampleIds[], stageCounts[]}` —— **每跳漏斗 = 命中路径高亮**:
  `我→管理→ 3 组织 →反成员→ 47 用户 →属于→ 5 学生`, 反向跳效果肉眼可见。
- 前端 **debounce 500ms**; 配合 §2 索引, 改一下算一次廉价。
- ⚠ super admin 不能用自身做模拟 (会 bypass 全放行 = 假数据)。

**复用字典**: relationTypeApi 扩 `from/to/reverseName`; 终端选项用现有 resource_relations;
类型/组织树沿用现状。

---

## §5 实施分期 + 风险回归

每期独立 commit + 一步一金标准 (dpt_ct=2) + 真库 shadow; UI 期加 type-check。

| 期 | 内容 | 验收 |
|---|---|---|
| **P-M1 模型+种子** | `reverse_name` 列 (baseline_v3 + post-v3 迁移) · 22 行补反向名 · `Hop.direction` 枚举(默认 FORWARD)+ JSON 回兼 | Hop JSON 含/不含 direction 往返; 金标准=2 (无引擎行为变) |
| **P-E1 反向走跳** | ChainHopResolver forward/reverse 分支 + place 反向投影; ChainCompiler 终端末=user 直接命中 | 单测 RED→GREEN; shadow: 组织→反成员→用户 出预期集 |
| **P-V1 校验方向** | relation_types 边查 + 方向校验 + **关掉审计#4** | 单测 + 真库往返: 方向反了 → HTTP 400 |
| **P-U1 流图** | 手写 flex 流图 + relationTypeApi 扩字段 | type-check + 手动/Playwright |
| **P-U2 预览+漏斗** | chain-preview 端点 + 右侧每跳漏斗 | 真库往返 + 金标准 |

(P-E2 对称已砍, 见"范围"。)

**关键地雷 (逐条防)**:
1. **direction 默认必须 FORWARD** —— 旧 grant JSON 无该字段 → 默认正向, 否则金标准全崩 (Hop ctor 兜底)。
2. **终端 user-direct 分支**改了 SUBJECT_GRAPH 共享路径 → **只在末=user 时走新分支**, 末=org/place
   字节不变, shadow 比对旧路径。
3. **reverse 索引**复用 idx_expand 残余过滤; 变慢再加 idx_chain_hop_rev (预留不预建)。
4. **中文种子 mojibake**: apply 含中文迁移必带 `--default-character-set=utf8mb4`。
5. **预览端点**: debounce + COUNT; 以模拟用户求值, super admin 不能用自身。

**回归纪律**: 每期 additive + 真启动 restart→金标准→shadow→commit; UI 期加 type-check。

---

## 附录 — 对称关系 (未来, 未实施)

若将来要做: `relation_types` 加 `is_symmetric` (要求 from==to); 遍历用 UNION 两端去重:
```sql
SELECT resource_id ... WHERE subject_id IN(inner) ...
UNION
SELECT subject_id  ... WHERE resource_id IN(inner) ...
```
限 OR-only (对称 + AND 组合的 GROUP-BY 去重坑暂不碰)。届时连带 UI 加「对称-自动」方向、
校验加 `is_symmetric ∧ 上一级==toType` 约束。
