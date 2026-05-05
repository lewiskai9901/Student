# ADR-008: Module Federation 暂不启动 — 商业触发条件

**Date**: 2026-05-05
**Status**: Deferred
**Phase**: 前端冲 A+ Phase 6 收官
**POC commit**: 在 `poc/module-federation` 分支 (不合 master)

## Context

前端冲 A+ 路线图最后一步可选 "**Module Federation 化**", 把 plugins/edu / plugins/health 作为独立 remote 部署. POC 已通过 (memory 记录) — `@originjs/vite-plugin-federation@1.4.1` + Vite 4 + Vue 3 + Element Plus 兼容性验证 OK.

但 Module Federation 是**重投入**:
- 每个 remote 独立 build / deploy 流水线
- shared deps 版本协商成本高 (Vue/Pinia/Element Plus 必须严格对齐)
- 调试更难 (devtools 跨 remote 链路弱)
- 上线后 hotfix 流程变复杂

## Decision

**当前不启动**, 写明触发条件, 待商业信号到位再启动.

### 触发条件 (任一即启动)

1. **外部插件 marketplace** — 第三方开发者在我们平台上传插件, 用户从市场启用. 例如 Salesforce AppExchange / VSCode Extension / Backstage Plugins.

2. **独立部署 EDU/HEALTH** — 教育版与医疗版上线时间不同, 教育客户不希望医疗模块更新影响他们. 例如同一域名 `app.foo.com` 但 `/inspection/*` (核心) 与 `/student/*` (EDU) 由两条独立流水线发版.

3. **第三方 ISV 生态** — 客户 ISV 自己开发的行业插件需独立技术栈 (Vue 2 / React) 共存于同一应用. 仅 MF 才能跨框架集成.

4. **超大规模团队 (>50 前端工程师)** — 单仓 monorepo 协作压力达极限, 必须按 plugin 分治 (Spotify-style autonomy).

### 当前都不满足

- ✗ 无外部插件 marketplace 计划
- ✗ EDU/HEALTH 同流水线发版即可
- ✗ 客户 ISV 不存在
- ✗ 团队 < 10 人

启动 MF 的 ROI 显著为负.

## Consequences

### Positive (Defer)
- 前端 Phase 1-5 已经把视图按插件物理隔离 + ESLint 守护, MF 化可一键完成
- 无 MF 复杂度负担, 部署/调试简单
- 单仓 monorepo 仍可通过 `views/plugins/{edu,health}/` 拆 chunk (vite manualChunks 已配)

### Negative (Defer)
- 不能"运行时下载并启用第三方插件" (但当前业务也不需要)
- plugins/edu 与 plugins/health 必须共发版

### Re-evaluation Trigger

**每 6 个月 review 一次** — 检查是否触发以上 4 条任一. Memory 已记录 (`project_plugin_phase4a_complete.md` 加部分). 团队 lead 在 Q3/Q4 review 时 raise 议题.

## Related

- POC 分支: `poc/module-federation`
- POC 发现文档 (memory 记录): `docs/plans/poc-module-federation-findings.md` (未提交但 memory 留底)
- [ADR-005 视图插件化](./005-frontend-view-plugin-isolation.md) — MF 的物理基础
- Memory: `project_plugin_phase4a_complete.md`
