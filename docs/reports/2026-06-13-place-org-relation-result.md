# 场所归属关系化 (A3) — 实施与验证结果

> 2026-06-13。配套计划: 审计→方案 A3 (关系真相 + 物化投影)。
> 纪律落档: `docs/design/relation-catalog-discipline.md` §2.5。

## 1. 目标模型 (已落地)

| 语义 | 旧 | 新 |
|---|---|---|
| 场所→组织 | `places.org_unit_id` 列 (NULL=继承) | `belongs_to\|place\|org_unit` 关系 = 覆盖点真相; 无关系=沿场所树继承 |
| 有效组织 | 逐节点递归计算 | `places.effective_org_unit_id` 投影列, **唯一属主 `PlaceOrgProjector`** (关系事件同事务重算子树), 业务禁写 (PO `FieldStrategy.NEVER`) |
| 场所负责人 | `responsible_user_id` 列 (已 DROP) | `responsible_for\|user\|place` 关系覆盖点, 无投影, 读时沿树上溯 |
| 统一入口 | — | `application/place/PlaceOrgResolver` (对称 MembershipResolver: orgOf/setBelonging/clearBelonging/responsibleOf/setResponsible/clearResponsible/批量 overridesFor) |
| DB 兜底 | 无 | `place_belongs_lock_key` + `place_responsible_lock_key` 生成列 UNIQUE (仿 membership_lock_key) |
| 字典 | belongs_to 基数写反 (`maxPerResource=1`) | 修为 `maxPerSubject=1`; responsible_for(user,place) 加 `maxPerResource=1` |

**API 契约保持**: Create/Update 命令仍收 `orgUnitId`/`responsibleUserId`/`clearOrgOverride`, 后端内部转译关系操作; DTO/树节点的 `orgUnitId` 语义 = **显式覆盖点** (前端表单据此判断显式 vs 继承), `effectiveOrgUnitId` = 投影。前端零改动 (仅场景文案消歧)。

## 2. 验证矩阵 (全部真库/真启动)

| # | 验证项 | 结果 |
|---|---|---|
| 1 | 迁移 V20260612_2 幂等 (dev 库跑两遍) | PASS — 全步骤条件化跳过 |
| 2 | `uk_place_belongs_unique` 真实拒绝第二条活跃 belongs_to | PASS — ERROR 1062 |
| 3 | 软删释放锁后可重新归属 | PASS |
| 4 | `uk_place_responsible_unique` 拒绝第二责任人 | PASS — ERROR 1062 |
| 5 | 全量后端测试 | PASS — 2245/2245 (新增 PlaceOrgResolverTest 11 + PlaceOrgProjectorTest 7 + PlaceDataPermissionOrgFieldTest 守护) |
| 6 | 前端 type-check | PASS — 0 错 |
| 7 | 真启动 | PASS — 28s, RelationTypeRegistry 21 关系 + belongs_to 基数 UPDATED |
| 8 | API: 建根带归属 → belongs_to 行 + 投影=覆盖值 | PASS |
| 9 | API: 子/孙场所不带归属 → 投影沿树继承 | PASS — 三层链 |
| 10 | API: 子场所覆盖归属 → 整支级联, 兄弟分支不动 | PASS |
| 11 | API: clearOrgOverride → 回落父值级联 + history 归档 REVOKE | PASS |
| 12 | 关系管理页直写 (POST/DELETE /access-relations belongs_to) → 投影同步 | PASS — 补发关系事实事件后双向同步 |
| 13 | 基数越界 grant 被拒 | PASS — DB 兜底 1062→2002 (见 §4 已知项) |
| 14 | 树节点显示: 覆盖点/effective/isInherited/责任人继承上溯 | PASS — 全字段语义正确 |
| 15 | 审计: PlaceOrgAssigned → place_audit_logs | PASS (表恢复后) |
| 16 | 删除场所 → 关系级联清理 + 归档行可堆叠 | PASS (uk_relation 重建后) |
| 17 | 数据权限模拟 (place 模块, effective 列) | PASS — 无 Unknown column |
| 18 | **修复缺陷正向证明**: CUSTOM=组织X 能见**继承态**子场所 | PASS — 计数 1→2 (旧模型 NULL 列永远漏) |
| 19 | fresh init-all (baseline_v3 + post-v3 全链) | PASS — scratch 库结构抽查全对 |

## 3. E2E 钓出的既有缺陷 (均已修)

