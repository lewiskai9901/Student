# 侧边导航DDD架构对齐重构计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将前端侧边导航菜单重构为与后端DDD架构（Organization、Inspection、Access三大领域）对齐的结构

**Architecture:** 采用渐进式重构策略，通过修改路由配置实现菜单重组，利用现有的自动菜单生成机制，保持向后兼容的重定向路由

**Tech Stack:** Vue Router 4, TypeScript, Vue 3 Composition API

---

## 一、重构目标对比

### 1.1 当前菜单结构 (V1风格)

```
├── 首页 (order: 1)
├── 学生事务 (order: 2)        ← Organization 碎片
│   ├── 学生管理
│   ├── 班级管理
│   └── 部门管理
├── 任务管理 (order: 3)        ← 独立业务
├── 宿舍管理 (order: 4)        ← Organization 碎片
├── 教学管理 (order: 5)        ← Organization 碎片
├── 量化检查 (order: 6)        ← Inspection ✓
├── 综合测评 (order: 7)        ← 独立业务
├── 教学设施 (order: 8)        ← Organization 碎片
├── 系统配置 (order: 8)        ← 配置
└── 系统管理 (order: 9)        ← Access 碎片
```

### 1.2 目标菜单结构 (DDD对齐)

```
├── 首页 (order: 1)
├── 组织管理 (order: 2)        ← Organization 领域聚合
│   ├── 组织架构                 (部门/院系)
│   ├── 班级管理
│   ├── 学生管理
│   ├── 年级专业
│   ├── 宿舍管理               (子菜单展开)
│   │   ├── 宿舍楼管理
│   │   ├── 宿舍房间
│   │   └── 宿舍总览
│   └── 教学设施               (子菜单展开)
│       ├── 教学楼管理
│       └── 教室管理
├── 量化检查 (order: 3)        ← Inspection 领域
│   ├── 量化配置
│   ├── 检查计划
│   ├── 申诉管理
│   ├── 我的检查任务
│   ├── 评级统计
│   └── 荣誉徽章
├── 综合测评 (order: 4)        ← Evaluation (独立业务)
├── 任务管理 (order: 5)        ← Task (独立业务)
├── 权限管理 (order: 6)        ← Access 领域
│   ├── 用户管理
│   ├── 角色管理
│   └── 权限管理
└── 系统设置 (order: 7)        ← Settings (运维配置)
    ├── 系统配置
    ├── 操作日志
    ├── 公告管理
    ├── 学期管理
    └── 加权配置
```

---

## 二、实施任务

### Task 1: 备份当前路由配置

**Files:**
- Backup: `frontend/src/router/index.ts` → `frontend/src/router/index.backup.ts`

**Step 1: 创建备份文件**

```bash
cp frontend/src/router/index.ts frontend/src/router/index.backup.ts
```

**Step 2: 确认备份成功**

```bash
ls -la frontend/src/router/
```
Expected: 看到 `index.backup.ts` 文件

**Step 3: Commit**

```bash
git add frontend/src/router/index.backup.ts
git commit -m "chore: backup router config before DDD alignment refactor"
```

---

### Task 2: 创建组织管理(Organization)路由模块

**Files:**
- Modify: `frontend/src/router/index.ts:64-110` (学生事务)
- Modify: `frontend/src/router/index.ts:192-251` (宿舍管理)
- Modify: `frontend/src/router/index.ts:252-288` (教学管理)
- Modify: `frontend/src/router/index.ts:694-730` (教学设施)

**Step 1: 重构学生事务为组织管理**

将 `StudentAffairs` 路由改为 `Organization`，并整合其他碎片：

