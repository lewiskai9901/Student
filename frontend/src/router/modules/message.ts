import type { RouteRecordRaw } from 'vue-router'

/** Message 模块路由 — 消息与事件. order: 1.5 */
const messageRoutes: RouteRecordRaw[] = [
  {
    path: '/message',
    name: 'MessageCenter',
    redirect: '/message/list',
    meta: {
      title: '消息与事件',
      icon: 'Bell',
      requiresAuth: true,
      order: 1.5,
      group: 'daily'
    },
    children: [
      {
        path: '/message/list',
        name: 'MessageList',
        component: () => import('@/views/message/MessageListView.vue'),
        meta: { title: '消息通知', requiresAuth: true, order: 1 }
      },
      {
        path: '/message/event-types',
        name: 'EventTypes',
        component: () => import('@/views/system/EventTypeManagementView.vue'),
        meta: { title: '事件类型', requiresAuth: true, permission: 'system:config:view', order: 2 }
      },
      {
        path: '/message/triggers',
        name: 'EventTriggers',
        component: () => import('@/views/system/EventTriggerView.vue'),
        meta: { title: '事件触发器', requiresAuth: true, permission: 'system:config:view', order: 3 }
      },
      {
        path: '/message/subscriptions',
        name: 'MessageSubscriptions',
        component: () => import('@/views/message/MessageConfigView.vue'),
        meta: { title: '订阅与模板', requiresAuth: true, permission: 'system:config:view', order: 4 }
      },
      {
        path: '/message/preferences',
        name: 'MessagePreferences',
        component: () => import('@/views/message/MessagePreferencesView.vue'),
        meta: { title: '消息偏好', requiresAuth: true, order: 5 }
      },
    ]
  },
]

export default messageRoutes
