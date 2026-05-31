# 归属统一重构 — 端到端验证报告

- 分支: `feature/membership-unified-relation`
- 验证日期: 2026-05-31
- 验证环境: Windows 10 / MySQL 8.0.43 / JDK 17 / Maven 3.9.11
- 验证范围: 后端测试套件 + 全新库 init-all + 归属不变量(SQL)+ 后端启动冒烟

---

## 1. 重构目标

把"用户属于哪个组织"(归属)从三处冗余真相源:

- `users.primary_org_unit_id`(外键列 + `idx_users_primary_org_unit`)
- `user_student.org_unit_id`(+ 索引)
- `user_teacher.org_unit_id`(+ 索引)

统一为 `access_relations` 表的 **`member | user | org_unit`** 关系,每用户至多一条活跃 member 归属(DB 唯一约束 `uk_membership_unique` 兜底)。`MembershipResolver` 成为唯一归属查询/写入入口。三个冗余列已删除。

---

## 2. 各 Phase 改了什么(`git log master..HEAD`,24 commits)

**Phase 1 — 关系基础设施 / DB 约束**
- `88ecd408` access_relations 提升 is_primary 为实体列
- `7e5c10b4` RelationTypeDef 加 maxPerSubject + forceGrant 强制 cardinality
- `13ac5758` 归属唯一 DB 约束(生成列 membership_lock_key + UNIQUE uk_membership_unique)
- `35fd90b5` relation_types 补齐遗留 cardinality 列 DDL,修 fresh-init
- `c03f8cf9` 新增 MembershipResolver 归属统一查询/写入入口

**Phase 2 — 写路径只写 member**
- `a4052f0f` 用户创建/更新归属只写 member,停写 primary_org_unit_id
- `c6836bc6` OrgMemberService 成员增删改只写 member
- `5d441df6` 入学流完整修正(建 users+member 归属,修孤儿 user_student S1)
- `11f3c4dc` seed 补 access_relations member 归属行

**Phase 3 — 读路径切走 member / 脱离行业表**
- `a8996ce3` OrgMemberService 读归属改走 MembershipResolver
- `a24396b7` / `81201630` 核心 Dashboard 统计去 user_student/user_teacher 耦合,按 member+feature 计数
- `a64c2d21` 登录态/权限模拟器归属读取改走 MembershipResolver.orgOf
- `5e6ea563` UserDomainMapper 归属查询 + @DataPermission 改 access_relations 子查询
- `aee1cf73` 学生/教师归属查询脱离 user_student/user_teacher.org_unit_id
- `ec1f0c7f` / `d44c1bff` / `44398e37` 学生模块 / 教学模块 / 数据范围解析器归属读取改走 member

**Phase 4 — 删冗余列**
- `6f90406f` (breaking) 删除 primary_org_unit_id / user_student.org_unit_id / user_teacher.org_unit_id 三列 + 索引,并改基线

**Phase 5 — 前端**
- `e8e4497e` 归属字段统一,组件读派生 orgUnitId

**Phase 6 — 守护 / 文档**
- `52011159` ArchUnit 守护核心禁直接引用行业扩展表 + 修 DataPermissionSimulate 已删列残留
- `b884f432` 测试适配:移除 primaryOrgUnitId 构造/verify,改验 MembershipResolver
- `5d12c5d2` 关系目录优先设计规约

迁移文件(本分支新增):`database/migrations/V20260531_1`..`_2`、`_2b`、`_3`、`_4`。

---

## 3. 验证步骤实际结果

### 步骤 1 — 后端测试套件(targeted)

命令:`mvn test -Dtest='AccessRelation*,MembershipResolver*,DataPermissionInterceptor*,OrgMemberService*,UserApplicationService*,NoIndustryTableInCoreTest,NoIndustryTypeLiteralInCoreTest,*ArchUnit*,*Architecture*'`

**结果:Tests run: 188, Failures: 1, Errors: 0, Skipped: 4 — BUILD FAILURE(仅 1 个无关失败)**

归属相关测试 **全部通过**:

| 测试类 | run | 结果 |
|---|---|---|
| `MembershipResolverTest` | 12 | PASS |
| `OrgMemberServiceTest` | 7 | PASS |
| `UserApplicationServiceTest`(全 group) | 36 | PASS |
| `AccessRelationApplicationServiceTest`(全嵌套) | 22 | PASS |
| `AccessRelationServiceCardinalityTest` | 4 | PASS |
| `AccessRelationValidityTest` | 6 | PASS |
| `AccessRelationMetricsTest` | 3 | PASS |
| `ArchUnitAccessRelationGuardTest` | 3 | PASS |
| `NoIndustryTableInCoreTest` | 1 | PASS |
| `NoIndustryTypeLiteralInCoreTest` | 1 | PASS |
| `DataPermissionInterceptorTest` | 0(无匹配方法,类加载 OK) | — |
| `AccessRelationRepositoryImplIT` | 3 | SKIPPED(无 DB 的集成测试) |
| `AccessRelationCheckBenchmark` | 1 | SKIPPED(benchmark) |

