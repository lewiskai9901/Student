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
  // V7 大屏展示（独立全屏页面）
  {
    path: '/inspection/v7/big-screen',
    name: 'V7BigScreen',
    component: () => import('@/views/inspection/v7/analytics/BigScreenView.vue'),
    meta: {
      title: '检查大屏',
      requiresAuth: true
    }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
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
            meta: { title: '事件类型', requiresAuth: true, order: 2 }
          },
          {
            path: '/message/triggers',
            name: 'EventTriggers',
            component: () => import('@/views/system/EventTriggerView.vue'),
            meta: { title: '事件触发器', requiresAuth: true, order: 3 }
          },
          {
            path: '/message/subscriptions',
            name: 'MessageSubscriptions',
            component: () => import('@/views/message/MessageConfigView.vue'),
            meta: { title: '订阅与模板', requiresAuth: true, order: 4 }
          },
        ]
      },

      // ==================== 我的班级 /my-class (order: 2) ====================
      {
        path: '/my-class',
        name: 'MyClass',
        redirect: '/my-class/list',
        meta: {
          title: '我的班级',
          icon: 'School',
          requiresAuth: true,
          requiresClass: true, // 需要有分配班级才显示
          order: 2,
          group: 'daily'
        },
        children: [
          {
            path: '/my-class/list',
            name: 'MyClassList',
            component: () => import('@/views/myclass/MyClassListView.vue'),
            meta: {
              title: '班级列表',
              requiresAuth: true,
              hidden: true
            }
          },
          {
            path: '/my-class/:orgUnitId',
            name: 'MyClassDetail',
            component: () => import('@/views/myclass/MyClassDetailView.vue'),
            meta: {
              title: '班级详情',
              requiresAuth: true,
              hidden: true
            }
          }
        ]
      },

      // ==================== 学术管理 /academic (order: 3) - Academic领域 ====================
      {
        path: '/academic',
        name: 'Academic',
        redirect: '/academic/majors',
        meta: {
          title: '学术管理',
          icon: 'GraduationCap',
          requiresAuth: true,
          order: 3,
          group: 'business'
        },
        children: [
          {
            path: '/academic/majors',
            name: 'AcademicMajors',
            component: () => import('@/views/academic/MajorListView.vue'),
            meta: {
              title: '专业管理',
              requiresAuth: true,
              order: 1
            }
          },
          {
            path: '/academic/courses',
            name: 'AcademicCourses',
            component: () => import('@/views/academic/CourseListView.vue'),
            meta: {
              title: '课程管理',
              requiresAuth: true,
              order: 2
            }
          },
          {
            path: '/academic/curriculum-plans',
            name: 'AcademicCurriculum',
            component: () => import('@/views/academic/CurriculumPlanView.vue'),
            meta: {
              title: '培养方案',
              requiresAuth: true,
              order: 3
            }
          }
        ]
      },

      // ==================== 学生管理 /student (order: 4) - Student领域 ====================
      {
        path: '/student',
        name: 'Student',
        redirect: '/student/list',
        meta: {
          title: '学生管理',
          icon: 'Users',
          requiresAuth: true,
          order: 4,
          group: 'business'
        },
        children: [
          {
            path: '/student/enrollment',
            name: 'StudentEnrollment',
            component: () => import('@/views/student/EnrollmentView.vue'),
            meta: {
              title: '招生管理',
              requiresAuth: true,
              order: 0
            }
          },
          {
            path: '/student/list',
            name: 'StudentList',
            component: () => import('@/views/student/StudentList.vue'),
            meta: {
              title: '学生列表',
              requiresAuth: true,
              permission: 'student:info:view',
              order: 1
            }
          },
          {
            path: '/student/classes',
            name: 'ClassManagement',
            component: () => import('@/views/student/ClassManagement.vue'),
            meta: {
              title: '班级管理',
              requiresAuth: true,
              permission: 'student:class:view',
              order: 2
            }
          },
          {
            path: '/student/grades',
            name: 'StudentGrades',
            component: () => import('@/views/organization/grades/GradeManagementV2.vue'),
            meta: {
              title: '年级管理',
              requiresAuth: true,
              permission: 'student:grade:view',
              order: 2
            }
          },
          {
            path: '/student/attendance',
            name: 'StudentAttendance',
            component: () => import('@/views/student/AttendanceView.vue'),
            meta: {
              title: '考勤管理',
              requiresAuth: true,
              order: 4
            }
          },
          {
            path: '/student/warnings',
            name: 'StudentWarnings',
            component: () => import('@/views/student/AcademicWarningView.vue'),
            meta: {
              title: '学业预警',
              requiresAuth: true,
              order: 5
            }
          }
        ]
      },

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

      // ==================== V7 检查平台 (order: 12) ====================
      {
        path: '/inspection/v7',
        name: 'InspectionV7',
        redirect: '/inspection/v7/config',
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
            path: '/inspection/v7/config',
            name: 'V7InspectionConfig',
            component: () => import('@/views/inspection/v7/config/InspectionConfigView.vue'),
            meta: {
              title: '检查配置',
              icon: 'Settings2',
              requiresAuth: true,
              permission: 'insp:template:view',
              order: 1
            }
          },
          {
            path: '/inspection/v7/templates/create',
            name: 'V7TemplateCreate',
            component: () => import('@/views/inspection/v7/templates/TemplateEditorView.vue'),
            meta: {
              title: '创建模板',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:template:create'
            }
          },
          {
            path: '/inspection/v7/templates/:id/edit',
            name: 'V7TemplateEdit',
            component: () => import('@/views/inspection/v7/templates/TemplateEditorView.vue'),
            meta: {
              title: '编辑模板',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:template:edit'
            }
          },
          {
            path: '/inspection/v7/scoring/:id',
            name: 'V7ScoringProfileEdit',
            component: () => import('@/views/inspection/v7/scoring/ScoringProfileEditor.vue'),
            meta: {
              title: '评分配置',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:scoring-profile:edit'
            }
          },
          {
            path: '/inspection/v7/scoring',
            name: 'V7ScoringProfileByTemplate',
            component: () => import('@/views/inspection/v7/scoring/ScoringProfileEditor.vue'),
            meta: {
              title: '评分配置',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:scoring-profile:view'
            }
          },
          // ---------- Item Library ----------
          {
            path: '/inspection/v7/library',
            name: 'V7ItemLibrary',
            component: () => import('@/views/inspection/v7/templates/ItemLibraryView.vue'),
            meta: {
              title: '检查项库',
              icon: 'Library',
              requiresAuth: true,
              permission: 'insp:template:view'
            }
          },
          // 等级方案管理
          {
            path: '/inspection/v7/grade-schemes',
            name: 'V7GradeSchemes',
            component: () => import('@/views/inspection/v7/scoring/GradeSchemeListView.vue'),
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
            path: '/inspection/v7/my',
            redirect: '/inspection/v7/tasks',
            meta: { hidden: true }
          },
          // ---------- Execution BC ----------
          {
            path: '/inspection/v7/projects',
            name: 'V7ProjectList',
            component: () => import('@/views/inspection/v7/projects/ProjectListView.vue'),
            meta: {
              title: '检查项目',
              icon: 'Briefcase',
              requiresAuth: true,
              permission: 'insp:project:view',
              order: 4
            }
          },
          {
            path: '/inspection/v7/projects/create',
            name: 'V7ProjectWizard',
            component: () => import('@/views/inspection/v7/projects/ProjectWizardView.vue'),
            meta: {
              title: '创建检查项目',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:project:create'
            }
          },
          {
            path: '/inspection/v7/projects/:id',
            name: 'V7ProjectDetail',
            component: () => import('@/views/inspection/v7/projects/ProjectDetailView.vue'),
            meta: {
              title: '项目详情',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:project:view'
            }
          },
          {
            path: '/inspection/v7/tasks',
            name: 'V7TaskList',
            component: () => import('@/views/inspection/v7/MyInspectionView.vue'),
            meta: {
              title: '我的任务',
              icon: 'ClipboardList',
              requiresAuth: true,
              permission: 'insp:task:view',
              order: 3
            }
          },
          {
            path: '/inspection/v7/tasks/review',
            name: 'V7TaskReviewWorkbench',
            component: () => import('@/views/inspection/v7/tasks/TaskReviewWorkbenchView.vue'),
            meta: {
              title: '审核工作台',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:task:review',
              order: 5.5
            }
          },
          {
            path: '/inspection/v7/tasks/:id/execute',
            name: 'V7TaskExecution',
            component: () => import('@/views/inspection/v7/tasks/TaskExecutionView.vue'),
            meta: {
              title: '执行检查',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:task:execute'
            }
          },
          {
            path: '/inspection/v7/tasks/:id/execute-mobile',
            name: 'V7TaskExecutionMobile',
            component: () => import('@/views/inspection/v7/tasks/TaskExecutionMobileView.vue'),
            meta: {
              title: '移动端检查',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:task:execute'
            }
          },
          // 成绩公示
          {
            path: '/inspection/v7/results',
            name: 'V7InspectionResults',
            component: () => import('@/views/inspection/v7/projects/InspectionResultsView.vue'),
            meta: {
              title: '检查结果',
              icon: 'BarChart3',
              requiresAuth: true,
              permission: 'insp:project:view',
              order: 7
            }
          },
          // V7 单维度排名详情
          {
            path: '/inspection/v7/rating-dimensions/:id/rankings',
            name: 'V7RatingDimensionRankings',
            component: () => import('@/views/inspection/v7/projects/RatingRankingView.vue'),
            meta: {
              title: '评级排名',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:project:view'
            }
          },
          // V7 整改管理
          {
            path: '/inspection/v7/corrective',
            name: 'V7CorrectiveCaseList',
            component: () => import('@/views/inspection/v7/corrective/CorrectiveCaseListView.vue'),
            meta: {
              title: '整改管理',
              requiresAuth: true,
              permission: 'insp:corrective:view'
            }
          },
          {
            path: '/inspection/v7/corrective/:id',
            name: 'V7CorrectiveCaseDetail',
            component: () => import('@/views/inspection/v7/corrective/CorrectiveCaseDetailView.vue'),
            meta: {
              title: '整改详情',
              hidden: true,
              requiresAuth: true,
              permission: 'insp:corrective:view'
            }
          },
          // V7 分析报表
          {
            path: '/inspection/v7/analytics',
            name: 'V7AnalyticsDashboard',
            component: () => import('@/views/inspection/v7/analytics/AnalyticsDashboardView.vue'),
            meta: {
              title: '分析报表',
              requiresAuth: true,
              permission: 'insp:analytics:view'
            }
          },
          {
            path: '/inspection/v7/observations',
            name: 'V7Observations',
            component: () => import('@/views/inspection/v7/analytics/ObservationListView.vue'),
            meta: {
              title: '评分观察',
              requiresAuth: true,
              permission: 'insp:analytics:view'
            }
          },
          {
            path: '/inspection/v7/rankings',
            name: 'V7RatingRankings',
            component: () => import('@/views/inspection/v7/analytics/RatingRankingView.vue'),
            meta: {
              title: '评级排名',
              icon: 'Trophy',
              requiresAuth: true,
              permission: 'insp:analytics:view',
              order: 8
            }
          },
          // V7 仪表盘
          {
            path: '/inspection/v7/dashboard',
            name: 'V7Dashboard',
            component: () => import('@/views/inspection/v7/dashboard/V7DashboardView.vue'),
            meta: {
              title: '检查平台总览',
              icon: 'LayoutDashboard',
              requiresAuth: true,
              permission: 'insp:project:view',
              order: 0
            }
          },
          // ---------- Platform BC ----------
          // V7 通知规则
          {
            path: '/inspection/v7/notification-rules',
            name: 'V7NotificationRuleList',
            component: () => import('@/views/inspection/v7/platform/NotificationRuleListView.vue'),
            meta: {
              title: '通知规则',
              icon: 'Bell',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 10
            }
          },
          // V7 Webhook 订阅
          {
            path: '/inspection/v7/webhooks',
            name: 'V7WebhookList',
            component: () => import('@/views/inspection/v7/platform/WebhookListView.vue'),
            meta: {
              title: 'Webhook',
              icon: 'Webhook',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 11
            }
          },
          // V7 审计日志
          {
            path: '/inspection/v7/audit-trail',
            name: 'V7AuditTrail',
            component: () => import('@/views/inspection/v7/platform/AuditTrailView.vue'),
            meta: {
              title: '审计日志',
              icon: 'FileSearch',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 12
            }
          },
          // V7 问题分类
          {
            path: '/inspection/v7/issue-categories',
            name: 'V7IssueCategoryManagement',
            component: () => import('@/views/inspection/v7/platform/IssueCategoryManagementView.vue'),
            meta: {
              title: '问题分类',
              icon: 'Tags',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 13
            }
          },
          // V7 报表模板
          {
            path: '/inspection/v7/report-templates',
            name: 'V7ReportTemplateList',
            component: () => import('@/views/inspection/v7/platform/ReportTemplateListView.vue'),
            meta: {
              title: '报表模板',
              icon: 'FileSpreadsheet',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 14
            }
          },
          // V7 假日日历
          {
            path: '/inspection/v7/holiday-calendar',
            name: 'V7HolidayCalendar',
            component: () => import('@/views/inspection/v7/platform/HolidayCalendarView.vue'),
            meta: {
              title: '假日日历',
              icon: 'CalendarDays',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 15
            }
          },
          // Phase 2: Compliance
          {
            path: '/inspection/v7/compliance',
            name: 'V7ComplianceStandards',
            component: () => import('@/views/inspection/v7/compliance/ComplianceStandardListView.vue'),
            meta: {
              title: '合规标准',
              icon: 'Shield',
              requiresAuth: true,
              permission: 'insp:template:view',
              order: 16
            }
          },
          // Phase 4: Alerts
          {
            path: '/inspection/v7/alerts',
            name: 'V7AlertDashboard',
            component: () => import('@/views/inspection/v7/analytics/AlertDashboardView.vue'),
            meta: {
              title: '预警看板',
              icon: 'Bell',
              requiresAuth: true,
              permission: 'insp:analytics:view',
              order: 17
            }
          },
          // Phase 5: Knowledge Base
          {
            path: '/inspection/v7/knowledge',
            name: 'V7KnowledgeBase',
            component: () => import('@/views/inspection/v7/knowledge/KnowledgeBaseView.vue'),
            meta: {
              title: '知识库',
              icon: 'BookOpen',
              requiresAuth: true,
              permission: 'insp:corrective:view',
              order: 18
            }
          },
          // Phase 6: NFC Tags
          {
            path: '/inspection/v7/nfc-tags',
            name: 'V7NfcTags',
            component: () => import('@/views/inspection/v7/platform/NfcTagManagementView.vue'),
            meta: {
              title: 'NFC标签',
              icon: 'Wifi',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 19
            }
          },
          // Phase 6: IoT Sensors
          {
            path: '/inspection/v7/iot-sensors',
            name: 'V7IoTSensors',
            component: () => import('@/views/inspection/v7/platform/IoTSensorView.vue'),
            meta: {
              title: 'IoT传感器',
              icon: 'Activity',
              requiresAuth: true,
              permission: 'insp:platform:view',
              order: 20
            }
          },
          // Phase 3.5: Rating Leaderboard
          {
            path: '/inspection/v7/ratings',
            name: 'V7RatingLeaderboard',
            component: () => import('@/views/inspection/v7/ratings/RatingLeaderboardView.vue'),
            meta: {
              title: '评级排名榜',
              icon: 'Trophy',
              requiresAuth: true,
              permission: 'insp:analytics:view',
              order: 8.5
            }
          }
        ]
      },

      // ==================== 教务管理 /teaching (order: 20) - Teaching领域 ====================
      {
        path: '/teaching',
        name: 'Teaching',
        redirect: '/teaching/workbench',
        meta: {
          title: '教务管理',
          icon: 'BookOpen',
          requiresAuth: true,
          order: 20,
          group: 'operations'
        },
        children: [
          {
            path: '/teaching/workbench',
            name: 'TeachingWorkbench',
            component: () => import('@/views/teaching/TeachingWorkbench.vue'),
            meta: {
              title: '教务工作台',
              requiresAuth: true,
              order: 0
            }
          },
          {
            path: '/teaching/calendar',
            name: 'TeachingCalendar',
            component: () => import('@/views/teaching/CalendarCenter.vue'),
            meta: {
              title: '校历管理',
              requiresAuth: true,
              order: 1
            }
          },
          // Redirects for moved routes
          {
            path: '/teaching/courses',
            redirect: '/academic/courses',
            meta: { hidden: true }
          },
          {
            path: '/teaching/curriculum-plans',
            redirect: '/academic/curriculum-plans',
            meta: { hidden: true }
          },
          {
            path: '/teaching/offerings',
            name: 'TeachingOfferings',
            component: () => import('@/views/teaching/OfferingManagementView.vue'),
            meta: {
              title: '开课管理',
              requiresAuth: true,
              hidden: true,
              order: 4
            }
          },
          {
            path: '/teaching/scheduling',
            name: 'TeachingScheduling',
            component: () => import('@/views/teaching/ScheduleCenter.vue'),
            meta: {
              title: '排课中心',
              requiresAuth: true,
              hidden: true,
              order: 5
            }
          },
          {
            path: '/teaching/tasks',
            name: 'TeachingTasks',
            component: () => import('@/views/teaching/TeachingTaskView.vue'),
            meta: {
              title: '教学任务',
              requiresAuth: true,
              hidden: true,
              order: 7
            }
          },
          {
            path: '/teaching/examinations',
            name: 'TeachingExams',
            component: () => import('@/views/teaching/ExaminationView.vue'),
            meta: {
              title: '考试管理',
              requiresAuth: true,
              order: 8
            }
          },
          {
            path: '/teaching/grades',
            name: 'TeachingGrades',
            component: () => import('@/views/teaching/GradeView.vue'),
            meta: {
              title: '成绩管理',
              requiresAuth: true,
              order: 9
            }
          }
        ]
      },

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

      // ==================== 宿舍管理 /dormitory (order: 11.5) ====================
      {
        path: '/dormitory',
        name: 'Dormitory',
        redirect: '/dormitory/overview',
        meta: {
          title: '宿舍管理',
          icon: 'Bed',
          requiresAuth: true,
          order: 11.5,
          group: 'business'
        },
        children: [
          {
            path: '/dormitory/overview',
            name: 'DormitoryOverview',
            component: () => import('@/views/dormitory/DormitoryOverview.vue'),
            meta: {
              title: '宿舍总览',
              requiresAuth: true,
              permission: 'place:view',
              order: 1
            }
          },
          {
            path: '/dormitory/students',
            name: 'StudentDormitory',
            component: () => import('@/views/dormitory/StudentDormitoryView.vue'),
            meta: {
              title: '住宿管理',
              requiresAuth: true,
              permission: 'place:view',
              order: 2
            }
          },
          {
            path: '/dormitory/department',
            name: 'DormitoryDepartmentAssign',
            component: () => import('@/views/dormitory/DepartmentDormitoryView.vue'),
            meta: {
              title: '组织分配',
              requiresAuth: true,
              permission: 'dormitory:org:view',
              order: 3
            }
          }
        ]
      },

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
