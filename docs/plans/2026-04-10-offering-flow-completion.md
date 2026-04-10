# 开课管理流程补齐 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补齐开课管理两个缺失端点，让 培养方案→开课计划→班级分配→教学任务 流程跑通。

**Architecture:** 在现有 OfferingController + OfferingApplicationService 上新增两个 POST 端点。复用现有 Repository 和领域模型，不改表结构。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus + JdbcTemplate

---

## 现状

| 功能 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 开课计划 CRUD | OfferingController ✅ | OfferingListTab.vue ✅ | 可用 |
| 班级分配 CRUD | ClassAssignmentController ✅ | ClassAssignmentTab.vue ✅ | 可用 |
| 从培养方案导入开课计划 | ❌ 缺失 | 前端已写好 | **需补** |
| 从开课分配生成教学任务 | ❌ 缺失 | 无 | **需补** |
| 教学任务 CRUD | TeachingTaskApplicationService ✅ | TeachingTaskView.vue ✅ | 可用 |

## Task 1: 从培养方案导入开课计划

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/OfferingController.java`
- Modify: `backend/src/main/java/com/school/management/application/teaching/OfferingApplicationService.java`

前端已有导入逻辑（OfferingListTab.vue），调用 `POST /teaching/offerings/import-from-plan`，
payload: `{ semesterId, planId, orgUnitIds? }`

后端需要：
1. 根据 planId 查 `curriculum_plan_courses` 获取该方案的所有课程
2. 读取方案的 `grade_year` 作为 `applicableGrade`
3. 为每门课创建 SemesterOffering（status=0 draft）
4. 跳过已存在的 (semester_id + course_id + applicable_grade 唯一)
5. 返回创建数量

**Service 方法：**
```java
public int importFromPlan(Long semesterId, Long planId, Long userId) {
    // 1. 查方案信息
    Map<String, Object> plan = jdbc.queryForMap(
        "SELECT grade_year FROM curriculum_plans WHERE id = ? AND deleted = 0", planId);
    String grade = plan.get("grade_year") != null ? plan.get("grade_year").toString() + "级" : "全年级";

    // 2. 查方案课程
    List<Map<String, Object>> planCourses = jdbc.queryForList(
        "SELECT course_id, weekly_hours, total_hours, course_category, course_type " +
        "FROM curriculum_plan_courses WHERE plan_id = ? AND deleted = 0", planId);

    // 3. 查已有的开课计划（去重）
    List<SemesterOffering> existing = offeringRepo.findBySemesterId(semesterId);
    Set<String> existingKeys = new HashSet<>();
    for (SemesterOffering o : existing) {
        existingKeys.add(o.getCourseId() + "_" + o.getApplicableGrade());
    }

    // 4. 创建
    int created = 0;
    for (Map<String, Object> pc : planCourses) {
        Long courseId = ((Number) pc.get("course_id")).longValue();
        if (existingKeys.contains(courseId + "_" + grade)) continue;

        Integer weeklyHours = pc.get("weekly_hours") != null ? ((Number) pc.get("weekly_hours")).intValue() : 2;
        SemesterOffering offering = SemesterOffering.create(
            semesterId, courseId, grade, weeklyHours, 1, null, userId);
        // set optional fields
        if (pc.get("course_category") != null || pc.get("course_type") != null) {
            offering.update(weeklyHours, 1, null,
                pc.get("course_category") != null ? ((Number) pc.get("course_category")).intValue() : null,
                pc.get("course_type") != null ? ((Number) pc.get("course_type")).intValue() : null,
                false, 2, false, null);
        }
        offeringRepo.save(offering);
        created++;
    }
    return created;
}
```

**Controller 端点：**
```java
@PostMapping("/import-from-plan")
@CasbinAccess(resource = "teaching:offering", action = "edit")
public Result<Map<String, Object>> importFromPlan(@RequestBody Map<String, Object> body) {
    Long semesterId = Long.parseLong(body.get("semesterId").toString());
    Long planId = Long.parseLong(body.get("planId").toString());
    Long userId = SecurityUtils.getCurrentUserId();
    int count = service.importFromPlan(semesterId, planId, userId);
    return Result.success(Map.of("imported", count));
}
```

需要加的 import: `java.util.HashSet`, `java.util.Set`, `org.springframework.jdbc.core.JdbcTemplate`
Service 需要注入 JdbcTemplate。

**编译验证：** `mvn compile -DskipTests` → BUILD SUCCESS

---

## Task 2: 从确认的班级分配生成教学任务

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/OfferingController.java`
- Modify: `backend/src/main/java/com/school/management/application/teaching/OfferingApplicationService.java`

