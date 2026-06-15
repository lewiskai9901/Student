# 可组合数据范围 M1（模型 + READ 全链路）实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把扁平 `DataScope` 枚举重建为正交可组合三轴的 `ScopeSpec`，READ 路径端到端落地（存储 / 解析 / 拦截器 compose / 能力声明 / 前端范围生成器），行为对既有 BOTH-seed 角色零回归。

**Architecture:** `role_data_scopes` 拆成结构化列（apply_to + 组织锚点 + 关系过滤 + 类型过滤）；`ScopeEvaluator.toSqlCondition(spec)` 围绕单一 `resolveOrgSet` 把三轴 compose 成 WHERE 片段，`DataPermissionInterceptor` 改为薄壳调用它（消除重复关系子查询）；老 7 枚举降级为 `ScopePreset`（UI 映射）。WRITE 执行（闸3）留 M2，本阶段 seed 全 BOTH。

**Tech Stack:** Spring Boot 3.2 + MyBatis(拦截器/JdbcTemplate) + MySQL；Vue3 + TS + Element Plus。后端 JUnit5/Mockito，语义以真库 SQL 对照 + API 往返兜底。

**约定：** 无数据兼容（直接改 baseline + apply）；分支 `feature/composable-data-scope`；每任务独立提交。重启后端命令见 CLAUDE.md。验证 DB：`MYSQL_PWD=123456 mysql -u root student_management`。

---

## 基线快照（T0，必须先做）

重构是安全内核，需"重构前后零回归"基准。

**Step 1** 取所有 BOTH-seed 角色当前可见集快照（重构后逐行比对）：
```bash
MYSQL_PWD=123456 mysql -u root student_management -N -e "
SELECT role_id, resource_code, scope_type, custom_org_unit_ids, type_filter
FROM role_data_scopes WHERE deleted=0 ORDER BY role_id, resource_code" > /tmp/scope_baseline_rows.txt
wc -l /tmp/scope_baseline_rows.txt
```
**Step 2** 记录 3 个代表角色（SUPER_ADMIN 跳过 / SCHOOL_ADMIN / 一个 SELF 角色）登录后 `GET /users/page` + `/students` 的 total，存 `/tmp/scope_baseline_counts.txt`（脚本见 §验证附录）。
**Step 3** commit 空标记：`git commit --allow-empty -m "chore: M1 baseline snapshot captured"`

---

## Task 1: OrgAnchor 枚举

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/access/model/OrgAnchor.java`
- Test: `backend/src/test/java/com/school/management/domain/access/model/OrgAnchorTest.java`

**Step 1: 失败测试**
```java
@Test void fromCode_parsesAll() {
  assertThat(OrgAnchor.fromCode("RELATION")).isEqualTo(OrgAnchor.RELATION);
  assertThat(OrgAnchor.fromCode("nope")).isNull();
}
```
**Step 2:** `mvn test -Dtest=OrgAnchorTest` → FAIL（类不存在）。
**Step 3: 实现**
```java
public enum OrgAnchor {
  ALL, SELF, PRIMARY_ORG, RELATION, CUSTOM_ORG, PLUGIN_DIM;
  public static OrgAnchor fromCode(String c){
    if(c==null) return null;
    for(OrgAnchor a:values()) if(a.name().equals(c)) return a;
    return null;
  }
}
```
**Step 4:** test PASS。**Step 5:** commit `feat(access): OrgAnchor 枚举(轴①真相)`。

---

## Task 2: ScopeSpec 值对象

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/access/model/valueobject/ScopeSpec.java`
- Test: `.../valueobject/ScopeSpecTest.java`

**Step 1: 失败测试** — 覆盖 builder + 便捷判定：
```java
@Test void allAnchor_isUnbounded() {
  ScopeSpec s = ScopeSpec.builder().orgAnchor(OrgAnchor.ALL).applyTo("BOTH").build();
  assertThat(s.isOrgUnbounded()).isTrue();
  assertThat(s.hasTypeFilter()).isFalse();
}
@Test void relationAnchor_carriesParam() {
  ScopeSpec s = ScopeSpec.builder().orgAnchor(OrgAnchor.RELATION).anchorParam("admin").build();
  assertThat(s.getAnchorParam()).isEqualTo("admin");
}
```
**Step 2:** FAIL。
**Step 3: 实现** — Lombok `@Data @Builder`，字段：`applyTo, orgAnchor, anchorParam, includeSubtree(boolean), customOrgIds(Set<Long>), subjectRelInclude(Set<String>), subjectRelExclude(Set<String>), typeFilter(Set<String>)`；便捷方法 `isOrgUnbounded()=orgAnchor==ALL`、`hasTypeFilter()/hasRelInclude()/hasRelExclude()=集合非空`。
**Step 4:** PASS。**Step 5:** commit `feat(access): ScopeSpec 值对象(三轴+apply_to)`。

