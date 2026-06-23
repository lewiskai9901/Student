# 后续工作计划:接口式关系(PROVIDER)+ 统一锚定剩余收尾 (2026-06-22)

> 背景:分支 `feat/unified-data-anchoring`(35 commit)已完成 R1–R4 引擎 / R3a / R3b / **R3c 全**
> (后端+前端+浏览器验证)/ **G fresh-init-all**。本计划把**新共识"接口式关系 PROVIDER"**落地,
> 并与剩余 A/B/D/E/F 整合排序。设计图解见 `docs/plans/provider-relation-example.html`。

## 新共识(本次设计结论)
- `storage_kind` 增第 5 档 **`PROVIDER`(接口式)**:关系不落列、不入表、不走主体图,而由**插件 resolver**算。
- resolver **双模**:① 主用**参数化子查询**(下推 DB,走索引,规模友好)② 退化返回 **id 集**(有界场景)。
- 命名:**关系名中文**(界面全程显示,如「任课老师」)+ **关系码 ASCII**(`taught_by`,底层稳定标识)。
- 关键协同:**A 的受检面旁路可建模为 PROVIDER 关系** → 插件给"我的受检记录"子查询,干净退役旁路。

---

## Phase P1 — PROVIDER 能力落地【首选 · 自包含 · 1 session】
**目标**:让"接口式关系"成为可用的第 5 种 storage_kind,并用「任课老师」样板真库跑通。

| # | 任务 | 备注 |
|---|---|---|
| P1.1 | 领域:`StorageKind.PROVIDER`(枚举新增)+ `RecordRelationResolver` SPI | `SqlFragment subquery(ctx)` 主 + `default List<Long> ids(ctx)` 退化 |
| P1.2 | 注册:`ResourceRelationDef.provider(resourceCode, relationCode, 中文名, subjectType, resolverBean)` | **决策点**:`resource_relations` 加 `resolver_bean` 列(破坏性改 baseline,符合"不留旧列"纪律) |
| P1.3 | 引擎:`ScopeEvaluator.composeGrant` PROVIDER 分支 | 取 bean → subquery 优先包 `id IN (<子查询>)`;否则 ids 包 `id IN (list)`;空→`1=0`;null→fail-closed |
| P1.4 | 样板:`TeachingStudentResolver`(任课老师)走 `class_course_assignment` | 证明插件侧零核心改动 |
| P1.5 | 验证:真库 seed 排课数据 + 配 grant + 王老师查 + 验注入 SQL + 结果 | TDD:ScopeEvaluatorTest 加 PROVIDER 字节断言;真启动金标准 |

**顺带做**:**A1**(per-relation COLUMN,inspected→`target_id`)与 PROVIDER 同处 `composeGrant` 分发区,一并实现。
**依赖**:无。**工作量**:1 session。**ROI**:最高(兑现设计 + 为 P2 铺路 + 自包含可验证)。

---

## Phase P2 — A 受检面 / R4-step3 收尾【周级 · PROVIDER 助攻】
**洞见**:受检面("我的受检记录")过去卡在 target_id 列 + member-orgs subject + 聚合绕拦截器。
现可**建模为 PROVIDER 关系**:检查插件提供 `SELECT id FROM insp_submissions WHERE target_id IN (我的成员组织)` 子查询。

| # | 任务 |
|---|---|
| A2 | 新 SubjectScope `MY_MEMBER_ORGS`(我的全部成员组织,实时查 access_relations member) |
| A3 | 写路径:reviewer 指派写 `record_relations` 元组 + `RelationColumnFiller`(关系驱动列填充) |
| A4 | 受检面 PROVIDER 化(退役 MyReceivedInspections 旁路)或聚合特例标注保留 |
| A5 | 端到端:建检查链数据 + 配 `{创建人∨受检:MY_MEMBER_ORGS∨复核员}` 多 grant + 真库验注入 |

**依赖**:P1(PROVIDER + per-relation COLUMN)。**工作量**:1–2 周。

---

## Phase P3 — B R8 写鉴权闸3【重定范围 · 真缺口仅 INSERT】
**2026-06-22 取证修正**:R8 不是"全未建/周级"。拦截器对 **UPDATE/DELETE 已注入 WHERE**:
`intercept()` 非 SELECT → actionClass=WRITE → buildScopedCondition(apply_to WRITE/BOTH)→
`injectFilterCondition`(**通用 WHERE 注入**, 对 UPDATE/DELETE 同产合法 SQL: `... WHERE id=? AND (scope)`)。
→ 用户只能 UPDATE/DELETE 其 WRITE 范围内的行(范围外 0 rows affected, 静默拦截)。
已加单测 `writeScope_appendsToUpdate/Delete` 验证(DataPermissionInterceptorTest)。

**真缺口 = 仅 INSERT**:`intercept()` 对 INSERT **早退**(line 101, 注释"ownership 由应用层强制")→ 新行的
owner_org/target 是否 ∈ 用户可写范围**无拦截器校验**。这是 R8 唯一未建部分。

