import type { RouteRecordRaw } from 'vue-router'

/** Test 模块路由 — 仅在开发环境可用 (调用方须用 import.meta.env.DEV 包裹) */
const testRoutes: RouteRecordRaw[] = [
  {
    path: '/test/pagination',
    name: 'PaginationTest',
    component: () => import('@/views/test/PaginationTest.vue'),
    meta: {
      title: '分页测试',
      requiresAuth: true,
      hidden: true
    }
  },
]

export default testRoutes
