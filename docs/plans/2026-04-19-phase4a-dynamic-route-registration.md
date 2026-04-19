# Phase 4A 前端动态路由注册 (非 Module Federation)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 启用/禁用 EDU 插件包后, 前端路由自动跟随. 禁用 EDU → `/student/list` 返回 404; 重新启用 → 恢复。用最小成本达成 Phase 4 陈述目标, **不引入 Module Federation**。

**Architecture:** 把路由拆为 `router/index.ts`(核心路由, 不动) + `router/plugins/edu.ts`(教育路由, 条件加载). 新 `router/bootstrap.ts` 在 app mount 前或 login 后拉 `/api/plugin-platform/overview`, 对 enabled 的非 CORE 行业码动态 `import('./plugins/{code}.ts')` 并 `router.addRoute('Layout', r)`. Element Plus / Pinia / 所有 Vue 深度依赖仍属同一 bundle, 无 shared 边界问题, 无额外 build chunk。

**Tech Stack:** Vite 4.4.9 + Vue 3.3.4 + vue-router 4.2.5 + Pinia 2.1.6 + Element Plus 2.4.1。零新增依赖。

---

## 代码布局变化

```
frontend/src/router/
├── index.ts              ← 只留核心 + Layout + 404 兜底 (login/dashboard/message/access/
│                             organization/inspection/system/place/asset/event/profile/my)
├── bootstrap.ts          ← 新: loadEnabledPlugins(router)
└── plugins/
    └── edu.ts            ← 新: /my-class/* /academic/* /student/* /teaching/* /dormitory/*
                             + 6 个 /organization/* → /student|academic 的 legacy redirect
```

## EDU 路由清单 (已清点, 从 index.ts 原 148~1176 行抽出)

- `/my-class` + 2 children (order: 2)
- `/academic` + 3 children (order: 3)
- `/student` + 6 children (order: 4) — /enrollment /list /classes /grades /attendance /warnings
- `/teaching` + 9 children (order: 20)
- `/dormitory` + children (order: 11.5)
- 6 个 /organization/ 下的 legacy redirect (students/grades/majors/attendance/warnings/dormitory)