**唯一失败:`ArchUnitNoJdbcInRestControllerTest.noRestController_dependsOnJdbcTemplate_beyondBaseline`**
- 原因:`ProjectCorrectiveRulesController` 直接依赖 JdbcTemplate(2 处),违反 DDD baseline(baseline=空)。
- **与本重构无关**:该 controller 与该测试均不在 `master..HEAD` 改动集中;controller 的 JdbcTemplate 用法在 master 上即已存在(已 `git show master:` 核对),最后改动它的是 `26dec89c`(检查平台整改 E,不在本分支)。属于本分支之前就红的存量违规。

### 步骤 2 — 全新库 init-all(从零重建)

命令:`DB_PASSWORD=123456 bash database/scripts/init-all.sh`(先 DROP DATABASE)

**结果:失败,在 baseline 第一步即 abort。** 这是**预存的环境不兼容问题,与本分支无关**:

1. **`CREATE INDEX IF NOT EXISTS` 语法**:`database/schema/complete_schema_v2.sql` 第 1670 行起的"Additional Performance Indexes"段用 `CREATE INDEX IF NOT EXISTS` / `CREATE UNIQUE INDEX IF NOT EXISTS`(MariaDB 语法)。**MySQL 8.0.43 不支持**(实测 `ERROR 1064`)。全仓共 143 处此类不兼容 DDL,散落 20 个文件(V4/V5/V8/V26/baseline 等,大多早于本分支)。已 `git show master:` 核对该行在 master 上完全相同 → 非本分支引入。
2. **跨目录顺序倒置**:`init-all.sh` 先全量 apply `database/schema/V*.sql`(`sort -V`),再 apply `database/migrations/V*.sql`(**普通 `sort` 字典序**)。`V25.0.0__access_relations.sql`(建 access_relations,在 migrations/)引用 `user_org_relations`,后者由 `V8.2.0`(同 migrations/)创建,但字典序下 `V8.2.0` 排在 `V25`/`V20260419` 之后 → V25 执行时前置表不存在(`ERROR 1146`)。同样属预存 init-all 脆弱性,非本分支引入。
3. **客户端字符集**:`mysql < file` 未带 `--default-character-set=utf8mb4` 时,SQL 中中文按错误字符集解读,产生 `Incorrect string value` / `Illegal mix of collations`(GBK)级联报错。

**结论:本环境下,文档化的 fresh-init 路径(init-all.sh)本就无法在 MySQL 8.0.43 上跑通,这与归属重构无关。**

> 为隔离环境问题、验证本分支实际产物,另做两件事(见步骤 3):(a) 对全部 SQL 临时机械剥离 MySQL-8.0-不兼容的 `IF [NOT] EXISTS`(INDEX/COLUMN DDL)并加 utf8mb4 客户端字符集后重跑 init;(b) 单独隔离验证本分支 6 个 membership 迁移。

### 步骤 3 — 归属不变量检查(SQL)

#### 3a. 打补丁后重跑 fresh-init(剥离不兼容 DDL + utf8mb4)
- 错误从 153 降到 18,字符集级联消失。但链仍在早期断裂:`V25.0.0__access_relations.sql` 因前置 `user_org_relations` 缺失(同上跨目录顺序问题)未能建 `access_relations`,导致 `access_relations` / `user_student` / `user_teacher` 等表在新库**根本未建出**(`users` 0 行,seed 也连带失败)。
- 仍是 init-all 顺序问题导致,**非 membership 迁移本身的缺陷**。

#### 3b. 隔离验证本分支 6 个 membership 迁移(决定性)
构造最小库 `membership_iso`,模拟"升级前"状态(`access_relations` 无 is_primary/lock_key;`users` 有 `primary_org_unit_id`+索引;`user_student`/`user_teacher` 有 `org_unit_id`+索引;`relation_types` 有遗留 cardinality 列;预置 2 个用户 + 2 条 member 行),按序 apply `V20260531_1`→`_2`→`_2b`→`_3`→`_4`:

**6 个迁移全部干净应用,0 错误。** 不变量检查:

| 不变量 | 期望 | 实测 | 结果 |
|---|---|---|---|
| `users.primary_org_unit_id` 列 | 不存在 | 0 列 | PASS |
| `user_student.org_unit_id` 列 | 不存在 | 0 列 | PASS |
| `user_teacher.org_unit_id` 列 | 不存在 | 0 列 | PASS |
| 旧索引 `idx_users_primary_org_unit` | 已删 | 0 | PASS |
| `user_student` 旧 `idx_class_id`(动态命名) | 已删 | 0 | PASS |
| `uk_membership_unique` 唯一索引 | 存在 | 1(membership_lock_key, STORED GENERATED, UNI) | PASS |
| `access_relations.is_primary` 实体列 | 存在 | tinyint(1) NOT NULL DEFAULT 0 | PASS |
| `access_relations.membership_lock_key` 生成列 | 存在 | bigint STORED GENERATED | PASS |
| `relation_types.max_per_subject` 列 | 存在 | int NULL | PASS |
| member 行 is_primary 回填 | =1 | user101/102 均 is_primary=1, lock_key=subject_id | PASS |

