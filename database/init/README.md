# database/init — 种子数据

> **Last update**: 2026-04-20 (W2.3 core vs EDU 拆分)

## 1. 作用

这里放**应用层种子数据** (角色权限绑定、消息模板、演示工作流等), 区别于 `database/schema/` 的 DDL 迁移与 `database/plugins/**/` 的 per-plugin schema.

## 2. 文件分类

### Core (通用核心, 所有部署必跑)

| 文件 | 说明 |
|---|---|
| `task_permissions.sql` | 任务管理 permissions 表基础数据 |
| `task_basic_permissions.sql` | 给 role 2/3/4 分配 core 基础查看权限 (system:*) |
| `task_role_permissions.sql` | 任务模块完整的角色→权限绑定 |
| `task_workflow_init.sql` | 任务审批流程演示数据 |
| `msg_seed_data.sql` | 消息模板与订阅规则 seed |
| `ci_e2e_seed.sql` | CI / E2E 测试基准数据 |
| `verify_all_permissions.sql` | 验证脚本 (只读 SELECT) |

### 插件 (按启用状态条件执行)

```
plugins/
└── education/
    └── edu_basic_permissions.sql   -- EDU 角色 role 2/3/4/5 的 student:* / quantification:* 绑定
```

## 3. 执行顺序

1. 先跑 `database/schema/` 的 Flyway migration 建表
2. 再跑本目录 core seed (顺序: permissions → basic_permissions → role_permissions → workflow / msg)
3. **最后按启用的插件**跑 `plugins/{industry}/*.sql`
   - 未装该插件则整个插件子目录跳过
   - 即使错误执行了, 由于 `INSERT IGNORE` 与 `WHERE permission_code IN (...)` 会在缺失 permission 行时静默跳过, 不会报错 (但仍应按启用状态有条件执行, 避免语义歧义)

## 4. W2.3 拆分记录 (2026-04-20)

原 `task_basic_permissions.sql` 混合 core (`system:*`) 与 EDU (`student:*` / `quantification:*`) 权限绑定, 违反"通用核心 + 行业插件"原则. 拆分为:

- `task_basic_permissions.sql` — 仅保留 core (role 2/3/4 的 `system:*` 绑定)
- `plugins/education/edu_basic_permissions.sql` — EDU 专属 (role 2/3/4/5 的 `student:*` / `quantification:*` 绑定)

**role 5 (班主任)**: 其所需基础权限全部为 EDU 专属, 因此 core seed 中**整块 INSERT 被移除**, 仅在 EDU 插件启用时分配.

## 5. 相关文档

- `database/MIGRATIONS.md` — schema migration 归类 policy
- `database/plugins/README.md` — 插件 schema 组织
