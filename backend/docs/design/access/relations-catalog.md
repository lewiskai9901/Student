# 关系管理 — 关系类型 Catalog

> 关系管理 A+ Phase 6 收尾产物
> 日期:2026-05-09
> 字典版本:Phase 3 后稳定版(精简扩充完毕)

## Tier 概览

3 层 tier 架构:

| Tier | 数量 | 性质 | 是否可删 |
|---|---|---|---|
| **CORE** | 15 | 平台核心通用关系,任何行业必装 | ❌ 不可 |
| **COMMON_EXT** | 2 | 跨行业通用扩展(学校/医院/养老都用) | ⚠️ 慎删 |
| **DOMAIN** | 7 | 行业特有关系(EDU 3 + HEALTH 4) | ✅ 按租户启用 |

**总 24 条关系。**(对比 Phase 1 起点 17 条,Phase 3 后净增 +7)

---

## CORE Tier(15 条)

### 归属类(MEMBERSHIP)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `member` | user → org_unit | 用户属于某组织 | 无 |
| `occupies` | user → place | 占用(宿舍入住/工位) | capacityBound — 受场所容量限制 |

### 管理类(OWNERSHIP)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `admin` | user → org_unit | 主管理员(班主任/部门主管) | maxPerResource=1, transitive, implied: viewer-on-user(MEMBERS_OF_ORG) + admin-on-org(DESCENDANTS_OF_ORG) |
| `deputy` | user → org_unit | 副管理员 | transitive |
| `admin` | user → place | 场所主负责人 | maxPerResource=1 |
| `manages` | user → place | 场所管理者(保洁/物业) | implied: viewer-on-user(OCCUPANTS_OF_PLACE) |
| `responsible_for` | user → user | 责任人(对人,如导师/医师) | — |
| `responsible_for` | user → org_unit | 责任人(对组织) | — |
| `responsible_for` | user → place | 责任人(对场所/设备) | — |

### 关联类(ASSOCIATION)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `belongs_to` | place → org_unit | 场所归属组织 | maxPerResource=1 |
| `viewer` | user → user | 通用查阅(用户档案) | — |
| `viewer` | user → org_unit | 通用查阅(组织信息) | — |
| `viewer` | user → place | 通用查阅(场所信息) | — |

### 委托类(DELEGATION)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `delegated_to` | user → user | 权限临时委托 | — |

### 订阅类(SUBSCRIPTION)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `watches` | user → org_unit | 用户关注某组织动态 | — |

---

## COMMON_EXT Tier(2 条)

跨行业通用人际关系。原 EDU 的 `guardian_of` 与 HEALTH 的 `family_of` 合并到此层。

| 关系码 | 主体→资源 | 描述 | 适用行业 |
|---|---|---|---|
| `family_of` | user → user | 亲属(家长↔学生 / 家属↔病人 / 子女↔老人) | EDU + HEALTH + 养老 |
| `emergency_contact` | user → user | 紧急联系人 | 全部行业 |

---

## DOMAIN Tier(7 条)

### EDU 教育(3 条)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `teaches` | user → org_unit | 教师任教班级 | maxBySubtype: CLASS=10(最多 10 任课老师) |
| `mentor_of` | user → user | 导师指导学生 | — |
| ~~`advisor_of`~~ | — | 已合并到 CORE.admin + metadata.role='ADVISOR'(Phase 3 W3.3) | ❌ deprecated |
| ~~`guardian_of`~~ | — | 已上移到 COMMON_EXT.family_of(Phase 3 W3.2) | ❌ deprecated |

### HEALTH 医疗(4 条)

| 关系码 | 主体→资源 | 描述 | 约束 |
|---|---|---|---|
| `attending_of` | user → user | 主治医师 | — |
| `nurse_of` | user → user | 责任护士 | — |
| `in_ward` | user → org_unit | 所在病区 | transitive |
| ~~`family_of`~~ | — | 已上移到 COMMON_EXT(Phase 3 W3.2) | ❌ deprecated |

---

## 关系链推导(Implied)

3 条 discovery rule(Phase 1 起就有):

| Rule code | 起点 | 派生目标 |
|---|---|---|
| `OCCUPANTS_OF_PLACE` | place | 该 place 内所有 occupant(user) |
| `MEMBERS_OF_ORG` | org_unit | 该 org 内所有 member(user) |
| `DESCENDANTS_OF_ORG` | org_unit | 该 org 所有子孙 org_unit |

实际派生映射:
- `admin(user→org)` →(MEMBERS_OF_ORG)→ `viewer(user→所有 member user)`
- `admin(user→org)` →(DESCENDANTS_OF_ORG)→ `admin(user→子孙 org_unit)`
- `manages(user→place)` →(OCCUPANTS_OF_PLACE)→ `viewer(user→所有 occupant user)`

`AccessRelationService.checkImplied()` 用 BFS 多层展开,最大深度 5 防环。

---

## 时间生效语义