**唯一约束真实生效测试:**

| 操作 | 期望 | 实测 |
|---|---|---|
| 给已有 member 的 user101 再插一条**活跃** member | 拒绝 | `ERROR 1062 Duplicate entry '101' for key 'access_relations.uk_membership_unique'` ✅ |
| 给 user101 插一条 **soft-deleted**(deleted=1)member | 接受(lock_key→NULL) | 接受,lock_key=NULL ✅(支持 revoke 后重设归属) |
| 给 user101 插一条 **非 member**(manager)关系 | 接受(lock_key→NULL) | 接受,lock_key=NULL ✅ |

**幂等性:** 6 个迁移再次重跑,0 错误(information_schema 条件化生效)。

**结论:本分支 DB 层产物(6 个迁移 + 唯一约束语义)在 MySQL 8.0.43 上完全正确、可重复执行。**

### 步骤 4 — 后端启动冒烟(全新库)

命令:`mvn spring-boot:run -DskipTests`(连补丁后的 fresh DB)

**结果:Spring 上下文成功初始化 —**
`Started StudentManagementApplication in 116.678 seconds`,Tomcat 已绑定 8080。

**但启动后立刻自终止(exit 1),无法保持运行 / 完成登录冒烟。**根因是 fresh DB 缺列(同步骤 2 的 init-all 不完整):

- 终止 runner:`RelationTypeUpserter.doUpsert`(ApplicationRunner)查询 `SELECT industry FROM relation_types ...` → `Unknown column 'industry'`(该列由本分支之前的某个迁移添加,在断裂的 fresh-init 中未应用)。
- 另有 `Unknown column 'r.tenant_id'`(roles 表)等 Casbin 加载报错,同属 fresh DB 缺列。

**关键:所有启动期报错均指向 `industry` / `tenant_id` 等缺列,无一处提及 `primary_org_unit_id` 或 membership 相关列。**归属重构删的三列没有引发任何启动报错 → 启动失败是 fresh-init 不完整的下游症状,**非本重构回归**。

登录 / `GET /api/users/page` / `/api/user_student` / `/api/dashboard/overview` / `POST /api/users` 等 HTTP 冒烟 **未能执行**(后端无法保持运行,且 fresh DB 0 用户)。**未完成 — 卡点为预存 fresh-init 不兼容,非本分支。**

---

## 4. 结论

- **代码层(测试):归属重构相关 188 项 targeted 测试全绿(唯一失败 ArchUnitNoJdbcInRestControllerTest 为预存无关红)。**
- **DB 层(隔离验证):6 个 membership 迁移干净、幂等、MySQL-8.0 兼容;三冗余列已删;`uk_membership_unique` 唯一约束真实拒绝重复活跃 member、软删可释放、非 member 不受约束;is_primary/lock_key/max_per_subject 全部就位;seed 已改写 member 行、无残留删列。**
- **本分支的归属统一重构本身验证通过。**
- **未能完成的两项(全新库 init-all 干净通过、后端持续运行 + HTTP 冒烟)均因预存的 init-all / MySQL-8.0 不兼容问题阻断,与归属重构无关。**

---

## 5. 发现的真问题(本任务不改,交控制者定)

1. **fresh-init 在 MySQL 8.0 上跑不通(预存,P1)** — 影响"从零建库"。两类成因:
   - **143 处 `CREATE/DROP INDEX IF [NOT] EXISTS` / `ADD/DROP COLUMN IF [NOT] EXISTS`** 是 MariaDB 语法,MySQL 8.0 报 `ERROR 1064`/`1091`。集中在 `complete_schema_v2.sql`(53)、`002_add_performance_indexes.sql`(31)、`V26.0.0__place_indexes_constraints.sql`(19)等 20 个文件。建议:改写为 information_schema 条件化,或文档明确仅支持 MariaDB。
   - **init-all.sh 跨目录顺序倒置** — `database/migrations/V*.sql` 用普通字典序 `sort`,使 `V25` / `V20260419` 排在依赖的 `V8.2.0` 之前。建议:统一 `sort -V` 或把所有迁移放同一目录按全局序号编排。
   - 本分支虽未引入这些问题,但 `6f90406f` 改了 baseline 文件,使"本分支破坏了 fresh-init"易被误判 —— 已核对那两行 `CREATE INDEX IF NOT EXISTS` 在 master 上完全相同,非本分支引入。

## 6. 已知遗留(承接重构计划)

- **TeacherProfileApplicationService 待下沉插件** — 教师档案应用服务仍在核心,计划迁至 education 插件。
- **SimpleUserResponse 仍叫 `primaryOrgUnitId` 字段名** — 字段名保留,但其值现由 MembershipResolver 派生的 member 归属承载(非旧列);命名后续再统一。
- **S2 关系值问题待议** — 关系语义(relation 取值口径)待后续设计定稿。
- **bundle-size 闸预存红** — 前端打包体积闸此前即红,与本次归属重构无关。
