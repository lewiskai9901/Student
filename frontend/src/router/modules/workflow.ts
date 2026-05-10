import type { RouteRecordRaw } from 'vue-router'

/** Workflow 模块路由 — 工作流. order: 23. Workflow领域 */
const workflowRoutes: RouteRecordRaw[] = [
  {
    path: '/workflow',
    name: 'Workflow',
    redirect: '/workflow/definitions',
    meta: {
      title: '工作流',
      icon: 'GitBranch',
      requiresAuth: true,
      order: 23,
      group: 'operations'
    },
    children: [
      {
        path: '/workflow/definitions',
        name: 'WorkflowDefinitions',
        component: () => import('@/views/workflow/ProcessDefinitionListView.vue'),
        meta: {
          title: '流程定义',
          icon: 'FileText',
          requiresAuth: true,
          permissions: ['workflow:definition:view'],
          order: 1
        }
      },
      {
        path: '/workflow/designer',
        name: 'WorkflowDesigner',
        component: () => import('@/views/workflow/WorkflowDesignerView.vue'),
        meta: {
          title: 'BPMN 设计器',
          icon: 'Palette',
          requiresAuth: true,
          // 设计器属于敏感操作 — 同时要求查看 + 编辑
          permissions: ['workflow:definition:view', 'workflow:definition:edit'],
          order: 2
        }
      },
      {
        path: '/workflow/my-tasks',
        name: 'WorkflowMyTasks',
        component: () => import('@/views/workflow/MyTasksView.vue'),
        meta: {
          title: '我的待办',
          icon: 'ClipboardList',
          requiresAuth: true,
          // 我的待办对所有登录用户开放, 不加 permissions (走 fallthrough)
          order: 3
        }
      }
    ]
  },
]

export default workflowRoutes
