# ADR 003: Phase 4A — 前端动态路由注册替代 Module Federation

- **Status**: Accepted
- **Date**: 2026-04-19
- **Commits**: `d8ecf1c3` `e37234b1` (侧栏响应式补丁)

## Context

原计划 Phase 4 用 **Module Federation** (vite-plugin-federation) 实现"禁用 EDU 后前端自动失去学生/教务/等路由". 评估后发现:

1. **MF 真正解决的问题**: 独立部署 remote / 多团队分仓 / 运行时加载第三方插件
2. **本项目需要**: 仅"禁用 EDU 后 /student/list 访问返 404". 单仓单体, 一套前端 build
3. **MF 成本**: 2-3 周 POC + 踩坑. 需要把前端拆 host + remote 两个 vite 项目, remote 只能 `vite build` 丧失 HMR, shared deps (Element Plus/Pinia) singleton 配置复杂, CSS 注入顺序问题, 发布上架 1+ 月

用 MF 解决本项目问题等价于**用飞机载外卖过马路**.

## Decision

**动态路由注册**: 在 Vue Router 4 的基础上, bootstrap 阶段按后端 enabled industries 动态 `router.addRoute()`.

```ts
// frontend/src/router/bootstrap.ts
const PLUGIN_LOADERS: Record<string, () => Promise<{ default: RouteRecordRaw[] }>> = {
  EDU:    () => import('./plugins/edu'),
  HEALTH: () => import('./plugins/health')
}

export async function loadEnabledPlugins(router: Router): Promise<void> {
  const overview = await request.get('/plugin-platform/overview')
  for (const ind of overview.industries) {
    if (ind.code === 'CORE' || !ind.enabled) continue
    if (store.isLoaded(ind.code)) continue
    const mod = await PLUGIN_LOADERS[ind.code]()
    for (const route of mod.default) {
      router.addRoute('Layout', route)
      layoutRoute.children.push(route) // ⚠️ 必须 push 到 children, 菜单响应式才生效
    }
    store.markLoaded(ind.code)
  }
}
```

配套:
- `router/plugins/edu.ts` / `health.ts` 按行业拆文件, 核心路由 (login/dashboard/system) 留 `router/index.ts`
- `main.ts` 启动时拉一次 + `stores/auth.ts` login 后再拉一次
- Pinia `usePluginsStore` 追踪 loadedCodes 驱动 MainLayout.menuList 响应式 (Phase 6.4 补丁)

## Considered Alternatives

### A. Module Federation (vite-plugin-federation)
否决, 见 Context. 做了 POC 证明技术可行, 留在分支 `poc/module-federation`, **不合 master**. 见 [POC findings](../../plans/poc-module-federation-findings.md)

### B. 全静态, 用 permission 过滤
现有的 `/api/menus/my` 已经按权限过滤 sidebar. 但**路由本身仍注册**, 用户直接访问 URL 可达. 无法做到"真 404". 否决

### C. iframe 隔离
过重 + 体验割裂 + 跨域通信复杂. 否决

## Consequences

### 正向
- 0 新依赖 (只用 vue-router 原生 addRoute)
- HMR / 单 vite build 完全不变
- Element Plus / Pinia 天然 singleton, 零陷阱
- 禁用 EDU 冷启动后 `/student/list` 确实 404 (NotFound 组件渲染), 验证通过 Playwright e2e 双测 + 浏览器手动回归
- 未来如果真要做独立部署, 路径很短: `PLUGIN_LOADERS[code] = () => loadRemoteModule(...)` 即可平滑升级到 MF

### 负向
- **所有行业代码仍打进 host bundle**: 开/关 EDU 只影响**路由可达性**, bundle 大小不变. 真正缩减 bundle 要走 MF. 当前不是痛点
- `router.addRoute('Layout', r)` 只更新 matcher **不**更新 `layoutRoute.children` 数组. MainLayout.menuList 从 `children` 读, 导致菜单不刷新. bug 带了 1 commit 才被 e37234b1 修 (补 push 到 children)
- `PLUGIN_LOADERS` 是硬编码表. 新行业要改前端一行. 未来可考虑把 loader 做成基于约定的 `import(\`./plugins/${code.toLowerCase()}\`)`

### 踩过的坑 (后人避雷)
1. **auth.ts import router 会循环依赖**: 用 `await import('@/router')` 动态 import 打破环
2. **MainLayout.menuList computed 依赖 router.getRoutes()** — router 不是 Vue reactive; Phase 6.4 加 Pinia store 驱动响应式
3. **Playwright e2e 原写的 `not.toContainText('NotFound')` 恒真** (NotFound.vue 里是中文"页面不存在"). 升级为 POSITIVE 断言 `toContainText('学生列表')` 和 sidebar 含 4 EDU 菜单

## Revisit 条件

如以下任一出现, 启动 Phase 4 MF 正式迁移 (基于 POC 分支):
1. 开始做 OEM 独立部署, 不同客户装不同 industry 插件, 且插件需要各自 CI/CD
2. 出现外部开发者生态, 要让第三方写插件 .js 装入 host
3. bundle 大小成为 critical path (当前没接近)
