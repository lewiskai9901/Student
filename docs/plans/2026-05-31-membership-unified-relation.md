# 组织归属统一为关系 + 通用核心去行业化 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把"用户属于哪个组织"(归属)从三处打架的表示法(`users.primary_org_unit_id` 外键 / `user_student.org_unit_id` 行业表 / `access_relations` member 关系)统一为**唯一一种** —— `access_relations` 里的 `member|user|org_unit` 关系(每用户唯一),删掉两个冗余列,核心查归属全部收口到一个薄服务,并清除通用核心对行业扩展表的直接引用。

**Architecture:** 归属 = ReBAC 关系(`member`,maxPerSubject=1)。新增 `MembershipResolver` 作为全系统唯一归属查询/写入入口(底层走已有的 `AccessRelationService`/`AccessRelationRepository`)。写路径统一为只写 member 关系(停写 FK),读路径全部改走 `MembershipResolver`,然后删 `users.primary_org_unit_id` / `user_student.org_unit_id` / `user_teacher.org_unit_id` 三列。职能关系(班主任=`admin`+metadata.role、任课=`teaches`)不动。开发期无数据兼容,seed 直接改写。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + JdbcTemplate + MySQL;前端 Vue3/TS(OpenAPI 生成类型);ArchUnit + 字面量扫描守护。

---

## 已定决策(2026-05-31 用户拍板)

1. 归属唯一真相源 = `access_relations` 的 `member|user|org_unit` 关系,**每用户唯一**(maxPerSubject=1)。
2. **删** `users.primary_org_unit_id`(外键)+ `user_student.org_unit_id` + `user_teacher.org_unit_id`(系部=主归属同一概念,用户确认)。
3. 核心查归属收口到薄服务 `MembershipResolver`,核心代码**永不引用** `user_student`/`user_teacher` 行业表。
4. Dashboard / TeacherProfile 的"数成员/数教师"改走 `MembershipResolver.countMembersByType` + feature 过滤,**不下沉插件**(保留在核心,只去行业表耦合)。
5. 职能关系不动:班主任=`admin`+`metadata.role`、任课=`teaches`、岗位=`org_positions`、班主任任职=`teacher_assignments`。
6. 设计规约:关系类型先在 Manifest `contribute()` 声明再写业务代码;CORE/system 关系 `is_system` 不可删改。新增 ArchUnit/扫描守护防回退。

## 关键审计结论(执行时的事实依据)

- **基建已就位**:`AccessRelationService.findSubjectsWithRelation` / `findActiveSubjectIds(org,member,user)` / `lookup` / 反查 `findBySubjectAndResourceType` / 幂等 `forceGrant` 全部存在。`DefaultNormalizationBasisResolver` PER_MEMBER **已经数 member 关系**(修完写路径后 S1 自动好)。`MembersOfOrgDiscovery` 已走 member。
- **三条写路径漂移**:`UserApplicationService.createUser`(双写 FK+member,一致)/ `updateUser`(只写 member 不写 FK)/ `OrgMemberService.addMember`(双写,一致)/ `EnrollmentApplicationService.registerApplication`(**只写 user_student.org_unit_id**,既不写 FK 也不写 member —— 最危险)。
- **cardinality 是纸面约束**:`RelationTypeDef.maxPerResource`(admin=1)声明了但 `forceGrant` **完全不校验**。maxPerSubject 列不存在。
- **`user_student.org_unit_id` 是重头**:`DddStudentMapper` 11+ 处查询 + `@DataPermission(orgUnitField="org_unit_id")` + `StudentPO.orgUnitId` + `BASE_JOIN_SELECT` 全依赖它。
- **`users.primary_org_unit_id` 无 DB FK 约束**,只有 `idx_users_primary_org_unit` 索引。`UserDomainMapper` 10+ 处 SQL + 4 处 `@DataPermission(orgUnitField="primary_org_unit_id")`。
- **守护抓不到**:`NoIndustryTypeLiteralInCoreTest` 只 `contains("\"STUDENT\"")` 扫双引号类型码,不扫 SQL 里的表名 `user_student` / 单引号 `'TEACHER'`。
- **唯一约束可行**:套 `inspection_appeals.pending_lock_key` 的生成列 + NULL-不参与-UNIQUE 模式(`V20260427_5`)。建议把 `isPrimary` 从 metadata JSON 提升为实体列 `is_primary TINYINT` 再做生成列(比对 JSON 路径建 STORED 生成列稳)。
- **前端**:不直接发 `primaryOrgUnitId`,只发 `orgUnitId`;`UserSelectorDialog.vue` / `OrgDetailPanel.vue` 直接读 `primaryOrgUnitId` 做"已归属"判断需改。
- **seed 受影响**:`ci_e2e_seed.sql`(users 带 primary_org_unit_id 列)、`demo_inspection_full.sql`(users + user_student + 已有 member 行)、`V26.3.0__assign_all_teachers.sql`(子查询用该列)、`V20260508_2__admin_primary_org_unit.sql`(给 admin 补该列,删后作废)。

