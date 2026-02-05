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
      // ==================== 首页 (order: 1) ====================
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

      // ==================== 我的班级 /my-class (order: 1.5) ====================
      {
        path: '/my-class',
        name: 'MyClass',
        redirect: '/my-class/list',
        meta: {
          title: '我的班级',
          icon: 'School',
          requiresAuth: true,
          requiresClass: true, // 需要有分配班级才显示
          order: 1.5,
          group: 'main'
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
            path: '/my-class/:classId',
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

      // ==================== 组织管理 /organization (order: 2) - Organization领域 ====================
      {
        path: '/organization',
        name: 'Organization',
        redirect: '/organization/units',
        meta: {
          title: '组织管理',
          icon: 'OfficeBuilding',
          requiresAuth: true,
          order: 2,
          group: 'organization'
        },
        children: [
          {
            path: '/organization/units',
            name: 'OrgUnits',
            component: () => import('@/views/organization/structure/OrgStructureV2.vue'),
            meta: {
              title: '组织架构',
              requiresAuth: true,
              permission: 'system:org:view',
              order: 1
            }
          },
          {
            path: '/organization/classes',
            name: 'OrgClasses',
            component: () => import('@/views/class/ClassList.vue'),
            meta: {
              title: '班级管理',
              requiresAuth: true,
              permission: 'student:class:view',
              order: 2
            }
          },
          {
            path: '/organization/students',
            name: 'OrgStudents',
            component: () => import('@/views/student/StudentList.vue'),
            meta: {
              title: '学生管理',
              requiresAuth: true,
              permission: 'student:info:view',
              order: 3
            }
          },
          // 年级管理 (V2 重设计版本)
          {
            path: '/organization/grades',
            name: 'OrgGrades',
            component: () => import('@/views/organization/grades/GradeManagementV2.vue'),
            meta: {
              title: '年级管理',
              requiresAuth: true,
              permission: 'quantification:grade:view',
              order: 4
            }
          },
          // 专业管理
          {
            path: '/organization/majors',
            name: 'OrgMajors',
            component: () => import('@/views/major/MajorList.vue'),
            meta: {
              title: '专业管理',
              requiresAuth: true,
              permission: 'major:list',
              order: 5
            }
          },
          // 宿舍管理和教学设施已整合到场所管理中心 (/space/center)
          // 保留组织宿舍管理（特定用途）
          {
            path: '/organization/dormitory',
            name: 'OrgDormitory',
            redirect: '/space/management',  // 重定向到场所管理中心
            meta: {
              title: '宿舍管理',
              requiresAuth: true,
              hidden: true  // 隐藏菜单，已整合到场所管理中心
            },
            children: [
              {
                path: '/organization/dormitory/department',
                name: 'DepartmentDormitory',
                component: () => import('@/views/dormitory/DepartmentDormitoryView.vue'),
                meta: {
                  title: '组织宿舍管理',
                  requiresAuth: true,
                  permission: 'dormitory:org:view',
                  hidden: true  // 暂时隐藏，后续可根据需求决定是否保留
                }
              }
            ]
          }
        ]
      },

      // ==================== 量化检查 /inspection (order: 3) - Inspection领域 ====================
      // V6重构：只保留V6路由，V4旧版路由全部隐藏
      {
        path: '/inspection',
        name: 'Inspection',
        redirect: '/inspection/v6/projects',  // 默认跳转到V6检查项目
        meta: {
          title: '量化检查',
          icon: 'DocumentChecked',
          requiresAuth: true,
          order: 3,
          group: 'inspection'
        },
        children: [
          // ==================== V6检查系统（当前主版本） ====================
          {
            path: '/inspection/v6/projects',
            name: 'V6InspectionProjects',
            component: () => import('@/views/inspection/v6/ProjectList.vue'),
            meta: {
              title: '检查项目',
              icon: 'Folder',
              requiresAuth: true,
              permission: 'inspection:project:view',
              order: 1
            }
          },
          {
            path: '/inspection/v6/projects/create',
            name: 'V6InspectionProjectCreate',
            component: () => import('@/views/inspection/v6/ProjectCreate.vue'),
            meta: {
              title: '创建检查项目',
              hidden: true,
              requiresAuth: true,
              permission: 'inspection:project:create'
            }
          },
          {
            path: '/inspection/v6/projects/:id',
            name: 'V6InspectionProjectDetail',
            component: () => import('@/views/inspection/v6/ProjectDetail.vue'),
            meta: {
              title: '项目详情',
              hidden: true,
              requiresAuth: true,
              permission: 'inspection:project:view'
            }
          },
          {
            path: '/inspection/v6/projects/:id/config',
            name: 'V6InspectionProjectConfig',
            component: () => import('@/views/inspection/v6/ProjectConfig.vue'),
            meta: {
              title: '项目配置',
              hidden: true,
              requiresAuth: true,
              permission: 'inspection:project:update'
            }
          },
          {
            path: '/inspection/v6/tasks',
            name: 'V6InspectionTasks',
            component: () => import('@/views/inspection/v6/TaskList.vue'),
            meta: {
              title: '检查任务',
              icon: 'List',
              requiresAuth: true,
              permission: 'inspection:task:view',
              order: 2
            }
          },
          {
            path: '/inspection/v6/tasks/:id',
            name: 'V6InspectionTaskDetail',
            component: () => import('@/views/inspection/v6/TaskDetail.vue'),
            meta: {
              title: '任务详情',
              hidden: true,
              requiresAuth: true,
              permission: 'inspection:task:view'
            }
          },
          {
            path: '/inspection/v6/tasks/:id/execute',
            name: 'V6InspectionTaskExecute',
            component: () => import('@/views/inspection/v6/TaskExecute.vue'),
            meta: {
              title: '执行检查',
              hidden: true,
              requiresAuth: true,
              permission: 'inspection:task:execute'
            }
          },
          {
            path: '/inspection/v6/my-tasks',
            name: 'V6InspectionMyTasks',
            component: () => import('@/views/inspection/v6/MyTasks.vue'),
            meta: {
              title: '我的任务',
              icon: 'User',
              requiresAuth: true,
              permission: 'inspection:task:execute',
              order: 3
            }
          },
          {
            path: '/inspection/v6/summary',
            name: 'V6InspectionSummary',
            component: () => import('@/views/inspection/v6/SummaryDashboard.vue'),
            meta: {
              title: '汇总统计',
              icon: 'DataLine',
              requiresAuth: true,
              permission: 'inspection:summary:view',
              order: 4
            }
          },
          {
            path: '/inspection/v6/ranking',
            name: 'V6InspectionRanking',
            component: () => import('@/views/inspection/v6/RankingView.vue'),
            meta: {
              title: '排名榜',
              icon: 'Trophy',
              requiresAuth: true,
              permission: 'inspection:summary:view',
              order: 5
            }
          },
          {
            path: '/inspection/v6/scoring-strategies',
            name: 'V6ScoringStrategyManagement',
            component: () => import('@/views/inspection/v6/ScoringStrategyManagement.vue'),
            meta: {
              title: '打分策略',
              icon: 'SetUp',
              requiresAuth: true,
              permission: 'inspection:config:view',
              order: 6
            }
          },
          {
            path: '/inspection/v6/templates',
            name: 'V6TemplateManagement',
            component: () => import('@/views/inspection/v6/TemplateManagement.vue'),
            meta: {
              title: '模板配置',
              icon: 'Document',
              requiresAuth: true,
              permission: 'inspection:template:view',
              order: 7
            }
          },
          {
            path: '/inspection/v6/corrective-actions',
            name: 'V6CorrectiveActionManagement',
            component: () => import('@/views/inspection/v6/CorrectiveActionManagement.vue'),
            meta: {
              title: '整改管理',
              icon: 'Edit',
              requiresAuth: true,
              permission: 'inspection:corrective:view',
              order: 8
            }
          }
        ]
      },

      // ==================== 综合测评 /evaluation (order: 4) ====================
      {
        path: '/evaluation',
        name: 'Evaluation',
        redirect: '/evaluation/periods',
        meta: {
          title: '综合测评',
          icon: 'DataAnalysis',
          requiresAuth: true,
          order: 4,
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

      // ==================== 教务管理 /teaching (order: 4.5) - Teaching领域 ====================
      {
        path: '/teaching',
        name: 'Teaching',
        redirect: '/teaching/calendar',
        meta: {
          title: '教务管理',
          icon: 'Reading',
          requiresAuth: true,
          order: 4.5,
          group: 'teaching'
        },
        children: [
          {
            path: '/teaching/calendar',
            name: 'TeachingCalendar',
            component: () => import('@/views/teaching/AcademicCalendarView.vue'),
            meta: {
              title: '校历管理',
              requiresAuth: true,
              order: 1
            }
          },
          {
            path: '/teaching/courses',
            name: 'TeachingCourses',
            component: () => import('@/views/teaching/CourseListView.vue'),
            meta: {
              title: '课程管理',
              requiresAuth: true,
              order: 2
            }
          },
          {
            path: '/teaching/curriculum-plans',
            name: 'TeachingCurriculumPlans',
            component: () => import('@/views/teaching/CurriculumPlanView.vue'),
            meta: {
              title: '培养方案',
              requiresAuth: true,
              order: 3
            }
          },
          {
            path: '/teaching/tasks',
            name: 'TeachingTasks',
            component: () => import('@/views/teaching/TeachingTaskView.vue'),
            meta: {
              title: '教学任务',
              requiresAuth: true,
              order: 4
            }
          },
          {
            path: '/teaching/schedules',
            name: 'TeachingSchedules',
            component: () => import('@/views/teaching/ScheduleView.vue'),
            meta: {
              title: '排课管理',
              requiresAuth: true,
              order: 5
            }
          },
          {
            path: '/teaching/adjustments',
            name: 'TeachingAdjustments',
            component: () => import('@/views/teaching/ScheduleAdjustmentView.vue'),
            meta: {
              title: '调课管理',
              requiresAuth: true,
              order: 6
            }
          },
          {
            path: '/teaching/examinations',
            name: 'TeachingExaminations',
            component: () => import('@/views/teaching/ExaminationView.vue'),
            meta: {
              title: '考试管理',
              requiresAuth: true,
              order: 7
            }
          },
          {
            path: '/teaching/grades',
            name: 'TeachingGrades',
            component: () => import('@/views/teaching/GradeView.vue'),
            meta: {
              title: '成绩管理',
              requiresAuth: true,
              order: 8
            }
          }
        ]
      },

      // ==================== 任务管理 /task (order: 5) ====================
      {
        path: '/task',
        name: 'TaskManagement',
        redirect: '/task/list',
        meta: {
          title: '任务管理',
          icon: 'Tickets',
          requiresAuth: true,
          order: 5,
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

      // ==================== 权限管理 /access (order: 6) - Access领域 ====================
      {
        path: '/access',
        name: 'Access',
        redirect: '/access/users',
        meta: {
          title: '权限管理',
          icon: 'Lock',
          requiresAuth: true,
          order: 6,
          group: 'access'
        },
        children: [
          {
            path: '/access/users',
            name: 'AccessUsers',
            component: () => import('@/views/system/UsersView.vue'),
            meta: {
              title: '用户管理',
              requiresAuth: true,
              permission: 'system:user:view',
              order: 1
            }
          },
          {
            path: '/access/roles',
            name: 'AccessRoles',
            component: () => import('@/views/system/RolesView.vue'),
            meta: {
              title: '角色管理',
              requiresAuth: true,
              permission: 'system:role:view',
              order: 2
            }
          },
          {
            path: '/access/permissions',
            name: 'AccessPermissions',
            component: () => import('@/views/system/PermissionsView.vue'),
            meta: {
              title: '权限管理',
              requiresAuth: true,
              permission: 'system:permission:view',
              order: 3
            }
          }
        ]
      },

      // ==================== 系统设置 /settings (order: 7) ====================
      {
        path: '/settings',
        name: 'Settings',
        redirect: '/settings/configs',
        meta: {
          title: '系统设置',
          icon: 'Setting',
          requiresAuth: true,
          order: 7,
          group: 'settings'
        },
        children: [
          {
            path: '/settings/configs',
            name: 'SettingsConfigs',
            component: () => import('@/views/system/SystemConfigsView.vue'),
            meta: {
              title: '系统配置',
              requiresAuth: true,
              permission: 'system:config:view',
              order: 1
            }
          },
          {
            path: '/settings/login-customization',
            name: 'SettingsLoginCustomization',
            component: () => import('@/views/settings/LoginCustomization.vue'),
            meta: {
              title: '登录页自定义',
              requiresAuth: true,
              permission: 'system:config:edit',
              order: 1.5
            }
          },
          {
            path: '/settings/semesters',
            name: 'SettingsSemesters',
            component: () => import('@/views/system/SemesterView.vue'),
            meta: {
              title: '学期管理',
              requiresAuth: true,
              permission: 'system:semester:view',
              order: 3
            }
          },
          {
            path: '/settings/announcements',
            name: 'SettingsAnnouncements',
            component: () => import('@/views/system/AnnouncementsView.vue'),
            meta: {
              title: '公告管理',
              requiresAuth: true,
              permission: 'system:announcement:view',
              order: 4
            }
          },
          {
            path: '/settings/operation-logs',
            name: 'SettingsOperationLogs',
            component: () => import('@/views/system/OperationLogsView.vue'),
            meta: {
              title: '操作日志',
              requiresAuth: true,
              permission: 'system:operlog:view',
              order: 5
            }
          },
          // V6 类型配置
          {
            path: '/settings/org-types',
            name: 'SettingsOrgTypes',
            component: () => import('@/views/system/OrgTypeConfig.vue'),
            meta: {
              title: '组织类型配置',
              requiresAuth: true,
              permission: 'system:org:view',
              order: 6
            }
          },
          {
            path: '/settings/space-types',
            name: 'SettingsSpaceTypes',
            component: () => import('@/views/system/UniversalSpaceTypeConfig.vue'),
            meta: {
              title: '空间类型配置',
              requiresAuth: true,
              permission: 'system:space-type:view',
              order: 7
            }
          },
          {
            path: '/settings/user-types',
            name: 'SettingsUserTypes',
            component: () => import('@/views/system/UserTypeConfig.vue'),
            meta: {
              title: '用户类型配置',
              requiresAuth: true,
              permission: 'system:user:view',
              order: 8
            }
          },
          // V6 计分系统配置
          {
            path: '/settings/scoring-strategies',
            name: 'SettingsScoringStrategies',
            component: () => import('@/views/system/ScoringStrategyConfig.vue'),
            meta: {
              title: '计分策略配置',
              requiresAuth: true,
              permission: 'inspection:config:view',
              order: 9
            }
          },
          {
            path: '/settings/input-types',
            name: 'SettingsInputTypes',
            component: () => import('@/views/system/InputTypeConfig.vue'),
            meta: {
              title: '打分方式配置',
              requiresAuth: true,
              permission: 'inspection:config:view',
              order: 10
            }
          },
          {
            path: '/settings/calculation-rules',
            name: 'SettingsCalculationRules',
            component: () => import('@/views/system/CalculationRuleConfig.vue'),
            meta: {
              title: '计算规则配置',
              requiresAuth: true,
              permission: 'inspection:config:view',
              order: 11
            }
          },
          // 楼宇管理已整合到场所管理中心 (/space/center)
          {
            path: '/settings/buildings',
            redirect: '/space/management',
            meta: {
              hidden: true  // 已整合到场所管理中心
            }
          }
        ]
      },

      // ==================== 场所管理 /space (order: 2.5) - Space领域 ====================
      // 统一管理所有场所：校区、楼宇（宿舍楼/教学楼/办公楼）、楼层、房间（宿舍/教室/办公室等）
      {
        path: '/space',
        name: 'Space',
        redirect: '/space/management',
        meta: {
          title: '空间管理',
          icon: 'OfficeBuilding',
          requiresAuth: true,
          order: 2.5,  // 放在组织管理之后，量化检查之前
          group: 'space'
        },
        children: [
          {
            path: '/space/management',
            name: 'UniversalSpaceManagement',
            component: () => import('@/views/space/UniversalSpaceManagement.vue'),
            meta: {
              title: '空间管理',
              requiresAuth: true,
              permission: 'space:view',
              order: 1
            }
          },
          {
            path: '/space/allocation',
            name: 'SpaceAllocation',
            redirect: '/space/management',
            meta: {
              title: '空间分配',
              requiresAuth: true,
              hidden: true  // 重定向到新版空间管理
            }
          },
          // 旧版路由重定向到新版
          {
            path: '/space/center',
            name: 'SpaceCenter',
            redirect: '/space/management',
            meta: {
              hidden: true
            }
          }
        ]
      },

      // ==================== 资产管理 /asset (order: 8) - Asset领域 ====================
      {
        path: '/asset',
        name: 'Asset',
        redirect: '/asset/center',
        meta: {
          title: '资产管理',
          icon: 'Box',
          requiresAuth: true,
          order: 8,
          group: 'asset'
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

      // ==================== 向后兼容重定向 ====================

      // 学生事务重定向到组织管理
      {
        path: '/student-affairs',
        redirect: '/organization/students',
        meta: { hidden: true }
      },
      {
        path: '/student-affairs/students',
        redirect: '/organization/students',
        meta: { hidden: true }
      },
      {
        path: '/student-affairs/classes',
        redirect: '/organization/classes',
        meta: { hidden: true }
      },
      {
        path: '/student-affairs/departments',
        redirect: '/organization/units',
        meta: { hidden: true }
      },
      {
        path: '/students',
        redirect: '/organization/students',
        meta: { hidden: true }
      },
      {
        path: '/classes',
        redirect: '/organization/classes',
        meta: { hidden: true }
      },

      // 宿舍管理重定向 - 统一到场所管理中心
      {
        path: '/dormitory',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/dormitory/buildings',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/dormitory/rooms',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/dormitory/overview',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/dormitory/building-assignments',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/dormitory/center',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/dormitory/buildings',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/dormitory/rooms',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/dormitory/overview',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/dormitory/building-assignments',
        redirect: '/space/management',
        meta: { hidden: true }
      },

      // 教学管理重定向
      {
        path: '/academic',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },
      // 旧的 academic 路径重定向
      {
        path: '/organization/academic',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },
      {
        path: '/organization/academic/grades',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },
      {
        path: '/organization/academic/majors',
        redirect: '/organization/majors',
        meta: { hidden: true }
      },
      {
        path: '/academic/grades',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },
      {
        path: '/academic/majors',
        redirect: '/organization/majors',
        meta: { hidden: true }
      },
      {
        path: '/majors',
        redirect: '/organization/majors',
        meta: { hidden: true }
      },
      {
        path: '/major-directions',
        redirect: '/organization/majors',
        meta: { hidden: true }
      },
      {
        path: '/grades',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },

      // 教学设施重定向 - 统一到场所管理中心
      // 注意：/teaching 是教务管理模块，不在此处重定向
      {
        path: '/facility/buildings',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/facility/classrooms',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/teaching/buildings',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/teaching/classrooms',
        redirect: '/space/management',
        meta: { hidden: true }
      },
      {
        path: '/organization/teaching/classroom-center',
        redirect: '/space/management',
        meta: { hidden: true }
      },

      // 量化检查重定向到V6版inspection
      {
        path: '/quantification',
        redirect: '/inspection/v6/projects',
        meta: { hidden: true }
      },
      {
        path: '/quantification/config',
        redirect: '/inspection/v6/templates',
        meta: { hidden: true }
      },
      {
        path: '/quantification/check-plan',
        redirect: '/inspection/v6/projects',
        meta: { hidden: true }
      },
      {
        path: '/quantification/my-tasks',
        redirect: '/inspection/v6/my-tasks',
        meta: { hidden: true }
      },
      {
        path: '/inspection/config',
        redirect: '/inspection/v6/templates',
        meta: { hidden: true }
      },

      // 系统管理重定向到access或settings
      {
        path: '/system',
        redirect: '/access/users',
        meta: { hidden: true }
      },
      {
        path: '/system/users',
        redirect: '/access/users',
        meta: { hidden: true }
      },
      {
        path: '/system/roles',
        redirect: '/access/roles',
        meta: { hidden: true }
      },
      {
        path: '/system/permissions',
        redirect: '/access/permissions',
        meta: { hidden: true }
      },
      {
        path: '/system/departments',
        redirect: '/organization/units',
        meta: { hidden: true }
      },
      {
        path: '/system/configs',
        redirect: '/settings/configs',
        meta: { hidden: true }
      },
      {
        path: '/system/operation-logs',
        redirect: '/settings/operation-logs',
        meta: { hidden: true }
      },
      {
        path: '/system/announcements',
        redirect: '/settings/announcements',
        meta: { hidden: true }
      },
      {
        path: '/system/semesters',
        redirect: '/settings/semesters',
        meta: { hidden: true }
      },
      {
        path: '/system/buildings',
        redirect: '/settings/buildings',
        meta: { hidden: true }
      },

      // 配置模块重定向
      {
        path: '/config',
        redirect: '/settings/weight',
        meta: { hidden: true }
      },
      {
        path: '/config/quantification',
        redirect: '/inspection/config',
        meta: { hidden: true }
      },
      {
        path: '/config/weight',
        redirect: '/settings/weight',
        meta: { hidden: true }
      },

      // V2 API路由别名 (与后端DDD架构对齐)
      {
        path: '/access/roles',
        redirect: '/access/roles',
        meta: { hidden: true }
      },
      {
        path: '/access/permissions',
        redirect: '/access/permissions',
        meta: { hidden: true }
      },
      {
        path: '/organization/classes',
        redirect: '/organization/classes',
        meta: { hidden: true }
      },
      {
        path: '/org-units',
        redirect: '/organization/units',
        meta: { hidden: true }
      },
      {
        path: '/grades',
        redirect: '/organization/grades',
        meta: { hidden: true }
      },
      {
        path: '/inspection-templates',
        redirect: '/inspection/v6/templates',
        meta: { hidden: true }
      },
      {
        path: '/inspection-records',
        redirect: '/inspection/v6/tasks',
        meta: { hidden: true }
      },
      {
        path: '/appeals',
        redirect: '/inspection/v6/corrective-actions',
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