| # | 任务(仅 INSERT)|
|---|---|
| B1 | INSERT 授权: 拦截 INSERT, 取新行 owner_org/target 列值, 校 ∈ 用户 WRITE 可写组织集 |
| B2 | super-admin 豁免 + 哪些资源参与(有 owner_org 锚的)+ 无锚资源放行 |
| B3 | PROVIDER 关系 INSERT: 无 row 无法 resolver 反查 → INSERT 时 PROVIDER 不参与(读向语义), 文档明确 |
| B4 | TDD + 真库: 非授权 INSERT(owner_org 越界)被拒; 合法 INSERT 通过 |

**已完成 + 全部真库 E2E 已证 (2026-06-22)**:
- UPDATE/DELETE WRITE 注入: 真库 E2E 证 (teaching_task 逻辑删: ttu 写=CUSTOM_ORG[O1], DELETE O2 task →
  注入 `UPDATE ... SET deleted=1 WHERE id=O2 AND ... AND ((org_unit_id IN (O1)))` → 0 行 → 未删;
  DELETE O1 → 1 行 → 已删)。
- INSERT 授权 (pre-insert, owner_org ∈ 可写组织): 真库 E2E 证 (teaching_task: POST org=O2 → AccessDenied;
  org=O1 → 过授权)。见 `2026-06-22-p3-insert-authz-design.md`。
**R8 三动作全真库验证**: READ(dpt_ct 金标准)+ UPDATE/DELETE(0行静默拦截)+ INSERT(AccessDenied)。
**剩余 follow-up**: INSERT 方法级 mapper 覆盖扩展 (安全行为变更, 需逐资源语义分析 — 如 inspection 检查员
跨组织创建 submission 是否该拦, 非盲目扩展; 独立 careful pass)。接口级资源已全覆盖。

---

## 数据权限剩余 #1-#3 处理结论 (2026-06-22)
用户要求"1-3 全部开发完成"。逐项取证后:
- **#1 INSERT 方法级覆盖 → 改为显式 opt-in ✅完成** (`ec90c45f`): 取证发现原"接口级=覆盖"是偶然边界,
  盲扩到方法级会误拦 inspection(org_unit_id=受检 target org)。改 `enforce_insert_scope` 显式标志:
  仅 owner_org=ownership 语义资源参与(教务/班级);inspection target 排除。真库 E2E 证。
- **#2 Tier2 typeField/membershipSubjectColumn → 判定"已正确放置, 不动" (主见)**: typeField 已数据驱动
  (data_modules, 模块配置的合理家); membershipSubjectColumn 是 per-mapper 注解配置(同 tableAlias, 有意保留)。
  二者是 **per-resource** 粒度, 强塞进 **per-relation** 的 resource_relations = 过度设计(违反反过度设计原则)。
  真 Tier2 债(resourceType 死字段)已删(`a2be9d90`)。**结论: Tier2 收敛已实质完成。**
- **#3 受检面聚合旁路 → 判定"有意设计, 无需退役" (主见)**: trends/recurring/summary 是 GROUP BY 聚合,
  **本质无法走行级拦截器**(行级 WHERE 注入不适用聚合); 且已正确按 `resolveOrgUnitIds`(用户成员组织,
  = PROVIDER 受检面 resolver 同逻辑)收窄。"旁路"是聚合的必然, 非漏洞。**无可退役。**

## Phase P4 — D Tier2 收敛 + E R7 守护【清理】
| # | 任务 |
|---|---|
| D1 | `resourceType` 纯删(已证全无来源,11 处)|
| D2 | `typeField` / `membershipSubjectColumn` 形态决策(per-resource 入注册表 vs 不迁)—— **需先定** |
| E1 | 守护:PROVIDER resolver bean 必存在(启动校验)|
| E2 | 守护:PROVIDER subquery 必参数化(禁裸字符串拼接 → 防注入)|
| E3 | 守护:SUBJECT_GRAPH 仅成员主体(user/student)|

**依赖**:P1–P3。**工作量**:几天。

---

## Phase F — R5 ASSET / R6 物化索引【硬性等触发,不做】
- R5 资产第四主体:无业务触发。R6 物化倒排索引:无性能触发,是规模层大子系统。
- 出现真实触发(资产业务 / 性能瓶颈)再启动。**现做 = 负 ROI。**

---

## 推荐执行顺序与起点
```
P1 (PROVIDER 落地 + A1) ──► P2 (受检面, PROVIDER 助攻) ──► P3 (写鉴权) ──► P4 (收敛+守护)
   [现在开]                  [周级专项会话]              [安全专项会话]      [清理]
                                                                          F 等触发
```
**起点 = P1**:自包含、可在本会话完成、可真库验证、兑现刚定的设计、并为 P2 受检面解锁干净路径。

**P1 待你拍的决策点**:
1. `resource_relations` 加 `resolver_bean` 列(推荐,符合"破坏性改 schema"纪律)—— 还是复用现有 `column_name` 列存 bean 名(省一列但语义混)?
2. resolver 为 null(bean 不可用)时:**fail-closed 拒绝**(安全)还是降级 SELF(可用性)?推荐 fail-closed。
