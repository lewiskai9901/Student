import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getToken, getTokenRemainingTime, isTokenExpired } from '@/utils/token'

// 模块导入 (F4: 路由拆分为 plugin manifest 模块)
import dashboardRoutes from './modules/dashboard'
import messageRoutes from './modules/message'
import accessRoutes from './modules/access'
import organizationRoutes from './modules/organization'
import placeRoutes from './modules/place'
import inspectionRoutes from './modules/inspection'
import assetRoutes from './modules/asset'
import workflowRoutes from './modules/workflow'
import systemRoutes from './modules/system'
import eventRoutes from './modules/event'
import testRoutes from './modules/test'

// Token刷新阈值：剩余时间少于5分钟时触发刷新
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000 // 5 minutes in milliseconds

// 防止并发刷新的标志
let isRefreshing = false

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  // 大屏展示（独立全屏页面）
  {
    path: '/inspection/big-screen',
    name: 'BigScreen',
    component: () => import('@/views/inspection/analytics/BigScreenView.vue'),
    meta: {
      title: '检查大屏',
      requiresAuth: true
    }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/inspection',
    meta: {
      requiresAuth: true
    },
    children: [
      ...dashboardRoutes,
      ...messageRoutes,
      ...accessRoutes,
      // EDU routes (my-class / academic / student) loaded dynamically via bootstrap.ts
      ...organizationRoutes,
      ...inspectionRoutes,
      // EDU routes (teaching) loaded dynamically via bootstrap.ts
      ...systemRoutes,
      ...placeRoutes,
      // EDU routes (dormitory) loaded dynamically via bootstrap.ts
      ...assetRoutes,
      ...workflowRoutes,
      ...eventRoutes,
      // 测试页面 - 仅在开发环境可用
      ...(import.meta.env.DEV ? testRoutes : []),
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: {
      title: '没有权限'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题 — 系统名取自 configStore (后台 system_config 可覆盖)
  if (to.meta?.title) {
    try {
      const { useConfigStore } = await import('@/stores/config')
      const cs = useConfigStore()
      document.title = `${to.meta.title} - ${cs.systemName || '通用管理平台'}`
    } catch {
      document.title = `${to.meta.title} - 通用管理平台`
    }
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const token = getToken()

    // 如果Token已过期，跳转登录页
    if (isTokenExpired(token)) {
      // 清理认证状态
      await authStore.logoutAction()
      next('/login')
      return
    }

    // 检查是否需要刷新Token（距离过期不足5分钟）
    const remainingTime = getTokenRemainingTime(token)
    if (remainingTime > 0 && remainingTime < TOKEN_REFRESH_THRESHOLD && !isRefreshing) {
      isRefreshing = true
      try {
        await authStore.refreshTokenAction()
      } catch (error) {
        // Token刷新失败，重新登录
        await authStore.logoutAction()
        next('/login')
        return
      } finally {
        isRefreshing = false
      }
    }

    // 再次检查认证状态（刷新后）
    if (!authStore.isAuthenticated) {
      next('/login')
      return
    }

    // 检查权限
    if (to.meta.permission && !authStore.hasPermission(to.meta.permission as string)) {
      // /dashboard 专属兜底：教师或任何无 dashboard:view 权限的用户改去 /my/dashboard
      // —— /my/dashboard 不受权限门控，对所有已登录用户开放
      if (to.path === '/dashboard' && to.meta.permission === 'dashboard:view') {
        next('/my/dashboard')
        return
      }
      next('/403')
      return
    }
  }

  next()
})

export default router
