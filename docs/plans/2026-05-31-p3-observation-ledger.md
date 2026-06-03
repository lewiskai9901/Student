# P3 重设计 — 观察台账(巡检一等实体)+ 周期评比 实施计划

> **For Claude:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development。本计划是大厂模型重设计,替代原 P3 的"录入模式/EVENT_STREAM"。开发阶段**无数据兼容包袱**(用户确认):遇结构变更直接改 schema+代码,不写迁移,DROP 重建测试数据。

> ⚠️ **通用性铁律(最重要):这是通用平台的检查模块,核心代码/模型/字段不得出现任何行业术语**(班级/学生/纪律/违纪/红旗 等)。检查对象一律用通用的 `TargetType: ORG/USER/PLACE/ASSET` + `targetId` + `orgUnitId`;实体叫 `Observation`(观察/事件)不叫"违纪";类目是通用"问题类目";评比产出叫"评比维度/奖项"。**本文出现的"班级/迟到/纪律红旗/卫生红旗"全是学校场景举例,不是模型概念**。已有 ArchUnit 守护 `NoIndustryTypeLiteralInCoreTest` 禁止 core 出现 STUDENT/CLASS 字面量——P3 代码必须过这关。已知行业残留待清:`PersonScoreObservationExtractor` 用 `studentId/studentName`,3A 收编时改成通用 `subjectId/subjectName`(或 targetId)。

**Goal:** 把"巡检/观察/事件"从"模板分区里的录入模式"升级成**一等的「观察事件台账」实体**(通用,不绑行业),配独立"记一笔"入口 + 类目库 + 评比维度选类目 + 自动周期聚合出**检查对象(组织/用户/场所)排名**;清单检查保持平行;**砍掉模板版本钉死/漂移迁移**,改为"项目跟随活模板 + 公布即冻结结果"。

**Architecture(全部设计决策,来自头脑风暴):**
1. **观察事件 = 一等实体** `Observation`:`{projectId, targetType, targetId, orgUnitId, categoryId, points(记录时从类目快照), severity, occurredAt, recordedBy, evidenceUrls, note}`。随时记,不依附任何清单任务。
2. **类目库** = 复用 `问题类目(issue categories)`,补 `defaultPoints/severity`。巡检时从库选,points 在记录瞬间快照进 Observation(改类目分值不回改历史)。
3. **平行"记一笔"入口**:搜对象(班/生)→ 选类目 → 自动带分 → 拍证据 → 存。自动解析到"当前进行中的评比项目 + 该对象 + 按 occurredAt 落周期"。无需派任务。
4. **评比维度(rating dimension)选类目**:复用 `insp_rating_dimensions`,从"只选清单分区(section_ids)"扩展为"也选巡检类目(category_ids)"。默认全选。一个事件池 × 多维度 = 多个奖项/排名口径(学校场景*举例*:纪律红旗只算纪律类、卫生红旗只算卫生类)。
5. **聚合 = 周期重算式,非累加**:每条观察/清单提交变更 → 重算 (维度, 对象, periodKey) 整段总分 → 幂等 upsert 唯一键。复用 `PeriodKeyResolver` + `period_summaries` + 组织 roll-up(评分公平性那套)。周期由 `occurredAt` + 方案的**日切规则**确定性派生(默认自然日 00:00,可配日切点如 06:00)。多触发自愈(事件驱动+定时兜底+查看懒重算)。
6. **公布即冻结结果**:某期排名正式公布 → 快照该期"分数/排名结果"。未公布全活、随时重算。冻结的是结果不是模板。
7. **砍掉版本/漂移**:删 `template_version` 钉死 + drift 检测 + `upgrade-template-version` 整套。项目引用活模板;模板改 → 未公布周期直接重算。
8. **废弃 inputMode/EVENT_STREAM**:删 section/item 的 `inputMode` 字段+列+执行端 EVENT_STREAM 分支(被观察台账取代)。
9. **观察对象范围过滤(检查对象限定)**:项目对每个 ORG 类型检查范围,可选配 `TargetFilter`:① **关系过滤** `relationCodes`(默认 `member`=归属;可选 `teaches`/`admin` 等职能关系)② **用户类型/能力过滤** `userTypeCodes` 或 `features`(如 `isLearner`)③ 组合语义:**同一维度内多值取并集,关系维度 ∩ 类型维度取交集(AND)**。"记一笔"可选对象 = `(∪ 选中关系的成员) ∩ (∪ 选中类型/能力的用户)`。**全部通用词**:关系码来自 `RelationTypeRegistry`、类型/能力来自 `entity_type_configs`;学校场景"只记学生、排除班主任"是*举例*。**硬依赖 A(归属统一,见 `2026-05-31-membership-unified-relation.md`)**:归属=member 关系后,"排除班主任(admin)、只留成员(member)"才表达得出;范围解析走新 `ObservationScopeResolver`(组合 A 的 `MembershipResolver` + `AccessRelationService.findSubjectsWithRelation` + 用户类型/feature 过滤),采 **STRICT** 语义(只有范围内对象可记一笔)。

