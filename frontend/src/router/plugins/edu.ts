import type { RouteRecordRaw } from 'vue-router'
import { registerRelationScenes } from '@/components/access/relationScenes'
import { registerScopeSpecializations } from '@/views/access/data-permissions/dataScopeSpecializations'
import { registerRoleTemplates } from '@/views/access/data-permissions/composables/useTemplateLibrary'

/**
 * 教育行业 (EDU) 路由 — Phase 4A 条件加载
 *
 * 这些路由只在 /api/plugin-platform/overview 返回 EDU.enabled=true 时注册 (见 router/bootstrap.ts).
 * 禁用 EDU 时前端自动失去这些入口, 访问对应 URL 走 catchAll > NotFound.
 *
 * 对应后端 plugins/education/domain/{student,academic,teaching,calendar} + dormitory / myclass.
 * /organization/* 下的 6 个 legacy redirect 仍留在 router/index.ts —
 * 它们禁用 EDU 后会 redirect > NotFound, 这是预期行为 (兼容老链接)。
 */

// 关系绑定"业务场景"贡献 — 本模块仅在 EDU 启用时被 bootstrap 动态 import, 故此登记只在启用时生效。
// "指定班主任" = 通用 admin 关系 + metadata.role='CLASS_TEACHER' (与后端 EducationManifest 建模一致,
// 区别于通用"组织管理员")。
registerRelationScenes('EDU', [
  {
    code: 'ASSIGN_CLASS_ADMIN', title: '指定班主任', desc: '给班级绑定班主任',
    relation: 'admin', subjectType: 'user', resourceType: 'org_unit',
    subjectLabel: '选择老师（作为班主任）', resourceLabel: '选择班级/年级',
    metadata: { role: 'CLASS_TEACHER' },
  },
])

// 数据权限"我的学生"特化维度 — 仅 EDU 启用时被 bootstrap 动态 import 才登记。
// scope code (BY_CLASS/BY_GRADE/BY_MAJOR) 是后端 EducationDataScopePlugin 贡献的维度,
// 这里给主体视角的中文措辞 + 覆盖哪些学生数据模块。
registerScopeSpecializations('EDU', [
  {
    pluginCode: 'EDU',
    groupCode: 'student',
    title: '我的学生',
    moduleCodes: ['student', 'attendance', 'grade_batch', 'student_grade', 'exam', 'enrollment', 'dormitory_student'],
    options: [
      { code: 'ALL', label: '全部学生', desc: '所有学生数据' },
      { code: 'BY_CLASS', label: '我带的班级', desc: '按班级关系' },
      { code: 'BY_GRADE', label: '我管的年级', desc: '按年级关系' },
      { code: 'BY_MAJOR', label: '我管的专业', desc: '按专业关系' },
      { code: 'SELF', label: '仅本人', desc: '只看自己绑定的学生' },
    ],
    fallbackChain: {
      BY_GRADE: ['BY_CLASS', 'BY_MAJOR', 'ALL', 'SELF'],
      BY_CLASS: ['BY_GRADE', 'BY_MAJOR', 'ALL', 'SELF'],
      BY_MAJOR: ['BY_GRADE', 'BY_CLASS', 'ALL', 'SELF'],
    },
  },
])