```typescript
// 组织管理模块 - Organization领域
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
      component: () => import('@/views/system/DepartmentsView.vue'),
      meta: {
        title: '组织架构',
        requiresAuth: true,
        permission: 'system:department:view',
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
    {
      path: '/organization/academic',
      name: 'OrgAcademic',
      redirect: '/organization/academic/grades',
      meta: {
        title: '年级专业',
        requiresAuth: true,
        order: 4
      },
      children: [
        {
          path: '/organization/academic/grades',
          name: 'OrgGrades',
          component: () => import('@/views/quantification/GradeManagement.vue'),
          meta: {
            title: '年级管理',
            requiresAuth: true,
            permission: 'quantification:grade:view',
            order: 1
          }
        },
        {
          path: '/organization/academic/majors',
          name: 'OrgMajors',
          component: () => import('@/views/major/MajorList.vue'),
          meta: {
            title: '专业管理',
            requiresAuth: true,
            permission: 'major:list',
            order: 2
          }
        }
      ]
    },
    {
      path: '/organization/dormitory',
      name: 'OrgDormitory',
      redirect: '/organization/dormitory/buildings',
      meta: {
        title: '宿舍管理',
        icon: 'HomeFilled',
        requiresAuth: true,
        order: 5
      },
      children: [
        {
          path: '/organization/dormitory/buildings',
          name: 'OrgDormBuildings',
          component: () => import('@/views/dormitory/DormitoryBuildingManagement.vue'),
          meta: {
            title: '宿舍楼管理',
            requiresAuth: true,
            permission: 'student:dormitory:view',
            order: 1
          }
        },
        {
          path: '/organization/dormitory/rooms',
          name: 'OrgDormRooms',
          component: () => import('@/views/dormitory/DormitoryList.vue'),
          meta: {
            title: '宿舍房间',
            requiresAuth: true,
            permission: 'student:dormitory:view',
            order: 2
          }
        },
        {
          path: '/organization/dormitory/overview',
          name: 'OrgDormOverview',
          component: () => import('@/views/dormitory/DormitoryOverview.vue'),
          meta: {
            title: '宿舍总览',
            requiresAuth: true,
            permission: 'student:dormitory:view',
            order: 3
          }
        }
      ]
    },
    {
      path: '/organization/teaching',
      name: 'OrgTeaching',
      redirect: '/organization/teaching/buildings',
      meta: {
        title: '教学设施',
        icon: 'School',
        requiresAuth: true,
        order: 6
      },
      children: [
        {
          path: '/organization/teaching/buildings',
          name: 'OrgTeachBuildings',
          component: () => import('@/views/teaching/BuildingManagement.vue'),
          meta: {
            title: '教学楼管理',
            requiresAuth: true,
            permission: 'teaching:building:list',
            order: 1
          }
        },
        {
          path: '/organization/teaching/classrooms',
          name: 'OrgClassrooms',
          component: () => import('@/views/teaching/ClassroomManagement.vue'),
          meta: {
            title: '教室管理',
            requiresAuth: true,
            permission: 'teaching:classroom:list',
            order: 2
          }
        }
      ]
    }
  ]
}
```

**Step 2: 运行类型检查**