## 执行顺序原则(保证每阶段后系统可启动)

不能"列还被读着就删列"。顺序:**先建基建 → 先统一写(让 member 关系变完整)→ 再切读 → 最后删列**。每个 Phase 结束必须:`mvn compile` 绿 + 相关单测绿 + 真启动绿(改 MetaObjectHandler/@DataPermission/MapperScan 相关时单测会骗人,**必须真启动 + 真查**,见 [[feedback_unit_test_is_not_enough]])。

---

## Phase 1: MembershipResolver 基建 + cardinality 强制(全加法,不破坏现有)

### Task 1.1: `is_primary` 提升为 access_relations 实体列

**Files:**
- Create: `database/migrations/V20260531_1__access_relations_is_primary_column.sql`
- Modify: `backend/.../infrastructure/persistence/access/AccessRelationPO.java`、`domain/access/model/entity/AccessRelation.java`

**Step 1:** 写条件化迁移(information_schema 幂等,模板见 `V104.0.0`):加 `is_primary TINYINT(1) DEFAULT 0` 列;从现有 `metadata->'$.isPrimary'` 回填(`UPDATE ... SET is_primary=1 WHERE JSON_EXTRACT(metadata,'$.isPrimary')=true`)。

**Step 2:** apply 迁移到本地库,确认列存在 + 回填正确(`SELECT relation,is_primary,COUNT(*) ... GROUP BY`)。

**Step 3:** PO/实体加 `isPrimary` 字段(`@TableField("is_primary")`)。读写 isPrimary 改用实体列,metadata 里的 isPrimary 保留兼容读但写以列为准(开发期可直接停写 metadata.isPrimary)。

**Step 4:** `mvn compile` 绿。**Commit**: `feat(access): access_relations 提升 is_primary 为实体列`

### Task 1.2: RelationTypeDef 加 maxPerSubject + forceGrant 真强制 cardinality

**Files:**
- Modify: `infrastructure/extension/RelationTypeDef.java`(加 `Integer maxPerSubject` + `withMaxPerSubject(int)`)
- Modify: `database/migrations/V20260531_2__relation_types_max_per_subject.sql`(relation_types 加 `max_per_subject INT`)
- Modify: `infrastructure/extension/RelationTypeUpserter.java`(写新列)
- Modify: `application/access/RelationTypeRegistry.java`(缓存 maxPerSubject/maxPerResource)
- Modify: `application/access/AccessRelationService.java`(`forceGrant` 加 cardinality 校验)
- Modify: `infrastructure/extension/plugins/core/CoreManifest.java`(member 关系 `.withMaxPerSubject(1)`)
- Test: `backend/.../application/access/AccessRelationServiceCardinalityTest.java`

**Step 1:** 写失败测试:`grant(member, user=U → org A)` 后再 `grant(member, user=U → org B)`,期望抛 `CardinalityViolationException`(或现有业务异常),除非先 revoke A。

**Step 2:** 跑测试确认失败(当前无校验,第二次 grant 成功)。

**Step 3:** 实现:RelationTypeDef 加字段;Registry 缓存;`forceGrant` 在幂等命中之后、INSERT 之前,查 `maxPerSubject`,若该 (subject, relation) 已有 N 条活跃关系 ≥ maxPerSubject 则按策略处理 —— **member 关系采用 grant-or-replace**(见 Task 1.3 的 `setMembership` 语义,唯一归属切换=先 revoke 旧再建新),非 member 的超限关系抛异常。同时实现 maxPerResource 强制(admin=1 顺带生效)。

