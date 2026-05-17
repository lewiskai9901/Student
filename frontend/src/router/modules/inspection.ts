import type { RouteRecordRaw } from 'vue-router'

/** Inspection 模块路由 — 检查平台. order: 12 */
const inspectionRoutes: RouteRecordRaw[] = [
  // P1: 旧路径重定向 (向后兼容书签/外链)
  { path: '/inspection/about-me', redirect: '/inspection/my-record', meta: { hidden: true } },
  { path: '/inspection/ratings', redirect: '/inspection/rankings', meta: { hidden: true } },
  { path: '/inspection/observations', redirect: '/inspection/analytics', meta: { hidden: true } },
  { path: '/inspection/library', redirect: '/inspection/config', meta: { hidden: true } },
  { path: '/inspection/scoring', redirect: '/inspection/scoring-profiles', meta: { hidden: true } },
  {
    path: '/inspection',
    name: 'Inspection',
    redirect: '/inspection/dashboard',
    meta: {
      title: '检查平台',
      icon: 'ClipboardCheck',
      requiresAuth: true,
      order: 12,
      group: 'business'
    },
    children: [
      // 统一检查配置（合并模板列表、模板分类、选项集、评分配置列表）
      {
        path: '/inspection/config',
        name: 'InspectionConfig',
        component: () => import('@/views/inspection/config/InspectionConfigView.vue'),
        meta: {
          title: '检查配置',
          icon: 'Settings2',
          requiresAuth: true,
          permission: 'insp:template:view',
          order: 1
        }
      },
      {
        path: '/inspection/templates/create',
        name: 'TemplateCreate',
        component: () => import('@/views/inspection/templates/TemplateEditorView.vue'),
        meta: {
          title: '创建模板',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:template:create'
        }
      },
      {
        path: '/inspection/templates/:id/edit',
        name: 'TemplateEdit',
        component: () => import('@/views/inspection/templates/TemplateEditorView.vue'),
        meta: {
          title: '编辑模板',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:template:edit'
        }
      },
      // P0-2: 评分方案独立列表入口
      {
        path: '/inspection/scoring-profiles',
        name: 'ScoringProfileList',
        component: () => import('@/views/inspection/scoring/ScoringProfileListView.vue'),
        meta: {
          title: '评分方案',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:scoring-profile:view'
        }
      },
      {
        path: '/inspection/scoring/:id',
        name: 'ScoringProfileEdit',
        component: () => import('@/views/inspection/scoring/ScoringProfileEditor.vue'),
        meta: {
          title: '评分配置',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:scoring-profile:edit'
        }
      },
      // 等级方案管理
      {
        path: '/inspection/grade-schemes',
        name: 'GradeSchemes',
        component: () => import('@/views/inspection/scoring/GradeSchemeListView.vue'),
        meta: {
          title: '等级方案',
          icon: 'Award',
          requiresAuth: true,
          permission: 'insp:template:view',
          order: 2.5
        }
      },
      // ---------- My Inspection (hidden, redirect to /tasks) ----------
      {
        path: '/inspection/my',
        redirect: '/inspection/tasks',
        meta: { hidden: true }
      },
      // ---------- Execution BC ----------
      {
        path: '/inspection/projects',
        name: 'ProjectList',
        component: () => import('@/views/inspection/projects/ProjectListView.vue'),
        meta: {
          title: '检查项目',
          icon: 'Briefcase',
          requiresAuth: true,
          permission: 'insp:project:view',
          order: 4
        }
      },
      {
        path: '/inspection/projects/create',
        name: 'ProjectWizard',
        component: () => import('@/views/inspection/projects/ProjectWizardView.vue'),
        meta: {
          title: '创建检查项目',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:project:create'
        }
      },
      {
        path: '/inspection/projects/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/inspection/projects/ProjectDetailView.vue'),
        meta: {
          title: '项目详情',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:project:view'
        }
      },
      {
        path: '/inspection/tasks',
        name: 'TaskList',
        component: () => import('@/views/inspection/MyInspectionView.vue'),
        meta: {
          title: '我的任务',
          icon: 'ClipboardList',
          requiresAuth: true,
          permission: 'insp:task:view',
          order: 3
        }
      },
      {
        path: '/inspection/tasks/review',
        name: 'TaskReviewWorkbench',
        component: () => import('@/views/inspection/tasks/TaskReviewWorkbenchView.vue'),
        meta: {
          title: '审核工作台',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:task:review',
          order: 5.5
        }
      },
      {
        path: '/inspection/tasks/:id/execute',
        name: 'TaskExecution',
        component: () => import('@/views/inspection/tasks/TaskExecutionView.vue'),
        meta: {
          title: '执行检查',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:task:execute'
        }
      },
      {
        path: '/inspection/tasks/:id/execute-mobile',
        name: 'TaskExecutionMobile',
        component: () => import('@/views/inspection/tasks/TaskExecutionMobileView.vue'),
        meta: {
          title: '移动端检查',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:task:execute'
        }
      },
      // 成绩公示
      {
        path: '/inspection/results',
        name: 'InspectionResults',
        component: () => import('@/views/inspection/projects/InspectionResultsView.vue'),
        meta: {
          title: '检查结果',
          icon: 'BarChart3',
          requiresAuth: true,
          permission: 'insp:project:view',
          order: 7
        }
      },
      // 单维度排名详情
      {
        path: '/inspection/rating-dimensions/:id/rankings',
        name: 'RatingDimensionRankings',
        component: () => import('@/views/inspection/projects/RatingRankingView.vue'),
        meta: {
          title: '评级排名',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:project:view'
        }
      },
      // 整改管理
      {
        path: '/inspection/corrective',
        name: 'CorrectiveCaseList',
        component: () => import('@/views/inspection/corrective/CorrectiveCaseListView.vue'),
        meta: {
          title: '整改管理',
          requiresAuth: true,
          permission: 'insp:corrective:view'
        }
      },
      {
        path: '/inspection/corrective/:id',
        name: 'CorrectiveCaseDetail',
        component: () => import('@/views/inspection/corrective/CorrectiveCaseDetailView.vue'),
        meta: {
          title: '整改详情',
          hidden: true,
          requiresAuth: true,
          permission: 'insp:corrective:view'
        }
      },
      // 检查员驾驶舱 (Cockpit redesign · Sweep+Matrix+Focus)
      {
        path: '/inspection/tasks/:id/cockpit',
        name: 'TaskCockpit',
        component: () => import('@/views/inspection/tasks/TaskCockpitView.vue'),
        meta: {
          title: '检查员驾驶舱',
          requiresAuth: true,
          permission: 'insp:task:view',   // J6: 补 meta 缺口, 之前只 requiresAuth 任意登录可进
          hidden: true,
        }
      },
      // 检查平台总台 (Audit Console redesign)
      {
        path: '/inspection',
        name: 'InspectionCommandDeck',
        component: () => import('@/views/inspection/InspectionCommandDeck.vue'),
        meta: {
          title: '检查平台',
          requiresAuth: true,
          hidden: true
        }
      },
      // 离职用户重派 - 显示在管理员菜单
      {
        path: '/inspection/admin/reassign-departed',
        name: 'DepartedUserReassign',
        component: () => import('@/views/inspection/admin/DepartedUserReassignView.vue'),
        meta: {
          title: '离职重派',
          icon: 'UserMinus',
          requiresAuth: true,
          permission: 'insp:corrective:manage',
          order: 90,
        }
      },
      // 我的申诉 - 主菜单可见
      {
        path: '/inspection/appeals/my',
        name: 'MyAppeals',
        component: () => import('@/views/inspection/appeals/MyAppealsView.vue'),
        meta: {
          title: '我的申诉',
          icon: 'MessageSquareWarning',
          requiresAuth: true,
          permission: 'inspection:appeal:view',
          order: 6,
        }
      },
      // 申诉审核 - 主菜单可见
      {
        path: '/inspection/appeals/review',
        name: 'AppealReview',
        component: () => import('@/views/inspection/appeals/AppealReviewView.vue'),
        meta: {
          title: '申诉审核',
          icon: 'Gavel',
          requiresAuth: true,
          permission: 'inspection:appeal:review',
          order: 7,
        }
      },
      // 分析报表
      {
        path: '/inspection/analytics',
        name: 'AnalyticsDashboard',
        component: () => import('@/views/inspection/analytics/AnalyticsDashboardView.vue'),
        meta: {
          title: '分析报表',
          requiresAuth: true,
          permission: 'insp:analytics:view'
        }
      },
      {
        path: '/inspection/rankings',
        name: 'RatingRankings',
        component: () => import('@/views/inspection/analytics/RatingRankingView.vue'),
        meta: {
          title: '评级排名',
          icon: 'Trophy',
          requiresAuth: true,
          permission: 'insp:analytics:view',
          order: 8
        }
      },
      // 受检主体视角 — 班主任/楼长/部门负责人 用 (See: 角色驱动 IA 设计)
      {
        path: '/inspection/my-record',
        name: 'InspMyRecord',
        component: () => import('@/views/inspection/myrecord/MyRecordView.vue'),
        meta: {
          title: '我的成绩单',
          icon: 'BadgeCheck',
          requiresAuth: true,
          permission: 'insp:analytics:view',
          order: 3
        }
      },
      // 整改责任人移动端 — 后勤/班长/维修工 用 (deadline 驱动 + 一键操作)
      {
        path: '/inspection/my-corrective',
        name: 'InspMyCorrective',
        component: () => import('@/views/inspection/corrective/MyCorrectiveView.vue'),
        meta: {
          title: '我的整改',
          icon: 'Wrench',
          requiresAuth: true,
          permission: 'insp:corrective:execute',
          order: 4
        }
      },
      // 受检主体面 (C3) — 班主任/场所负责人/受检单位的"被检查"视角
      {
        path: '/inspection/received',
        name: 'InspMyReceivedHub',
        component: () => import('@/views/inspection/received/MyReceivedHubView.vue'),
        meta: {
          title: '我的受检中心',
          icon: 'Building2',
          requiresAuth: true,
          permission: 'insp:received:view',
          order: 4.5,
        },
      },
      {
        path: '/inspection/received/inspections',
        name: 'InspMyReceivedInspections',
        component: () => import('@/views/inspection/received/MyInspectionsView.vue'),
        meta: {
          title: '我被检查的记录',
          icon: 'ClipboardList',
          requiresAuth: true,
          permission: 'insp:received:view',
          order: 5,
        },
      },
      {
        path: '/inspection/received/trends',
        name: 'InspMyReceivedTrends',
        component: () => import('@/views/inspection/received/MyTrendsView.vue'),
        meta: {
          title: '检查趋势',
          icon: 'TrendingUp',
          requiresAuth: true,
          permission: 'insp:received:view',
          order: 6,
        },
      },
      {
        path: '/inspection/received/recurring',
        name: 'InspMyReceivedRecurring',
        component: () => import('@/views/inspection/received/MyRecurringView.vue'),
        meta: {
          title: '高频问题',
          icon: 'AlertTriangle',
          requiresAuth: true,
          permission: 'insp:received:view',
          order: 7,
        },
      },
      // 审核员风险池 — 按风险分排序待审任务 (4 维度: 临期/突变/差异/延迟)
      {
        path: '/inspection/tasks/review-risk',
        name: 'TaskReviewRiskQueue',
        component: () => import('@/views/inspection/tasks/TaskReviewRiskQueueView.vue'),
        meta: {
          title: '待审风险池',
          icon: 'ListChecks',
          requiresAuth: true,
          permission: 'insp:task:review',
          order: 5
        }
      },
      // 数据消费者导出中心 — HR/考核办/教务用 (Excel 下载 + 直连 API)
      {
        path: '/inspection/export',
        name: 'InspExportCenter',
        component: () => import('@/views/inspection/export/ExportCenterView.vue'),
        meta: {
          title: '导出中心',
          icon: 'Download',
          requiresAuth: true,
          permission: 'insp:analytics:view',
          order: 22
        }
      },
      // 治理工作台 — 校长/督查办主任/部门头 用 (红线/检查员/申诉/整改 4 象限)
      {
        path: '/inspection/governance',
        name: 'InspGovernance',
        component: () => import('@/views/inspection/governance/GovernanceView.vue'),
        meta: {
          title: '治理工作台',
          icon: 'ShieldCheck',
          requiresAuth: true,
          permission: 'insp:analytics:view',
          order: 9
        }
      },
      // 检查平台总览 — 多角色聚合首页 (我的任务/待审/申诉/整改 widget)
      // 改用 InspectionCommandDeck (原 /inspection 隐藏页), 替代占位 DashboardView
      {
        path: '/inspection/dashboard',
        name: 'InspectionDashboard',
        component: () => import('@/views/inspection/InspectionCommandDeck.vue'),
        meta: {
          title: '检查平台总览',
          icon: 'LayoutDashboard',
          requiresAuth: true,
          permission: 'insp:project:view',
          order: 0
        }
      },

      // 审计日志 (检查平台专属)
      {
        path: '/inspection/audit-trail',
        name: 'InspectionAuditTrail',
        component: () => import('@/views/inspection/platform/AuditTrailView.vue'),
        meta: {
          title: '审计日志',
          icon: 'FileSearch',
          requiresAuth: true,
          permission: 'insp:platform:view',
          order: 12
        }
      },
      // 问题分类
      {
        path: '/inspection/issue-categories',
        name: 'IssueCategoryManagement',
        component: () => import('@/views/inspection/platform/IssueCategoryManagementView.vue'),
        meta: {
          title: '问题分类',
          icon: 'Tags',
          requiresAuth: true,
          permission: 'insp:platform:view',
          order: 13
        }
      },

      // 假日日历
      {
        path: '/inspection/holiday-calendar',
        name: 'HolidayCalendar',
        component: () => import('@/views/inspection/platform/HolidayCalendarView.vue'),
        meta: {
          title: '假日日历',
          icon: 'CalendarDays',
          requiresAuth: true,
          permission: 'insp:platform:view',
          order: 15
        }
      },

      // Phase 4: Alerts
      {
        path: '/inspection/alerts',
        name: 'AlertDashboard',
        component: () => import('@/views/inspection/analytics/AlertDashboardView.vue'),
        meta: {
          title: '预警看板',
          icon: 'Bell',
          requiresAuth: true,
          permission: 'insp:analytics:view',
          order: 17
        }
      }
    ]
  },
]

export default inspectionRoutes