> 🔗 **前置依赖:本计划(B)整体依赖 A「组织归属统一为关系」先完成** —— 3A.4 范围解析、检查对象成员判定、PER_MEMBER 归一化都建在 A 的干净归属(MembershipResolver / member 关系唯一)之上。A 未落地前不要执行 B 的 3A.4 及依赖它的任务。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus + Vue3。复用既有:问题类目、ViolationRecord/observation extractor(提升为 Observation)、rating_dimensions、PeriodKeyResolver、period_summaries、org roll-up、EventStreamRecorder 组件(改造成记一笔)。

---

## ⚠️ 执行前置:实现者必须先 grep 确认现状(别臆造)
- 现有 `insp_violation_records` + `ViolationRecord` 模型 + `PersonScoreObservationExtractor` / `SubmissionObservation` —— 决定 Observation 是"提升 ViolationRecord"还是"新建并迁移语义"。
- 现有 `问题类目`(issue categories)表/模型/controller —— 字段、有无 points。
- 现有 `insp_rating_dimensions`(选 section_ids)+ 其聚合服务 —— 扩 category_ids 的接入点。
- 现有版本/漂移代码全集(`template_versions`、`template_version_id`、`upgrade-template-version`、`preCheckTemplateDrift`、`template-version-status`、`TemplateVersion*`、snapshot 包)—— 要删的范围。
- `PeriodKeyResolver` / `period_summaries` / `OrgUnitScoreRollupService` —— 复用的聚合基建。
- **(C 范围过滤底座)** A 的 `MembershipResolver`(membersOfSubtree/countMembersByType)+ `AccessRelationService.findSubjectsWithRelation(resourceType,resourceId,relation,subjectTypeFilter)` + 用户类型模块 `EntityTypeConfigRepository.findByFeature` / `users.user_type_code` —— 确认 A 已落地、这些方法可用,再写 3A.4。

---

# Phase 3A — 观察事件一等实体(后端地基)

**目标:** 建 `Observation` 实体 + 表 + 仓储,类目带分值,points 记录时快照。

### Task 3A.1 类目库补分值
- Modify 问题类目模型/PO/表:加 `default_points`(DECIMAL,可负)、`default_severity`(可选)。
- 迁移 `V20260531_x__category_points.sql`(条件化 ADD COLUMN)。
- 前端问题类目管理页加"默认分值"编辑。
- TDD + 编译。

### Task 3A.2 Observation 领域模型 + 持久化
- 决策(grep 后定):提升 `ViolationRecord` → 通用 `Observation`,或新建 `Observation` 收编 violation/person-score 语义。**倾向:统一成 `Observation`**(violation 是其一类),避免两套。
- 新表/改表 `insp_observations`:`id, tenant_id, project_id, target_type, target_id, org_unit_id(横切填充), category_id, category_name(快照), points(快照), severity, occurred_at, recorded_by, evidence_urls(JSON), note, deleted, created_at`。
- 模型 + PO + Mapper + RepositoryImpl(仿现有 inspection PO;org_unit_id 走 `CompositeMetaObjectHandler`/`InspectionUpstreamRouter` 注册,防 `InspectionWriteMustSetOrgUnitIdTest` 红)。
- TDD(单测建/查/按 project+period+target 查)+ 编译 + 真启动(orchestrator)。

