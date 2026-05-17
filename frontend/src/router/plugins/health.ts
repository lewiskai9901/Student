import type { RouteRecordRaw } from 'vue-router'

/**
 * 医疗行业 (HEALTH) 路由 — Phase 4A 扩展示例
 *
 * 仅在 /api/plugin-platform/overview 返回 HEALTH.enabled=true 时注册
 * (见 router/bootstrap.ts 的 PLUGIN_LOADERS).
 */
const healthRoutes: RouteRecordRaw[] = [
  {
    path: '/patient',
    name: 'Patient',
    redirect: '/patient/list',
    meta: {
      title: '病人管理',
      icon: 'HeartPulse',
      requiresAuth: true,
      order: 5,
      group: 'business'
    },
    children: [
      {
        path: '/patient/list',
        name: 'PatientList',
        component: () => import('@/views/plugins/health/healthcare/PatientList.vue'),
        meta: {
          title: '病人列表',
          requiresAuth: true,
          permission: 'health:patient:view',   // J6: 示例插件之前完全无 perm 门控
          order: 1
        }
      }
    ]
  }
]

export default healthRoutes
