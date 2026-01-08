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
  // 公开查看通报页面（无需登录）
  {
    path: '/notification/view/:id',
    name: 'NotificationPublicView',
    component: () => import('@/views/notification/NotificationPublicView.vue'),
    meta: {
      title: '查看通报',
      requiresAuth: false
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
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {
          title: '首页',
          icon: 'House',
          requiresAuth: true,
          order: 1,
          group: 'main'
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
      // 学生事务模块
      {
        path: '/student-affairs',
        name: 'StudentAffairs',
        redirect: '/student-affairs/students',
        meta: {
          title: '学生事务',
          icon: 'User',
          requiresAuth: true,
          order: 2,
          group: 'student-affairs'
        },
        children: [
          {
            path: '/student-affairs/students',
            name: 'Students',
            component: () => import('@/views/student/StudentList.vue'),
            meta: {
              title: '学生管理',
              requiresAuth: true,
              permission: 'student:info:view',
              order: 1
            }
          },
          {
            path: '/student-affairs/classes',
            name: 'Classes',
            component: () => import('@/views/class/ClassList.vue'),
            meta: {
              title: '班级管理',
              requiresAuth: true,
              permission: 'student:class:view',
              order: 2
            }
          },
          {
            path: '/student-affairs/departments',
            name: 'Departments',
            component: () => import('@/views/system/DepartmentsView.vue'),
            meta: {
              title: '部门管理',
              requiresAuth: true,
              permission: 'system:department:view',
              order: 3
            }
          },
        ]
      },
      // 保持旧路由兼容性 - 学生事务重定向
      {
        path: '/students',
        redirect: '/student-affairs/students',
        meta: { hidden: true }
      },
      {
        path: '/classes',
        redirect: '/student-affairs/classes',
        meta: { hidden: true }
      },
      // 任务管理模块
      {
        path: '/task',
        name: 'TaskManagement',
        redirect: '/task/list',
        meta: {
          title: '任务管理',
          icon: 'Tickets',
          requiresAuth: true,
          order: 3,
          group: 'task'
        },
        children: [
          {
            path: '/task/list',
            name: 'TaskList',
            component: () => import('@/views/task/TaskList.vue'),
            meta: {
              title: '任务列表',
              requiresAuth: true,
              permission: 'task:list',
              order: 1
            }
          },
          {
            path: '/task/my',
            name: 'MyTask',
            component: () => import('@/views/task/MyTask.vue'),
            meta: {
              title: '我的任务',
              requiresAuth: true,
              permission: 'task:my',
              order: 2
            }
          },
          {
            path: '/task/approval',
            name: 'TaskApproval',
            component: () => import('@/views/task/TaskApproval.vue'),
            meta: {
              title: '任务审批',
              requiresAuth: true,
              permission: 'task:approve',
              order: 3
            }
          },
          {
            path: '/task/workflow',
            name: 'WorkflowTemplate',
            component: () => import('@/views/task/WorkflowTemplate.vue'),
            meta: {
              title: '流程模板',
              requiresAuth: true,
              permission: 'task:workflow:manage',
              order: 4
            }
          },
          {
            path: '/task/workflow/:id/design',
            name: 'WorkflowDesigner',
            component: () => import('@/views/task/WorkflowDesigner.vue'),
            meta: {
              title: '流程设计',
              requiresAuth: true,
              permission: 'task:workflow:manage',
              hidden: true
            }
          }
        ]
      },
      // 基础设施模块 - 宿舍管理
      {
        path: '/dormitory',
        name: 'Dormitory',
        redirect: '/dormitory/buildings',
        meta: {
          title: '宿舍管理',
          icon: 'HomeFilled',
          requiresAuth: true,
          order: 4,
          group: 'infrastructure'
        },
        children: [
          {
            path: '/dormitory/buildings',
            name: 'DormitoryBuildings',
            component: () => import('@/views/dormitory/DormitoryBuildingManagement.vue'),
            meta: {
              title: '宿舍楼管理',
              requiresAuth: true,
              permission: 'student:dormitory:view',
              order: 1
            }
          },
          {
            path: '/dormitory/rooms',
            name: 'DormitoryRooms',
            component: () => import('@/views/dormitory/DormitoryList.vue'),
            meta: {
              title: '宿舍房间管理',
              requiresAuth: true,
              permission: 'student:dormitory:view',
              order: 2
            }
          },
          {
            path: '/dormitory/overview',
            name: 'DormitoryOverview',
            component: () => import('@/views/dormitory/DormitoryOverview.vue'),
            meta: {
              title: '宿舍总览',
              requiresAuth: true,
              permission: 'student:dormitory:view',
              order: 3
            }
          },
          {
            path: '/dormitory/building-assignments',
            name: 'BuildingDepartmentAssignment',
            component: () => import('@/views/dormitory/BuildingDepartmentAssignment.vue'),
            meta: {
              title: '院系分配',
              requiresAuth: true,
              permission: 'system:dormitory_building:view',
              order: 4,
              hidden: true  // 已废弃：院系分配功能已整合到宿舍楼管理中
            }
          }
        ]
      },
      // 教学管理模块
      {
        path: '/academic',
        name: 'Academic',
        redirect: '/academic/grades',
        meta: {
          title: '教学管理',
          icon: 'Reading',
          requiresAuth: true,
          order: 5,
          group: 'academic'
        },
        children: [
          {
            path: '/academic/grades',
            name: 'Grades',
            component: () => import('@/views/quantification/GradeManagement.vue'),
            meta: {
              title: '年级管理',
              requiresAuth: true,
              permission: 'quantification:grade:view',
              order: 1
            }
          },
          {
            path: '/academic/majors',
            name: 'Majors',
            component: () => import('@/views/major/MajorList.vue'),
            meta: {
              title: '专业管理',
              requiresAuth: true,
              permission: 'major:list',
              order: 2
            }
          },
        ]
      },
      // 保持旧路由兼容性 - 重定向
      {
        path: '/majors',
        redirect: '/academic/majors',
        meta: { hidden: true }
      },
      {
        path: '/major-directions',
        redirect: '/academic/majors',
        meta: { hidden: true }
      },
      {
        path: '/grades',
        redirect: '/academic/grades',
        meta: { hidden: true }
      },
      // 量化检查模块 - 日常操作
      {
        path: '/quantification',
        name: 'Quantification',
        redirect: '/quantification/templates',
        meta: {
          title: '量化检查',
          icon: 'DocumentChecked',
          requiresAuth: true,
          order: 6,
          group: 'quantification'
        },
        children: [
          {
            path: '/quantification/config',
            name: 'QuantificationUnified',
            component: () => import('@/views/quantification/QuantificationUnifiedView.vue'),
            meta: {
              title: '量化配置',
              requiresAuth: true,
              permission: 'quantification:config:view',
              order: 1
            }
          },
          {
            path: '/quantification/check-plan',
            name: 'CheckPlanList',
            component: () => import('@/views/quantification/CheckPlanListView.vue'),
            meta: {
              title: '检查计划',
              requiresAuth: true,
              permission: 'quantification:plan:view',
              order: 2
            }
          },
          {
            path: '/quantification/check-plan/create',
            name: 'CheckPlanCreate',
            component: () => import('@/views/quantification/CheckPlanCreateView.vue'),
            meta: {
              title: '新建检查计划',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:plan:add'
            }
          },
          {
            path: '/quantification/check-plan/:id',
            name: 'CheckPlanDetail',
            component: () => import('@/views/quantification/CheckPlanDetailView.vue'),
            meta: {
              title: '检查计划详情',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:plan:view'
            }
          },
          {
            path: '/quantification/check-plan/:id/smart-statistics',
            name: 'SmartStatistics',
            component: () => import('@/views/quantification/SmartStatisticsView.vue'),
            meta: {
              title: '智能统计分析',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:record:view'
            }
          },
          {
            path: '/quantification/check-plan/:planId/rating-frequency',
            name: 'RatingFrequency',
            component: () => import('@/views/quantification/components/rating/RatingFrequencyView.vue'),
            meta: {
              title: '评级频次统计',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:rating:view'
            }
          },
          {
            path: '/quantification/check-plan/:planId/rating-audit',
            name: 'RatingAudit',
            component: () => import('@/views/quantification/components/rating/RatingAuditView.vue'),
            meta: {
              title: '评级审核管理',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:rating:approve'
            }
          },
          {
            path: '/quantification/daily-checks',
            name: 'DailyChecks',
            component: () => import('@/views/quantification/DailyCheckView.vue'),
            meta: {
              title: '日常检查',
              requiresAuth: true,
              permission: 'quantification:check:view',
              order: 3,
              hidden: true  // 已整合到检查计划详情页中
            }
          },
          {
            path: '/quantification/check-record-v3',
            name: 'CheckRecord',
            component: () => import('@/views/quantification/CheckRecordListView.vue'),
            meta: {
              title: '检查记录',
              icon: 'Notebook',
              requiresAuth: true,
              permission: 'quantification:record:v3:list',
              order: 4,
              hidden: true  // 已整合到检查计划详情页中
            }
          },
          {
            path: '/quantification/check-record-v3/:id',
            name: 'CheckRecordDetail',
            component: () => import('@/views/quantification/CheckRecordDetailView.vue'),
            meta: {
              title: '检查记录详情',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:record:v3:detail'
            }
          },
          {
            path: '/quantification/check-record/:id',
            name: 'CheckRecordDetailLegacy',  // 旧版路由，保持兼容性
            component: () => import('@/views/quantification/CheckRecordDetailView.vue'),
            meta: {
              title: '检查记录详情',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:record:view'
            }
          },
          {
            path: '/quantification/check-record-v3/:id/my-class',
            name: 'CheckRecordMyClass',
            component: () => import('@/views/quantification/CheckRecordMyClassView.vue'),
            meta: {
              title: '本班检查详情',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:record:v3:my-class'
            }
          },
          {
            path: '/quantification/appeals-v3',
            name: 'AppealManagementV3',
            component: () => import('@/views/quantification/AppealManagement.vue'),
            meta: {
              title: '申诉管理',
              icon: 'ChatDotRound',
              requiresAuth: true,
              permission: 'quantification:appeal:v3:view',
              order: 5
            }
          },
          {
            path: '/quantification/check-record-scoring',
            name: 'CheckRecordScoring',
            component: () => import('@/views/quantification/CheckRecordScoring.vue'),
            meta: {
              title: '检查打分',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:check:score'
            }
          },
          {
            path: '/quantification/analysis-configs',
            name: 'AnalysisConfigList',
            component: () => import('@/views/quantification/AnalysisConfigListView.vue'),
            meta: {
              title: '统计分析配置',
              hidden: true, // 已集成到检查计划详情页，隐藏菜单入口
              requiresAuth: true,
              permission: 'quantification:statistics:view',
              order: 6
            }
          },
          {
            path: '/quantification/analysis/:configId',
            name: 'AnalysisResult',
            component: () => import('@/views/quantification/AnalysisResultView.vue'),
            meta: {
              title: '分析结果',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:statistics:view'
            }
          },
          {
            path: '/quantification/analysis/snapshot/:snapshotId',
            name: 'AnalysisSnapshot',
            component: () => import('@/views/quantification/AnalysisResultView.vue'),
            meta: {
              title: '历史快照',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:statistics:view'
            }
          },
          {
            path: '/quantification/my-tasks',
            name: 'MyCheckTasks',
            component: () => import('@/views/quantification/MyCheckTasksView.vue'),
            meta: {
              title: '我的检查任务',
              requiresAuth: true,
              order: 7
            }
          },
          {
            path: '/quantification/notification/:id/edit',
            name: 'NotificationEdit',
            component: () => import('@/views/quantification/NotificationEditView.vue'),
            meta: {
              title: '编辑通报',
              hidden: true,
              requiresAuth: true,
              permission: 'quantification:plan:edit'
            }
          },
          {
            path: '/quantification/rating-statistics',
            name: 'RatingStatisticsCenter',
            component: () => import('@/views/quantification/RatingStatisticsCenter.vue'),
            meta: {
              title: '评级统计中心',
              icon: 'TrendCharts',
              requiresAuth: true,
              permission: 'quantification:rating:view',
              order: 8
            }
          },
          {
            path: '/quantification/badge-management',
            name: 'RatingBadgeManagement',
            component: () => import('@/views/quantification/RatingBadgeManagement.vue'),
            meta: {
              title: '荣誉徽章管理',
              icon: 'Medal',
              requiresAuth: true,
              permission: 'quantification:badge:manage',
              order: 9
            }
          },
          {
            path: '/quantification/class-honor/:classId?',
            name: 'ClassHonorDisplay',
            component: () => import('@/views/quantification/ClassHonorDisplay.vue'),
            meta: {
              title: '班级荣誉展示',
              icon: 'Trophy',
              requiresAuth: true,
              permission: 'quantification:rating:view',
              order: 10
            }
          }
        ]
      },
      // 综合素质测评模块
      {
        path: '/evaluation',
        name: 'Evaluation',
        redirect: '/evaluation/periods',
        meta: {
          title: '综合测评',
          icon: 'DataAnalysis',
          requiresAuth: true,
          order: 7,
          group: 'evaluation'
        },
        children: [
          {
            path: '/evaluation/periods',
            name: 'EvaluationPeriods',
            component: () => import('@/views/evaluation/period/index.vue'),
            meta: {
              title: '综测周期',
              requiresAuth: true,
              permission: 'evaluation:period:list',
              order: 1
            }
          },
          {
            path: '/evaluation/honors',
            name: 'HonorApplications',
            component: () => import('@/views/evaluation/honor/index.vue'),
            meta: {
              title: '荣誉申报',
              requiresAuth: true,
              permission: 'evaluation:honor:list',
              order: 2
            }
          },
          {
            path: '/evaluation/results',
            name: 'EvaluationResults',
            component: () => import('@/views/evaluation/result/index.vue'),
            meta: {
              title: '综测结果',
              requiresAuth: true,
              permission: 'evaluation:result:list',
              order: 3
            }
          },
          {
            path: '/evaluation/my-result',
            name: 'MyEvaluationResult',
            component: () => import('@/views/evaluation/result/my.vue'),
            meta: {
              title: '我的综测',
              requiresAuth: true,
              permission: 'evaluation:result:my',
              order: 4
            }
          },
          {
            path: '/evaluation/courses',
            name: 'EvaluationCourses',
            component: () => import('@/views/evaluation/course/index.vue'),
            meta: {
              title: '课程管理',
              requiresAuth: true,
              permission: 'evaluation:course:list',
              order: 5
            }
          },
          {
            path: '/evaluation/scores',
            name: 'StudentScores',
            component: () => import('@/views/evaluation/score/index.vue'),
            meta: {
              title: '成绩管理',
              requiresAuth: true,
              permission: 'evaluation:score:list',
              order: 6
            }
          },
          {
            path: '/evaluation/semesters',
            name: 'SemesterManagement',
            component: () => import('@/views/evaluation/semester/index.vue'),
            meta: {
              title: '学期管理',
              requiresAuth: true,
              permission: 'evaluation:period:list',
              order: 7
            }
          },
          {
            path: '/evaluation/honor-types',
            name: 'HonorTypeManagement',
            component: () => import('@/views/evaluation/honor-type/index.vue'),
            meta: {
              title: '荣誉类型管理',
              requiresAuth: true,
              permission: 'evaluation:honor:list',
              order: 8
            }
          },
          {
            path: '/evaluation/behavior-types',
            name: 'BehaviorTypeManagement',
            component: () => import('@/views/evaluation/behavior-type/index.vue'),
            meta: {
              title: '行为类型管理',
              requiresAuth: true,
              permission: 'evaluation:behavior:list',
              order: 9
            }
          },
          {
            path: '/evaluation/dimensions',
            name: 'DimensionConfig',
            component: () => import('@/views/evaluation/dimension/index.vue'),
            meta: {
              title: '维度配置',
              requiresAuth: true,
              permission: 'evaluation:config:list',
              order: 10
            }
          }
        ]
      },
      // 基础设施模块 - 教学设施
      {
        path: '/teaching',
        name: 'Teaching',
        redirect: '/teaching/buildings',
        meta: {
          title: '教学设施',
          icon: 'Management',
          requiresAuth: true,
          order: 8,
          group: 'infrastructure'
        },
        children: [
          {
            path: '/teaching/buildings',
            name: 'TeachingBuildings',
            component: () => import('@/views/teaching/BuildingManagement.vue'),
            meta: {
              title: '教学楼管理',
              requiresAuth: true,
              permission: 'teaching:building:list',
              order: 1
            }
          },
          {
            path: '/teaching/classrooms',
            name: 'TeachingClassrooms',
            component: () => import('@/views/teaching/ClassroomManagement.vue'),
            meta: {
              title: '教室管理',
              requiresAuth: true,
              permission: 'teaching:classroom:list',
              order: 2
            }
          }
        ]
      },
      // 系统配置模块
      {
        path: '/config',
        name: 'Config',
        redirect: '/config/weight',
        meta: {
          title: '系统配置',
          icon: 'Tools',
          requiresAuth: true,
          order: 8,
          group: 'config'
        },
        children: [
          {
            path: '/config/quantification',
            name: 'QuantificationConfigRedirect',
            redirect: '/quantification/config',
            meta: {
              title: '量化配置',
              hidden: true,
              requiresAuth: true,
              order: 1
            }
          },
          {
            path: '/config/weight',
            name: 'WeightConfig',
            component: () => import('@/views/quantification/WeightConfigManagement.vue'),
            meta: {
              title: '加权配置',
              requiresAuth: true,
              permission: 'quantification:weight:config',
              order: 2
            }
          },
        ]
      },
      // 保持旧路由兼容性 - 量化配置重定向
      {
        path: '/quantification/config',
        redirect: '/config/quantification',
        meta: { hidden: true }
      },
      {
        path: '/quantification/weight-config',
        redirect: '/config/weight',
        meta: { hidden: true }
      },
      // 测试页面 - 仅在开发环境可用
      ...(import.meta.env.DEV ? [{
        path: '/test/pagination',
        name: 'PaginationTest',
        component: () => import('@/views/test/PaginationTest.vue'),
        meta: {
          title: '分页测试',
          requiresAuth: true, // 开发环境也需要登录
          hidden: true // 不显示在菜单中
        }
      }] : []),
      // 系统管理模块
      {
        path: '/system',
        name: 'System',
        redirect: '/system/users',
        meta: {
          title: '系统管理',
          icon: 'Setting',
          requiresAuth: true,
          order: 9,
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
            path: '/system/buildings',
            name: 'SystemBuildings',
            component: () => import('@/views/system/BuildingsView.vue'),
            meta: {
              title: '楼宇管理',
              requiresAuth: true,
              permission: 'system:building:view',
              order: 3
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
              order: 4
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
              order: 5
            }
          },
          {
            path: '/system/configs',
            name: 'SystemConfigs',
            component: () => import('@/views/system/SystemConfigsView.vue'),
            meta: {
              title: '系统配置',
              requiresAuth: true,
              permission: 'system:config:view',
              order: 6
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
              order: 7
            }
          }
        ]
      },
      // 部门管理旧路由重定向到学生事务
      {
        path: '/system/departments',
        redirect: '/student-affairs/departments',
        meta: { hidden: true }
      },
      // V2 Access模块路由别名 (与后端DDD架构对齐)
      {
        path: '/v2/access/roles',
        redirect: '/system/roles',
        meta: { hidden: true }
      },
      {
        path: '/v2/access/permissions',
        redirect: '/system/permissions',
        meta: { hidden: true }
      },
      // V2 Organization模块路由别名
      {
        path: '/v2/organization/classes',
        redirect: '/student-affairs/classes',
        meta: { hidden: true }
      },
      {
        path: '/v2/org-units',
        redirect: '/student-affairs/departments',
        meta: { hidden: true }
      },
      {
        path: '/v2/grades',
        redirect: '/academic/grades',
        meta: { hidden: true }
      },
      // V2 Inspection模块路由别名
      {
        path: '/v2/inspection-templates',
        redirect: '/quantification/config',
        meta: { hidden: true }
      },
      {
        path: '/v2/inspection-records',
        redirect: '/quantification/check-record-v3',
        meta: { hidden: true }
      },
      {
        path: '/v2/appeals',
        redirect: '/quantification/appeals-v3',
        meta: { hidden: true }
      }
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
      next('/403')
      return
    }
  }

  next()
})

export default router