### Task 3A.3 记一笔应用服务 + API
- `ObservationApplicationService.record(projectId?, targetType, targetId, categoryId, occurredAt, evidence, note, recorder)`:
  - 类目分值/名称**记录瞬间快照**进 Observation(不存引用)。
  - `projectId` 未给则按 (targetType,targetId,occurredAt) 解析"当前进行中的评比项目"(若唯一);多个则要求显式选。
  - org_unit_id 由 target 推导。
  - **(C)范围 STRICT 校验**:调 `observationScopeResolver.assertRecordable(project, targetType, targetId)` —— 对象不在项目可观察范围(含 TargetFilter 过滤后)则拒(返回明确错误,不静默)。
- `POST /inspection/observations`(记一笔)+ `GET /inspection/observations?projectId=&periodKey=&targetId=`(查/回显)+ `DELETE`(撤销)。`@CasbinAccess` 跟同模块。
- TDD + 编译。

### Task 3A.4 观察对象范围解析器 + 项目 TargetFilter 配置(C:关系 + 用户类型 AND 过滤)
> **依赖 A 已落地**(MembershipResolver / member 归属 / AccessRelationService.findSubjectsWithRelation)。通用性铁律:relationCode / userTypeCode 都是数据,不写行业字面量,过 `NoIndustryTypeLiteralInCoreTest`。

- **项目存 TargetFilter**:`insp_projects`(或其 scope 配置)加 `target_filter` JSON,可按 scope 条目存:`{ relationCodes: string[] | null, userTypeCodes: string[] | null, features: string[] | null }`。全 NULL = 不限(=该 ORG 全体成员)。迁移条件化 ADD COLUMN。
- **新建** `domain/inspection/service/ObservationScopeResolver.java`(或 application 层,因要 JOIN users):
  - `recordableUserIds(orgUnitId, filter) -> Set<Long>`:
    - **关系侧**:`filter.relationCodes` 为空或仅 `["member"]` → `membershipResolver.membersOfSubtree(orgUnitId)`;否则对每个 relationCode 调 `accessRelationService.findSubjectsWithRelation("org_unit", orgUnitId, code, "user")`,**多关系取并集**。
    - **类型侧**:`filter.features` 非空 → 经 `EntityTypeConfigRepository.findByFeature(feature)` 解析出命中的 typeCodes ∪;`filter.userTypeCodes` 直接用。两者并成一个 typeCode 集合,过滤 `users.user_type_code IN (...)`(单次 JdbcTemplate IN 查,注意 [[project_data_permission_layer_broken]] org scope 收窄)。类型侧为空 = 不按类型过滤。
    - **组合**:返回 `关系侧并集 ∩ 类型侧并集`(任一维度为空则只用另一维度;都空 = 该 org 全体成员)。
  - `recordableTargets(project) -> {orgs, userIds, places}`:项目 scope 内各 ORG 展开成员(经 filter)+ scope 内 USER/PLACE 直选 + 关联场所;USER/PLACE 观察 roll up 到所属 org(归属经 MembershipResolver)。
  - `assertRecordable(project, targetType, targetId)`:STRICT 判定,供 3A.3 调用。
- **REST**:`GET /inspection/observations/recordable-targets?projectId=&q=`(供前端记一笔对象选择器;返回过滤后的可记对象列表,叠加记录人数据权限收窄)。
- **TDD**(全用通用词,注释标学校*举例*):组织 O 下有 member 关系用户一批(*举例:学生*)、admin 关系用户(*举例:班主任*)、teaches 关系用户(*举例:任课*);① filter{relationCodes:["member"], features:["isLearner"]} → 只返回"既是成员又是 learner 类型"的;② filter 仅 features → 返回全成员里该类型;③ filter 仅 relationCodes:["member"] → 返回全体成员(排除只有 admin/teaches 关系、不是成员的);④ filter 全 NULL → 全体成员;⑤ assertRecordable 对范围外对象返 false。
- 编译 + 真启动(范围解析涉及 access_relations + users JOIN,真查验证)。

