# 领域架构重构方案（完整版）

> 将系统从"组织中心"模式重构为"四域分治"模式
> 开发阶段，不考虑任何向后兼容

---

## 一、四域定义

| 域 | 英文 | 职责 | 核心问题 |
|----|------|------|---------|
| **organization** | 组织架构 | 系部、部门、教研室 | "学校怎么组织的" |
| **academic** | 学术目录 | 专业、课程、培养方案 | "教什么" |
| **student** | 学生管理 | 学生、年级、班级、考勤、预警 | "谁在学" |
| **teaching** | 教学执行 | 校历、开课、排课、考试、成绩 | "怎么教" |

---

## 二、实体迁移总表

| 实体 | 来源域 | 目标域 | 迁移类型 |
|------|--------|--------|---------|
| Major | organization | **academic** | 搬运(改包名) |
| MajorDirection | organization | **academic** | 搬运 |
| Course | teaching(JdbcTemplate) | **academic** | **新建DDD** |
| CurriculumPlan | teaching(JdbcTemplate) | **academic** | **新建DDD** |
| Grade | organization | **student** | 搬运 |
| GradeStatus | organization | **student** | 搬运 |
| GradeOpenedDirection | organization | **student** | 搬运 |
| SchoolClass | organization | **student** | 搬运 |
| ClassStatus | organization | **student** | 搬运 |
| GradeCreatedEvent | organization | **student** | 搬运 |
| GradeStatusChangedEvent | organization | **student** | 搬运 |
| ClassCreatedEvent | organization | **student** | 搬运 |
| ClassStatusChangedEvent | organization | **student** | 搬运 |
| AcademicYear/Semester | teaching | teaching | **不动**（保留在teaching） |

---

## 三、后端迁移——逐文件清单

### Phase A：创建 academic 域

#### A1. 搬运 Major（28文件）

**Domain层：**
```
organization/model/Major.java              → academic/model/Major.java
organization/model/MajorDirection.java     → academic/model/MajorDirection.java
organization/repository/MajorRepository.java → academic/repository/MajorRepository.java
```

**Application层：**
```
organization/MajorApplicationService.java  → academic/MajorApplicationService.java
organization/GradeMajorDirectionApplicationService.java → academic/GradeMajorDirectionApplicationService.java
organization/command/CreateMajorCommand.java → academic/command/CreateMajorCommand.java
organization/command/UpdateMajorCommand.java → academic/command/UpdateMajorCommand.java
organization/command/CreateMajorDirectionCommand.java → academic/command/CreateMajorDirectionCommand.java
organization/command/UpdateMajorDirectionCommand.java → academic/command/UpdateMajorDirectionCommand.java
organization/query/MajorDTO.java           → academic/query/MajorDTO.java
organization/query/MajorDirectionDTO.java  → academic/query/MajorDirectionDTO.java
organization/query/GradeMajorDirectionDTO.java → academic/query/GradeMajorDirectionDTO.java
```

**Infrastructure层：**
```
persistence/organization/MajorPO.java      → persistence/academic/MajorPO.java
persistence/organization/MajorPersistenceMapper.java → persistence/academic/MajorPersistenceMapper.java
persistence/organization/MajorRepositoryImpl.java → persistence/academic/MajorRepositoryImpl.java
persistence/organization/MajorDirectionPO.java → persistence/academic/MajorDirectionPO.java
persistence/organization/MajorDirectionPersistenceMapper.java → persistence/academic/MajorDirectionPersistenceMapper.java
persistence/organization/GradeMajorDirectionPO.java → persistence/academic/GradeMajorDirectionPO.java
persistence/organization/GradeMajorDirectionMapper.java → persistence/academic/GradeMajorDirectionMapper.java
```

**Interfaces层：**
```
rest/organization/MajorController.java     → rest/academic/MajorController.java  (路径改 /academic/majors)
rest/organization/CreateMajorRequest.java  → rest/academic/CreateMajorRequest.java
rest/organization/UpdateMajorRequest.java  → rest/academic/UpdateMajorRequest.java
rest/organization/MajorDirectionController.java → rest/academic/MajorDirectionController.java  (路径改 /academic/major-directions)
rest/organization/CreateMajorDirectionRequest.java → rest/academic/CreateMajorDirectionRequest.java
rest/organization/UpdateMajorDirectionRequest.java → rest/academic/UpdateMajorDirectionRequest.java
rest/organization/GradeMajorDirectionController.java → rest/academic/GradeMajorDirectionController.java
rest/organization/CreateGradeMajorDirectionRequest.java → rest/academic/CreateGradeMajorDirectionRequest.java
rest/organization/UpdateGradeMajorDirectionRequest.java → rest/academic/UpdateGradeMajorDirectionRequest.java
```

