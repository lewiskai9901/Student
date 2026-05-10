import type { RouteRecordRaw } from 'vue-router'

/** Event 模块路由 — Phase 3.4: 实体事件时间线 (隐藏路由,通过详情页跳转) */
const eventRoutes: RouteRecordRaw[] = [
  {
    path: '/entity-events/:subjectType/:subjectId',
    name: 'EntityEventTimeline',
    component: () => import('@/views/event/EntityEventTimelineView.vue'),
    meta: {
      title: '事件时间线',
      requiresAuth: true,
      hidden: true
    }
  },
]

export default eventRoutes
