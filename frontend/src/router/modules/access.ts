import type { RouteRecordRaw } from 'vue-router'

/** Access 模块路由 — 访问控制 (关系 / 关系字典 / 数据权限). order: 1.7 */
const accessRoutes: RouteRecordRaw[] = [
  {
    path: '/access',
    name: 'AccessCenter',
    redirect: '/system/users',
    meta: {
      title: '访问控制',
      icon: 'Connection',
      requiresAuth: true,
      order: 1.7,
      group: 'daily'
    },
    children: [
      {
        path: '/system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/UsersView.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: 'system:user:view', order: 1 }
      },
      // 角色权限工作台 — 合并原「角色管理 / 权限管理 / 数据权限」三页:
      //   角色模式 = explorer + 基本信息/功能权限/数据权限 tabs; 权限目录模式 = 全局只读权限字典.
      {
        path: '/access/console',
        name: 'AccessConsole',
        component: () => import('@/views/access/AccessConsoleView.vue'),
        meta: {
          title: '角色权限',
          requiresAuth: true,
          permission: 'system:role:view',
          permissions: ['system:role:view', 'system:config:view'],
          order: 2
        }
      },
      // ReBAC 关系
      {
        path: '/access/relations',
        name: 'RelationManager',
        component: () => import('@/views/access/RelationManagerView.vue'),
        meta: { title: '关系绑定', requiresAuth: true, permission: 'system:config:view', order: 4 }
      },
      {
        path: '/access/relation-types',
        name: 'RelationTypes',
        component: () => import('@/views/access/RelationTypesView.vue'),
        meta: { title: '关系字典', requiresAuth: true, permission: 'system:config:view', order: 5 }
      }
    ]
  },
]

export default accessRoutes
