# Tier2 收尾：membershipSubjectColumn 收进 resource_relations 注册表

> 2026-06-27 · 统一数据归属 R2.x · 分支 `feat/unified-data-anchoring`

## 背景

R2.4 Tier1 已把锚定语义的三个字段（`orgUnitField` / `creatorField` / `viaMembership`）
从 `@DataPermission` 注解迁进 `resource_relations` 注册表，注册表成为这三者的唯一真相源，
注解兜底已删（缺登记 → fail-fast）。

`ResourceScopeMeta`（拦截器热路径用的每资源锚定配置）仍有三个字段未在注册表，
代码里自标 `Tier 2 再迁`。本次核实后发现这三者**分属不同层级**，并非都该进注册表：

| 字段 | 层级 | 当前来源 | 正确归属 | 本次动作 |
|---|---|---|---|---|
| org/creator/viaMembership | 关系级（锚定语义） | resource_relations | resource_relations | 已迁(R2.4) |
| `membershipSubjectColumn` | 关系级（锚定语义：本表哪列充当 member 关系的 subject_id） | 注解 | **resource_relations** | **本次迁** |
| `typeField` | **资源级**配置（轴③类型列名） | data_resources.type_field | data_resources（资源级表，本就正确） | 不迁，去"再迁"注释 |
| `tableAlias` | **查询级** SQL 形态（这条 `@Select` 的 FROM 用啥别名） | 注解 | 注解（查询级，本就正确） | 不迁，去"再迁"注释 |

## 设计决策（要点）

1. **只迁 `membershipSubjectColumn`**。它是注解上**最后一个锚定语义**属性——
   描述"本资源主表的哪一列作为 access_relations 的 subject_id"。
   这是资源↔成员图的映射语义，和 org/creator 同层，理应和它们一起归注册表。
   现实里唯一真实 override 是 `user_student → user_id`（其余全默认 `id`=主表行即用户）。

2. **`typeField` 留 data_resources**。它是**资源级**配置（一资源一类型列），
   data_resources 就是资源级配置表，是正确的家。把资源级属性塞进关系级的
   resource_relations 是错误建模。它从未在注解里，不是"注解债务"。

3. **`tableAlias` 留注解**。它是**查询形态**——同一资源被不同 `@Select` 查询时，
   各自 FROM 子句用的别名可能不同（虽当前一资源一别名，但这是查询级巧合非语义约束）。
   注册表拥有资源语义，注解拥有查询形态，是干净的职责分离，非债务。

### 终态

迁完后 `@DataPermission` 注解只剩：`module`（身份）+ `tableAlias`（查询形态）+
`enabled`（开关）。**全部锚定语义 100% 归 resource_relations 注册表**。
`ResourceScopeMeta` 的字段来源：锚定(含 subjectColumn)→注册表 / typeField→data_resources /
tableAlias→注解。三层各得其所，自标"再迁"的注释全部终态化。

## 实现

### 后端
1. `ResourceRelationDef`：加 `subjectColumn` 字段（默认 null）+ `withSubjectColumn(String)` 链式修饰。
   所有现有 factory 传 null，subjectGraph 经 `.withSubjectColumn()` 显式声明。
2. `ResourceRelationRegistry`：
   - `DerivedAnchor` 加 `membershipSubjectColumn` 字段。
   - `AnchorRow` 加 `subjectColumn`（从 resource_relations 行读）。
   - `deriveAnchor`：owner_org SUBJECT_GRAPH 行 → `membershipSubjectColumn = row.subjectColumn()`。
3. `DataPermissionInterceptor.buildMeta`：`membershipSubjectColumn` 改从 `DerivedAnchor` 取，
   删 `annotation.membershipSubjectColumn()` 读取。
4. `@DataPermission`：删 `membershipSubjectColumn()` 属性。
5. `DddStudentMapper`：删注解里的 `membershipSubjectColumn = "user_id"`。
6. `EducationManifest`：student owner_org `.withSubjectColumn("user_id")`。
7. `ResourceRelationUpserter`：写 `subject_column` 列；注册表加载读该列。
8. 注释终态化（buildMeta / ResourceRelationRegistry 头注释 / DataPermission javadoc 的 typeField/tableAlias 段）。

### 数据库（永不考虑数据兼容：直接改 baseline + post-v3 迁移给 dev 库追赶）
- `resource_relations` 加 `subject_column VARCHAR(64) NULL`。
- baseline_v3 同步该列；post-v3 迁移 `V20260627_5__resource_relations_subject_column.sql`。
- 行值由 ResourceRelationUpserter 在启动时按 contribution 写入（contribution 驱动，非 seed）。

## 验证
- **金标准强相关**：`user_student` 的过滤正是靠 `user_id`（membershipSubjectColumn）。
  dpt_ct/CLASS_TEACHER GET /api/user_student 必须恰好 2 生（3001/3002）——
  若迁移后注册表没正确把 `user_id` 喂给 meta，该查询会回归（变 0 或全量或 SQL 错）。
- 99 access/arch 单测绿（含 buildMeta 等价测试）。
- 8081 真启动：注册表载入 user_student owner_org 行带 subject_column='user_id'。
- 全程 worktree /d/uda-wt + 8081 共享 DB，验完即停，游戏会话 8080 不碰。

## 非目标（YAGNI）
- 不动 typeField / tableAlias 的存储位置（已正确）。
- 不碰 typeColumn（polymorphic resource_id 的概念，与 subjectColumn 无关）。
- 不引入新能力（R2.3 多锚点 OR / R3 relation_grants 暂无消费方，不做）。
