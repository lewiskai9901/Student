# ADR-002: 关系传递性语义统一(`isTransitive` 是单一真相,`include_children` 已废弃)

**Status**: Accepted (2026-05-09)
**Phase**: 关系管理 A+ Phase 2 W2.3

## Context

`access_relations` 表和 `RelationTypeDef`/`relation_types` 字典里有两个看似重叠的字段:

| 位置 | 字段 | 类型 | 语义 |
|---|---|---|---|
| `relation_types.is_transitive` (字典) | `isTransitive: boolean` | 关系类型级 | 该关系是否沿组织树传递(类型默认) |
| `access_relations.include_children` (记录) | `includeChildren: boolean` | 单条记录级 | 此条记录是否包含子级组织 |

新人困惑:**两个都说"沿组织树继续传"**,谁优先?设关系时该用哪个?

## 决策

**`isTransitive`(类型级)是唯一真相,`include_children`(记录级)已废弃。**

### 详细规则

1. **关系是否沿组织树传递,由关系类型字典决定**
   - `RelationTypeDef.isTransitive=true` 的关系类型(如 `admin`、`advisor_of`、`in_ward`)— 沿组织树自动展开
   - `RelationTypeDef.isTransitive=false` 的关系类型(如 `member`、`occupies`)— 不传递,只对直接 resource 生效

2. **传递展开通过 `impliedRelations + DESCENDANTS_OF_ORG` 显式声明**
   - `transitive()` 是声明语法糖,真实展开靠 `Implied(target, relation, DESCENDANTS_OF_ORG)`
   - `AccessRelationService.checkImplied` BFS 读这个推导链,不读 `include_children`

3. **`access_relations.include_children` 字段保留但已废弃**
   - 历史遗留:旧表 `user_org_relations`/`place_org_relations` 时代用,V25 合并到统一表后被边缘化
   - **当前 BFS 不读此字段** — 完全靠 `isTransitive` + `impliedRelations`
   - **新业务代码不要再设 `includeChildren=true`** — 设了也没用
   - **不删字段**(避免破坏既有数据迁移),但下个大版本(V26)考虑删

## 实施

### 不需要改的:
- `AccessRelationService.checkImplied` BFS 已经只读 `relation_types.is_transitive` + `implied_relations` 字段(不读 `include_children`)— 行为正确,无需调整
- `access_relations.include_children` 列保留(数据兼容)

### 需要改的:
- 文档:本 ADR
- 实体 `AccessRelation.includeChildren` 加 javadoc 说明已废弃
- `GrantRequest.includeChildren` 加 deprecated 提示
- 加单测覆盖"isTransitive 是真相"

## 单测覆盖矩阵

| 场景 | isTransitive | includeChildren | 期望:子组织里 subject 是否能 check 通过? |
|---|---|---|---|
| 1 | true | true | ✅ 是(类型可传递,DESCENDANTS implied 展开) |
| 2 | true | false | ✅ 是(类型可传递为准,记录级 false 被忽略) |
| 3 | false | true | ❌ 否(类型不传递,记录级 true 也不生效) |
| 4 | false | false | ❌ 否(都不传递,对子组织无权限) |

场景 2 与 3 是关键 — 证明"`isTransitive` 优先于 `includeChildren`"。

## 未来计划

V26 大版本:
- 真正 DROP `access_relations.include_children` 列
- 实体上移除 `includeChildren` 字段
- 现有数据迁移:对 `is_transitive=true` 的关系,无操作(包含子级);对 `is_transitive=false`,`include_children=true` 的孤立行警告

预计还有 6-12 个月窗口,等业务侧无人误用此字段后再做。

## 相关

- ADR-001: DataScope 与 AccessRelation 的关系
- `domain/access/model/entity/AccessRelation.java`
- `infrastructure/extension/RelationTypeDef.java`
- `application/access/AccessRelationService.java#checkImplied`
- 字段:`access_relations.include_children`(废弃)、`relation_types.is_transitive`(真相)
