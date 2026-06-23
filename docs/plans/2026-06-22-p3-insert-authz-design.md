# P3-INSERT 授权 — 实施设计 (2026-06-22 深挖结论)

> R8 取证已确认 **UPDATE/DELETE 写鉴权已建**(拦截器通用 WHERE 注入)。唯一真缺口 = **INSERT 授权**:
> 拦截器对 INSERT 早退(`intercept()` line 101,注释"ownership 由应用层"),新行的 owner_org/target
> 是否 ∈ 用户可写范围**无校验**。本文是深挖后的实施设计 —— 安全代码必须按此谨慎做,勿即兴。

## 为什么不能即兴加(本次深挖发现)
INSERT 没有 row 可注入 WHERE,所以 UPDATE/DELETE 那套"注入即拦截"用不上。必须**判断新行 owner ∈
用户可写集**,而这有**逐锚点语义**,盲目复用现有 condition 会错:

| WRITE 锚点 | INSERT 应允许当… | 盲目复用 condition 的坑 |
|---|---|---|
| ALL | 总允许 | condition 空 → 需识别为"放行"非"拒绝" |
| SELF(creator) | **总允许**(新行我即创建者) | SELF 产 `created_by=?` → 对 org 探针无意义,会误拦 |
| PRIMARY_ORG | owner_org ∈ 我的主组织(±子树) | — |
| RELATION(admin) | owner_org ∈ 我管理的组织 | — |
| CUSTOM_ORG | owner_org ∈ 指定组织集 | — |
| PLUGIN_DIM | owner_org ∈ 维度 resolve 的组织 | — |
| PROVIDER | **不参与**(读向,无 owner 列) | — |
| 无 org 锚(仅 creator)| 总允许(我即创建者)| meta.orgUnitField 空 → 须 skip |

**关键语义**:`SELF`/creator 锚点 INSERT **恒允许**(新行天然属于创建者自己);只有**组织有界**锚点
(PRIMARY_ORG/RELATION/CUSTOM/PLUGIN_DIM)才限制 owner_org ∈ 可写组织集。混了就错。

## Pre-insert vs Post-insert(事务安全)
- **Post-insert 探针**(insert 后 re-select 判可见,不可见则 throw 回滚):复用现有 SELECT 拦截器最省,
  但 **非 @Transactional 的 create 已 autocommit** → throw 无法回滚 → 行已落库 = 安全洞。**否决**。
- **Pre-insert 校验**(insert 前判 owner_org ∈ 可写集,越界 throw):无 row 副作用,安全。**采用**。
  代价:需"可写组织集"能力(现机制是 resource-shaped WHERE,非 org-set)。

## 推荐实现
1. **新拦截分支**(DataPermissionInterceptor INSERT 不再早退,改走 INSERT 授权):super-admin/无上下文 → 放行。
2. **取 owner_org 列**:registry `forResource(module).orgUnitField()`;空(viaMembership/无 org 锚/PROVIDER)→ 放行。
3. **取新行 owner_org 值**:`boundSql.getParameterObject()` + MetaObject 读字段(列 `org_unit_id`→ 驼峰 `orgUnitId`)。取不到 → fail-safe 放行 + WARN。
4. **算可写组织集 + 判定**:对用户每个 scoped role 取 WRITE spec(`getScopeSpec(...,"WRITE")`):
   - spec 含 SELF/creator 锚 或 ALL → **该角色放行**(allow short-circuit)。
   - 组织有界锚 → `ScopeEvaluator.toSqlCondition(spec, 合成 org 元(alias="",orgField="id"), ...)` → `id <cond>`;
     OR 合并;探针 `SELECT EXISTS(SELECT 1 FROM org_units WHERE id=? AND (合并cond) AND deleted=0)`。
   - 任一角色允许 → 放行;全部不允许且 owner_org 越界 → **throw AccessDenied**。
5. **fail-safe**:取列/取值/探针异常 → 放行 + WARN(绝不因插桩 bug 阻断合法 INSERT;比现状"全裸"严格更好)。

## 测试矩阵(真库 E2E 双向,缺一不可)
- ✅ 授权:org-bounded 角色 INSERT owner_org ∈ 可写 → 成功。
- ✅ 拒绝:同角色 INSERT owner_org **越界** → AccessDenied,行未落库。
- ✅ SELF/creator 角色 INSERT 任意 → 成功(我即创建者)。
- ✅ super-admin INSERT 任意 → 成功。
- ✅ 无 org 锚资源 INSERT → 成功(放行)。
- ✅ PROVIDER 资源 INSERT → 成功(不参与)。
- ✅ 字节等价 harness 零回归 + 真启动 + dpt_ct 金标准。

## 工作量与纪律
**几天**(非周级),但**安全关键 + 逐锚点边界 + 真库双向 E2E** → 必须**独立专注会话**,不在长会话尾即兴。
fail-safe 设计保证最坏情况只是"漏拦"(回到现状),绝不"误拦合法写"。

## 实施状态 (2026-06-22, commit 见下)
**核心机制 + deny 逻辑已落地并验证**:
- ✅ 拦截器 INSERT 分支(不再早退)+ enforceInsertAuthz + extractColumnValue + isOrgWritable + 合成 org 元探针。
- ✅ deny 逻辑**确定性单测**(DataPermissionInterceptorTest): owner_org 越界 org-bounded grant + 探针 false
  → AccessDeniedException; 在范围内 → 放行; creator grant → 放行不探针。55 测试绿。
- ✅ 装配验证: 真启动 0 ERROR + dpt_ct READ 金标准零回归 + 字节等价 harness 绿。
- ✅ fail-safe: 无 org 列 / 取不到值 / 无 grants / 异常 → 放行。

**✅ 真库端到端 E2E 已证 (2026-06-22, teaching_task)**:
throwaway 角色 teaching_task WRITE=CUSTOM_ORG[O1] + 用户 ttu; POST /teaching/tasks:
- orgUnitId=O2(越界)→ `AccessDeniedException: 无权在该组织下创建记录 (org=O2, module=teaching_task)`
  从 enforceInsertAuthz 抛; 探针 `SELECT EXISTS(SELECT 1 FROM org_units WHERE id=O2 AND ((id IN (O1))) AND deleted=0)` → false → 拒绝。
- orgUnitId=O1(可写)→ 过授权(探针 true), 死在 insert FK 约束(非授权错)= 授权放行。
真 MyBatis insert → 取参(真 PO)→ 探针(真 org_units)→ deny 全链证实。

**⚠ 已知覆盖边界 (诚实记录, 待 follow-up)**:
1. **仅接口级 @DataPermission mapper 的 insert 被覆盖**。注解解析方法级优先、回退接口级
   (`resolveDataPermissionAnnotation`);方法级 mapper(多数 inspection mapper)的 `BaseMapper.insert`
   既无方法注解也无接口注解 → INSERT-authz **不触发**。要全覆盖需给这些 mapper 加接口级注解或显式注解 insert。
   接口级资源(grade_batch/teaching_task/student_grade/course_evaluation 等教务表)已覆盖 (teaching_task 已 E2E 证)。
2. **PROVIDER / 无 org 锚资源**: 不参与(放行), 设计如此。
