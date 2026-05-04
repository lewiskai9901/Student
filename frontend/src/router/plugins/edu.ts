import type { RouteRecordRaw } from 'vue-router'

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
        component: () => import('@/views/academic/MajorListView.vue'),
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
        component: () => import('@/views/academic/CourseListView.vue'),
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
        component: () => import('@/views/academic/CurriculumPlanView.vue'),
        meta: {
          title: '培养方案',
          requiresAuth: true,
          permission: 'academic:curriculum:view',
          order: 3
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
        component: () => import('@/views/student/EnrollmentView.vue'),
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
          permission: 'student:info:view',
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
        component: () => import('@/views/teaching/TeachingWorkbench.vue'),
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
        component: () => import('@/views/teaching/CalendarCenter.vue'),
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
        component: () => import('@/views/teaching/OfferingManagementView.vue'),
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
        component: () => import('@/views/teaching/ScheduleCenter.vue'),
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
        component: () => import('@/views/teaching/TeachingTaskView.vue'),
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
        component: () => import('@/views/teaching/ExaminationView.vue'),
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
        component: () => import('@/views/teaching/GradeView.vue'),
        meta: {
          title: '成绩管理',
          requiresAuth: true,
          permission: 'teaching:grade:view',
          order: 9
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
  }
]

export default eduRoutes
