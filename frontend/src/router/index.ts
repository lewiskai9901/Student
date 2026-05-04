import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getToken, getTokenRemainingTime, isTokenExpired } from '@/utils/token'

// Token刷新阈值：剩余时间少于5分钟时触发刷新
const TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000 // 5 minutes in milliseconds

// 防止并发刷新的标志
let isRefreshing = false

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  // 大屏展示（独立全屏页面）
  {
    path: '/inspection/big-screen',
    name: 'BigScreen',
    component: () => import('@/views/inspection/analytics/BigScreenView.vue'),
    meta: {
      title: '检查大屏',
      requiresAuth: true
    }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/inspection',
    meta: {
      requiresAuth: true
    },
    children: [
      // ==================== 首页 (order: 1) ====================
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {
          title: '首页',
          icon: 'House',
          requiresAuth: true,
          permission: 'dashboard:view',
          order: 1,
          group: 'daily'
        }
      },
      // 教师工作台 — TEACHER 登录后自动落地；其它用户也可访问但为空态
      {
        path: '/my/dashboard',
        name: 'MyDashboard',
        component: () => import('@/views/my/MyDashboard.vue'),
        meta: {
          title: '工作台',
          icon: 'Briefcase',
          requiresAuth: true,
          order: 1,
          group: 'daily',
          hidden: true
        }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: {
          title: '个人资料',
          requiresAuth: true,
          hidden: true
        }
      },

      // ==================== 消息与事件 /message (order: 1.5) ====================
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

      // ==================== 关系管理 /access (order: 1.7) ====================
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
            meta: { title: '数据权限', requiresAuth: true, permission: 'system:role:view', order: 3 }
          }
        ]
      },

      // EDU routes (my-class / academic / student) loaded dynamically via bootstrap.ts
      // ==================== 组织管理 /organization (order: 10) - Organization领域 ====================
      {
        path: '/organization',
        name: 'Organization',
        redirect: '/organization/units',
        meta: {
          title: '组织管理',
          icon: 'Hierarchy',
          requiresAuth: true,
          order: 10,
          group: 'business'
        },
        children: [
          {
            path: '/organization/units',
            name: 'OrgUnits',
            component: () => import('@/views/organization/structure/OrgStructure.vue'),
            meta: {
              title: '组织架构',
              requiresAuth: true,
              permission: 'system:org:view',
              order: 1
            }
          },
          // Old route redirects for backward compatibility
          {
            path: '/organization/students',
            redirect: '/student/list',
            meta: { hidden: true }
          },
          {
            path: '/organization/grades',
            redirect: '/student/grades',
            meta: { hidden: true }
          },
          {
            path: '/organization/majors',
            redirect: '/academic/majors',
            meta: { hidden: true }
          },
          {
            path: '/organization/attendance',
            redirect: '/student/attendance',
            meta: { hidden: true }
          },
          {
            path: '/organization/warnings',
            redirect: '/student/warnings',
            meta: { hidden: true }
          },
          // 宿舍管理旧路由重定向
          {
            path: '/organization/dormitory',
            redirect: '/dormitory',
            meta: { hidden: true }
          }
        ]
      },

      // ==================== 检查平台 (order: 12) ====================
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

      // EDU routes (teaching) loaded dynamically via bootstrap.ts

      // ==================== 系统管理 /system (order: 30) - 合并权限管理+系统设置 ====================
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

      // ==================== 场所管理 /place (order: 11) - Place领域 ====================
      // 统一管理所有场所：校区、楼宇（宿舍楼/教学楼/办公楼）、楼层、房间（宿舍/教室/办公室等）
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

      // EDU routes (dormitory) loaded dynamically via bootstrap.ts

      // ==================== 资产管理 /asset (order: 22) - Asset领域 ====================
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

      // ==================== Phase 3.4: 实体事件时间线 ====================
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

      // ==================== 测试页面 - 仅在开发环境可用 ====================
      ...(import.meta.env.DEV ? [
        {
          path: '/test/pagination',
          name: 'PaginationTest',
          component: () => import('@/views/test/PaginationTest.vue'),
          meta: {
            title: '分页测试',
            requiresAuth: true,
            hidden: true
          }
        }
      ] : []),

    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: {
      title: '没有权限'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 学生管理系统`
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const token = getToken()

    // 如果Token已过期，跳转登录页
    if (isTokenExpired(token)) {
      // 清理认证状态
      await authStore.logoutAction()
      next('/login')
      return
    }

    // 检查是否需要刷新Token（距离过期不足5分钟）
    const remainingTime = getTokenRemainingTime(token)
    if (remainingTime > 0 && remainingTime < TOKEN_REFRESH_THRESHOLD && !isRefreshing) {
      isRefreshing = true
      try {
        await authStore.refreshTokenAction()
      } catch (error) {
        // Token刷新失败，重新登录
        await authStore.logoutAction()
        next('/login')
        return
      } finally {
        isRefreshing = false
      }
    }

    // 再次检查认证状态（刷新后）
    if (!authStore.isAuthenticated) {
      next('/login')
      return
    }

    // 检查权限
    if (to.meta.permission && !authStore.hasPermission(to.meta.permission as string)) {
      // /dashboard 专属兜底：教师或任何无 dashboard:view 权限的用户改去 /my/dashboard
      // —— /my/dashboard 不受权限门控，对所有已登录用户开放
      if (to.path === '/dashboard' && to.meta.permission === 'dashboard:view') {
        next('/my/dashboard')
        return
      }
      next('/403')
      return
    }
  }

  next()
})

export default router