**CORE 路由 (留在 index.ts)**: login / inspection/v7/big-screen / dashboard / my/dashboard / profile / message / access / organization/units / inspection/v7/* (V7 是通用检查平台, 后端 Phase 3.5 未迁) / system / place / asset / event / 测试页面。

## API 契约 (后端已就绪, 无需改动)

- `GET /api/plugin-platform/overview` → `{data: {industries: [{code, enabled, ...}]}}`
- `POST /api/plugin-platform/enable/{code}`
- `POST /api/plugin-platform/disable/{code}`

这些 API 现有 `PluginPlatformController` 已实现。

---

### Task 1: 写 plugins/edu.ts

**Files:**
- Create: `frontend/src/router/plugins/edu.ts` (export default `RouteRecordRaw[]`)

**Step 1:** 从 `router/index.ts` 的 6 个 EDU 代码块复制路由节点到新文件, 去掉最外层 Layout 包裹, 导出为 `RouteRecordRaw[]`.

**Step 2:** `mvn` 等价的前端验证: `npm run type-check` 通过.

---

### Task 2: 从 index.ts 移除 EDU 路由

**Files:**
- Modify: `frontend/src/router/index.ts`

**Step 1:** 按清单删除对应行块, 用单行占位注释 `// EDU routes loaded dynamically via bootstrap.ts`.

**Step 2:** 确保保留的内部闭合括号/逗号语法正确 (Vite dev 模式下会立刻语法报错)。

---

### Task 3: 写 bootstrap.ts

**Files:**
- Create: `frontend/src/router/bootstrap.ts`

**Step 1:** 实现:

```ts
import type { Router, RouteRecordRaw } from 'vue-router'
import request from '@/utils/request'

const PLUGIN_LOADERS: Record<string, () => Promise<{ default: RouteRecordRaw[] }>> = {
  EDU: () => import('./plugins/edu')
  // 未来: HEALTH / CARE 等
}

const loaded = new Set<string>()

export async function loadEnabledPlugins(router: Router): Promise<void> {
  try {
    const res = await request.get('/plugin-platform/overview')
    const industries: Array<{ code: string; enabled: boolean }> = res.data.industries
    for (const ind of industries) {
      if (ind.code === 'CORE' || !ind.enabled) continue
      if (loaded.has(ind.code)) continue
      const loader = PLUGIN_LOADERS[ind.code]
      if (!loader) {
        console.warn(`[plugin-bootstrap] 无对应前端 loader: ${ind.code}`)
        continue
      }
      const mod = await loader()
      for (const route of mod.default) {
        router.addRoute('Layout', route)
      }
      loaded.add(ind.code)
      console.info(`[plugin-bootstrap] 加载 ${ind.code} ${mod.default.length} 顶级路由`)
    }
  } catch (e: any) {
    // 401 / 403 / 网络失败: 静默. 用户下次登录成功后会再次调用.
    if (e?.response?.status === 401 || e?.response?.status === 403) return
    console.error('[plugin-bootstrap] 加载插件路由失败:', e)
  }
}
```

**关键设计**:
- `loaded` Set 防重复注册 (login → re-login 场景)
- 只对 `enabled=true && code !== 'CORE'` 的行业加载
- 未知 industry code 打 warning 而不是 crash — 允许后端先上新行业再部署前端
- 失败降级: 401/403 静默 (登录前或 token 失效), 其他打错并继续 (不阻塞 app 启动)

---

### Task 4: main.ts 钩子

**Files:**
- Modify: `frontend/src/main.ts`

**Step 1:** 在 `authStore.initAuth()` 之后, `app.mount('#app')` 之前:

```ts
import { loadEnabledPlugins } from './router/bootstrap'

authStore.initAuth()
if (authStore.isAuthenticated) {
  await loadEnabledPlugins(router)
}
```

注意: 整个 main.ts 需要变成 async 上下文 (Vite 顶层 await 默认支持 ES2022, 目标浏览器应 ok)。

---

### Task 5: auth store 登录钩子

**Files:**
- Modify: `frontend/src/stores/auth.ts`

**Step 1:** `loginAction` 在 `setToken(accessToken)` 之后, 调用 bootstrap:

```ts
import { loadEnabledPlugins } from '@/router/bootstrap'
import router from '@/router'

// 在 loginAction 结尾:
await loadEnabledPlugins(router)
return response
```

注意: 避免循环依赖 (auth store ↔ router ↔ bootstrap). 用 dynamic import `await import('@/router')` 打断循环。

---

### Task 6: Dev 验证

**Step 1:** 后端启动: `cd backend && mvn spring-boot:run -DskipTests`

**Step 2:** 前端启动: `cd frontend && npm run dev` (端口 3000)

**Step 3:** 浏览器测试:
1. 打开 `http://localhost:3000/login`, admin/admin123 登录
2. 访问 `/student/list` → 应正常渲染 (EDU 默认 enabled)
3. 浏览器控制台应看到 `[plugin-bootstrap] 加载 EDU 6 顶级路由` (或类似)
4. 开另一个 terminal 禁用 EDU:
   ```bash
   TOKEN=$(curl -sX POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)
   curl -sX POST localhost:8080/api/plugin-platform/disable/EDU -H "Authorization: Bearer $TOKEN"
   ```
5. 前端硬刷新 (Ctrl+F5) → 重新登录 → 访问 `/student/list` → 应 404 或跳 NotFound
6. 重新启用 EDU: `curl -sX POST localhost:8080/api/plugin-platform/enable/EDU -H "Authorization: Bearer $TOKEN"`
7. 前端再刷新 → `/student/list` 恢复

**Step 4:** 确保:
- 登录流程正常
- 菜单 (MainLayout 侧栏) 在禁用 EDU 后不显示教育菜单项 (因为菜单也靠 route 存在或 `/api/menus/my` 过滤)
- 控制台无 TypeScript 编译错误

---

### Task 7: 提交

**Files staged:**
- `frontend/src/router/plugins/edu.ts` (NEW)
- `frontend/src/router/bootstrap.ts` (NEW)
- `frontend/src/router/index.ts` (MODIFIED, 减行数)
- `frontend/src/main.ts` (MODIFIED)
- `frontend/src/stores/auth.ts` (MODIFIED)
- `docs/plans/2026-04-19-phase4a-dynamic-route-registration.md` (NEW)
- `docs/plugin-architecture-status.md` (MODIFIED, Phase 4A 完成, A+ 14/15→15/15)

提交消息概要: `feat(plugin-arch): Phase 4A — 动态路由注册 (无 MF)`

---

## 风险与应对

| 风险 | 概率 | 应对 |
|---|---|---|
| Overview API 响应结构跟我假设不一样 | 低 | 先 grep 后端代码确认 ok |
| `addRoute('Layout', r)` 失败 | 低 | Layout 是命名路由 (已确认), 标准用法 |
| 顶层 await 打不进 build | 极低 | Vite 4 + ES2022 原生支持 |
| 循环依赖 auth↔router↔bootstrap | 中 | bootstrap 动态 import router, 或 auth 动态 import router |
| 禁用 EDU 后侧栏仍显示教育菜单 | 中 | 菜单有独立 `/api/menus/my` 或 route.meta 驱动, 需二次验证 |
| 重新启用 EDU 后路由已注册但刷新才生效 | 低 | 可接受 — UX 合理, 真切 "下次会话生效" |

## 回滚

单 commit 失败: `git reset --hard HEAD~1`。之前 3.5 的 commit 不受影响。