**Step 4:** 跑测试绿。

**Step 5:** `mvn compile` 绿。**Commit**: `feat(access): RelationTypeDef 加 maxPerSubject + forceGrant 强制 cardinality`

### Task 1.3: 归属唯一 DB 约束(生成列 + UNIQUE)

**Files:**
- Create: `database/migrations/V20260531_3__membership_unique_constraint.sql`

**Step 1:** 写条件化迁移:加生成列
```sql
ALTER TABLE access_relations ADD COLUMN membership_lock_key BIGINT
  GENERATED ALWAYS AS (CASE WHEN relation='member' AND subject_type='user'
    AND resource_type='org_unit' AND deleted=0 THEN subject_id ELSE NULL END) STORED;
ALTER TABLE access_relations ADD UNIQUE INDEX uk_membership_unique (membership_lock_key);
```
(条件化:先查 information_schema.columns/STATISTICS 防重复 apply。)

**Step 2:** apply。先清理本地库可能存在的重复 member 行(同一 user 多个 org)再加约束,否则加索引失败。验证:插重复 member 行被 DB 拒。

**Step 3:** **Commit**: `feat(access): 归属唯一约束 — 每用户至多一条 member 归属`

### Task 1.4: 新建 MembershipResolver 服务

**Files:**
- Create: `application/organization/MembershipResolver.java`(或 `domain/access/service/`,放 application 层更合适,因要 JOIN users)
- Test: `backend/.../application/organization/MembershipResolverTest.java`

**Step 1:** 写测试覆盖核心 API(用 mock AccessRelationService/JdbcTemplate):
```java
orgOf(userId) -> Optional<Long>            // 反查唯一归属
membersOf(orgUnitId) -> List<Long>          // 直接成员
membersOfSubtree(orgUnitId) -> List<Long>   // 子树成员(org tree_path 展开 union members)
countMembers(orgUnitId) -> long
countMembersByType(orgUnitId) -> Map<String,Long>  // JOIN users.user_type_code
setMembership(userId, orgUnitId)            // grant-or-replace(唯一)
clearMembership(userId)                      // revoke member
```

**Step 2:** 跑测试失败(类不存在)。

**Step 3:** 实现:
- `orgOf` → `accessRelationService.lookup("user",userId,"member","org_unit")` 取唯一(有约束兜底)。
- `membersOf` → `findActiveSubjectIds("org_unit",orgUnitId,"member","user")`。
- `membersOfSubtree` → 先用 org tree_path/BFS 取子树 org id(复用 `OrgUnitRepository` 既有递归,**注意 H1**:必须展开子树),再 union 各 org 的 members。
- `countMembersByType` → JdbcTemplate JOIN(模板参考 `DashboardReadModel.getOrgUnitStats:157-163`):
  `SELECT u.user_type_code, COUNT(*) FROM access_relations ar JOIN users u ON ar.subject_id=u.id WHERE ar.relation='member' AND ar.resource_type='org_unit' AND ar.resource_id=? AND ar.deleted=0 AND u.deleted=0 GROUP BY u.user_type_code`(加 org scope 收窄注意 [[project_data_permission_layer_broken]] L9)。
- `setMembership` → 先 `clearMembership` 再 `forceGrant(member, is_primary=1)`(借 Task 1.2 的唯一替换)。

**Step 4:** 跑测试绿。

**Step 5:** `mvn compile` + 真启动绿。**Commit**: `feat(organization): 新增 MembershipResolver 归属统一查询入口`

---

## Phase 2: 统一写路径为只写 member(让 member 关系变完整)

> 目标:此 Phase 后,所有"分配归属"都只写 member 关系,`primary_org_unit_id`/`user_student.org_unit_id` 不再被写入(列还在但变成死列,下个 Phase 切读后删)。

### Task 2.1: createUser/updateUser 改只写 member

