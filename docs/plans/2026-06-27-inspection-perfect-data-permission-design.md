# 检查执行域数据权限「完美」重构 — 设计 + 分期

> 日期: 2026-06-27 · 分支: feat/unified-data-anchoring (worktree /d/uda-wt) · 状态: 设计待确认→实施
> 触发: 用户要"彻底落地" reviewer/record_relations,核实后发现真问题不在 record_relations,
> 而在「共享资源码 inspection_record 跨多表、各表列不同」导致的**建模错配 + 一个真 bug**。

## 1. 问题(核实所得,非臆测)

`@DataPermission(module="inspection_record")` 被 **5 个 mapper 共用**,但各表锚点列不同:

| mapper | 表 | org 列 | creator | 该表特有锚点 |
|---|---|---|---|---|
| InspTaskMapper | insp_tasks | org_unit_id | created_by | **reviewer_id(我复核)/ inspector_id(我检查)** |
| InspSubmissionMapper | insp_submissions | org_unit_id | created_by | **target_id(受检面=被检组织)** |
| SubmissionDetailMapper | insp_submission_details | org_unit_id | created_by | — |
| InspEvidenceMapper | insp_evidence | (待核实) | (待核实) | — |
| ProjectInspectorMapper | project_inspectors | (待核实) | (待核实) | — |

**两个真缺陷**:
1. **受检面 PROVIDER resolver 写死 `FROM insp_submissions`** (`MyReceivedInspectionsResolver`) —— 被
   `inspection_record` 共享后, 查 insp_tasks/evidence 时引擎包成 `tasks.id IN (SELECT sub.id FROM insp_submissions…)`,
   **task id 比 submission id = 错/空**。这是潜在数据权限正确性 bug。
2. **reviewer 误声明为 RECORD_RELATION**: 实为 `insp_tasks.reviewer_id` 单值列, 写入方 `startReview` 早就有。
   多值表 record_relations 是错配。**域内无真实"一记录挂多主体"多值关系 → record_relations 此域不用(正确结果)**。

## 2. 完美方案: 按表拆资源码 + per-relation COLUMN 锚

把共享的 `inspection_record` 按执行表**拆成独立资源码**, 各表声明**自己真实列**的锚点:

| 新资源码 | 表 | 锚点(全 COLUMN) |
|---|---|---|
| `inspection_task` (已存在,复用) | insp_tasks | owner_org=org_unit_id · creator=created_by · **reviewer=reviewer_id** · **inspector=inspector_id** |
| `inspection_submission` (新) | insp_submissions | owner_org=org_unit_id · creator=created_by · **inspected=target_id** |
| `inspection_submission_detail` (新) | insp_submission_details | owner_org=org_unit_id · creator=created_by |
| `inspection_evidence` (新) | insp_evidence | 按实列(待核实) |
| `inspection_project_inspector` (新) | project_inspectors | 按实列(待核实) |

收益:
- **受检面** 变干净列锚 `submission.target_id`(配 `{inspected, RELATION:member}` 或仍 PROVIDER 但 resolver 只挂 submission 资源)→ **多表 bug 自然消失**(每码只对一张表)。
- **我复核/我检查** = `{reviewer, SELF}`/`{inspector, SELF}` → `reviewer_id=me`/`inspector_id=me`(写入方 startReview/claim 已有)。
- record_relations 不参与 = 正确(无多值关系)。

## 3. 引擎依赖: A1 per-relation COLUMN(本会话已实现过, 已 revert, 需 un-revert + 扩)

- `ScopeEvaluator.composeGrant`: 非 owner_org/creator 的 COLUMN 锚点 → 用 `registry.relationOf().columnName()` 换列。
- **扩**: subject=SELF 时绑到该列(`reviewer_id = me`), 非仅 org-set 路径。做法: `meta.withColumn(col)` 同时
  覆盖 orgUnitField + creatorField(SELF 走 creatorField 分支自然得 `reviewer_id=me`; org-set 走 orgUnitField 得 `col IN(S)`)。
- 单测已写过(perRelationColumnGrant), 复活即可。

## 4. 分期(每期独立 commit + 一步一金标准 dpt_ct=2 + 真库 shadow; 高风险切换)

- **P0 引擎**: un-revert A1 + 扩 SELF 绑列 + `ResourceScopeMeta.withColumn` + 单测。无消费者前零行为变(金标准)。
- **P1 submission 拆码**: 新 `inspection_submission` 资源码 + 锚点(含 inspected=COLUMN target_id);
  InspSubmissionMapper/SubmissionDetailMapper @DataPermission 改码; **删 MyReceivedInspectionsResolver**(改列锚)
  或仅挂 submission; PluginDeclarationCoverageTest 守护补码; 真库 shadow 受检面恰命中。
- **P2 task 拆码**: InspTaskMapper 改 `inspection_task` + reviewer/inspector COLUMN 锚; 真库 shadow
  `{reviewer,SELF}`→`reviewer_id=me`(配一条 grant, 以审核员真查任务恰命中)。
- **P3 evidence/detail/project_inspector 拆码**: 按实列锚(先核实列); 无特有锚点的退 owner_org/creator。
- **P4 清理 + 守护**: 删 `inspection_record` 资源码(无残留引用; 现仅 1 条 role_data_scopes 配置, 重新指向);
  改前后字节等价 harness + 全量 access 回归 + 真启动 + 金标准。
- **P5 E2E**: 配检查链数据 + 审核员/受检者真账号 → GET 各端点验注入正确(含 reviewer/target 列谓词)。

## 5. 风险 / 验证纪律

- 共享码拆分**改 5 mapper 注解 + manifest + 1 条既有配置**; 每期 corrupt-restore 真启动 + 表快照零回归。
- evidence/project_inspectors 表列**未核实** → P3 前先 SHOW COLUMNS 定列, 无 org/creator 列则该 mapper 不挂列锚(退 SELF 或 ALL by design)。
- 既有 `inspection_task` data_resource 已存在但执行 mapper 没用它 → 复用时核对其 scope 列表不冲突。
- **record_relations 不动**(休眠); reviewer 从 RECORD_RELATION 改 COLUMN = manifest 一行改 + 删 record_relations 的 reviewer 残留(无数据)。
- **宜在独立会话逐期执行**(高风险切换, 同 R2.2/R3a 经验): 本会话只锁设计, P0 起逐期推。

## 6. record_relations 处置

保留表/仓储/引擎 RECORD_RELATION 分支(休眠基建, 同 R5/R6)。**不强行制造多值关系**。
若将来出现真实"一记录挂多主体"(如多会签人/抄送),再启用——届时 storage_kind 选 RECORD_RELATION 是对的。
