# baseline_v3 — 权威库基线 (2026-06-01 squash)

## 它是什么
`baseline_v3.sql` 是把本项目整条历史迁移链 **squash** 出来的单一权威快照:
- **完整 schema**:333 张表/视图,含组织归属统一重构(`access_relations` member 关系 + `uk_membership_unique`)、插件基础设施(`plugin_packages`/`data_scope_dims`/`industry`/`origin`/`plugin_class`)、检查平台、教学/学术扩展等。
- **bootstrap 种子数据**:`admin/admin123` 超管 + `admin→SUPER_ADMIN` 绑定 + 默认角色/权限 + 类型配置(`entity_type_configs`)+ 关系类型 + `data_modules` 等。
- 一次加载即得到"后端可直接启动并登录"的库,已经过**真实启动 + 登录/用户/角色/组织树冒烟 200** 验证。

新库初始化只需:`DB_PASSWORD=… bash database/scripts/init-all.sh`(create DB → **仅加载 baseline_v3**)。fresh 库到此即完整最新 schema, **init-all 不 apply post-v3**(详见下"将来加新迁移")。

## 为什么要 squash(背景)
项目早期未用 Flyway,迁移靠文件名排序逐个 apply。长期演进导致**历史链无法从 baseline 干净重放**:
- `database/schema/V*.sql` 的 `V` 号是各子系统内部版本号、**非时间序**(如 V40 drop 的列 V61 又要用 → 按号回放先删后用必崩)。
- 大量"先用后加"的列、重复列/键、`USE student_management` 硬编码、MySQL8 不兼容 DDL。
- **7 个非 `V` 前缀 schema 文件 init 从不应用**;**6 张表 + 多个反范式列(org_units.type_code / entity_type_configs.industry 等)只存在于线上 ad-hoc SQL,从未回写仓库**。

2026-06-01 重建方法:以**应用代码 PO 的 114 张 @TableName 表 + 列**为标准答案,补齐缺表(从 PO 反推 6 张无 DDL 表)/缺列(PO vs DB 对账)/插件基础设施列/被误删表/`classes` 视图,**真实启动后端 + 冒烟 200** 证明 schema 正确,再 `mysqldump` 成本文件。

## 旧文件怎么办
- `complete_schema_v2.sql`、`database/schema/V*.sql`、`database/migrations/V*.sql`(含本次重建用的 `V111`~`V115`)**保留作历史记录,不再参与 init**。
- `init-all.sh` 已改为只加载 `baseline_v3.sql`,不再逐个回放旧链。
- 重建过程产生的修复迁移(V111 重建 6 表 / V112 列对账 / V113 恢复误删表+classes 视图 / V114 插件基础设施列 / V115 自增恢复)其效果**已全部烘焙进 baseline_v3**。

## 将来加新迁移
本项目纪律:**改动 schema = 同时改 baseline_v3 建表 SQL + 加一个 post-v3 迁移**,故 baseline 恒为最新、fresh init 只加载它即完整。
- `database/migrations/post-v3/V<日期>_<n>__*.sql` **不参与 init-all**(init-all 只加载 baseline)。
- post-v3 仅供"**已有、且落后于 baseline 的 dev 库**"手动按序追赶增量。
- ⚠ post-v3 迁移**非幂等**(项目纪律"不写条件化迁移",是裸 `ALTER`/`INSERT`)→ 对**已最新或 fresh 的库重放会报"重复列/DROP 不存在/主键冲突"**。所以**勿**让 init-all 去 apply 它们(那样 fresh 库必崩)。
- 积累到一定量(或库可随时重建时)即把效果 squash 进 baseline_v3 并**清空 post-v3**。
