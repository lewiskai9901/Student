# ADR-003: 前端 A 级架构重构 (2026-05-10)

## Status

Accepted — 2026-05-10, branch `feature/frontend-aplus`, commits F1-F8。

## Context

后端在 2026-04 ~ 2026-05 走完插件化 Phase 1-4A、Core Purification、Inspection 通用化、Workflow Engine Phase 1-8 后,综合分稳定在 **A (90)**。前端则长期停留在 **B+ (~78)**:

- TypeScript 错误 160 条、无 type-check 闸,新代码可继续引入新错
- `router/index.ts` 单文件 1212 行,集中懒加载是 `api → utils/request → router → views → api` 循环依赖的核心节点
- 无 dependency-cruiser / 任何架构边界守护
- 无 bundle size CI gate,vendor chunk 体积失控也无人察觉
- 单测 155 用例,覆盖核心 store/util/api 不足
- 路由权限校验散落在 view 内部 `hasPermission()` 调用,无统一守门
- 无 i18n 基础设施(`useI18n.ts` composable 是手搓的本地 ref,与 vue-i18n 生态隔绝)
- 无 ADR、无路线图文档

为了让前端达到与后端相称的工程门槛,在 1 天内密集推完 8 阶段重构。

## Decision

### F1 — TypeScript baseline 闸 (commit `735efca7`)

- 一次性修 100+ 类型错误,基线从 **160 → 35**
- `scripts/type-check-ceiling.sh` 比对 vue-tsc 错误数 vs ceiling,新增即 fail
- 不追"零错误":35 条多为第三方类型 + 历史 any 收口,逐次递降,基线钉死
- CI 强校验,新代码不许引入新错

### F2 — 单测密度提升 (commit `15371b68`)

- 净增 193 用例:**155 → 348**
- 覆盖范围:核心 stores(auth/sidebar/theme/locale)、utils(request/permission/format)、api 模块、关键业务视图
- vitest 7.4s 全跑完,低门槛持续补充
- jsdom + happy-dom 混合环境,Element Plus 组件挂载验证

### F3 — dependency-cruiser 6 规则 (commit `13ba5066`)

- `no-circular`:warn 级(F4 router 拆分后已具备升 error 条件,见 follow-up)
- `no-orphans`:warn,识别死代码
- `utils-no-business-import`:warn(`request.ts → stores/auth` 已知豁免)
- `composables-no-views-import`:**error**,反向依赖直接拒
- `no-direct-element-plus-deep-import`:**error**,禁 `element-plus/es/...` 深 import
- `views-cant-cross-import-stores`:warn,跨业务 store 通过 composable 中转

### F4 — router/index.ts 拆 plugin manifest (commit `1df1a8f3`)

- 1212 行 → 197 行入口 + 11 个模块文件 (`src/router/modules/{dashboard,message,access,organization,place,inspection,asset,workflow,system,event,test}.ts`)
- 加 2 个插件路由 (`src/router/plugins/{edu,health}.ts`),对偶后端 `RouteContribution` sealed pattern
- 入口仅做 createRouter + auth 守护 + 模块装载,无业务路由定义
- 总行数 1793,平均 ~102/模块,可读性大幅提升

### F5 — bundle size CI gate (commit `911cb648`)

- `.bundle-size-baseline.json`:totalBytes / largestChunkBytes 双上限
- baseline = 实际值 + 5% 余量,超出即 fail
- `scripts/bundle-size-ceiling.sh` + `npm run build:check-size`
- 当前测量 total=5461025 / largest=824903 (vendor-*.js)

### F6 — router 权限统一守护 (commit `a34e21c9`)

- `beforeEach` 集中校验 `route.meta.permissions`,任一不满足 → 跳 403
- view 不再需要手写 `hasPermission()` 守门(UI 显隐场景仍可用,但路由级一刀切)
- 9 个新单测覆盖匿名/已登录/缺权限/角色匹配/多权限求并集/路径白名单/嵌套路由继承/重定向

### F7 — vue-i18n 骨架 (commit `d487d1f2`)

- `vue-i18n@9` 装好,`src/locales/{zh-CN,en-US,index}.ts` 顶层 key 框架
- demo view 验证切换链路(`localStorage.app:locale` 持久化)
- 后续按模块逐步迁移既有 hard-coded 中文

### F8 — ADR + 完结报告 (本 commit)

- 本 ADR-003 + `docs/reports/frontend-aplus-final.md` 收官
- 评分对比 + commit 序列 + roadmap forward

## Consequences

### 正面

1. **类型安全显式化**:基线递降而非"零容忍",平衡现实与方向
2. **架构边界自动化**:6 条 depcheck 规则 + 1 条路由权限守护,新人 PR 误入即 fail
3. **bundle 失控自动拦截**:5% 余量给正常增长留空间,膨胀即 fail
4. **路由权限单点统一**:`beforeEach` 集中校验,view 内 `hasPermission()` 仅用于 UI 显隐(7 处遗留,F8 记账,非路由级可不强清)
5. **测试速度 7.4s**:348 用例,低门槛持续补充,日常迭代无负担
6. **可观测**:type-check / depcheck / bundle / vitest 4 个数字一目了然

### 负面 (已知 follow-up)

1. **depcheck circular = 403,持平** —— 根因是 `utils/request.ts` 401 拦截器反引 `router` 实例触发跳转。需懒加载抽 composable 解耦后才能升 error 级。本期基础设施已具备,降级到下一轮做。
2. **vue-i18n 与既有 `useI18n.ts` composable 双轨** —— `app:locale` (vue-i18n) vs `app-locale` (老 composable) 不同 key,互不感知。后续逐步统一到 vue-i18n。
3. **既有 view 仍内嵌 `hasPermission()` 守门** —— 7 处使用,UI 显隐(按钮/菜单)场景非路由级,不强清。
4. **bundle 最大 chunk 805KB (vendor-*.js)** —— 可拆 3 块(Element Plus / Vue 全家桶 / echarts/quill 等富组件),手动 manualChunks 配置。本期不动,先看 ceiling 是否触发。
5. **i18n 仅骨架** —— nav + 50+ 高频 view 字符串未迁移。
6. **Playwright e2e 覆盖少** —— 仅 F4 时新增的 2 个插件路由 e2e。

### Roadmap forward (可选)

A+ 路线(单仓单体 Vue 的天花板,做完才有意义):

- **circular = 0**:`request.ts` 懒加载 router,改 inject token getter,解耦 stores/auth
- **bundle 再拆 lazy chunk**:vendor 805KB → 3×~270KB
- **完整 i18n 迁移**:覆盖 nav + 50+ 高频 view,后端 message catalog 也接入
- **Playwright 高覆盖**:核心 5 大模块(dashboard/access/inspection/organization/workflow)各 1 条主流程
- **Module Federation**:POC 已存(`poc/module-federation` 分支),不启动正式迁移除非出现以下任一商业触发信号:
  - 外部开发者生态需要独立交付前端插件
  - 单仓 build 时间突破 5min
  - 多团队并行频繁触发 merge 冲突
  - 第三方插件 marketplace 商业化

## References

- 后端对偶 ADR:`backend/docs/adr/ADR-002-workflow-engine.md`(Workflow Phase 1-8)
- 完结报告:`frontend/docs/reports/frontend-aplus-final.md`
- POC 分支:`poc/module-federation`(2026-04 完成,findings: `docs/plans/poc-module-federation-findings.md`)
