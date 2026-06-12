# 关系目录优先设计规约 (Relation-Catalog-First Discipline)

> 状态:生效中(2026-05-31 组织归属统一重构确立; 2026-06-13 场所归属关系化收官 — 全实体归属规约)。配套守护:`NoIndustryTableInCoreTest`、`NoIndustryTypeLiteralInCoreTest`、`PlaceDataPermissionOrgFieldTest`。

本平台用户/组织/场所之间的一切连接都用统一关系表 `access_relations`(Zanzibar ReBAC)表达。为避免"归属表示法分裂"(本次重构修复的根因:`users.primary_org_unit_id` 外键 / `user_student.org_unit_id` 行业列 / `member` 关系三处打架),确立以下规约。

## 1. 归属 = `member` 关系,唯一

- **组织归属(用户属于哪个组织)= `access_relations` 的 `member|user|org_unit` 关系**,每用户至多一条(`RelationTypeDef.maxPerSubject=1`,`forceGrant` 强制 + DB 生成列 `uk_membership_unique` 唯一索引兜底)。
- **不再有任何外键/行业列表示归属**。`users.primary_org_unit_id`、`user_student.org_unit_id`、`user_teacher.org_unit_id` 已删除。
- 学生、教师、任何用户的归属一视同仁,都是 `member` 关系;身份差异由**用户类型**(`entity_type_configs`,USER 维度,feature 如 `isLearner`/`canTeach`)表达,不靠归属关系区分。

## 2. 归属 vs 职能关系

| 语义 | 关系 | 说明 |
|---|---|---|
| 我**属于**哪个组织 | `member`(唯一) | 成员身份。一人一主归属。 |
| 我对某组织承担**职能** | `admin`(+`metadata.role`)/ `teaches` / 岗位等 | 班主任=`admin`+role、任课=`teaches`;可多条、可跨组织。**不是归属**。 |

"班主任不属于这个班,而是对这个班承担班主任职能" —— 职能关系永远走独立关系类型,不挤占 `member`。
将来若需"次要挂靠"(一人跨多组织),开一个**不带唯一约束**的新关系(如 `affiliated_with`),`member` 保持唯一。

## 2.5 场所归属 = `belongs_to` 关系 (覆盖点) + 投影列 (2026-06-13)

- **场所归属(场所属于哪个组织)= `access_relations` 的 `belongs_to|place|org_unit` 关系**,
  每场所至多一条(`maxPerSubject=1`,DB 生成列 `uk_place_belongs_unique` 兜底)。
- **覆盖点模型**:关系只存显式绑定;无关系 = 沿**场所树**继承父场所(楼栋绑组织,房间自动跟随)。
- **投影列** `places.effective_org_unit_id`:解析后的有效组织(含继承),供查询/数据权限
  快路径过滤 —— 同 `org_units.tree_path` 的"真相+物化"模式。**唯一属主是
  `PlaceOrgProjector`**(监听 belongs_to 关系事件同事务重算子树;场所移动/新建由
  ApplicationService 显式触发),业务代码禁直写(PO `FieldStrategy.NEVER` 兜底)。
- **场所责任人 = `responsible_for|user|place` 关系**(覆盖点,每场所至多一人
  `maxPerResource=1`,`uk_place_responsible_unique` 兜底),无投影列,读时沿场所树上溯解析。
- **`admin|user|place` 与 `responsible_for|user|place` 职责不同,并存**:
  `admin`=场所**管理权**(权限语义,关系管理页"场所管理员"场景);`responsible_for`=
  业务**问责**(场所表单"负责人"字段,参与继承解析)。不要合并,不要混用。
- **belongs_to 不支持 `valid_to` 时效**:投影器无法感知过期,投影读关系时忽略时效字段。
- **核心读写场所归属只走 `PlaceOrgResolver`**(`orgOf`/`setBelonging`/`clearBelonging`/
  `responsibleOf`/`setResponsible`/`clearResponsible`/批量 `overridesFor`),对称
  `MembershipResolver`。API/DTO 中 `orgUnitId`/`responsibleUserId` 语义 = **显式覆盖点**,
  有效值读 `effectiveOrgUnitId` —— 前端表单以覆盖点是否为空判断"显式 vs 继承",勿混。

## 3. 核心查归属:只走 `MembershipResolver`

- `application/organization/MembershipResolver` 是**全系统唯一的归属查询/写入入口**:`orgOf` / `membersOf` / `membersOfSubtree` / `countMembers` / `countMembersByType` / `countMembersByFeature` / `countUsersByFeature` / `setMembership`(grant-or-replace 唯一)/ `clearMembership`。
- **通用核心代码禁止**:① 直接 `FROM/JOIN user_student / user_teacher`(行业扩展表);② 裸 SQL 查归属;③ 出现行业类型码字面量(`"STUDENT"`/`'TEACHER'` 等)。改用 MembershipResolver + 用户类型 feature。
- 全局总数(系统有多少某类型用户)用 `countUsersByFeature`(不要求归属);组织范围内成员数用 `countMembers*`(member 关系)。

## 4. 写业务代码前先设计关系目录

- **任何新业务关系,先在对应 `PluginPackage` 的 `contribute()` 里声明 `RelationTypeDef`**(三元签名 `(subjectType, relation, objectType)` + cardinality + implied),由 `RelationTypeUpserter` 启动 UPSERT 进 `relation_types`。不要在业务代码里裸用未声明的 relation 字符串。
- **CORE/system 关系不可删改**:`is_system=true`,`RelationTypeUpserter` 保护;管理员自定义关系标 `industry=CUSTOM`,不被插件覆盖。
- 核心关系在 `CoreManifest`;行业关系在对应行业 Manifest(如 `EducationManifest`)。

## 5. 守护

- `NoIndustryTableInCoreTest`:核心路径(application/domain/infrastructure/interfaces,排除 plugins/test/architecture)禁 `FROM/JOIN user_student|user_teacher|user_counselor`。已知豁免:`TeacherProfileApplicationService`(整服务应下沉 education 插件的既有债,`LEGACY_EXEMPT_FILES` 标注,防新增)。
- `NoIndustryTypeLiteralInCoreTest`:核心禁行业类型码字面量(双引号 `"STUDENT"` + 单引号 SQL `'TEACHER'` 等)。

## 6. 反模式速记

- ❌ 在核心写 `FROM user_student` / `JOIN user_teacher`
- ❌ 用外键/行业列存归属(已彻底删除,勿复活 — 用户与场所归属同此纪律)
- ❌ 业务代码直写 `places.effective_org_unit_id`(投影列,属主是 PlaceOrgProjector)
- ❌ 把 DTO 的 `orgUnitId`(覆盖点)和 `effectiveOrgUnitId`(投影)混为一谈
- ❌ 用 `member` 关系表达班主任/任课(那是职能关系)
- ❌ 裸 SQL 查归属(走 MembershipResolver)
- ❌ 业务代码裸用未在 Manifest 声明的 relation 字符串
- ✅ 归属=member(唯一)、职能=独立关系、查询走 MembershipResolver、关系先声明后使用