#### A2. 新建 Course DDD 模型

**当前状态：** `TeachingCourseController.java` 用 JdbcTemplate 直写 `courses` 表，无领域模型。

**新建文件：**
```
domain/academic/model/Course.java                    (聚合根)
domain/academic/repository/CourseRepository.java     (接口)
infrastructure/persistence/academic/CoursePO.java    (PO)
infrastructure/persistence/academic/CourseMapper.java (Mapper)
infrastructure/persistence/academic/CourseRepositoryImpl.java (实现)
application/academic/CourseApplicationService.java   (应用服务)
interfaces/rest/academic/CourseController.java       (控制器, 路径 /academic/courses)
```

**Course 聚合根设计：**
```java
@Getter
public class Course extends AggregateRoot<Long> {
    private String courseCode;
    private String courseName;
    private String courseNameEn;
    private Integer courseCategory;  // 1公共基础 2专业基础 3专业核心 4专业选修 5通识选修 6实践
    private Integer courseType;      // 1必修 2限选 3任选
    private Integer courseNature;    // 1理论 2实验 3理论+实验 4实践
    private BigDecimal credits;
    private Integer totalHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer weeklyHours;
    private Integer examType;        // 1考试 2考查
    private Long orgUnitId;          // 开课部门(引用organization域)
    private String description;
    private Integer status;          // 1启用 0停用
}
```

**删除：** `interfaces/rest/teaching/TeachingCourseController.java`

#### A3. 新建 CurriculumPlan DDD 模型

**当前状态：** `TeachingCurriculumController.java` 用 JdbcTemplate。

**新建文件：**
```
domain/academic/model/CurriculumPlan.java            (聚合根)
domain/academic/model/PlanCourse.java                (实体 - 方案内的课程)
domain/academic/repository/CurriculumPlanRepository.java
infrastructure/persistence/academic/CurriculumPlanPO.java
infrastructure/persistence/academic/CurriculumPlanMapper.java
infrastructure/persistence/academic/CurriculumPlanRepositoryImpl.java
infrastructure/persistence/academic/PlanCoursePO.java
infrastructure/persistence/academic/PlanCourseMapper.java
application/academic/CurriculumPlanApplicationService.java
interfaces/rest/academic/CurriculumPlanController.java  (路径 /academic/curriculum-plans)
```

**CurriculumPlan 聚合根设计：**
```java
@Getter
public class CurriculumPlan extends AggregateRoot<Long> {
    private String planCode;
    private String planName;
    private Long majorId;              // 引用academic.Major
    private Long majorDirectionId;     // 可选，引用academic.MajorDirection
    private Integer gradeYear;
    private BigDecimal totalCredits;
    private BigDecimal requiredCredits;
    private BigDecimal electiveCredits;
    private Integer version;
    private Integer status;            // 0草稿 1已发布 2已归档
    private List<PlanCourse> courses;  // 方案课程列表
}
```

**删除：** `interfaces/rest/teaching/TeachingCurriculumController.java`

---

### Phase B：扩充 student 域

#### B1. 搬运 Grade（20文件）

**Domain层：**
```
organization/model/Grade.java              → student/model/Grade.java
organization/model/GradeStatus.java        → student/model/GradeStatus.java
organization/model/GradeOpenedDirection.java → student/model/GradeOpenedDirection.java
organization/event/GradeCreatedEvent.java  → student/event/GradeCreatedEvent.java
organization/event/GradeStatusChangedEvent.java → student/event/GradeStatusChangedEvent.java
organization/repository/GradeRepository.java → student/repository/GradeRepository.java
organization/repository/GradeOpenedDirectionRepository.java → student/repository/GradeOpenedDirectionRepository.java
```

**Application层：**
```
organization/GradeApplicationService.java  → student/GradeApplicationService.java
organization/command/CreateGradeCommand.java → student/command/CreateGradeCommand.java
organization/command/UpdateGradeCommand.java → student/command/UpdateGradeCommand.java
organization/command/AssignGradeLeaderCommand.java → student/command/AssignGradeLeaderCommand.java
organization/query/GradeDTO.java           → student/query/GradeDTO.java
organization/query/GradeStatisticsDTO.java → student/query/GradeStatisticsDTO.java
```