```bash
cd frontend && npm run type-check
```
Expected: 无类型错误

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): create Organization domain menu structure"
```

---

### Task 3: 重构量化检查(Inspection)路由

**Files:**
- Modify: `frontend/src/router/index.ts:305-568` (量化检查模块)

**Step 1: 调整量化检查路由order和路径**

```typescript
// 量化检查模块 - Inspection领域
{
  path: '/inspection',
  name: 'Inspection',
  redirect: '/inspection/config',
  meta: {
    title: '量化检查',
    icon: 'DocumentChecked',
    requiresAuth: true,
    order: 3,  // 调整顺序
    group: 'inspection'
  },
  children: [
    {
      path: '/inspection/config',
      name: 'InspectionConfig',
      component: () => import('@/views/quantification/QuantificationUnifiedView.vue'),
      meta: {
        title: '量化配置',
        requiresAuth: true,
        permission: 'quantification:config:view',
        order: 1
      }
    },
    {
      path: '/inspection/plans',
      name: 'InspectionPlans',
      component: () => import('@/views/quantification/CheckPlanListView.vue'),
      meta: {
        title: '检查计划',
        requiresAuth: true,
        permission: 'quantification:plan:view',
        order: 2
      }
    },
    {
      path: '/inspection/appeals',
      name: 'InspectionAppeals',
      component: () => import('@/views/quantification/AppealManagement.vue'),
      meta: {
        title: '申诉管理',
        icon: 'ChatDotRound',
        requiresAuth: true,
        permission: 'quantification:appeal:v3:view',
        order: 3
      }
    },
    {
      path: '/inspection/my-tasks',
      name: 'InspectionMyTasks',
      component: () => import('@/views/quantification/MyCheckTasksView.vue'),
      meta: {
        title: '我的检查任务',
        requiresAuth: true,
        order: 4
      }
    },
    {
      path: '/inspection/rating-statistics',
      name: 'InspectionRatingStats',
      component: () => import('@/views/quantification/RatingStatisticsCenter.vue'),
      meta: {
        title: '评级统计',
        icon: 'TrendCharts',
        requiresAuth: true,
        permission: 'quantification:rating:view',
        order: 5
      }
    },
    {
      path: '/inspection/badges',
      name: 'InspectionBadges',
      component: () => import('@/views/quantification/RatingBadgeManagement.vue'),
      meta: {
        title: '荣誉徽章',
        icon: 'Medal',
        requiresAuth: true,
        permission: 'quantification:badge:manage',
        order: 6
      }
    },
    {
      path: '/inspection/class-honor/:classId?',
      name: 'InspectionClassHonor',
      component: () => import('@/views/quantification/ClassHonorDisplay.vue'),
      meta: {
        title: '班级荣誉',
        icon: 'Trophy',
        requiresAuth: true,
        permission: 'quantification:rating:view',
        order: 7
      }
    },
    // 隐藏的内部页面 (保留原有hidden路由)
    {
      path: '/inspection/plans/create',
      name: 'InspectionPlanCreate',
      component: () => import('@/views/quantification/CheckPlanCreateView.vue'),
      meta: { title: '新建检查计划', hidden: true, requiresAuth: true, permission: 'quantification:plan:add' }
    },
    {
      path: '/inspection/plans/:id',
      name: 'InspectionPlanDetail',
      component: () => import('@/views/quantification/CheckPlanDetailView.vue'),
      meta: { title: '检查计划详情', hidden: true, requiresAuth: true, permission: 'quantification:plan:view' }
    },
    {
      path: '/inspection/records/:id',
      name: 'InspectionRecordDetail',
      component: () => import('@/views/quantification/CheckRecordDetailView.vue'),
      meta: { title: '检查记录详情', hidden: true, requiresAuth: true, permission: 'quantification:record:v3:detail' }
    },
    {
      path: '/inspection/plans/:id/smart-statistics',
      name: 'InspectionSmartStats',
      component: () => import('@/views/quantification/SmartStatisticsView.vue'),
      meta: { title: '智能统计分析', hidden: true, requiresAuth: true, permission: 'quantification:record:view' }
    },
    {
      path: '/inspection/plans/:planId/rating-frequency',
      name: 'InspectionRatingFrequency',
      component: () => import('@/views/quantification/components/rating/RatingFrequencyView.vue'),
      meta: { title: '评级频次统计', hidden: true, requiresAuth: true, permission: 'quantification:rating:view' }
    },
    {
      path: '/inspection/plans/:planId/rating-audit',
      name: 'InspectionRatingAudit',
      component: () => import('@/views/quantification/components/rating/RatingAuditView.vue'),
      meta: { title: '评级审核管理', hidden: true, requiresAuth: true, permission: 'quantification:rating:approve' }
    },
    {
      path: '/inspection/records/:id/my-class',
      name: 'InspectionRecordMyClass',
      component: () => import('@/views/quantification/CheckRecordMyClassView.vue'),
      meta: { title: '本班检查详情', hidden: true, requiresAuth: true, permission: 'quantification:record:v3:my-class' }
    },
    {
      path: '/inspection/scoring',
      name: 'InspectionScoring',
      component: () => import('@/views/quantification/CheckRecordScoring.vue'),
      meta: { title: '检查打分', hidden: true, requiresAuth: true, permission: 'quantification:check:score' }
    },
    {
      path: '/inspection/notification/:id/edit',
      name: 'InspectionNotificationEdit',
      component: () => import('@/views/quantification/NotificationEditView.vue'),
      meta: { title: '编辑通报', hidden: true, requiresAuth: true, permission: 'quantification:plan:edit' }
    }
  ]
}
```

**Step 2: 运行开发服务器验证**

```bash
cd frontend && npm run dev
```
Expected: 无错误启动

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): align Inspection menu with DDD domain"
```

---

### Task 4: 创建权限管理(Access)路由模块