---

# Phase 3B — 周期聚合 + 评比维度选类目(后端核心)

### Task 3B.1 评比维度扩展选类目
- `insp_rating_dimensions` 加 `category_ids`(JSON,选哪些巡检类目计入);`section_ids` 保留(选清单分区)。默认 NULL=全选。
- 聚合服务读 category_ids 过滤 observation。
- TDD。

### Task 3B.2 周期日切规则 + periodKey
- 评比方案(项目级)配 `period_type`(DAILY/WEEKLY/...)+ `day_cutoff`(默认 00:00,可配 06:00 等)。
- 扩展/复用 `PeriodKeyResolver`:`occurredAt + period_type + day_cutoff → periodKey`。清单提交与观察用同一规则。
- TDD(边界:23:55 vs 00:05;日切 06:00 时凌晨事件归前一日)。

### Task 3B.3 重算式聚合 → 检查对象周期分 + 排名
- `RankingAggregationService.recompute(projectId, dimensionId, target, periodKey)`:从源**重算** = 该周期该检查对象的(清单分 [按 section_ids] + 观察分 [按 category_ids 求和/加权])→ upsert 进汇总表唯一键 `(tenant, dimension, target, periodKey)`。
- 触发:观察/清单提交变更 → 重算其 (维度,对象,periodKey);定时兜底全量;查看懒重算。复用 org roll-up 把对象分沿组织树 roll 到上级组织单元排名。
- 幂等 upsert + DuplicateKey 兜底(仿 ProjectScore/org_unit_scores)。
- TDD(钉死:重算两次结果一致=防重;漏触发后重算自愈=防漏)。

### Task 3B.4 公布即冻结结果
- 排名"公布"动作 → 快照该 (project, dimension, periodKey) 的结果(分数+排名)到冻结表/标记 locked。
- locked 周期:重算跳过(或写入影子不覆盖公布快照);新发生的属于已锁周期的观察 → 计入下一周期 或 需显式重开。
- TDD(锁后补记不改已公布排名)。

---

# Phase 3C — 砍掉版本/漂移 + 项目跟随活模板(后端简化)

### Task 3C.1 删模板版本钉死 + 漂移迁移
- grep 全集后删除:`template_version_id` 项目字段、`upgrade-template-version`/`template-version-status`/`preCheckTemplateDrift` 端点+逻辑、drift 相关前端按钮。
- 项目改为引用**活模板(root section)**;算分/任务生成直接读当前模板(不读快照)。
- **保留** `TemplateVersion` 仅作"发布历史留痕"(若有价值)或一并删——grep 确认无其它消费后定。
- 编译 + 真启动 + 全 inspection 测试绿(修因删除而破的测试)。

> 注:这步动现有快照/任务填充路径较深,单独一 Task 充分验证。开发无数据,放心改。

### Task 3C.2 废弃 inputMode / EVENT_STREAM
- 删 section/item 的 `inputMode` 字段+列(V65/V66 加的)+ 执行端 `detail.inputMode==='EVENT_STREAM'` 分支 + 模板侧"录入模式"开关 UI。
- DROP COLUMN 迁移。被观察台账取代。
- 编译 + type-check。

---

# Phase 3D — 前端:记一笔入口 + 项目配置 + 类目页 + 排名

### Task 3D.1 "记一笔"入口(改造 EventStreamRecorder)
- 桌面 + 移动:全局/检查平台内一个"记一笔"按钮 → 搜检查对象(组织/用户/场所)→ 选类目(带分值)→ occurredAt(默认现在可改)→ 拍证据 → 存 → 调 `POST /inspection/observations`。
- **(C)对象搜索限定在可记范围**:对象选择器只列 `GET /inspection/observations/recordable-targets?projectId=`(后端走 `ObservationScopeResolver.recordableTargets`,即应用了项目 TargetFilter 的关系+类型过滤);范围外对象不可选(STRICT)。
- 复用既有 `EventStreamRecorder.vue` + `useGeolocation`/上传。
- type-check。