**Files:** Modify `application/user/UserApplicationService.java`(`:124` setPrimaryOrgUnitId 删、`:146-157` 保留 member 写;`:240-274` updateUser 改走 `membershipResolver.setMembership`)

**Step 1:** createUser:删 `user.setPrimaryOrgUnitId(...)` 及对应 PO 写;归属改 `membershipResolver.setMembership(userId, orgUnitId)`。updateUser:归属变更走 `setMembership`(自动 revoke 旧 member 建新)。

**Step 2:** 真启动 + 真建用户:`POST /api/users` 带 orgUnitId → 验证 access_relations 出现 member 行 + `users.primary_org_unit_id` 不再被写(NULL)。

**Step 3:** **Commit**: `refactor(user): 用户归属只写 member 关系,停写 primary_org_unit_id`

### Task 2.2: OrgMemberService 改只写 member

**Files:** Modify `application/organization/OrgMemberService.java`(`:122` setPrimaryOrgUnitId 删、`:159`/`:178` clearFK 删,addMember/removeMember/endAll 改走 MembershipResolver)

**Step 1:** addMember → `membershipResolver.setMembership`;removeMember → `clearMembership`;endAllByOrgUnitId → 批量 revoke member(保留 access_relations 删除)。删三处 FK 写。

**Step 2:** 真启动 + 组织成员增删 API 验证 member 行变化,FK 不动。

**Step 3:** **Commit**: `refactor(organization): OrgMemberService 归属只写 member 关系`

### Task 2.3: 入学流补 member 关系(修第三条漂移写路径)

**Files:** Modify `infrastructure/extension/plugins/education/application/student/EnrollmentApplicationService.java:387-399`

**Step 1:** `registerApplication` 插 user_student 后(暂保留 org_unit_id 写,Phase 4 删列时再去),**追加** `membershipResolver.setMembership(newUserId, orgUnitId)` —— 学生归属进 member 关系。注意:这里要拿到 user_student 对应的 users.id(确认入学是否创建 users 行;若学生只有 user_student 无 users 行,则先补建 users 行——**需在执行时确认 user_student 与 users 的关系**)。

**Step 2:** 真启动 + 走一次入学 → 验证学生出现 member 关系 + PER_MEMBER 归一化对该班不再为 0(顺带验证 S1 修复)。

**Step 3:** **Commit**: `fix(education): 入学流补写 member 归属关系,修 PER_MEMBER 归一化漏算`

### Task 2.4: 改 seed 写 member 行

**Files:** Modify `database/init/ci_e2e_seed.sql`、`database/seeds/demo_inspection_full.sql`;作废 `V26.3.0`/`V20260508_2`(改为注释说明或转写 member)

**Step 1:** ci_e2e_seed:users INSERT 去掉 `primary_org_unit_id` 列值(Phase 4 删列前先停止依赖),改为追加 access_relations member INSERT(每个有归属的 user 一条 `(org_unit, orgId, member, user, userId, is_primary=1)`)。demo_inspection_full 同理(它已有 member 行,确保去 FK 后仍完整;user_student 的 org_unit_id 转 member)。

**Step 2:** `bash database/scripts/init-all.sh` 重建库,验证启动 + 成员统计/数据权限正常。

**Step 3:** **Commit**: `chore(seed): 归属种子改写 access_relations member 行`

---

## Phase 3: 切读路径到 MembershipResolver(列变完全无引用)

### Task 3.1: OrgMemberService 读改 MembershipResolver

**Files:** Modify `OrgMemberService.java:43-107`(getBelongingMembers/getMembersRecursive/getOrgStatistics)

**Step 1:** 三个读方法改走 `membershipResolver.membersOf/membersOfSubtree/countMembersByType`。

**Step 2:** 真启动 + 组织详情/统计 API 数值与改前一致。

**Step 3:** **Commit**: `refactor(organization): OrgMemberService 读归属改走 MembershipResolver`

### Task 3.2: Dashboard 三件套 + TeacherProfile 去行业表耦合

**Files:** Modify `application/dashboard/DashboardOverviewQueryService.java:104-108`、`infrastructure/query/DashboardReadModel.java`(getOrgUnitStats 等)、`application/my/MyDashboardQueryService.java`、`application/organization/TeacherProfileApplicationService.java:67-128`