**Files:**
- Modify: `frontend/src/router/index.ts:790-892` (系统管理)

**Step 1: 从系统管理中提取Access领域路由**

```typescript
// 权限管理模块 - Access领域
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
}
```

**Step 2: 运行类型检查**

```bash
cd frontend && npm run type-check
```
Expected: 无类型错误

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): create Access domain menu structure"
```

---

### Task 5: 重构系统设置(Settings)路由

**Files:**
- Modify: `frontend/src/router/index.ts` (系统管理剩余部分)
- Modify: `frontend/src/router/index.ts:731-767` (系统配置)

**Step 1: 创建系统设置路由**

```typescript
// 系统设置模块
{
  path: '/settings',
  name: 'Settings',
  redirect: '/settings/config',
  meta: {
    title: '系统设置',
    icon: 'Setting',
    requiresAuth: true,
    order: 7,
    group: 'settings'
  },
  children: [
    {
      path: '/settings/config',
      name: 'SettingsConfig',
      component: () => import('@/views/system/SystemConfigsView.vue'),
      meta: {
        title: '系统配置',
        requiresAuth: true,
        permission: 'system:config:view',
        order: 1
      }
    },
    {
      path: '/settings/weight',
      name: 'SettingsWeight',
      component: () => import('@/views/quantification/WeightConfigManagement.vue'),
      meta: {
        title: '加权配置',
        requiresAuth: true,
        permission: 'quantification:weight:config',
        order: 2
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
    {
      path: '/settings/buildings',
      name: 'SettingsBuildings',
      component: () => import('@/views/system/BuildingsView.vue'),
      meta: {
        title: '楼宇管理',
        requiresAuth: true,
        permission: 'system:building:view',
        order: 6
      }
    }
  ]
}
```

**Step 2: 运行类型检查**

```bash
cd frontend && npm run type-check
```
Expected: 无类型错误

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): create Settings menu structure"
```

---

### Task 6: 调整综合测评和任务管理顺序

**Files:**
- Modify: `frontend/src/router/index.ts:569-693` (综合测评)
- Modify: `frontend/src/router/index.ts:122-191` (任务管理)

**Step 1: 更新order值**

```typescript
// 综合测评模块 - 调整order为4
{
  path: '/evaluation',
  name: 'Evaluation',
  redirect: '/evaluation/periods',
  meta: {
    title: '综合测评',
    icon: 'DataAnalysis',
    requiresAuth: true,
    order: 4,  // 从7改为4
    group: 'evaluation'
  },
  // ... children保持不变
}

// 任务管理模块 - 调整order为5
{
  path: '/task',
  name: 'TaskManagement',
  redirect: '/task/list',
  meta: {
    title: '任务管理',
    icon: 'Tickets',
    requiresAuth: true,
    order: 5,  // 从3改为5
    group: 'task'
  },
  // ... children保持不变
}
```

**Step 2: 运行开发服务器验证菜单顺序**

```bash
cd frontend && npm run dev
```
Expected: 菜单按新顺序显示

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): adjust menu order for DDD alignment"
```

---

### Task 7: 添加向后兼容重定向路由

**Files:**
- Modify: `frontend/src/router/index.ts` (重定向路由区域)

**Step 1: 添加旧路由到新路由的重定向**

```typescript
// ========== 向后兼容重定向 ==========

// 学生事务 -> 组织管理
{ path: '/student-affairs', redirect: '/organization', meta: { hidden: true } },
{ path: '/student-affairs/students', redirect: '/organization/students', meta: { hidden: true } },
{ path: '/student-affairs/classes', redirect: '/organization/classes', meta: { hidden: true } },
{ path: '/student-affairs/departments', redirect: '/organization/units', meta: { hidden: true } },
{ path: '/students', redirect: '/organization/students', meta: { hidden: true } },
{ path: '/classes', redirect: '/organization/classes', meta: { hidden: true } },

// 宿舍管理 -> 组织管理/宿舍
{ path: '/dormitory', redirect: '/organization/dormitory', meta: { hidden: true } },
{ path: '/dormitory/buildings', redirect: '/organization/dormitory/buildings', meta: { hidden: true } },
{ path: '/dormitory/rooms', redirect: '/organization/dormitory/rooms', meta: { hidden: true } },
{ path: '/dormitory/overview', redirect: '/organization/dormitory/overview', meta: { hidden: true } },

// 教学管理 -> 组织管理/年级专业
{ path: '/academic', redirect: '/organization/academic', meta: { hidden: true } },
{ path: '/academic/grades', redirect: '/organization/academic/grades', meta: { hidden: true } },
{ path: '/academic/majors', redirect: '/organization/academic/majors', meta: { hidden: true } },
{ path: '/grades', redirect: '/organization/academic/grades', meta: { hidden: true } },
{ path: '/majors', redirect: '/organization/academic/majors', meta: { hidden: true } },

// 教学设施 -> 组织管理/教学设施
{ path: '/teaching', redirect: '/organization/teaching', meta: { hidden: true } },
{ path: '/teaching/buildings', redirect: '/organization/teaching/buildings', meta: { hidden: true } },
{ path: '/teaching/classrooms', redirect: '/organization/teaching/classrooms', meta: { hidden: true } },

// 量化检查 -> Inspection
{ path: '/quantification', redirect: '/inspection', meta: { hidden: true } },
{ path: '/quantification/config', redirect: '/inspection/config', meta: { hidden: true } },
{ path: '/quantification/check-plan', redirect: '/inspection/plans', meta: { hidden: true } },
{ path: '/quantification/appeals-v3', redirect: '/inspection/appeals', meta: { hidden: true } },
{ path: '/quantification/my-tasks', redirect: '/inspection/my-tasks', meta: { hidden: true } },
{ path: '/quantification/rating-statistics', redirect: '/inspection/rating-statistics', meta: { hidden: true } },
{ path: '/quantification/badge-management', redirect: '/inspection/badges', meta: { hidden: true } },
{ path: '/quantification/class-honor/:classId?', redirect: to => `/inspection/class-honor/${to.params.classId || ''}`, meta: { hidden: true } },
{ path: '/quantification/check-plan/create', redirect: '/inspection/plans/create', meta: { hidden: true } },
{ path: '/quantification/check-plan/:id', redirect: to => `/inspection/plans/${to.params.id}`, meta: { hidden: true } },
{ path: '/quantification/check-record-v3/:id', redirect: to => `/inspection/records/${to.params.id}`, meta: { hidden: true } },
{ path: '/quantification/check-record/:id', redirect: to => `/inspection/records/${to.params.id}`, meta: { hidden: true } },

// 系统管理 -> Access + Settings
{ path: '/system', redirect: '/access', meta: { hidden: true } },
{ path: '/system/users', redirect: '/access/users', meta: { hidden: true } },
{ path: '/system/roles', redirect: '/access/roles', meta: { hidden: true } },
{ path: '/system/permissions', redirect: '/access/permissions', meta: { hidden: true } },
{ path: '/system/configs', redirect: '/settings/config', meta: { hidden: true } },
{ path: '/system/operation-logs', redirect: '/settings/operation-logs', meta: { hidden: true } },
{ path: '/system/announcements', redirect: '/settings/announcements', meta: { hidden: true } },
{ path: '/system/semesters', redirect: '/settings/semesters', meta: { hidden: true } },
{ path: '/system/buildings', redirect: '/settings/buildings', meta: { hidden: true } },
{ path: '/system/departments', redirect: '/organization/units', meta: { hidden: true } },

// 系统配置 -> Settings
{ path: '/config', redirect: '/settings', meta: { hidden: true } },
{ path: '/config/weight', redirect: '/settings/weight', meta: { hidden: true } },
{ path: '/config/quantification', redirect: '/inspection/config', meta: { hidden: true } },

// V2 API别名更新
{ path: '/v2/access/roles', redirect: '/access/roles', meta: { hidden: true } },
{ path: '/v2/access/permissions', redirect: '/access/permissions', meta: { hidden: true } },
{ path: '/v2/organization/classes', redirect: '/organization/classes', meta: { hidden: true } },
{ path: '/v2/org-units', redirect: '/organization/units', meta: { hidden: true } },
{ path: '/v2/grades', redirect: '/organization/academic/grades', meta: { hidden: true } },
{ path: '/v2/inspection-templates', redirect: '/inspection/config', meta: { hidden: true } },
{ path: '/v2/inspection-records', redirect: '/inspection/records', meta: { hidden: true } },
{ path: '/v2/appeals', redirect: '/inspection/appeals', meta: { hidden: true } },
```

**Step 2: 运行类型检查**

```bash
cd frontend && npm run type-check
```
Expected: 无类型错误

**Step 3: 手动测试旧路由重定向**

启动开发服务器后访问：
- `/student-affairs` → 应重定向到 `/organization`
- `/quantification/config` → 应重定向到 `/inspection/config`
- `/system/users` → 应重定向到 `/access/users`

**Step 4: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): add backward-compatible redirects for old routes"
```

---

### Task 8: 更新MainLayout图标映射

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue:293-325` (图标映射)

**Step 1: 添加新图标组件**

```typescript
// 在 iconComponents 中添加新图标
import {
  House,
  User as UserFilled,
  School,
  OfficeBuilding,
  HomeFilled,
  DataAnalysis,
  Setting,
  Reading,
  Management,
  Tickets,
  DocumentChecked,
  Notebook,
  ChatDotRound,
  TrendCharts,
  Medal,
  Trophy,
  Tools,
  Lock  // 新增: 权限管理图标
} from '@element-plus/icons-vue'

const iconComponents: Record<string, any> = {
  House,
  UserFilled,
  School,
  OfficeBuilding,
  HomeFilled,
  DataAnalysis,
  Setting,
  Reading,
  Management,
  Tickets,
  DocumentChecked,
  Notebook,
  ChatDotRound,
  TrendCharts,
  Medal,
  Trophy,
  Tools,
  Lock  // 新增
}
```

**Step 2: 验证图标显示**

```bash
cd frontend && npm run dev
```
Expected: 所有菜单图标正常显示

**Step 3: Commit**

```bash
git add frontend/src/layouts/MainLayout.vue
git commit -m "feat(layout): add Lock icon for Access menu"
```

---

### Task 9: 删除废弃的旧路由定义

**Files:**
- Modify: `frontend/src/router/index.ts`

**Step 1: 删除被合并的旧路由块**

删除以下路由定义（已被Task 2-5的新路由替代）：
- 旧的 `StudentAffairs` 路由块
- 旧的 `Dormitory` 路由块
- 旧的 `Academic` 路由块
- 旧的 `Teaching` 路由块
- 旧的 `Config` 路由块
- 旧的 `System` 路由块
- 旧的 `Quantification` 路由块

**Step 2: 运行完整构建测试**

```bash
cd frontend && npm run build
```
Expected: 构建成功，无错误

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor(router): remove deprecated route definitions"
```

---

### Task 10: 更新权限编码（可选优化）

**Files:**
- Modify: `frontend/src/router/index.ts` (权限编码)
- Create: `frontend/src/constants/permissions.ts`

**Step 1: 创建权限常量文件**

```typescript
// frontend/src/constants/permissions.ts

/**
 * 权限编码常量
 * 与后端DDD领域对齐
 */
export const PERMISSIONS = {
  // Organization 领域
  ORGANIZATION: {
    UNIT_VIEW: 'organization:unit:view',
    UNIT_EDIT: 'organization:unit:edit',
    CLASS_VIEW: 'organization:class:view',
    CLASS_EDIT: 'organization:class:edit',
    STUDENT_VIEW: 'organization:student:view',
    STUDENT_EDIT: 'organization:student:edit',
    GRADE_VIEW: 'organization:grade:view',
    MAJOR_VIEW: 'organization:major:view',
    DORMITORY_VIEW: 'organization:dormitory:view',
    DORMITORY_EDIT: 'organization:dormitory:edit',
    TEACHING_BUILDING_VIEW: 'organization:teaching:building:view',
    CLASSROOM_VIEW: 'organization:teaching:classroom:view',
  },

  // Inspection 领域
  INSPECTION: {
    CONFIG_VIEW: 'inspection:config:view',
    CONFIG_EDIT: 'inspection:config:edit',
    PLAN_VIEW: 'inspection:plan:view',
    PLAN_ADD: 'inspection:plan:add',
    PLAN_EDIT: 'inspection:plan:edit',
    RECORD_VIEW: 'inspection:record:view',
    RECORD_DETAIL: 'inspection:record:detail',
    APPEAL_VIEW: 'inspection:appeal:view',
    APPEAL_HANDLE: 'inspection:appeal:handle',
    RATING_VIEW: 'inspection:rating:view',
    RATING_APPROVE: 'inspection:rating:approve',
    BADGE_MANAGE: 'inspection:badge:manage',
    SCORE: 'inspection:score',
  },

  // Access 领域
  ACCESS: {
    USER_VIEW: 'access:user:view',
    USER_EDIT: 'access:user:edit',
    ROLE_VIEW: 'access:role:view',
    ROLE_EDIT: 'access:role:edit',
    PERMISSION_VIEW: 'access:permission:view',
    PERMISSION_EDIT: 'access:permission:edit',
  },

  // Settings
  SETTINGS: {
    CONFIG_VIEW: 'settings:config:view',
    CONFIG_EDIT: 'settings:config:edit',
    LOG_VIEW: 'settings:log:view',
    ANNOUNCEMENT_VIEW: 'settings:announcement:view',
    ANNOUNCEMENT_EDIT: 'settings:announcement:edit',
    SEMESTER_VIEW: 'settings:semester:view',
    BUILDING_VIEW: 'settings:building:view',
    WEIGHT_CONFIG: 'settings:weight:config',
  },

  // Evaluation 领域
  EVALUATION: {
    PERIOD_VIEW: 'evaluation:period:view',
    HONOR_VIEW: 'evaluation:honor:view',
    RESULT_VIEW: 'evaluation:result:view',
    RESULT_MY: 'evaluation:result:my',
    COURSE_VIEW: 'evaluation:course:view',
    SCORE_VIEW: 'evaluation:score:view',
    CONFIG_VIEW: 'evaluation:config:view',
  },

  // Task 领域
  TASK: {
    LIST: 'task:list',
    MY: 'task:my',
    APPROVE: 'task:approve',
    WORKFLOW_MANAGE: 'task:workflow:manage',
  },
} as const

// 向后兼容映射（旧权限码 -> 新权限码）
export const PERMISSION_LEGACY_MAP: Record<string, string> = {
  'system:department:view': PERMISSIONS.ORGANIZATION.UNIT_VIEW,
  'student:class:view': PERMISSIONS.ORGANIZATION.CLASS_VIEW,
  'student:info:view': PERMISSIONS.ORGANIZATION.STUDENT_VIEW,
  'quantification:grade:view': PERMISSIONS.ORGANIZATION.GRADE_VIEW,
  'major:list': PERMISSIONS.ORGANIZATION.MAJOR_VIEW,
  'student:dormitory:view': PERMISSIONS.ORGANIZATION.DORMITORY_VIEW,
  'teaching:building:list': PERMISSIONS.ORGANIZATION.TEACHING_BUILDING_VIEW,
  'teaching:classroom:list': PERMISSIONS.ORGANIZATION.CLASSROOM_VIEW,

  'quantification:config:view': PERMISSIONS.INSPECTION.CONFIG_VIEW,
  'quantification:plan:view': PERMISSIONS.INSPECTION.PLAN_VIEW,
  'quantification:plan:add': PERMISSIONS.INSPECTION.PLAN_ADD,
  'quantification:record:view': PERMISSIONS.INSPECTION.RECORD_VIEW,
  'quantification:record:v3:detail': PERMISSIONS.INSPECTION.RECORD_DETAIL,
  'quantification:appeal:v3:view': PERMISSIONS.INSPECTION.APPEAL_VIEW,
  'quantification:rating:view': PERMISSIONS.INSPECTION.RATING_VIEW,
  'quantification:rating:approve': PERMISSIONS.INSPECTION.RATING_APPROVE,
  'quantification:badge:manage': PERMISSIONS.INSPECTION.BADGE_MANAGE,
  'quantification:check:score': PERMISSIONS.INSPECTION.SCORE,

  'system:user:view': PERMISSIONS.ACCESS.USER_VIEW,
  'system:role:view': PERMISSIONS.ACCESS.ROLE_VIEW,
  'system:permission:view': PERMISSIONS.ACCESS.PERMISSION_VIEW,

  'system:config:view': PERMISSIONS.SETTINGS.CONFIG_VIEW,
  'system:operlog:view': PERMISSIONS.SETTINGS.LOG_VIEW,
  'system:announcement:view': PERMISSIONS.SETTINGS.ANNOUNCEMENT_VIEW,
  'system:semester:view': PERMISSIONS.SETTINGS.SEMESTER_VIEW,
  'system:building:view': PERMISSIONS.SETTINGS.BUILDING_VIEW,
  'quantification:weight:config': PERMISSIONS.SETTINGS.WEIGHT_CONFIG,
}
```

**Step 2: 运行类型检查**

```bash
cd frontend && npm run type-check
```
Expected: 无类型错误

**Step 3: Commit**

```bash
git add frontend/src/constants/permissions.ts
git commit -m "feat(constants): add DDD-aligned permission constants"
```

---

### Task 11: 端到端测试验证

**Files:**
- Test: Manual testing in browser

**Step 1: 启动开发服务器**

```bash
cd frontend && npm run dev
```

**Step 2: 验证清单**

| 测试项 | 预期结果 | 实际结果 |
|-------|---------|---------|
| 菜单顺序 | 首页→组织管理→量化检查→综合测评→任务管理→权限管理→系统设置 | ☐ |
| 组织管理子菜单 | 组织架构、班级、学生、年级专业、宿舍管理、教学设施 | ☐ |
| 量化检查子菜单 | 量化配置、检查计划、申诉管理、我的检查任务、评级统计、荣誉徽章 | ☐ |
| 权限管理子菜单 | 用户管理、角色管理、权限管理 | ☐ |
| 旧路由重定向 | /student-affairs → /organization | ☐ |
| 图标显示正常 | 所有菜单有正确图标 | ☐ |
| 权限过滤正常 | 无权限的菜单被隐藏 | ☐ |

**Step 3: 提交测试报告**

确认所有测试通过后提交。

**Step 4: Commit**

```bash
git add -A
git commit -m "test: verify DDD navigation alignment complete"
```

---

### Task 12: 最终整理与文档更新

**Files:**
- Modify: `CLAUDE.md` (更新导航说明)
- Delete: `frontend/src/router/index.backup.ts` (删除备份)

**Step 1: 更新CLAUDE.md中的路由说明**

在Frontend Architecture部分更新：

```markdown
**DDD对齐导航结构:**
```
/ (MainLayout)
├── /dashboard (首页, order: 1)
├── /organization (组织管理, order: 2, group: organization)
│   ├── /organization/units - 组织架构
│   ├── /organization/classes - 班级管理
│   ├── /organization/students - 学生管理
│   ├── /organization/academic - 年级专业
│   ├── /organization/dormitory - 宿舍管理
│   └── /organization/teaching - 教学设施
├── /inspection (量化检查, order: 3, group: inspection)
├── /evaluation (综合测评, order: 4, group: evaluation)
├── /task (任务管理, order: 5, group: task)
├── /access (权限管理, order: 6, group: access)
└── /settings (系统设置, order: 7, group: settings)
```
```

**Step 2: 删除备份文件**

```bash
rm frontend/src/router/index.backup.ts
```

**Step 3: Final commit**

```bash
git add -A
git commit -m "docs: update navigation structure documentation

- Remove router backup file
- Update CLAUDE.md with DDD-aligned navigation structure"
```

---

## 三、风险与回滚

### 3.1 回滚方案

如果重构出现问题，可以通过git回滚：

```bash
git revert HEAD~12..HEAD  # 回滚最近12个commit
# 或者
git checkout HEAD~12 -- frontend/src/router/index.ts
```

### 3.2 已知风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 书签/收藏失效 | 用户旧书签无法访问 | 重定向路由保证兼容 |
| 权限配置不同步 | 菜单显示异常 | 保留旧权限码向后兼容 |
| 第三方集成断链 | 外部系统链接失效 | 重定向路由保证兼容 |

---

## 四、验收标准

1. ✅ 菜单结构与后端DDD领域对齐（Organization/Inspection/Access）
2. ✅ 所有旧路由正确重定向到新路由
3. ✅ 菜单权限过滤正常工作
4. ✅ 菜单图标正确显示
5. ✅ 无TypeScript类型错误
6. ✅ 生产构建成功
7. ✅ CLAUDE.md文档更新
