import type { RouteRecordRaw } from 'vue-router'

/** Asset 模块路由 — 资产管理. order: 22. Asset领域 */
const assetRoutes: RouteRecordRaw[] = [
  {
    path: '/asset',
    name: 'Asset',
    redirect: '/asset/center',
    meta: {
      title: '资产管理',
      icon: 'Package',
      requiresAuth: true,
      order: 22,
      group: 'operations'
    },
    children: [
      {
        path: '/asset/center',
        name: 'AssetManagementCenter',
        component: () => import('@/views/asset/AssetManagementCenter.vue'),
        meta: {
          title: '资产台账',
          requiresAuth: true,
          permission: 'asset:list',
          order: 1
        }
      },
      {
        path: '/asset/categories',
        name: 'AssetCategories',
        component: () => import('@/views/asset/AssetCategoryView.vue'),
        meta: {
          title: '分类管理',
          requiresAuth: true,
          permission: 'asset:category:list',
          order: 2
        }
      },
      {
        path: '/asset/inventory',
        name: 'AssetInventory',
        component: () => import('@/views/asset/AssetInventoryView.vue'),
        meta: {
          title: '资产盘点',
          requiresAuth: true,
          permission: 'asset:inventory:list',
          order: 3
        }
      },
      {
        path: '/asset/borrows',
        name: 'AssetBorrowList',
        component: () => import('@/views/asset/AssetBorrowListView.vue'),
        meta: {
          title: '借用管理',
          requiresAuth: true,
          permission: 'asset:borrow:list',
          order: 4
        }
      },
      {
        path: '/asset/maintenance',
        name: 'AssetMaintenance',
        component: () => import('@/views/asset/AssetMaintenanceListView.vue'),
        meta: {
          title: '维修管理',
          requiresAuth: true,
          permission: 'asset:manage',
          order: 5
        }
      },
      {
        path: '/asset/approvals',
        name: 'AssetApprovalList',
        component: () => import('@/views/asset/AssetApprovalListView.vue'),
        meta: {
          title: '审批管理',
          requiresAuth: true,
          permission: 'asset:list',
          order: 6
        }
      },
      {
        path: '/asset/alerts',
        name: 'AssetAlertList',
        component: () => import('@/views/asset/AssetAlertListView.vue'),
        meta: {
          title: '预警中心',
          requiresAuth: true,
          permission: 'asset:list',
          order: 7
        }
      },
      {
        path: '/asset/depreciation',
        name: 'AssetDepreciation',
        component: () => import('@/views/asset/AssetDepreciationView.vue'),
        meta: {
          title: '折旧管理',
          requiresAuth: true,
          permission: 'asset:list',
          order: 8
        }
      }
    ]
  },
]

export default assetRoutes