---

## Task 3: ScopePreset 目录（老枚举降级映射）

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/access/model/ScopePreset.java`
- Test: `.../model/ScopePresetTest.java`

**Step 1: 失败测试** — 预设→Spec 映射（§2 表）：
```java
@Test void managedOrgs_mapsToRelationAdmin() {
  ScopeSpec s = ScopePreset.MANAGED_ORGS.toSpec();
  assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.RELATION);
  assertThat(s.getAnchorParam()).isEqualTo("admin");
  assertThat(s.isIncludeSubtree()).isFalse();
}
@Test void deptAndBelow_mapsToPrimaryOrgSubtree() {
  ScopeSpec s = ScopePreset.DEPARTMENT_AND_BELOW.toSpec();
  assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.PRIMARY_ORG);
  assertThat(s.isIncludeSubtree()).isTrue();
}
```
**Step 2:** FAIL。
**Step 3: 实现** enum，每值带 `(displayName, level, orgAnchor, anchorParam, subtree)` + `toSpec()`；含 §2 全部 8 预设。提供 `fromLegacyScopeType(String)`（旧 scope_type 字符串→预设，供 seed 翻译 + 兼容读取）。
**Step 4:** PASS。**Step 5:** commit `feat(access): ScopePreset 目录 + 旧 scope_type 翻译`。

---

## Task 4: DB 重建 `role_data_scopes` + seed 翻译

**Files:**
- Modify: `database/schema/baseline_v3.sql`（CREATE TABLE 列 + INSERT 翻译）
- Create: `database/migrations/post-v3/V20260615_2__data_scope_composable.sql`
- 直接 apply 到 live DB

**Step 1:** 写 migration（live DB 追上）：`ALTER TABLE role_data_scopes` 加列 `apply_to/org_anchor/anchor_param/include_subtree/custom_org_ids/subject_rel_include/subject_rel_exclude`（`type_filter` 已存在）；用一段 `UPDATE` 按旧 `scope_type` 翻译填新列（ALL→ALL、DEPARTMENT→PRIMARY_ORG、DEPARTMENT_AND_BELOW→PRIMARY_ORG+subtree、MANAGED_ORGS→RELATION/admin、MANAGED_ORGS_AND_BELOW→RELATION/admin+subtree、SELF→SELF、CUSTOM→CUSTOM_ORG 且 custom_org_ids=custom_org_unit_ids、BY_*→PLUGIN_DIM+anchor_param=scope_type）；`apply_to` 全 'BOTH'；改唯一键为 `(role_id,resource_code,apply_to,tenant_id)`。末尾 `-- DROP COLUMN scope_type, custom_org_unit_ids`（确认代码不再读后在 Task 9 删）。
**Step 2:** apply：`mysql ... < V20260615_2__...sql`，然后
```bash
MYSQL_PWD=123456 mysql -u root student_management -e \
"SELECT scope_type,org_anchor,anchor_param,include_subtree FROM role_data_scopes WHERE deleted=0 GROUP BY 1,2,3,4"
```
Expected: 每个旧 scope_type 都翻译到正确 anchor 元组（人工核对 §2 表）。
**Step 3:** 同步 baseline_v3.sql（CREATE TABLE 新列 + INSERT 显式列列表，与 2b 同手法；seed 行直接写新列值，不带 scope_type）。
**Step 4:** 验证 baseline 可建：`mysql 临时库 < baseline_v3.sql` 成功。
**Step 5:** commit `feat(access)!: role_data_scopes 重建为可组合三轴列 + seed 翻译`。

---

## Task 5: PolicyService 读/存新列 → ScopeSpec

**Files:**
- Modify: `backend/.../infrastructure/access/DataPermissionPolicyService.java`
- Modify: `backend/.../domain/access/model/entity/RoleDataPermission.java`（字段对齐 spec 或直接返回 ScopeSpec）
- Test: `backend/src/test/.../DataPermissionPolicyServiceTest.java`（如无则建）

**Step 1: 失败测试** — mock JdbcTemplate 返回一行新列，断言 `getScopeSpec(tenant,role,resource,READ)` 还原出正确 ScopeSpec（orgAnchor=RELATION/admin/subtree=true/typeFilter=[STUDENT]）。
**Step 2:** FAIL。
**Step 3: 实现** 新方法 `ScopeSpec getScopeSpec(tenantId, roleId, resourceCode, actionClass)`：SQL 选新列，`actionClass` 取 apply_to ∈ {该类, BOTH}（READ→'READ'/'BOTH'，WRITE→'WRITE'/'BOTH'），解析 JSON 集合 → ScopeSpec。保留 `getScopeCodeForRole/getMergedScope` 暂时改为 thin-delegate 或标记 `@Deprecated`（Task 8 清理调用方后删）。`saveRolePermission`/`saveRolePermissions` 改写新列（接收 ScopeSpec 或 RoleDataPermission 携带三轴）。
**Step 4:** PASS。**Step 5:** commit `feat(access): PolicyService 读写可组合 ScopeSpec`。

---

## Task 6: ScopeEvaluator.toSqlCondition（compose 管线）

**Files:**
- Create: `backend/.../infrastructure/access/ScopeEvaluator.java`（持 resolveOrgSet + toSqlCondition）
- Test: `backend/src/test/.../ScopeEvaluatorTest.java`

**Step 1: 失败测试** — 给定 ScopeSpec + 资源元数据（orgField/viaMembership/typeField/alias）+ UserContext，断言产出的 SQL 片段字符串与参数序列（§3）。覆盖：
- PRIMARY_ORG+subtree（org 字段路径）
- RELATION:admin（成员路径，含 admin 子查询）
- RELATION:admin + subjectRelExclude=[admin]（成员路径 + `AND id NOT IN(...)`）
- + typeFilter=[STUDENT]（末尾 `AND alias.user_type_code IN (?)`）
- ALL + typeFilter（仅类型谓词）
- 空/`1=0` 边界

```java
@Test void relationAnchor_membership_withExcludeAndType() {
  ScopeSpec s = ScopeSpec.builder().orgAnchor(RELATION).anchorParam("admin")
     .subjectRelExclude(Set.of("admin")).typeFilter(Set.of("STUDENT")).build();
  Cond c = evaluator.toSqlCondition(s, userResourceMeta(), ctx, 0);
  assertThat(c.sql).contains("ar.subject_id").contains("relation = 'admin'")
     .contains("NOT IN").contains("user_type_code IN");
}
```
**Step 2:** FAIL。
**Step 3: 实现** —— 把 §3 管线从拦截器现有 `buildSingleRoleCondition/buildMembershipCondition/buildAccessRelationCondition/buildPluginDimCondition/applyTypeFilter` **迁移并重构**进 ScopeEvaluator：`resolveOrgSet(spec)` 单一产 orgSet 子查询；`subjectSelect` 按资源路径消费；`subjectRelFilter`（include/exclude，仅成员型）；`typeFilter`（复用 2b）。参数按 orgSet→②→③ 顺序，保位置绑定。
**Step 4:** PASS（全部 case）。**Step 5:** commit `feat(access): ScopeEvaluator.toSqlCondition 统一 compose 管线`。

---

## Task 7: DataPermissionInterceptor 改用 ScopeEvaluator

**Files:**
- Modify: `backend/.../infrastructure/access/DataPermissionInterceptor.java`
- Modify: `.../DataPermissionInterceptorTest.java`（适配；保留 SQL 注入/参数位置/alias-strip/分页占位等仍归拦截器的测试）

**Step 1:** 适配/补测试：`buildScopedCondition` 现在对每个 scopedRole 取 `getScopeSpec(...,READ)`（SELECT）→ `scopeEvaluator.toSqlCondition` → OR 合并；断言多角色 OR、ALL 短路、deny `1=0`。
**Step 2:** FAIL（拦截器尚未改）。
**Step 3: 实现** —— 删拦截器内 build* 私有方法（搬去 ScopeEvaluator），`buildScopedCondition` 瘦成：解析每角色 spec → 调 evaluator → 合并；SQL 注入/参数映射/alias 处理保留。actionClass 由 `mappedStatement.getSqlCommandType()` 推（SELECT→READ，UPDATE/DELETE→WRITE，本阶段都映射到 BOTH 故等价）。
**Step 4:** PASS（拦截器单测 + ScopeEvaluator 单测全绿）；`mvn -q test -Dtest='DataPermission*'`。
**Step 5:** commit `refactor(access): 拦截器瘦壳化, 复用 ScopeEvaluator`。

---

## Task 8: 资源能力声明 + 上层调用方迁移

**Files:**
- Modify: `database/...`（data_resources 增 `subject_relation_filterable TINYINT` 列；user/system_user=1）+ baseline + migration
- Modify: `DataResourcePO / DataModulePO / DynamicModuleService`（透传 capability）
- Modify: `DataPermissionApplicationService.toFlatMap`（返回 `capabilities`：anchors/subtreeCapable/relationFilter+validRelations/typeFilter+entity）
- Modify: `RoleDataPermissionController` + `DataPermissionApplicationService.saveRoleDataPermissions`（DTO 携带三轴：org_anchor/anchor_param/subtree/custom/relInclude/relExclude/typeFilter；preset 仅前端概念）
- Modify: 任何 `getScopeForRole/getMergedScope/getScopeCodeForRole` 残留调用方 → 改用 `getScopeSpec`
- Test: `DataPermissionApplicationServiceTest`（getAllScopeTypes 现返预设目录；保存往返带三轴）

**Step 1-4:** TDD 各点（capability 计算、保存往返、关系字典来源 = 复用 relation_types 查询）。
**Step 5:** commit `feat(access): 资源 scope 能力声明 + 上层三轴存取`。

---

## Task 9: 删除死枚举/死列

**Files:** 删 `DataScope` 残留（或仅留 `@Deprecated` 指向 ScopePreset）、`role_data_scopes.scope_type/custom_org_unit_ids` 列（baseline + migration `DROP COLUMN`）、`RoleDataPermission` 旧字段。

**Step 1:** `grep -rn "DataScope\.\|scope_type\|custom_org_unit_ids\|getMergedScope" backend/src/main` → 必须为空（除注释/历史）。
**Step 2:** 删列 migration + baseline 同步。
**Step 3:** `mvn -q test-compile` 绿。
**Step 4:** commit `refactor(access)!: 删 DataScope/scope_type/custom_org_unit_ids 死路径`。

---

## Task 10: 前端范围生成器（READ 侧，§6）

**Files:**
- Modify: `frontend/src/types/access.ts`（`ModulePermission` 三轴字段；`ResourceScopeCapabilities`）
- Modify: `frontend/src/views/access/data-permissions/components/AdvancedModuleEditor.vue`（行内"预设下拉 + ▸展开 范围生成器"，能力驱动显隐 ①②③）
- Modify: `PermissionConfigurator.vue`（透传 capabilities；save/load 带三轴）
- Modify: `AccessConsoleView.vue groupByIndustry`（透传 capabilities）
- Modify: `useSceneTemplate.ts`（预设↔spec 映射保 ②③ 不被场景抹掉，已部分做）
- Modify: `PreviewPanel.vue`（自然语言渲染三轴）
- Test: 复用 `no-industry-vocab.spec.ts`（关系/类型选项来自 API，无行业字面量）；`npm run type-check`

**Step 1-4:** 组件实现 + `vue-tsc --noEmit` 0 error + 守护测试绿。关系下拉调关系字典 API；类型下拉 = 2b 的 entityTypeApi（已有）。
**Step 5:** commit `feat(access/fe): 数据范围生成器(预设+高级三轴叠加)`。

---

## Task 11: 集成验证（M1 验收门）

**真启动**（kill 旧 + 重启，见 CLAUDE.md）。

1. **逐轴真库对照**（照 2b 手法，登录非超管真账号 + general_log 抓注入 SQL）：
   - anchor=RELATION:admin → 集合 = 我管理组织成员
   - +subjectRelExclude=[admin] → 集合剔除管理者
   - +typeFilter=[STUDENT] → 再窄到学生
   - 每轴独立切换看集合单调变化；general_log 抓的 SQL 含对应 `NOT IN` / `IN(...)` 片段。
2. **API 往返**：PUT 组合 spec（三轴）→ 新列落库（mysql 查证）→ GET 重建一致。
3. **浏览器 E2E**：非超管登录 → 数据权限→高级→用户行展开生成器 → 配置组合 → 保存 → 用户列表反映。
4. **回归零差异**（最关键）：对 §T0 基线的每个 BOTH-seed 角色重跑 `GET /users/page`、`/students` total，与 `/tmp/scope_baseline_counts.txt` **逐行 diff = 空**。
5. `mvn test`（access 相关全套）+ `vue-tsc` + `no-industry-vocab` 全绿。

**Step 末:** commit `test(access): M1 集成验证通过(逐轴真库+往返+E2E+回归零差异)`；清理本任务建的测试账号/关系。

---

## 验证附录：基线 count 脚本

```python
# /tmp/scope_counts.py — 对给定账号列表登录后取 /users/page、/students total
import json,urllib.request
BASE="http://localhost:8080/api"; op=urllib.request.build_opener(urllib.request.ProxyHandler({}))
def login(u,p): ...; def get_total(tok,path): ...
for acct in ["<role代表账号...>"]:
    print(acct, get_total(login(acct,"<pw>"),"/users/page?pageNum=1&pageSize=1"))
```
（重构前存基线、重构后再跑 diff。无现成非超管账号时，临时建账号+赋角色，验证后清理。）

---

## 风险与回滚
- 安全内核重构：唯一硬验收 = **回归零差异**（T11.4）+ 逐轴语义（T11.1）。任一不过不得合并。
- 回滚：本分支未合 master；任一任务失败 `git reset` 到上个绿提交。
- M2（WRITE/闸3）、M3（守护/清理）在 M1 合并并稳定后另起计划。
