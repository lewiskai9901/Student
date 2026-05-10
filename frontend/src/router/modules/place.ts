import type { RouteRecordRaw } from 'vue-router'

/** Place 模块路由 — 场所管理. order: 11. Place领域
 *  统一管理所有场所：校区、楼宇（宿舍楼/教学楼/办公楼）、楼层、房间（宿舍/教室/办公室等）
 */
const placeRoutes: RouteRecordRaw[] = [
  {
    path: '/place',
    name: 'Place',
    redirect: '/place/management',
    meta: {
      title: '场所管理',
      icon: 'MapPin',
      requiresAuth: true,
      order: 11,
      group: 'business'
    },
    children: [
      {
        path: '/place/management',
        name: 'UniversalPlaceManagement',
        component: () => import('@/views/place/UniversalPlaceManagement.vue'),
        meta: {
          title: '场所管理',
          requiresAuth: true,
          permission: 'place:view',
          order: 1
        }
      },
      {
        path: '/place/allocation',
        name: 'PlaceAllocation',
        redirect: '/place/management',
        meta: {
          title: '场所分配',
          requiresAuth: true,
          hidden: true  // 重定向到新版场所管理
        }
      },
      // 旧版路由重定向到新版
      {
        path: '/place/center',
        name: 'PlaceCenter',
        redirect: '/place/management',
        meta: {
          hidden: true
        }
      }
    ]
  },
]

export default placeRoutes
