# 教务模块修复计划

## 当前状态

教务模块已创建但存在领域层与应用层的字段/方法不匹配问题，暂时禁用放在 `backend/temp_disabled/teaching/` 目录。

## 已完成的修复

### 1. 聚合根继承问题
- 所有8个聚合根已从 `implements AggregateRoot<Long>` 改为 `extends AggregateRoot<Long>`
- 涉及文件：AcademicYear, Semester, Course, CourseSchedule, CurriculumPlan, Examination, StudentGrade, TeachingTask

### 2. CurriculumPlan 聚合修复
- 字段名统一：`gradeYear` → `enrollYear`, `educationLength` → `duration`
- 字段名统一：`trainingObjective` → `objectives`, `graduationRequirement` → `requirements`
- 添加 `remark` 字段
- `version` 类型从 `Integer` 改为 `Long` 以匹配 AggregateRoot 基类
- 添加 `@EqualsAndHashCode(callSuper = false)` 注解

### 3. PlanCourse 实体修复
- 添加 `semester` 字段（别名 `semesterNumber`）
- 添加 `isRequired` 字段

### 4. CurriculumPlanRepository 修复
- 添加 `findByMajorIdAndEnrollYear` 默认方法
- 添加 `findPage(page, size, majorId, enrollYear, status)` 重载
- 添加 `count(majorId, enrollYear, status)` 重载
- 添加 `findPlanCourses(planId)` 方法

### 5. CourseSchedule 聚合修复
- `version` 类型从 `Integer` 改为 `Long`
- 添加 `getVersion()` 覆写方法

### 6. Course 聚合修复
- 添加 `englishName` 字段及 getter/setter
- 添加 `@EqualsAndHashCode(callSuper = false)` 注解

### 7. CourseScheduleRepositoryImpl 修复
- 添加 `countScheduledHours(Long taskId)` 方法
- 添加 `saveEntries`, `findEntriesBySemesterId`, `findEntriesByTaskId` 等缺失方法

## 待修复问题

### 1. ScheduleAdjustmentRepository (调课管理)
**文件**: `ScheduleAdjustmentRepositoryImpl.java`
- 缺少 `generateAdjustmentCode()` 方法
- 多个方法签名不匹配

### 2. ExaminationRepository (考试管理)
**文件**: `ExaminationRepositoryImpl.java`, `ExaminationApplicationService.java`
- 缺少 `deleteInvigilatorById(Long)` 方法
- 缺少 `saveExamRooms`, `saveInvigilators` 方法
- 缺少 `deleteArrangement`, `findArrangementByIdWithRooms` 等方法
- 缺少 `findBatchesPage`, `countBatches` 方法

### 3. ExamRoom 实体
**文件**: `ExamRoom.java`
- 缺少 `capacity` 字段
- 缺少 `actualCount` 字段

### 4. ExamInvigilator 实体
**文件**: `ExamInvigilator.java`
- 缺少 `isMain` 字段

### 5. ExamArrangement 实体
**文件**: `ExamArrangement.java`
- 缺少 `examRooms` 列表字段

### 6. TeachingTaskRepository
**文件**: `TeachingTaskApplicationService.java`
- 缺少 `existsBySemesterAndCourseAndClass` 方法

### 7. TeachingTask 聚合
**文件**: `TeachingTask.java`
- 缺少 `classroomId` 字段

### 8. Controller PageResult 类型推断
**文件**: 多个Controller
- `PageResult<>` 类型参数推断问题，需要显式指定类型

## 修复优先级

| 优先级 | 模块 | 说明 |
|--------|------|------|
| P0 | ExamRoom/ExamInvigilator | 实体字段补全 |
| P0 | ExamArrangement | 添加关联列表 |
| P1 | ExaminationRepository | 补全缺失方法 |
| P1 | TeachingTask | 添加classroomId |
| P2 | ScheduleAdjustmentRepository | 补全缺失方法 |
| P2 | Controller PageResult | 修复类型推断 |

## 修复步骤

1. 修复实体字段
2. 修复Repository接口
3. 修复Repository实现
4. 修复ApplicationService调用
5. 修复Controller类型问题
6. 移回src目录并编译测试

## 模块位置

```
backend/temp_disabled/teaching/
├── domain/           # 领域层（聚合、实体、值对象、仓储接口）
├── application/      # 应用层（命令、查询、DTO）
├── infrastructure/   # 基础设施层（仓储实现、Mapper、PO）
└── rest/             # 接口层（Controller）
```