1. **`trg_cascade_org_unit_update` 触发器** 引用 `NEW.org_unit_id` — V23 残留, 且 baseline squash 丢了它写入的表却留了触发器。已 DROP (V20260612_2 §6), 审计语义由 PlaceEventHandler 事件承接。
2. **`place_audit_logs` 表在 baseline_v3 中丢失** — 场所审计自 06-01 起全部静默失败 (WARN 吞掉)。已按 V20.0.0 原 DDL + tenant_id 恢复 (V20260613_2)。
3. **`access_relations_history` 缺 Phase 7 W7.3 三列** (operator_ip/operator_user_agent/operation) — baseline squash 遗漏, **所有 revoke 路径自 06-01 起即坏**。已补列 (V20260613_1)。
4. **`uk_relation` 含二值 `deleted` 列** — 同 tuple 至多一条归档行, grant→revoke→grant→revoke 第二次软删撞唯一键 (member 换绑两次同样中招)。已改为 `active_uniq` 生成列豁免归档行 (V20260613_3), 活跃 tuple 仍严格唯一。
5. **V23 调试 DB 对象** (v_inheritance_tree / v_places_effective_org 视图、get_effective_org_unit_id 函数、get_affected_children 过程) 引用旧列且代码零消费 — 已从 baseline 切除 + live DROP。

## 4. 口径变化与已知项

**口径变化 (有意, 是修复):**
- 数据权限/`findByOrgUnitId`/组织删除影响分析/检查目标人群: 由"显式绑定的场所"变为"**有效归属(含继承)**的场所" — 继承态场所第一次被正确计入。
- inspection PER_PLACE 归一化分母变"含继承场所数", 检查得分数值可能变化。
- 树节点 `parentOrgUnitId` 取父的 effective (旧实现父自身继承时显示不出)。

**已知项 (不在本次范围):**
- "移动场所父节点"经 REST 不可达 (控制器从不透传 parentId, 既有行为); 代码路径已接投影重算 + 单测覆盖。
- PlaceEventHandler @Async 后审计行 user_name 为 NULL (handler 既有设计, fallback 'system')。

## 6. 追加: create vs grant 双路径统一 (2026-06-13 当日收债)

`AccessRelationApplicationService` 的 create/delete/batchCreate/batchDelete **统一委托
`AccessRelationService.grant/revoke`**, CRUD 路径(关系管理页唯一外部写入口)由此获得与
内部 grant 完全一致的保障链, Policy hook (BEFORE/AFTER_GRANT/REVOKE) 保留在 CRUD 层:

| 保障 | 统一前 (CRUD 直写 repo) | 统一后 |
|---|---|---|
| relation 注册校验 | 无 (任意字符串可入库) | grant 拒绝未注册 relation — **关掉审计发现的"前端场景与字典无对账"API 缺口** |
| 审批路由 (approval_required) | 被绕过 (直接落库) | 进入审批队列, 返回未持久化回执 (负 pendingId 约定) |
| metadata schema 校验 | 无 | grant 路径校验; update 合并后整体校验 |
| 幂等 | 撞 DB 1062 → 通用报错 | 幂等命中返回现有关系 |
| 基数强制 | 仅 DB 锁键兜底 (1062 透传) | CardinalityViolation 友好文案("主体已达上限…请先解除原关系") |
| revoke 归档 | repo.deleteById 仅软删 | history 归档(operation/operator_ip/UA) + valid_to 截断 |
| check 缓存失效 | **无 (正确性漏洞: 授权缓存吃陈旧关系)** | grant/revoke 内置; update 补对称失效 |
| 关系事实事件 | 上一提交手工补发 | grant/revoke 单点发布 (手工补发已移除) |

E2E (真启动): U1 未注册 relation 422 拒绝 / U2 合法 belongs_to 创建+投影同步 /
U3 基数越界友好拒绝 / U4 重复 POST 幂等命中同 id / U5 CRUD DELETE→history 归档+投影回落。
测试: AccessRelationApplicationServiceTest 重写为委托语义 23/23 绿; 全量回归绿。

## 5. 文件清单

- 迁移: `database/migrations/post-v3/V20260612_2__place_org_responsible_to_relations.sql` (+V20260613_1/2/3 三个钓出缺陷的修复); `database/schema/baseline_v3.sql` 同步 (places/access_relations/relation_types/触发器/视图函数)
- 后端新增: `application/place/PlaceOrgResolver.java`、`PlaceOrgProjector.java`
- 后端改造: UniversalPlace 聚合 / UniversalPlacePO / UniversalPlaceRepositoryImpl / UniversalPlaceApplicationService / UniversalPlaceMapper / CoreManifest / AccessRelationApplicationService (补事件) / TargetPopulationService / OrgUnitJdbcApplicationService / DataPermissionSimulateController; 删除 PlaceInheritanceService
- 测试: PlaceOrgResolverTest / PlaceOrgProjectorTest / PlaceDataPermissionOrgFieldTest (新守护)
- 前端: relationScenes.ts (ASSIGN_PLACE_ADMIN 消歧为"场所管理员", PLACE_BELONGS_ORG 从死场景变真功能)
- 种子: demo_inspection_full.sql (org_unit_id→effective+belongs_to 关系, 顺修 room_type 死列)
- 文档: relation-catalog-discipline.md §2.5 场所归属裁决