**Step 1:** 把 `FROM user_student`/`FROM user_teacher` 的"数成员/数教师/数学生"改走 `membershipResolver.countMembersByType(orgId)` + feature/user_type 过滤(数学生=`countMembersByType` 取 STUDENT 桶或 `hasFeature("isLearner")`;数教师=TEACHER 桶或 `canTeach`)。**TeacherProfile 的纯教师档案属性查询**(电话/职称等非归属字段)若必须读 user_teacher,该 service 应下沉教育插件 —— 执行时判断:若 TeacherProfileApplicationService 只做教师档案 CRUD(行业概念),整体迁 `plugins/education`;若混了通用统计,拆分。**需执行时确认**。

**Step 2:** 真启动 + dashboard/我的首页/教师统计数值正确,grep 确认核心这些文件不再出现 `user_student`/`user_teacher` 表名。

**Step 3:** **Commit**: `refactor(dashboard): 核心统计去 user_student/user_teacher 耦合,走 MembershipResolver`

### Task 3.3: OrgUnitJdbc impact + CustomUserDetailsService + DataPermissionSimulate 切读

**Files:** Modify `application/organization/OrgUnitJdbcApplicationService.java:62-88`、`security/CustomUserDetailsService.java:128`、`application/access/DataPermissionSimulateApplicationService.java:33`、`interfaces/rest/access/DataPermissionSimulateController.java:112,120-121`

**Step 1:**
- OrgUnitJdbc impact:`FROM user_student` + `primary_org_unit_id` 数 → `membershipResolver.countMembersByType`。
- CustomUserDetailsService:登录取用户 orgUnitId 填 UserContext → `membershipResolver.orgOf(userId)`。
- DataPermissionSimulate:DEPT scope 取 user org → `orgOf`;Controller 里 `case "student"` 硬编码 ModuleMeta(`user_student`,`org_unit_id`)→ 由插件注册(借 `PluginDataScopeRouter` 维度注册机制),核心不写 student。

**Step 2:** 真启动 + 登录(UserContext orgUnitId 正确)+ 删组织 impact + 数据权限模拟器正常。

**Step 3:** **Commit**: `refactor(core): 登录态/impact/权限模拟器归属读取改走 MembershipResolver`

### Task 3.4: UserDomainMapper 用户列表/统计 + @DataPermission 改 access_relations 子查询

**Files:** Modify `infrastructure/persistence/user/UserDomainMapper.java`(10+ 处)、`infrastructure/access/DataPermissionInterceptor.java`

**Step 1(最关键最难):** 用户列表/计数 SQL 里 `JOIN/WHERE primary_org_unit_id` → 改为 JOIN access_relations member 取归属 org(列表仍返回 `orgUnitId` 派生字段,保持前端契约)。`@DataPermission(orgUnitField="primary_org_unit_id")` 失效 —— users 表归属现在在关系表,数据权限注入需改:要么 DataPermissionInterceptor 对 user 资源支持"membership 子查询模式"(`WHERE id IN (SELECT subject_id FROM access_relations WHERE relation='member' AND resource_type='org_unit' AND resource_id IN (:scope) AND deleted=0)`),要么给 `@DataPermission` 加 `viaMembership=true` 标志走子查询分支。**实现 membership 子查询注入分支**,user 资源用之。

**Step 2:** 真启动 + **真查 MySQL general log 验证**(见 [[project_data_permission_layer_broken]]):teacher02 BY_CLASS 登录看到的用户数被 access_relations 子查询正确收窄(对照 admin 全量)。这是数据权限路径,单测会骗人,必须真启动真查。

**Step 3:** **Commit**: `refactor(user): UserDomainMapper 归属查询 + @DataPermission 改 access_relations 子查询`

### Task 3.5: DddStudentMapper / StudentPO 切读 member(行业重头)

**Files:** Modify `infrastructure/extension/plugins/education/infrastructure/persistence/student/DddStudentMapper.java`(11+ 处)、`StudentPO.java`、`ClassPlugin.java:60`、`MajorDataScopeResolver.java:76-99`

