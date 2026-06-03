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
      // RBAC: 用户 / 角色 / 权限 (从 system.ts 迁入, 路径保持 /system/*; 与关系 ReBAC + 数据权限统一)
      {
        path: '/system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/UsersView.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: 'system:user:view', order: 1 }
      },
      {
        path: '/system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/RolesView.vue'),
        meta: { title: '角色管理', requiresAuth: true, permission: 'system:role:view', order: 2 }
      },
      {
        path: '/system/permissions',
        name: 'SystemPermissions',
        component: () => import('@/views/system/PermissionsView.vue'),
        meta: { title: '权限管理', requiresAuth: true, permission: 'system:permission:view', order: 3 }
      },
      // ReBAC + 数据范围
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
          order: 6
        }
      }
    ]
  },
]

export default accessRoutes
