# 统一数据归属架构 — 剩余实施路线图 (2026-06-20)

> 背景: 分支 `feat/unified-data-anchoring` 已完成 **R1→R4 引擎全链 + R3a cutover + R3b(grant-aware
> + 污染正解)+ 引擎死代码清理**,共 27 commit、全绿、字节等价可用、未合 master。
> 本文把剩余工作从"记忆接续点"展开为**可执行计划**,标注本会话实查到的**真阻断/前置**。
> 设计真相源: `docs/plans/2026-06-18-unified-data-anchoring-design.md`(R1–R8 模型)。

## 已完成(勿重做)
- **R1/R2.1/R2.2/R2.4-T1**:registry 骨架 + 全资源锚点登记 + buildMeta 读注册表(锚点唯一真相源)。
- **R3a**:M1 三轴 → `role_data_scopes.relation_grants` 字节等价 cutover(列已删,grants 唯一存储)。
- **R3b**:拦截器 grant-aware(isOrgUnbounded 多锚点)+ withResourceType 污染正解(plugin-dim 用 resourceCode)。
- **R4 引擎**:`record_relations` 表 + 仓储 + `registry.relationOf()` + ScopeEvaluator `composeGrant`
  的 RECORD_RELATION 分支 + inspection_record `reviewer` 关系注册(verified-by-composition)。
- **清理**:删 legacy-dead `accessRelationSelect`(resourceType 全无来源,经核实)。

---

## 阶段 A — R4 step3 完成(写路径 + 收编旁路)【周级,有真阻断】

**目标**:让 RECORD_RELATION 有真实写入与端到端真证;收编 `MyReceivedInspectionsController` 旁路。

**本会话实查到的真阻断**(读 `MyReceivedInspectionsApplicationService` 得):
1. "受检面" = `insp_submissions WHERE target_id IN (我的成员组织)` —— 锚是 **target_id 列**(被检查 org),
   非 owner 的 `org_unit_id`。引擎现仅认 owner_org→`meta.orgUnitField`,**别的 COLUMN 关系无法出 SQL**。
2. subject 是 "**我的全部成员组织**" 非 primary org → 现 `SubjectScope` 无此语义。
3. 旁路查询是**聚合**(trends/recurring/summary,走 JdbcTemplate **绕过 mapper 拦截器**)→ 即使建模也不走 `@DataPermission`。
4. DB **零 insp_submissions 数据** → 端到端真证需先建检查链(project→template→task→submission)。

**方案(按依赖)**:
- A1 **引擎支持 per-relation COLUMN**:`registry.relationOf().columnName()` 已可取;让 `composeGrant`/
  `orgFieldSelect` 对非 owner_org 的 COLUMN 关系用该列(现写死 `meta.orgUnitField`)。注册 inspection_record
  的 `inspected`=COLUMN(target_id, ORG_UNIT)。
- A2 **新 SubjectScope `MY_MEMBER_ORGS`**(我的全部成员组织,实时查 access_relations member),供
  `{inspected, MY_MEMBER_ORGS}` grant 表达受检面。
- A3 **写路径**:`RelationColumnFiller`(registry autoFill 关系驱动列填充,泛化既有 CompositeMetaObjectHandler;
  ⚠ 别与 orgUnitId 填充冲突)+ reviewer 指派写 record_relations tuple。
- A4 **聚合改造或保留**:聚合查询难走 mapper 拦截器 —— 要么把受检面过滤抽成可复用的 `ScopeEvaluator` 产物
  注入聚合 SQL,要么这部分**有意保留**旁路(标注为"聚合特例,已走统一 scope 计算")。
- A5 **端到端真证**:建一条检查链数据 + 配 `{creator∨inspected:MY_MEMBER_ORGS∨reviewer}` grant + 真库查
  `GET /inspection/submissions?taskId=` 验注入(含 record_relations 子查询)。
- **验证**:字节等价 harness + 真启动 + 真库金标准。**工作量**:1–2 周。

---

## 阶段 B — R8 写鉴权闸3(WRITE 范围真拦截)【周级,安全关键】

**目标**:`apply_to=WRITE/BOTH` 的范围在**写操作**(INSERT/UPDATE/DELETE)时真实拦截(现仅 READ 经 SELECT 拦截器生效)。