**Step 1:** 所有 `user_student.org_unit_id` 的查询/过滤改为 JOIN access_relations member 取学生归属 org(`BASE_JOIN_SELECT` 加 `LEFT JOIN access_relations ar ON ar.subject_id=us.user_id AND ar.relation='member' AND ar.resource_type='org_unit' AND ar.deleted=0` 取 `ar.resource_id AS org_unit_id`)。`@DataPermission(orgUnitField="org_unit_id")` 同样改 membership 子查询(复用 Task 3.4 的分支)。ClassPlugin 数班级学生 → `membershipResolver.countMembersByType` 取 STUDENT。MajorDataScopeResolver 的 `user_teacher.org_unit_id` JOIN → 走 member。

**Step 2:** 真启动 + 学生列表/班级学生数/专业数据权限验证(MySQL general log 验数据权限收窄)。

**Step 3:** **Commit**: `refactor(education): 学生归属查询改走 access_relations member,脱离 user_student.org_unit_id`

---

## Phase 4: 删冗余列 + DB 收尾

### Task 4.1: 删三列 + 索引

**Files:** Create `database/migrations/V20260531_4__drop_redundant_membership_columns.sql`;Modify `domain/user/model/aggregate/User.java`(删 `primaryOrgUnitId` 字段 + `validateAgainstType` 的 requiresOrg 改查 MembershipResolver/或移到应用层)、`infrastructure/persistence/user/UserPO.java`、`StudentPO.java`、行业表建表 SQL

**Step 1:** 先 grep 全仓确认 `primary_org_unit_id`/`primaryOrgUnitId`/`user_student.*org_unit_id`/`user_teacher.*org_unit_id` 在 main 代码已零引用(测试除外)。

**Step 2:** 条件化迁移:DROP `idx_users_primary_org_unit` → DROP `users.primary_org_unit_id`;DROP `user_student.org_unit_id`(+其索引);DROP `user_teacher.org_unit_id`(+其索引)。**同步改建表基线**(`complete_schema_v2.sql` 等)——按 [[开发原则]] 彻底删列连建表 SQL 一起改。

**Step 3:** User 聚合删字段;requiresOrg 校验(原 `User.validateAgainstType:189`)移到 UserApplicationService(创建时校验 setMembership 必填),User 模型不再持有归属。PO 删字段。

**Step 4:** `bash database/scripts/init-all.sh` 全新库重建 + `mvn compile` + 真启动绿。

**Step 5:** **Commit**: `feat(db)!: 删除 primary_org_unit_id / user_student.org_unit_id / user_teacher.org_unit_id 冗余归属列`

### Task 4.2: 修测试

**Files:** Modify `application/user/UserApplicationServiceTest.java:105,126,723`、`application/place/UniversalPlaceCheckInTest.java:98`、其他构造时传 primaryOrgUnitId 的测试

**Step 1:** 测试里 user 构造去掉 primaryOrgUnitId 参数;归属断言改查 MembershipResolver/access_relations。

**Step 2:** `mvn test` 相关模块绿。

**Step 3:** **Commit**: `test: 适配归属统一,移除 primaryOrgUnitId 构造`

---

## Phase 5: 前端

### Task 5.1: 类型重生成 + 组件归属判断改派生字段

**Files:** Modify `frontend/src/types/user.ts:97`、`types/position.ts:101`、`components/common/UserSelectorDialog.vue:72,188-189`、`views/organization/structure/components/OrgDetailPanel.vue:461,590`;regen `api-generated/types.gen.ts`

**Step 1:** 后端用户 DTO 改了字段后 `npm run` openapi 重生成(参考 [[project_openapi_sdk_integration_gotchas]])。后端列表继续返回 `orgUnitId`(派生),前端表单仍发 `orgUnitId` —— 表单基本不动。`UserSelectorDialog`/`OrgDetailPanel` 里 `user.primaryOrgUnitId` 的"已归属/未归属"判断改读后端返回的派生 `orgUnitId` 字段。

**Step 2:** `npm run type-check`(baseline 0,见 [[project_aplus_ratchet_gates]])+ `npm run build` 绿。

**Step 3:** **Commit**: `refactor(frontend): 归属字段统一,组件读派生 orgUnitId`

---