**Infrastructure层：**
```
persistence/organization/GradePO.java      → persistence/student/GradePO.java
persistence/organization/GradePersistenceMapper.java → persistence/student/GradePersistenceMapper.java
persistence/organization/GradeRepositoryImpl.java → persistence/student/GradeRepositoryImpl.java
persistence/organization/GradeOpenedDirectionRepositoryImpl.java → persistence/student/GradeOpenedDirectionRepositoryImpl.java
```

**Interfaces层：**
```
rest/organization/GradeController.java     → rest/student/GradeController.java  (路径改 /students/grades)
rest/organization/CreateGradeRequest.java  → rest/student/CreateGradeRequest.java
rest/organization/UpdateGradeRequest.java  → rest/student/UpdateGradeRequest.java
rest/organization/AssignGradeLeaderRequest.java → rest/student/AssignGradeLeaderRequest.java
```

#### B2. 搬运 SchoolClass（12文件）

**Domain层：**
```
organization/model/SchoolClass.java        → student/model/SchoolClass.java
organization/model/ClassStatus.java        → student/model/ClassStatus.java
organization/event/ClassCreatedEvent.java  → student/event/ClassCreatedEvent.java
organization/event/ClassStatusChangedEvent.java → student/event/ClassStatusChangedEvent.java
organization/repository/SchoolClassRepository.java → student/repository/SchoolClassRepository.java
```

**Infrastructure层：**
```
persistence/organization/SchoolClassPO.java → persistence/student/SchoolClassPO.java
persistence/organization/SchoolClassMapper.java → persistence/student/SchoolClassMapper.java
persistence/organization/SchoolClassRepositoryImpl.java → persistence/student/SchoolClassRepositoryImpl.java
```

**Interfaces层：**
```
rest/organization/SchoolClassController.java → rest/student/SchoolClassController.java  (路径改 /students/classes)
rest/organization/CreateClassRequest.java  → rest/student/CreateClassRequest.java
rest/organization/UpdateClassRequest.java  → rest/student/UpdateClassRequest.java
rest/organization/SchoolClassResponse.java → rest/student/SchoolClassResponse.java
```

---

### Phase C：修复跨域引用（31个文件）

每个文件需要更新 import 路径：

#### C1. 引用 SchoolClass 的外部文件
```
application/myclass/MyClassApplicationService.java
  import domain.organization.model.SchoolClass → domain.student.model.SchoolClass
  import domain.organization.repository.SchoolClassRepository → domain.student.repository.SchoolClassRepository

application/organization/GradeApplicationService.java
  import SchoolClassRepository → domain.student.repository.SchoolClassRepository

application/organization/OrgUnitApplicationService.java
  import SchoolClassRepository → domain.student.repository.SchoolClassRepository

application/teaching/ScheduleExportService.java
  SQL引用 school_classes → 不需改（表名不变）

application/teaching/AutoSchedulingService.java
  SQL引用 courses, school_classes → 不需改（表名不变）

interfaces/rest/dashboard/DashboardController.java
  SQL引用 majors, school_classes, courses → 不需改（表名不变）
```

#### C2. 引用 Grade 事件的文件
```
application/events/OrganizationEventHandler.java
  需要拆分：
  - Grade/Class相关事件处理 → 新建 application/events/StudentEventHandler.java（或合并到已有的）
  - 保留 OrgUnit 事件处理在 OrganizationEventHandler

  具体拆分：
  - handle(GradeCreatedEvent) → 移到 StudentEventHandler
  - handle(GradeStatusChangedEvent) → 移到 StudentEventHandler
  - handle(ClassCreatedEvent) → 移到 StudentEventHandler
  - handle(ClassStatusChangedEvent) → 移到 StudentEventHandler
  - handleClassGraduation() → 移到 StudentEventHandler
  - handleClassActivation() → 移到 StudentEventHandler
```

#### C3. 引用 Major 的外部文件
```
components/class/ClassForm.vue
  import getDirectionsByYear → 从新的 api/academic.ts 导入

components/student/StudentForm.vue
  import getDirectionsByYear → 从新的 api/academic.ts 导入

views/organization/grades/GradeManagementV2.vue
  import getAllEnabledMajors, getDirectionsByMajor → 从 api/academic.ts

views/teaching/CurriculumPlanView.vue
  import Major, MajorDirection → 从 api/academic.ts
```