// 数据权限模板库 — EDU 行业模板 (班主任/年级主任), 仅 EDU 启用时出现在模板库。
registerRoleTemplates('EDU', [
  {
    id: 'class-teacher', name: '班主任', icon: 'BookOpen', industry: 'EDU',
    description: '我带的班级学生 + 本部门数据', scenario: '中小学班主任、辅导员',
    scene: { primary: 'DEPARTMENT', specializations: { student: 'BY_CLASS' }, bizAutoFollow: true },
  },
  {
    id: 'grade-director', name: '年级主任', icon: 'GraduationCap', industry: 'EDU',
    description: '我管的年级全部数据 + 部门及以下', scenario: '年级组长、高中年级主任',
    scene: { primary: 'DEPARTMENT_AND_BELOW', specializations: { student: 'BY_GRADE' }, bizAutoFollow: true },
  },
])
const eduRoutes: RouteRecordRaw[] = [
  // ==================== 我的班级 /my-class (order: 2) ====================
  {
    path: '/my-class',
    name: 'MyClass',
    redirect: '/my-class/list',
    meta: {
      title: '我的班级',
      icon: 'School',
      requiresAuth: true,
      requiresClass: true,
      order: 2,
      group: 'daily'
    },
    children: [
      {
        path: '/my-class/list',
        name: 'MyClassList',
        component: () => import('@/views/plugins/edu/myclass/MyClassListView.vue'),
        meta: {
          title: '班级列表',
          requiresAuth: true,
          permission: 'student:info:view',   // J6: 补 meta 缺口, requiresClass 守卫从不读
          hidden: true
        }
      },
      {
        path: '/my-class/:orgUnitId',
        name: 'MyClassDetail',
        component: () => import('@/views/plugins/edu/myclass/MyClassDetailView.vue'),
        meta: {
          title: '班级详情',
          requiresAuth: true,
          permission: 'student:info:view',   // J6: 补 meta 缺口
          hidden: true
        }
      }
    ]
  },

  // ==================== 学术管理 /academic (order: 3) ====================
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
        component: () => import('@/views/plugins/edu/academic/MajorListView.vue'),
        meta: {
          title: '专业管理',
          requiresAuth: true,
          permission: 'academic:major:view',
          order: 1
        }
      },
      {
        path: '/academic/courses',
        name: 'AcademicCourses',
        component: () => import('@/views/plugins/edu/academic/CourseListView.vue'),
        meta: {
          title: '课程管理',
          requiresAuth: true,
          permission: 'academic:course:view',
          order: 2
        }
      },
      {
        path: '/academic/curriculum-plans',
        name: 'AcademicCurriculum',
        component: () => import('@/views/plugins/edu/academic/CurriculumPlanView.vue'),
        meta: {
          title: '培养方案',
          requiresAuth: true,
          permission: 'academic:curriculum:view',
          order: 3
        }
      },
      {
        // 教育菜单: 从 system.ts 迁入 (路径保持 /system/semesters, 后端菜单已归位学术管理下)
        path: '/system/semesters',
        name: 'SystemSemesters',
        component: () => import('@/views/system/SemesterView.vue'),
        meta: {
          title: '学期管理',
          requiresAuth: true,
          permission: 'system:semester:view',
          order: 9
        }
      }
    ]
  },

  // ==================== 学生管理 /student (order: 4) ====================
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
        component: () => import('@/views/plugins/edu/student/EnrollmentView.vue'),
        meta: {
          title: '招生管理',
          requiresAuth: true,
          permission: 'enrollment:view',
          order: 0
        }
      },
      {
        path: '/student/list',
        name: 'StudentList',
        component: () => import('@/views/plugins/edu/student/StudentList.vue'),
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
        component: () => import('@/views/plugins/edu/student/ClassManagement.vue'),
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
        component: () => import('@/views/plugins/edu/organization/grades/GradeManagementV2.vue'),
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
        component: () => import('@/views/plugins/edu/student/AttendanceView.vue'),
        meta: {
          title: '考勤管理',
          requiresAuth: true,
          permission: 'student:info:view',
          order: 4
        }
      },
      {
        path: '/student/warnings',
        name: 'StudentWarnings',
        component: () => import('@/views/plugins/edu/student/AcademicWarningView.vue'),
        meta: {
          title: '学业预警',
          requiresAuth: true,
          permission: 'student:info:view',
          order: 5
        }
      }
    ]
  },

  // ==================== 教务管理 /teaching (order: 20) ====================
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
        component: () => import('@/views/plugins/edu/teaching/TeachingWorkbench.vue'),
        meta: {
          title: '教务工作台',
          requiresAuth: true,
          permission: 'teaching:grade:view',
          order: 0
        }
      },
      {
        path: '/teaching/calendar',
        name: 'TeachingCalendar',
        component: () => import('@/views/plugins/edu/teaching/CalendarCenter.vue'),
        meta: {
          title: '校历管理',
          requiresAuth: true,
          permission: 'calendar:view',
          order: 1
        }
      },
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
        component: () => import('@/views/plugins/edu/teaching/OfferingManagementView.vue'),
        meta: {
          title: '开课管理',
          requiresAuth: true,
          permission: 'teaching:manage',
          hidden: true,
          order: 4
        }
      },
      {
        path: '/teaching/scheduling',
        name: 'TeachingScheduling',
        component: () => import('@/views/plugins/edu/teaching/ScheduleCenter.vue'),
        meta: {
          title: '排课中心',
          requiresAuth: true,
          permission: 'teaching:manage',
          hidden: true,
          order: 5
        }
      },
      {
        path: '/teaching/tasks',
        name: 'TeachingTasks',
        component: () => import('@/views/plugins/edu/teaching/TeachingTaskView.vue'),
        meta: {
          title: '教学任务',
          requiresAuth: true,
          permission: 'teaching:manage',
          hidden: true,
          order: 7
        }
      },
      {
        path: '/teaching/examinations',
        name: 'TeachingExams',
        component: () => import('@/views/plugins/edu/teaching/ExaminationView.vue'),
        meta: {
          title: '考试管理',
          requiresAuth: true,
          permission: 'teaching:exam:view',
          order: 8
        }
      },
      {
        path: '/teaching/grades',
        name: 'TeachingGrades',
        component: () => import('@/views/plugins/edu/teaching/GradeView.vue'),
        meta: {
          title: '成绩管理',
          requiresAuth: true,
          permission: 'teaching:grade:view',
          order: 9
        }
      },
      {
        // 教育菜单: 从 system.ts 迁入 (路径保持 /system/teachers, 后端菜单已归位教务管理下)
        path: '/system/teachers',
        name: 'TeacherProfiles',
        component: () => import('@/views/system/TeacherProfileView.vue'),
        meta: {
          title: '教师档案',
          requiresAuth: true,
          permission: 'system:teacher:view',
          order: 10
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
        component: () => import('@/views/plugins/edu/dormitory/DormitoryOverview.vue'),
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
        component: () => import('@/views/plugins/edu/dormitory/StudentDormitoryView.vue'),
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
        component: () => import('@/views/plugins/edu/dormitory/DepartmentDormitoryView.vue'),
        meta: {
          title: '组织分配',
          requiresAuth: true,
          permission: 'dormitory:org:view',
          order: 3
        }
      }
    ]
  }
]

export default eduRoutes
