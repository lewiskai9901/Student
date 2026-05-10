import type { RouteRecordRaw } from 'vue-router'

/** Organization 模块路由 — 组织管理. order: 10. Organization领域 */
const organizationRoutes: RouteRecordRaw[] = [
  {
    path: '/organization',
    name: 'Organization',
    redirect: '/organization/units',
    meta: {
      title: '组织管理',
      icon: 'Hierarchy',
      requiresAuth: true,
      order: 10,
      group: 'business'
    },
    children: [
      {
        path: '/organization/units',
        name: 'OrgUnits',
        component: () => import('@/views/organization/structure/OrgStructure.vue'),
        meta: {
          title: '组织架构',
          requiresAuth: true,
          permission: 'system:org:view',
          order: 1
        }
      },
      // Old route redirects for backward compatibility
      {
        path: '/organization/students',
        redirect: '/student/list',
        meta: { hidden: true }
      },
      {
        path: '/organization/grades',
        redirect: '/student/grades',
        meta: { hidden: true }
      },
      {
        path: '/organization/majors',
        redirect: '/academic/majors',
        meta: { hidden: true }
      },
      {
        path: '/organization/attendance',
        redirect: '/student/attendance',
        meta: { hidden: true }
      },
      {
        path: '/organization/warnings',
        redirect: '/student/warnings',
        meta: { hidden: true }
      },
      // 宿舍管理旧路由重定向
      {
        path: '/organization/dormitory',
        redirect: '/dormitory',
        meta: { hidden: true }
      }
    ]
  },
]

export default organizationRoutes