每条 access_relations 行支持 `valid_from` / `valid_to`(Phase 1 W1.2 实体级暴露):

- `valid_from = NULL` → 立即生效
- `valid_to = NULL` → 永久有效(直到 revoke)
- `valid_from <= NOW() < valid_to` → 当前活跃
- `valid_to <= NOW()` → 已到期(history 留痕,但 check 不命中)

`AccessRelation.isCurrentlyActive()` / `isActiveAt(LocalDateTime)` 助手方法。

---

## 访问级别(AccessLevel)

Phase 1 W1.1 引入 enum,3 档(由低到高):

| Level | 语义 | 能做什么 |
|---|---|---|
| `READ_ONLY` | 只读 | 仅查询 |
| `FULL` | 读写(默认) | 查询 + 编辑 |
| `OWNER` | 拥有 | FULL + 可再授权(grant/revoke) |

每条 access_relations 行有自己的 accessLevel,允许"看得见但改不了"的精细化授权。

---

## 审批流(Phase 5 W5.2)

某些敏感关系可声明 `RelationTypeDef.approvalRequired=true`(关系字典层),grant 走两步:

1. `RelationApprovalService.requestApproval(...)` → 写 `pending_relation_approvals` 表 status=PENDING
2. 审批人 `approve(id)` 通过后,再调 `AccessRelationService.grant(...)` 真正落库

REST 端点(`/api/access-relations/approvals`):
- `GET` 列 PENDING
- `POST /{id}/approve` 通过
- `POST /{id}/reject` 拒绝(带 reason)
- `POST /{id}/cancel` 申请人撤回

⚠️ Phase 5 是骨架,grant() 自动判 requiresApproval 留 Phase 7+ 实质化。

---

## 字段级脱敏(Phase 5 W5.1)

`MaskingService` + `DefaultMaskingPolicy` 决定看 user 时哪些字段脱敏:

| viewer ↔ target 关系 | 脱敏字段 |
|---|---|
| 自己 | (全显) |
| admin / responsible_for / family_of / mentor_of / attending_of / ... | (全显,高级关系) |
| member / viewer / manages / occupies / watches / in_ward | phone, email |
| 无关系 | phone, email, idCard, realName |

`UserDomainResponse.applyMasking(service, fields)` 在 Controller 接入。

---

## 监控(Phase 5 W5.3)

Prometheus metrics(`AccessRelationMetrics`):
- `access_relation_check_total{result="true|false"}` — check() 命中/未命中
- `access_relation_check_seconds` — check 延迟分布
- `access_relation_grant_total{relation, tier}` — grant 次数
- `access_relation_revoke_total{relation}` — revoke 次数

History REST(`/api/access-relations/history`):
- `GET /by-subject?subjectType=user&subjectId=7` — 某人的所有关系变更
- `GET /by-resource?resourceType=org_unit&resourceId=100` — 某资源的所有变更
- `GET /recent?days=7` — 最近 N 天审计

---

## 性能基线(Phase 4)

- `AccessCheckCache` Redis 缓存 check() 结果,TTL 60s
- grant/revoke 主动 invalidate(按 subject + resource pattern 清)
- `idx_expand` 专用索引覆盖 expand() 查询
- 单测、ArchUnit 守护到位

---

## ArchUnit 守护(Phase 6)

`ArchUnitAccessRelationGuardTest` 防止 Phase 1-5 修过的债务回归:

- application/access 非 Service/Discovery/Registry 类不准依赖 JdbcTemplate
- 不准引用旧的 `application.access.AuthorizationService`(已重命名)
- 守护规则 3/3 绿

---

## ADR 索引

- [ADR-001 — DataScope 是 AccessRelation 的宏观快捷表达](./ADR-001-datascope-as-access-relation-macro.md)
- [ADR-002 — 关系传递性语义统一(isTransitive 单一真相)](./ADR-002-transitivity-semantics.md)

---

## 业务代码引用守则

- 关系码用常量,**不用裸字符串**:
  - CORE: `CoreRelations.ADMIN` / `CoreRelations.MEMBER` / `CoreRelations.VIEWER` / `CoreRelations.RESPONSIBLE_FOR` / ...
  - COMMON_EXT: `CommonExtRelations.FAMILY_OF` / `CommonExtRelations.EMERGENCY_CONTACT`
  - EDU: `EducationRelations.TEACHES` / `EducationRelations.MENTOR_OF`
  - HEALTH: `HealthcareRelations.ATTENDING_OF` / `HealthcareRelations.NURSE_OF` / `HealthcareRelations.IN_WARD`

- AccessLevel 用 enum,**不用字符串字面量**:
  - `AccessLevel.FULL` / `AccessLevel.READ_ONLY` / `AccessLevel.OWNER`

- check / lookup / expand / grant 一律走 `AccessRelationService`(application 层),**不要直接 SQL 查 access_relations**

- 数据范围(粗粒度)用 `@DataPermission(scope=DataScope.X)`,详见 ADR-001