### Task 3D.2 项目创建:评比维度配置 + 观察对象过滤(C)
- 创建项目时配:范围、周期类型 + 日切点、评比维度(每个奖选 清单分区 + 巡检类目 + 权重 + 等级线 + 是否排名)、启用哪些类目。
- **(C)TargetFilter 编辑**:对 ORG 范围条目,可选"限定关系类型"(多选,来自关系字典 `RelationTypeRegistry` 的 user→org_unit 关系,默认 member=归属)+ "限定用户类型/能力"(多选,来自 `entity_type_configs` USER 维度的 typeCode/feature),两组之间 AND。空=不限。UI 用通用词("关系类型""用户类型"),不硬编码学校词。
- type-check。

### Task 3D.3 问题类目管理页(类目库 + 分值)
- 类目 CRUD + 默认分值/严重度编辑(配置中心"问题类目"卡进入)。
- type-check。

### Task 3D.4 排名/评比视图 + 公布
- 按评比维度展示周期班级排名(复用 org-scores 排名组件思路)+ "公布"按钮(触发冻结)+ 已公布/未公布标识。
- type-check。

---

# Phase 3E — 端到端验证
- 全 inspection 测试套件绿 + 前端 type-check 0 + build 绿 + 真启动。
- **浏览器端到端 smoke**(orchestrator 驱动,*学校场景举例验证*):建清单模板 → 建评比项目(配评比维度选类目 + **配 TargetFilter:关系=member + 用户类型=某学生类型**)→ 跑清单任务打分 + 记一笔观察 → **验证记一笔对象选择器只列范围内成员(排除班主任/任课等非 member 或非该类型的用户)**,选范围外对象被拒(STRICT)→ 看本周期检查对象(组织单元)排名正确(清单分+观察分合并)→ 公布锁定 → 补记一条验证不改已公布。

---

## 阶段依赖与顺序
```
[前置] A 组织归属统一(另一份 plan,必须先完成)
        ↓
3A 观察实体(地基, 含 3A.4 范围解析 C) → 3B 聚合+评比维度(核心) → 3D 前端入口/配置/排名
3C 砍版本+废 inputMode(独立简化,可与 3A/3B 并行或穿插,不依赖 A)
3E 端到端验证(最后)
```
- **3A.4(C 范围过滤)硬依赖 A**;3A.1/3A.2/3A.3 主体不依赖 A,但 3A.3 的 STRICT 校验调 3A.4,故 3A.4 须在 A 之后。
- 每阶段独立编译+真启动+测试后再进下一阶段(subagent 驱动,每任务 spec+quality review)。

## 明确不做(YAGNI / 开发期)
- 任何旧数据迁移、老新映射、版本回滚 —— 无数据,DROP 重建。
- 多租户隔离(单租户)。
- 观察的复杂审批流(先 记录→聚合→排名 主线;申诉走已有 appeal)。

## 待执行时再敲定的小决策(实现中遇到按"彻底解决"原则定,标注即可)
- Observation 是提升 ViolationRecord 还是新建收编(grep 后定,倾向统一)。
- TemplateVersion 留痕 vs 全删。
- 已锁周期的迟到观察:默认"计入下一周期"还是"需显式重开"(倾向计入下一周期 + 可手动重开)。
- (C)记一笔范围:默认 **STRICT**(只能记范围内对象)。若某项目需 PERMISSIVE(可记范围外、仅标记),作为项目级开关后补,不在首版。
- (C)TargetFilter 多关系并集 vs 交集:首版"同维度并集、跨维度(关系×类型)交集",符合用户"两种取 AND"原意;若将来需"关系也取交集"(同时满足多关系)再扩。
