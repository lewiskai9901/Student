# ADR-005: 前端视图按插件物理隔离

**Date**: 2026-05-05
**Status**: Accepted
**Phase**: 前端冲 A+ Phase 1
**Commit**: `3adcf4b3`

## Context

后端在 `bd6d0552` (2026-04-19) 把 260+ Java 文件迁至 `plugins/education/{domain,application,infrastructure,interfaces}` 4 层, 通过 ArchUnit 守护"core 不依赖 plugins". 但**前端没跟进** — `src/views/` 下混杂了通用核心 (inspection/access/asset) 和教育行业 (academic/dormitory/myclass/student/teaching).

后果:
- 任何通用 view 一不小心 import `@/views/student/...` 就再也无法在医疗场景重用
- 后端 A+ 前端 B, 整体被前端拉低
- 切换行业需手工排查全代码, 没自动化

## Decision

**前端视图按插件归位**: 物理移动到 `views/plugins/{edu,health}/` 下, 与后端 `plugins/{education,healthcare}/` 一一对应. 加 ESLint 守护 (类似 ArchUnit) 让越界 import 编译期失败.

### 视图分层

```
src/views/
├── inspection/      # 通用核心 ← 不变
├── access/          # 通用核心 ← 不变
├── asset/           # 通用核心 ← 不变
├── plugins/
│   ├── edu/         ← 教育行业插件全部
│   │   ├── academic/
│   │   ├── dormitory/
│   │   ├── myclass/
│   │   ├── organization/grades/
│   │   ├── student/
│   │   └── teaching/
│   └── health/      ← 医疗插件
│       └── healthcare/
└── ...
```

### ESLint 守护规则

`.eslintrc.cjs` overrides:

1. **core → plugins 禁止**: `views/inspection/**`, `views/access/**` 等不得 `import '@/views/plugins/*'`
2. **跨插件禁止**: `views/plugins/edu/**` 不得 `import '@/views/plugins/health/*'`, 反之亦然

实测 (在 `views/inspection/` 下故意添加 `import '@/views/plugins/edu/...'` ):

```
✖ '@/views/plugins/edu/student/StudentList.vue' import is restricted from
  being used by a pattern. 通用 (core) 视图不得 import 行业插件 (plugins/*) —
  违反插件化边界. 如需共享, 抽到 src/components 或 src/composables.
```

## Consequences

### Positive
- 与后端 A+ 视图层 100% 对齐
- 切换行业 (开/关 EDU) 物理隔离, 无遗漏
- 任何越界 PR 在 CI lint 阶段失败, 不进 master
- `git mv` 保留文件历史, code review 友好
- 未来 Module Federation 化 plugins/edu/* 作为 remote 时, 已经物理就绪

### Negative
- 路由文件 `router/plugins/edu.ts` import 路径全部改写 (一次性, 已自动化迁移)
- 团队需记住"教育业务必须放 views/plugins/edu/, 通用必须不在 plugins/"
- 共享代码 (如 StudentSelector.vue 等) 需主动抽到 `src/components/` — 否则跨插件就违反规则

### Trade-offs Considered

**Module Federation (S 级)** — 当前不做, 商业触发条件未到 (无外部插件 marketplace, 无独立部署需求). POC 已通 (memory 记录), 触发后再启动. 见 `docs/plans/poc-module-federation-findings.md`.

**Lint-only 不物理移动** — 拒绝, 文件混杂下视觉/grep/IDE 体验差.

## Migration

```
views/academic/        → views/plugins/edu/academic/
views/dormitory/       → views/plugins/edu/dormitory/
views/myclass/         → views/plugins/edu/myclass/
views/student/         → views/plugins/edu/student/
views/teaching/        → views/plugins/edu/teaching/
views/organization/grades/ → views/plugins/edu/organization/grades/
views/healthcare/      → views/plugins/health/healthcare/
```

`router/plugins/edu.ts` + `health.ts` 同步更新 import. 6 处 import 自动 sed 迁移.

## Verification

- `git diff --stat` 显示 76 个 R (rename) 文件, 0 处编辑代码 (除 router 配置)
- `npx vue-tsc --noEmit` 0 新 TS 错误 (155 总数全部为预存)
- `npx eslint` 故意越界 import 立即 ✖ error
- ArchUnit 后端守护 + ESLint 前端守护 = 前后端架构边界全闭环

## Related

- [ADR-002 后端 DDD 重构](./002-phase35-ddd-restructure.md)
- [ADR-003 前端动态路由](./003-phase4a-dynamic-routes.md)
- [Memory: 插件架构 Phase 3.5 完成](memory/project_plugin_phase35_complete.md)
