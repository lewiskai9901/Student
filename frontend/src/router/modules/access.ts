import type { RouteRecordRaw } from 'vue-router'

/** Access 模块路由 — 关系管理. order: 1.7 */
const accessRoutes: RouteRecordRaw[] = [
  {
    path: '/access',
    name: 'AccessCenter',
    redirect: '/access/relations',
    meta: {
      title: '关系管理',
      icon: 'Connection',
      requiresAuth: true,
      order: 1.7,
      group: 'daily'
    },
    children: [
      {
        path: '/access/relations',
        name: 'RelationManager',
        component: () => import('@/views/access/RelationManagerView.vue'),
        meta: { title: '关系管理', requiresAuth: true, permission: 'system:config:view', order: 1 }
      },
      {
        path: '/access/relation-types',
        name: 'RelationTypes',
        component: () => import('@/views/access/RelationTypesView.vue'),
        meta: { title: '关系字典', requiresAuth: true, permission: 'system:config:view', order: 2 }
      },
      {
        path: '/access/data-permissions',
        name: 'DataPermissions',
        component: () => import('@/views/access/data-permissions/DataPermissionsLayout.vue'),
        // 数据权限配置高敏 — 同时要求 role:view 和 config:view
        meta: {
          title: '数据权限',
          requiresAuth: true,
          permission: 'system:role:view',
          permissions: ['system:role:view', 'system:config:view'],
          order: 3
        }
      }
    ]
  },
]

export default accessRoutes