---

### Phase D：权限系统迁移

#### D1. 权限常量更新

文件：`common/constants/PermissionConstants.java`

```java
// Before
MAJOR_ADD = "major:add"
MAJOR_DELETE = "major:delete"
// After
MAJOR_ADD = "academic:major:add"
MAJOR_DELETE = "academic:major:delete"

// Course permissions
// Before: teaching:course:view / teaching:course:edit
// After: academic:course:view / academic:course:edit

// Curriculum permissions
// Before: teaching:curriculum:view / teaching:curriculum:edit
// After: academic:curriculum:view / academic:curriculum:edit

// Class permissions stay as: student:class:view / student:class:edit (already correct)
// Grade permissions: add academic:grade:view / academic:grade:edit?
// → No, Grade is student domain: student:grade:view / student:grade:edit
```

#### D2. 控制器注解更新

```
MajorController:
  @CasbinAccess(resource = "major", action = "view")
  → @CasbinAccess(resource = "academic:major", action = "view")

CourseController (new):
  @CasbinAccess(resource = "academic:course", action = "view")

CurriculumPlanController (new):
  @CasbinAccess(resource = "academic:curriculum", action = "view")

GradeController:
  保持 student:grade 或改为更一致的命名
```

#### D3. 数据库权限种子数据

文件：创建 `database/schema/V83.0.0__restructure_permissions.sql`

```sql
-- 添加 academic 域权限
INSERT IGNORE INTO permissions (permission_code, permission_name, parent_id, type, sort_order)
VALUES
('academic', '学术管理', NULL, 1, 3),
('academic:major', '专业管理', (SELECT id FROM permissions WHERE permission_code='academic' LIMIT 1), 1, 1),
('academic:major:view', '查看专业', (SELECT id FROM permissions WHERE permission_code='academic:major' LIMIT 1), 2, 1),
('academic:major:edit', '编辑专业', (SELECT id FROM permissions WHERE permission_code='academic:major' LIMIT 1), 2, 2),
('academic:course', '课程管理', (SELECT id FROM permissions WHERE permission_code='academic' LIMIT 1), 1, 2),
('academic:course:view', '查看课程', (SELECT id FROM permissions WHERE permission_code='academic:course' LIMIT 1), 2, 1),
('academic:course:edit', '编辑课程', (SELECT id FROM permissions WHERE permission_code='academic:course' LIMIT 1), 2, 2),
('academic:curriculum', '培养方案', (SELECT id FROM permissions WHERE permission_code='academic' LIMIT 1), 1, 3),
('academic:curriculum:view', '查看方案', (SELECT id FROM permissions WHERE permission_code='academic:curriculum' LIMIT 1), 2, 1),
('academic:curriculum:edit', '编辑方案', (SELECT id FROM permissions WHERE permission_code='academic:curriculum' LIMIT 1), 2, 2);

-- 废弃旧权限（不删除，标记）
UPDATE permissions SET permission_name = CONCAT('[已废弃]', permission_name)
WHERE permission_code IN ('teaching:course', 'teaching:course:view', 'teaching:course:edit',
  'teaching:curriculum', 'teaching:curriculum:view', 'teaching:curriculum:edit');

-- 给管理员角色赋予新权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'academic:%';
```

---

### Phase E：事件系统迁移

#### E1. 拆分 OrganizationEventHandler

**当前文件：** `application/events/OrganizationEventHandler.java`

**操作：**
1. 从中移除 Grade/Class 事件处理方法
2. 检查已有的 `application/events/StudentEventHandler.java`，将 Grade/Class 事件处理合并进去

**OrganizationEventHandler 保留：**
- OrgUnitCreatedEvent 相关处理

**StudentEventHandler 新增：**
- handle(GradeCreatedEvent)
- handle(GradeStatusChangedEvent)
- handle(ClassCreatedEvent)
- handle(ClassStatusChangedEvent)
- handleClassGraduation()
- handleClassActivation()

#### E2. 事件类包名更新

```
domain/organization/event/GradeCreatedEvent.java       → domain/student/event/
domain/organization/event/GradeStatusChangedEvent.java  → domain/student/event/
domain/organization/event/ClassCreatedEvent.java        → domain/student/event/
domain/organization/event/ClassStatusChangedEvent.java  → domain/student/event/
```

