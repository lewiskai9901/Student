# Database Migrations Policy

> **Last update**: 2026-04-20 (Phase 6.6)
> **Status**: **policy-enforced** for new work, **grandfathered** for existing 89 files

## 1. 为什么有这个文件

`database/schema/` 历史上一锅端 89 个 `V*.sql` 迁移, 问题:
- 新人看不清 "哪个迁移属于哪个领域"
- 想 "独立卸载 HEALTH 插件" 时, 对应 DDL 无法单独回滚
- 跨租户差异化部署 (某客户不装 EDU) 需人工挑选 SQL

Phase 6.6 引入**领域分目录 policy**, 新迁移必须归类到子目录.

## 2. 目标结构

```
database/
├── schema/                      ← 已有 89 个 legacy 文件, 保持原位 (见 §4 历史遗留)
│   ├── V1.0.0__*.sql
│   ├── complete_schema_v2.sql   ← baseline
│   └── ...
├── plugins/                     ← 新 policy 目标位置
│   ├── core/
│   │   ├── access/
│   │   ├── messaging/
│   │   ├── place/
│   │   ├── organization/
│   │   ├── inspection/
│   │   └── ...
│   ├── education/
│   │   ├── calendar/
│   │   ├── student/
│   │   ├── academic/
│   │   ├── teaching/
│   │   └── dormitory/
│   ├── healthcare/              ← HEALTH 插件从起点就按目录走
│   │   └── patient/
│   └── README.md                ← 每个插件放 README 说明自己的 schema 版本
├── init/                        ← 种子数据, 保持原位
└── migrations/                  ← 补丁/修正, 保持原位
```

## 3. 新迁移的规则 (强制执行, 启动失败判定)

所有 **2026-04-20 之后** 新增的 schema migration 必须满足:

1. **放到正确目录**: `database/plugins/{industry}/{domain}/V{major.minor.patch}__{description}.sql`
2. **命名规范**: 版本号延续全局 (避免跟 legacy 冲突), description 用 snake_case 动词开头 (e.g. `V200.0.0__add_patient_admission_ward.sql`)
3. **原子性**: 一个 migration 只改一个领域一组表. 跨领域变更拆成多个
4. **描述**: 文件顶部必须有块注释说明 (问题/改动/回滚策略)

## 4. 89 个 legacy 文件的分类 (信息性, 不做物理移动)

Flyway 强制 checksum, 已应用的 migration 一旦物理移动会导致已部署环境启动失败. 因此 **legacy 文件保留在 `database/schema/` 原位**, 仅标注归属.

运行 `python scripts/classify-migrations.py` 重生成最新表:

| Category | Count | 代表文件 |
|---|---:|---|
| baseline | 2 | `complete_schema_v2.sql`, `teaching_academic_complete_schema.sql` |
| core/access | 5 | V81/V85/V100 等 (permissions, roles) |
| core/audit | 1 | V93__audit_trail |
| core/inspection | 23 | V30~V102 大部分 (V7 检查平台) |
| core/messaging | 3 | V97/V101/V103 (event_trigger, notifications) |
| core/organization | 1 | org_type_unification.sql |
| core/place | 4 | V26/V88/rename_space_to_place |
| core/task | 4 | V62 系列 + task_approval_config |
| core/tenant | 1 | V92__add_tenant_id |
| education/academic | 1 | V84__academic_domain |
| education/calendar | 1 | V90__calendar_permissions |
| education/dormitory | 1 | V82__unify_dormitory_to_place |
| education/student | 5 | V78/V79/V80 等 (status_changes/attendance/warning) |
| education/teaching | 3 | V73/V76/V86 |
| unknown | 34 | 多为 V26~V57 早期 V7 inspection 子模块 (命名未带 "insp" 前缀) |

**`unknown` 34 个**: 大多是早期 V7 检查平台子模块 (如 `V38__condition_logic_v2.sql` / `V46__item_library.sql`), 应归 `core/inspection`. 将来若做 "inspection 插件化" (rejected 过, 见 road-to-a-plus.md Phase 6/7 外) 再精细化.

## 5. 未来 workflow

启动一个新功能需要加表:

```bash
# 1. 决定归属领域 — 如教育的教师评分
mkdir -p database/plugins/education/teaching
cat > database/plugins/education/teaching/V200.0.0__teacher_rating.sql <<'EOF'
-- 教师评分: 学生对任课教师打分
-- 改动: 新增 teacher_rating + teacher_rating_dim 两张表
-- 回滚: DROP 上述两张表即可 (无外键引用)

CREATE TABLE teacher_rating ( ... );
CREATE TABLE teacher_rating_dim ( ... );
EOF

# 2. Flyway 会自动发现 database/plugins/**/V*.sql (见 application.yml 配置)
# 3. CI 启动时会尝试执行, 失败即阻断
```

Flyway config (application.yml) 已支持 `locations` 多路径, 之后添加 `classpath:db/migration/plugins/**` 即可.

## 6. 真要 "独立卸载插件" 的时候怎么办

现在不支持. 启动 Phase 7 计划的 `PluginPackage.schemaVersion()` + per-plugin migration 后, 禁用 HEALTH 可选择:
- **soft disable** (默认): schema 保留, 只停用 routes/menus/permissions
- **hard uninstall**: 运行 `database/plugins/healthcare/rollback.sql` 物理删表

详见 Phase 7 PR (TODO).

## 7. 相关 Policy & 工具

- `scripts/classify-migrations.py` — 重新分类 legacy 文件
- `scripts/phase35-rewrite.py` — Phase 3.5 literal-replace 工具
- `docs/plans/2026-04-20-road-to-a-plus.md` — 整体 7 周路线图

## 8. 历史教训

- **V97.0.0__event_trigger_system.sql 多次就地修订** — 同一个文件反复改 `INSERT applicable_subjects 字符串` 的格式, 造成 Flyway checksum 不一致. 教训: **已 release 的 migration 不改**, 新开 V{N+1} 增量改.

## 9. Seed 数据拆分 (2026-04-20, W2.3)

`database/init/task_basic_permissions.sql` 原混合 core (`system:*`) 与 EDU (`student:*` / `quantification:*`) 权限绑定, 违反通用核心 + 插件架构. 已拆分:

- `database/init/task_basic_permissions.sql` — 只保留 core 绑定 (role 2/3/4 的 `system:*`)
- `database/init/plugins/education/edu_basic_permissions.sql` — EDU 启用时执行 (role 2/3/4/5 的 `student:*` / `quantification:*`)

role 5 (班主任) 所有基础权限均为 EDU 专属, core seed 中整块 INSERT 被移除. 详见 `database/init/README.md`.