**方案**:复用 `ScopeEvaluator.toSqlCondition(spec, ..., "WRITE")` 产可见集 → 在写入前校验目标行 ∈ 可见集
(或 UPDATE/DELETE 注入同 WHERE;INSERT 校 owner_org/target 合法)。接入既有 `PolicyEngine`(闸3,deny-overrides)。
**验证**:TDD + 真启动 + 真库(非授权写被拒)。**工作量**:1 周。**风险**:安全关键,需独立专注。

---

## 阶段 C — R3c 数据权限 UI(按关系授予 + 预览)【前端独立轮】

**目标**:管理员配 relation_grants(多锚点)+ 常驻预览。复用 M1 的"默认+例外+预览+模拟用户"脚手架
(`ScopeBuilder`/`scopePolicy`/`DataScopeStudio`),把"换轴编辑器"改为"关系授予编辑器"。
**前置**:后端 `getRolePermissions`/`saveRolePermission` 已支持 grants(R3a-2b 完成,scopeCode↔grants 单 grant)
—— 多 grant UI 需后端 save 接受 grants 数组(现 save 从 scopeCode 派生单 grant)。**工作量**:前端 1 轮。

---

## 阶段 D — Tier 2 注册表收敛【中等,形态需先解】

`membershipSubjectColumn`/`typeField`/`resourceType` 仍注解/data_resources 驱动。
- `resourceType`:本会话已证**全无来源 + accessRelationSelect 已删** → 直接**删字段**(改 11 处 `new ResourceScopeMeta`
  + buildMeta + 注解 attr)。纯删,低风险高 churn。
- `typeField`:**per-resource**,与 per-relation 的 resource_relations **形态不匹配** → 要么放 data_resources 让
  registry 读、要么不迁。需先定形态。
- `membershipSubjectColumn`:per-resource(user_student 用),同上。
**工作量**:resourceType 删半天;typeField/membershipSubjectColumn 需设计。

---

## 阶段 E — R7 守护【小,价值薄】

核心不变量多已**构造期强制**(`ResourceRelationDef` 构造器:storage_kind↔cardinality 一致性;
`PluginDeclarationCoverageTest`:每 @DataPermission 模块必登锚点)。可补:
- "SUBJECT_GRAPH 仅成员主体(user/student)"数据守护(查 resource_relations)。
- "无新增裸 JdbcTemplate 直查数据表"(难静态测)。
**工作量**:0.5 天。**价值**:防退化,薄。

---

## 阶段 F — R5 ASSET 第四主体 / R6 物化倒排索引【无触发,不建议现做】

- **R5**:relation_types/access_relations/record_relations 的 subject_type 接受 asset + 闭合图。**无资产主体业务触发**。
- **R6**:规模层物化倒排索引(新表 + 事件驱动增量 + 双路读 + 失效)。**无性能触发**(实时子查询够用);
  设计稿自标"R5–R8 无紧迫触发信号"。**现做 = 造无人用的大子系统,负 ROI**。
**建议**:出现真实触发(资产主体业务 / 性能瓶颈)再启动。

---

## 阶段 G — fresh-init-all 修复【卡决策】

post-v3 6 个历史迁移非幂等 → fresh/CI build 坏(日常对活库开发故潜伏)。**需先定约定**(与"不写条件化"指令冲突):
- 选项① post-v3 迁移幂等化(information_schema 守护)—— 违"不写条件化"。
- 选项② init-all 改 fresh=baseline-only(post-v3 仅 dev-DB 追赶,不参与 fresh)。
**阻断**:需用户拍约定。

---

## 推荐执行顺序
1. **先沉淀**:review/push/合并当前 27 commit(R1–R4 引擎是完整可用的主体)。
2. **G**(定 fresh-init-all 约定)+ **D-resourceType**(纯删,顺手)—— 低成本清债。
3. **A**(R4 step3 完成)—— 让 RECORD_RELATION 有真消费者,收编旁路。最高业务价值。
4. **B**(R8 写鉴权)—— 安全补全。
5. **C**(R3c UI)—— 管理员可配。
6. **E**(R7 守护)—— 收尾防退化。
7. **F**(R5/R6)—— 等触发,可能永不需要。
