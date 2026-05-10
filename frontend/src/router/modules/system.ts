import type { RouteRecordRaw } from 'vue-router'

/** System 模块路由 — 系统管理. order: 30. 合并权限管理+系统设置 */
const systemRoutes: RouteRecordRaw[] = [
  {
    path: '/system',
    name: 'System',
    redirect: '/system/users',
    meta: {
      title: '系统管理',
      icon: 'Setting',
      requiresAuth: true,
      order: 30,
      group: 'system'
    },
    children: [
      {
        path: '/system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/UsersView.vue'),
        meta: {
          title: '用户管理',
          requiresAuth: true,
          permission: 'system:user:view',
          order: 1
        }
      },
      {
        path: '/system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/RolesView.vue'),
        meta: {
          title: '角色管理',
          requiresAuth: true,
          permission: 'system:role:view',
          order: 2
        }
      },
      {
        path: '/system/permissions',
        name: 'SystemPermissions',
        component: () => import('@/views/system/PermissionsView.vue'),
        meta: {
          title: '权限管理',
          requiresAuth: true,
          permission: 'system:permission:view',
          order: 3
        }
      },
      {
        path: '/system/entity-types',
        name: 'EntityTypeConfig',
        component: () => import('@/views/system/EntityTypeConfig.vue'),
        meta: {
          title: '类型配置',
          requiresAuth: true,
          permission: 'system:config:view',
          order: 4
        }
      },
      {
        path: '/system/plugins',
        name: 'PluginManagement',
        component: () => import('@/views/system/plugin-platform/PluginPlatformLayout.vue'),
        meta: {
          title: '插件平台',
          requiresAuth: true,
          permission: 'system:config:view',
          order: 5
        }
      },
      // 旧路由重定向到统一类型配置
      { path: '/system/org-types', redirect: '/system/entity-types', meta: { hidden: true } },
      { path: '/system/place-types', redirect: '/system/entity-types', meta: { hidden: true } },
      { path: '/system/user-types', redirect: '/system/entity-types', meta: { hidden: true } },
      // 事件类型和触发器已移到"消息与事件"菜单下
      {
        path: '/system/configs',
        name: 'SystemConfigs',
        component: () => import('@/views/system/SystemConfigsView.vue'),
        meta: {
          title: '系统配置',
          requiresAuth: true,
          permission: 'system:config:view',
          order: 7
        }
      },
      {
        path: '/system/login-customization',
        name: 'SystemLoginCustomization',
        component: () => import('@/views/settings/LoginCustomization.vue'),
        meta: {
          title: '登录页自定义',
          requiresAuth: true,
          permission: 'system:config:edit',
          order: 8
        }
      },
      {
        path: '/system/design-system',
        name: 'SystemDesignSystem',
        component: () => import('@/views/system/DesignSystemView.vue'),
        meta: {
          title: '设计系统',
          requiresAuth: true,
          permission: 'system:config:view',
          order: 9
        }
      },
      {
        path: '/system/semesters',
        name: 'SystemSemesters',
        component: () => import('@/views/system/SemesterView.vue'),
        meta: {
          title: '学期管理',
          requiresAuth: true,
          permission: 'system:semester:view',
          order: 9
        }
      },
      {
        path: '/system/announcements',
        name: 'SystemAnnouncements',
        component: () => import('@/views/system/AnnouncementsView.vue'),
        meta: {
          title: '公告管理',
          requiresAuth: true,
          permission: 'system:announcement:view',
          order: 10
        }
      },
      {
        path: '/system/operation-logs',
        name: 'SystemOperationLogs',
        component: () => import('@/views/system/OperationLogsView.vue'),
        meta: {
          title: '操作日志',
          requiresAuth: true,
          permission: 'system:operlog:view',
          order: 11
        }
      },
      {
        path: '/system/tenants',
        name: 'SystemTenants',
        component: () => import('@/views/system/TenantsView.vue'),
        meta: {
          title: '租户管理',
          requiresAuth: true,
          permission: 'system:tenant:view',
          order: 12
        }
      },
      {
        path: '/system/teachers',
        name: 'TeacherProfiles',
        component: () => import('@/views/system/TeacherProfileView.vue'),
        meta: {
          title: '教师档案',
          requiresAuth: true,
          permission: 'system:teacher:view',
          order: 13
        }
      },
      {
        path: '/system/audit',
        name: 'AuditTrail',
        component: () => import('@/views/system/AuditTrailView.vue'),
        meta: {
          title: '审计日志',
          requiresAuth: true,
          permission: 'system:audit:view',
          order: 14
        }
      },
    ]
  },
]

export default systemRoutes