## Phase 6: 守护 + 端到端验证

### Task 6.1: 新增"核心禁引用行业扩展表"守护

**Files:** Modify/Create `backend/.../architecture/NoIndustryTableInCoreTest.java`

**Step 1:** 写守护:扫核心包(`application/**`、`domain/**`、`infrastructure/**` 排除 `/plugins/`、`/test/`)的 .java 源,禁止出现行业扩展表名字面量 `user_student`/`user_teacher`/`user_counselor`(作为 SQL 表名,即 `FROM user_student`/`JOIN user_student`)。同时放宽 `NoIndustryTypeLiteralInCoreTest` 检测,纳入单引号类型码(`'STUDENT'`/`'TEACHER'`)。允许白名单:注释/Javadoc 里的说明(EntityTypePlugin 守则、Contribution 契约文档)—— 用"非注释行 + 含 `FROM `/`JOIN ` + 表名"的判定降误报。

**Step 2:** 跑守护:期望绿(Phase 3 已清完核心真泄漏)。若红,补清剩余。

**Step 3:** **Commit**: `test(arch): 守护核心禁直接引用行业扩展表 + 单引号类型码`

### Task 6.2: 关系目录设计规约文档 + Manifest 声明守护

**Files:** Create `docs/design/relation-catalog-discipline.md`;可选 Modify ArchUnit 守护"新关系必须 Manifest 声明 + CORE is_system 不可删"

**Step 1:** 写规约:① 写业务代码前先在 Manifest `contribute()` 声明 RelationTypeDef(三元签名 + cardinality);② CORE/system 关系 `is_system=true` 不可删改;③ 归属=member(maxPerSubject=1),职能关系另立;④ 核心查归属一律走 MembershipResolver,禁裸 SQL。

**Step 2:** **Commit**: `docs(access): 关系目录优先设计规约`

### Task 6.3: 全链路端到端验证

**Step 1:** 全新库 `init-all.sh` → 真启动。验证清单:
- [ ] 创建用户带归属 → access_relations member 行 + 无 primary_org_unit_id(列已不存在)
- [ ] 用户改归属 → 旧 member revoke + 新 member,唯一约束生效(手动插重复 member 被 DB 拒)
- [ ] 入学一个学生 → member 行 + 该班 PER_MEMBER 归一化分母正确(S1 验证)
- [ ] 组织成员列表/统计/dashboard 数值正确
- [ ] teacher02 BY_CLASS 数据权限:用户列表/学生列表正确收窄(MySQL general log 验子查询)
- [ ] 班级学生数、专业数据权限正确
- [ ] 前端 type-check 0 + build 绿;用户表单选归属能存能回显
- [ ] `NoIndustryTableInCoreTest` + 全部 ArchUnit 绿
- [ ] 全部 inspection/access 单测绿

**Step 2:** 写验证报告 `docs/reports/membership-unified-relation-result.md`。**Commit**: `docs: 归属统一端到端验证报告`

---

## 明确不做(YAGNI)

- **多重归属**(一人跨多组织):本次归属保持唯一。将来需要时开 `affiliated_with`(不带唯一约束)新关系,不改 member。
- **Dashboard 卡片下沉插件**:本次只去行业表耦合(路由 MembershipResolver),不做 dashboard 贡献机制(用户选项 1)。
- **职能关系重构**:班主任(`admin`+metadata.role)、任课(`teaches`)、岗位、teacher_assignments 不动。
- **多租户**:沿用单租户(memory 核心认知)。
- **Inspection extractor 的 studentId JSON key**:属模板控件契约,归到 [[project_template_full_impl]] P3 通用化处理,不在本次。

## 风险与回退

- **数据权限子查询性能**:user/student 列表带 membership 子查询,确保 access_relations 索引 `idx_lookup`/`idx_subject` 命中;单租户量级可接受。
- **执行时需确认项**(审计标注):① user_student 是否有独立 users 行(Task 2.3);② TeacherProfileApplicationService 是否整体下沉(Task 3.2);③ MySQL 版本对生成列支持(Task 1.3,已用实体列规避 JSON 路径风险)。
- 每 Phase 独立可启动,出问题回退到上一个 Phase 的 commit。
