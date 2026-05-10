import type { RouteRecordRaw } from 'vue-router'

/** Dashboard 模块路由 — 挂载到 Layout 子级. order: 1 */
const dashboardRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: {
      title: '首页',
      icon: 'House',
      requiresAuth: true,
      permission: 'dashboard:view',
      order: 1,
      group: 'daily'
    }
  },
  // 教师工作台 — TEACHER 登录后自动落地；其它用户也可访问但为空态
  {
    path: '/my/dashboard',
    name: 'MyDashboard',
    component: () => import('@/views/my/MyDashboard.vue'),
    meta: {
      title: '工作台',
      icon: 'Briefcase',
      requiresAuth: true,
      order: 1,
      group: 'daily',
      hidden: true
    }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/ProfileView.vue'),
    meta: {
      title: '个人资料',
      requiresAuth: true,
      hidden: true
    }
  },
]

export default dashboardRoutes