SchoolClass.java 和 Grade.java 内部 registerEvent() 调用会自动跟着类移动。

---

### Phase F：缓存配置更新

文件：`config/RedisConfig.java`

```java
// Line 65: cacheConfigurations.put("school_classes", ...) → 保留（缓存key不必跟域走）
// Line 67: cacheConfigurations.put("majors", ...) → 保留
```

缓存key是数据层概念，不需要跟领域包名对齐。**不改。**

---

### Phase G：删除旧JdbcTemplate控制器

```
删除: interfaces/rest/teaching/TeachingCourseController.java
  （被 interfaces/rest/academic/CourseController.java 替代）

删除: interfaces/rest/teaching/TeachingCurriculumController.java
  （被 interfaces/rest/academic/CurriculumPlanController.java 替代）
```

---

## 四、前端迁移——逐文件清单

### 前端 Phase 1：API 层重组

#### 新建 `api/academic.ts`
```typescript
// 合并自: api/major.ts + api/majorDirection.ts + 部分 api/teaching.ts
export const majorApi = { ... }          // 从 major.ts 搬入
export const majorDirectionApi = { ... } // 从 majorDirection.ts 搬入
export const courseApi = { ... }         // 从 teaching.ts 搬入，路径改 /academic/courses
export const curriculumPlanApi = { ... } // 从 teaching.ts 搬入，路径改 /academic/curriculum-plans
```

#### 修改 `api/teaching.ts`
```
移除: courseApi（移到 academic.ts）
移除: curriculumPlanApi（移到 academic.ts）
保留: semesterApi, scheduleApi, teachingTaskApi, adjustmentApi, examApi, gradeApi,
      offeringApi, classAssignmentApi, teachingClassApi, constraintApi, conflictApi
```

#### 删除旧文件
```
删除: api/major.ts
删除: api/majorDirection.ts
删除: api/gradeMajorDirection.ts
```

### 前端 Phase 2：Types 层重组

#### 新建 `types/academic.ts`
```typescript
// 合并自: types/major.ts + 部分 types/teaching.ts
export interface Major { ... }
export interface MajorDirection { ... }
export interface Course { ... }
export interface CurriculumPlan { ... }
export interface PlanCourse { ... }
```

#### 修改 `types/teaching.ts`
```
移除: Course, CourseQueryParams（移到 academic.ts）
移除: CurriculumPlan, CurriculumPlanQueryParams, PlanCourse（移到 academic.ts）
保留: Semester, TeachingWeek, TeachingTask, ScheduleEntry, ExamBatch, StudentGrade 等
```

#### 删除
```
删除: types/major.ts（合并到 academic.ts）
```

### 前端 Phase 3：视图迁移

```
views/major/MajorList.vue        → views/academic/MajorListView.vue
views/teaching/CourseListView.vue → views/academic/CourseListView.vue
views/teaching/CurriculumPlanView.vue → views/academic/CurriculumPlanView.vue

views/organization/grades/GradeManagementV2.vue → views/student/GradeManagementView.vue
views/organization/grades/components/*.vue → views/student/grade-components/*.vue
views/class/ClassList.vue        → views/student/ClassListView.vue
components/class/*.vue           → components/student-class/*.vue（或保留位置改import）
components/grade/*.vue           → 保留位置改import
```

### 前端 Phase 4：路由重构

```typescript
// 重构后的路由
{
  path: '/organization',
  meta: { title: '组织管理', icon: 'Building2', order: 2 },
  children: [
    { path: 'units', component: OrgStructure, meta: { title: '组织架构' } },
  ]
},
{
  path: '/academic',
  meta: { title: '学术管理', icon: 'GraduationCap', order: 3 },
  children: [
    { path: 'majors', component: MajorListView, meta: { title: '专业管理' } },
    { path: 'courses', component: CourseListView, meta: { title: '课程管理' } },
    { path: 'curriculum-plans', component: CurriculumPlanView, meta: { title: '培养方案' } },
  ]
},
{
  path: '/student',
  meta: { title: '学生管理', icon: 'Users', order: 4 },
  children: [
    { path: 'list', component: StudentList, meta: { title: '学生列表' } },
    { path: 'grades', component: GradeManagementView, meta: { title: '年级管理' } },
    { path: 'classes', component: ClassListView, meta: { title: '班级管理' } },
    { path: 'attendance', component: AttendanceView, meta: { title: '考勤管理' } },
    { path: 'warnings', component: AcademicWarningView, meta: { title: '学业预警' } },
  ]
},
{
  path: '/teaching',
  meta: { title: '教学管理', icon: 'BookOpen', order: 5 },
  children: [
    { path: 'calendar', meta: { title: '校历管理' } },
    { path: 'offerings', meta: { title: '开课管理' } },
    { path: 'scheduling', meta: { title: '排课中心' } },
    { path: 'constraints', meta: { title: '排课约束' } },
    { path: 'tasks', meta: { title: '教学任务' } },
    { path: 'examinations', meta: { title: '考试管理' } },
    { path: 'grades', meta: { title: '成绩管理' } },
  ]
},
```