前端待加按钮，先补后端 API。
`POST /teaching/offerings/generate-tasks`
payload: `{ semesterId }`

逻辑：
1. 查所有 confirmed 的 ClassCourseAssignment（status=1）
2. 查已有教学任务（去重：semester_id + course_id + org_unit_id）
3. 为每个 assignment 创建 TeachingTask
4. 返回创建数量

**Service 方法：**
```java
public int generateTasksFromAssignments(Long semesterId, Long userId) {
    List<ClassCourseAssignment> assignments = assignmentRepo.findBySemesterId(semesterId);
    // 只处理 confirmed 的
    assignments = assignments.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).toList();

    // 查已有教学任务去重
    List<Map<String, Object>> existingTasks = jdbc.queryForList(
        "SELECT course_id, org_unit_id FROM teaching_tasks WHERE semester_id = ? AND deleted = 0",
        semesterId);
    Set<String> existingKeys = new HashSet<>();
    for (Map<String, Object> t : existingTasks) {
        existingKeys.add(t.get("course_id") + "_" + t.get("org_unit_id"));
    }

    int created = 0;
    for (ClassCourseAssignment a : assignments) {
        String key = a.getCourseId() + "_" + a.getOrgUnitId();
        if (existingKeys.contains(key)) continue;

        long taskId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
        // 查开课计划获取周课时和起止周
        SemesterOffering offering = a.getOfferingId() != null ?
            offeringRepo.findById(a.getOfferingId()).orElse(null) : null;

        int weeklyHours = a.getWeeklyHours() != null ? a.getWeeklyHours() :
            (offering != null && offering.getWeeklyHours() != null ? offering.getWeeklyHours() : 2);
        int startWeek = offering != null && offering.getStartWeek() != null ? offering.getStartWeek() : 1;
        Integer endWeek = offering != null ? offering.getEndWeek() : null;

        TeachingTask task = TeachingTask.create(
            "TT" + taskId, semesterId, a.getCourseId(), a.getOrgUnitId(),
            a.getStudentCount() != null ? a.getStudentCount() : 0,
            weeklyHours, null, startWeek, endWeek,
            TaskStatus.CONFIRMED, null, userId);
        task.setId(taskId);
        // 设置关联
        if (offering != null) task.setOfferingId(offering.getId());
        taskRepo.save(task);
        created++;
    }
    return created;
}
```

注意：TeachingTask 需要有 offeringId setter。检查 TeachingTask 领域模型是否有此字段，如果没有需要加上。

**Controller 端点：**
```java
@PostMapping("/generate-tasks")
@CasbinAccess(resource = "teaching:offering", action = "edit")
public Result<Map<String, Object>> generateTasks(@RequestBody Map<String, Object> body) {
    Long semesterId = Long.parseLong(body.get("semesterId").toString());
    Long userId = SecurityUtils.getCurrentUserId();
    int count = service.generateTasksFromAssignments(semesterId, userId);
    return Result.success(Map.of("generated", count));
}
```

需要额外 import: TeachingTask, TaskStatus, TeachingTaskRepository

**编译验证：** `mvn compile -DskipTests` → BUILD SUCCESS

---

## Task 3: 前端加"生成教学任务"按钮

**Files:**
- Modify: `frontend/src/views/teaching/offering/ClassAssignmentTab.vue`
- Modify: `frontend/src/api/teaching.ts`

在 ClassAssignmentTab 加一个按钮"从已确认分配生成教学任务"，调 `POST /teaching/offerings/generate-tasks`。

api/teaching.ts 在 offeringApi 加：
```typescript
generateTasks: (semesterId: number | string) =>
  http.post('/teaching/offerings/generate-tasks', { semesterId }),
```

---

## 执行顺序

```
Task 1 (import-from-plan) → Task 2 (generate-tasks) → Task 3 (前端按钮)
                                                          ↓
                                                     编译+重启+浏览器测试
```
