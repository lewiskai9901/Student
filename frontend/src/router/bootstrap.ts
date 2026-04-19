import type { Router, RouteRecordRaw } from 'vue-router'
import request from '@/utils/request'

/**
 * Phase 4A — 动态路由注册
 *
 * 启动/登录后调用 loadEnabledPlugins(router), 从后端拉 /api/plugin-platform/overview,
 * 对 enabled === true 且非 CORE 的行业码, 动态 import 对应 plugins/*.ts 并注册到 Layout 下.
 *
 * 特性:
 * - 幂等: 用 loaded Set 防重复注册 (login/re-login 场景)
 * - 降级: 401/403 静默 (登录前或 token 失效), 其他 log 但不 throw (不阻塞 app 启动)
 * - 扩展: 新行业只需在 PLUGIN_LOADERS 里加一条 loader
 */

const PLUGIN_LOADERS: Record<string, () => Promise<{ default: RouteRecordRaw[] }>> = {
  EDU: () => import('./plugins/edu')
  // 未来行业:
  // HEALTH: () => import('./plugins/health'),
  // CARE:   () => import('./plugins/care'),
}

const loaded = new Set<string>()

interface Industry {
  code: string
  enabled: boolean
}

interface Overview {
  industries: Industry[]
}

export async function loadEnabledPlugins(router: Router): Promise<void> {
  try {
    // request 拦截器已把 response.data 展开, 这里直接拿到业务 data
    const overview = (await request.get('/plugin-platform/overview')) as unknown as Overview
    const industries = overview?.industries ?? []

    for (const ind of industries) {
      if (ind.code === 'CORE' || !ind.enabled) continue
      if (loaded.has(ind.code)) continue

      const loader = PLUGIN_LOADERS[ind.code]
      if (!loader) {
        console.warn(`[plugin-bootstrap] 无对应前端 loader: ${ind.code} (后端已启用但前端未注册)`)
        continue
      }

      const mod = await loader()
      const routes = mod.default
      // router.addRoute('Layout', r) 会注册到 matcher, 但不会 push 到 layoutRoute.children 数组;
      // MainLayout 的 menuList 是 router.getRoutes().find('Layout').children — 需手动同步.
      const layoutRoute = router.getRoutes().find(r => r.name === 'Layout')
      for (const route of routes) {
        router.addRoute('Layout', route)
        if (layoutRoute?.children && !layoutRoute.children.some(c => c.path === route.path)) {
          layoutRoute.children.push(route as never)
        }
      }
      loaded.add(ind.code)
      console.info(`[plugin-bootstrap] 加载 ${ind.code} ${routes.length} 顶级路由`)
    }
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    // 401/403: 登录前或 token 失效 — 静默. 下次登录成功后会再调.
    if (status === 401 || status === 403) return
    console.error('[plugin-bootstrap] 加载插件路由失败:', err)
  }
}

/**
 * 仅用于测试: 清空已加载状态. 生产环境不用.
 */
export function __resetLoadedForTest(): void {
  loaded.clear()
}