### 前端 Phase 5：跨域 import 修复

需要更新 import 的视图文件：

| 文件 | 旧import | 新import |
|------|---------|---------|
| `components/class/ClassForm.vue` | `@/api/majorDirection` | `@/api/academic` |
| `components/student/StudentForm.vue` | `@/api/majorDirection` | `@/api/academic` |
| `views/teaching/OfferingManagementView.vue` | `courseApi from @/api/teaching` | `courseApi from @/api/academic` |
| `views/teaching/ScheduleView.vue` | (如有courseApi引用) | `from @/api/academic` |
| `views/student/AttendanceView.vue` | `courseApi from @/api/teaching` | `courseApi from @/api/academic` |
| `views/teaching/GradeView.vue` | (如有courseApi引用) | `from @/api/academic` |
| `views/teaching/ExaminationView.vue` | (如有courseApi引用) | `from @/api/academic` |
| `views/myclass/MyClassDetailView.vue` | types引用 | 更新路径 |
| `views/myclass/MyClassListView.vue` | types引用 | 更新路径 |

---

## 五、MyClass 模块处理

`/my-class` 路由保持独立（教师视角看班级），但内部引用更新：

```
后端:
  MyClassApplicationService.java
    import SchoolClass → domain.student.model.SchoolClass
    import SchoolClassRepository → domain.student.repository.SchoolClassRepository
    import Grade → domain.student.model.Grade (如有引用)

前端:
  views/myclass/*.vue — import路径不变（用的是 api/myClass.ts）
  api/myClass.ts — 后端路径不变，不需改
```

---

## 六、"组织管理"菜单处理

重构后组织管理只剩一个页面（组织架构树）。处理方案：

**保留独立菜单，但增加功能丰富度：**
```
/organization
├── /organization/units        组织架构树（已有）
└── /organization/positions    岗位管理（可后续添加）
```

组织架构树本身功能丰富（增删改查节点、拖拽排序、状态管理），一个页面足以支撑一级菜单。

---

## 七、校历归属决定

**决定：校历保留在 teaching 域。**

理由：
1. 校历的核心用途是"安排教学活动"（教学周、考试周、假期）
2. 虽然其他域也引用学期，但只是通过 `semesterId` 引用，不操作校历本身
3. 搬到 academic 或 shared 域收益不大，增加复杂度
4. `semesterApi` 保留在 `api/teaching.ts`，其他域通过 ID 引用即可

---

## 八、执行顺序（8步）

```
Step 1: 创建 academic 域骨架 + 搬运 Major/MajorDirection
  → 编译验证

Step 2: 新建 Course DDD 模型到 academic 域
  → 编译验证

Step 3: 新建 CurriculumPlan DDD 模型到 academic 域
  → 编译验证 + 删除旧 JdbcTemplate 控制器

Step 4: 搬运 Grade + 事件到 student 域
  → 编译验证

Step 5: 搬运 SchoolClass + 事件到 student 域
  → 编译验证

Step 6: 修复所有跨域引用 + 拆分 EventHandler
  → 编译验证

Step 7: 权限系统更新（常量 + 注解 + 数据库种子）
  → 编译验证

Step 8: 前端全量迁移（API + Types + Views + Router + 跨域import修复）
  → 编译验证 + 全功能测试
```

---

## 九、文件统计

| 操作 | 后端 | 前端 | 合计 |
|------|------|------|------|
| 搬运（改包名） | 60 | 8 | 68 |
| 新建（DDD模型） | 16 | 2 | 18 |
| 修改（import路径） | 15 | 12 | 27 |
| 删除（旧控制器） | 2 | 3 | 5 |
| 数据库迁移 | 1 | - | 1 |
| **合计** | **94** | **25** | **119** |
