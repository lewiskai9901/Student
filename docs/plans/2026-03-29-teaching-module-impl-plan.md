# 教务管理模块全面重构 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign the teaching management module with DDD architecture, adding class offering management, teaching-class abstraction, four-level constraint engine, conflict detection, and a fully redesigned scheduling UI with drag-and-drop.

**Architecture:** DDD hexagonal (domain→application→infrastructure→interfaces), following the exact patterns established in the inspection v7 module. Backend uses Spring Boot + MyBatis Plus, frontend uses Vue 3 + TypeScript + Element Plus + Tailwind CSS.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis Plus 3.5.7, MySQL, Vue 3, TypeScript, Pinia, Element Plus, Tailwind CSS, Lucide icons.

**Design Document:** `docs/plans/2026-03-29-teaching-module-redesign.md`

---

## Phase 1: Database Migrations

### Task 1: Create database migration for new teaching tables

**Files:**
- Create: `database/schema/V73.0.0__teaching_module_redesign.sql`

**Step 1: Write the migration SQL**

```sql
-- =====================================================
-- 教务管理模块重构 - 数据库迁移
-- =====================================================

-- 1. 学期开课计划表
CREATE TABLE IF NOT EXISTS semester_course_offerings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    applicable_grade VARCHAR(50) COMMENT '适用年级',
    weekly_hours INT NOT NULL COMMENT '周课时数',
    total_weeks INT COMMENT '总周数',
    start_week INT DEFAULT 1 COMMENT '起始周',
    end_week INT COMMENT '结束周',
    course_category TINYINT COMMENT '课程类别',
    course_type TINYINT COMMENT '课程性质',
    allow_combined TINYINT DEFAULT 0 COMMENT '是否允许合堂',
    max_combined_classes INT DEFAULT 2 COMMENT '最大合堂班数',
    allow_walking TINYINT DEFAULT 0 COMMENT '是否允许走班',
    status TINYINT DEFAULT 0 COMMENT '0草稿 1已确认 2已完成分配',
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_semester_course_grade (semester_id, course_id, applicable_grade),
    INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期开课计划表';

-- 2. 班级开课表
CREATE TABLE IF NOT EXISTS class_course_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL COMMENT '行政班ID',
    offering_id BIGINT NOT NULL COMMENT '学期开课计划ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    weekly_hours INT NOT NULL COMMENT '周课时',
    student_count INT COMMENT '选课人数',
    status TINYINT DEFAULT 0 COMMENT '0待确认 1已确认',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_class_course (semester_id, class_id, course_id),
    INDEX idx_semester_class (semester_id, class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级开课表';

-- 3. 教学班表
CREATE TABLE IF NOT EXISTS teaching_classes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    class_name VARCHAR(100) NOT NULL COMMENT '教学班名称',
    class_code VARCHAR(50) COMMENT '教学班编号',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    class_type TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2合堂 3走班',
    weekly_hours INT NOT NULL COMMENT '周课时数',
    student_count INT DEFAULT 0 COMMENT '学生数',
    required_room_type VARCHAR(50) COMMENT '教室类型要求',
    required_capacity INT COMMENT '教室容量要求',
    start_week INT DEFAULT 1,
    end_week INT,
    status TINYINT DEFAULT 1 COMMENT '1有效 0无效',
    remark VARCHAR(500),
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班表';

-- 4. 教学班成员表
CREATE TABLE IF NOT EXISTS teaching_class_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teaching_class_id BIGINT NOT NULL,
    member_type TINYINT NOT NULL COMMENT '1整班 2个人',
    admin_class_id BIGINT COMMENT '行政班ID',
    student_id BIGINT COMMENT '学生ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_teaching_class (teaching_class_id),
    INDEX idx_admin_class (admin_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学班成员表';

-- 5. 排课约束规则表
CREATE TABLE IF NOT EXISTS scheduling_constraints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL COMMENT '学期ID',
    constraint_name VARCHAR(100) NOT NULL COMMENT '约束名称',
    constraint_level TINYINT NOT NULL COMMENT '1全局 2教师 3班级 4课程',
    target_id BIGINT COMMENT '目标ID',
    target_name VARCHAR(100) COMMENT '目标名称',
    constraint_type VARCHAR(50) NOT NULL COMMENT '约束类型枚举',
    is_hard TINYINT DEFAULT 1 COMMENT '1硬约束 0软约束',
    priority INT DEFAULT 50 COMMENT '优先级权重(1-100)',
    params JSON NOT NULL COMMENT '约束参数',
    effective_weeks VARCHAR(100) COMMENT '生效周次',
    enabled TINYINT DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_semester (semester_id),
    INDEX idx_level_target (constraint_level, target_id),
    INDEX idx_type (constraint_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排课约束规则表';

-- 6. 排课冲突记录表
CREATE TABLE IF NOT EXISTS schedule_conflict_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    detection_batch VARCHAR(50) COMMENT '检测批次号',
    conflict_category TINYINT NOT NULL COMMENT '1资源冲突 2约束冲突 3软冲突',
    conflict_type VARCHAR(50) NOT NULL,
    severity TINYINT NOT NULL COMMENT '1阻塞 2警告 3提示',
    description VARCHAR(500) NOT NULL,
    detail JSON COMMENT '冲突详情',
    entry_id_1 BIGINT,
    entry_id_2 BIGINT,
    constraint_id BIGINT,
    resolution_status TINYINT DEFAULT 0 COMMENT '0未处理 1已解决 2已忽略',
    resolution_note VARCHAR(500),
    resolved_by BIGINT,
    resolved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_semester (semester_id),
    INDEX idx_batch (detection_batch),
    INDEX idx_status (resolution_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排课冲突记录表';

-- 7. 给 schedule_entries 补充 teaching_class_id 字段
ALTER TABLE schedule_entries ADD COLUMN IF NOT EXISTS teaching_class_id BIGINT COMMENT '教学班ID' AFTER task_id;
ALTER TABLE schedule_entries ADD INDEX IF NOT EXISTS idx_teaching_class (teaching_class_id);

-- 8. 给 schedule_entries 补充 consecutive_group 字段(连排分组)
ALTER TABLE schedule_entries ADD COLUMN IF NOT EXISTS consecutive_group VARCHAR(50) COMMENT '连排分组标识' AFTER week_type;
```

**Step 2: Apply migration to database**

Run: `mysql -u root -p student_management < database/schema/V73.0.0__teaching_module_redesign.sql`
Expected: Tables created successfully, no errors.

**Step 3: Commit**

```bash
git add database/schema/V73.0.0__teaching_module_redesign.sql
git commit -m "feat(teaching): add database migration for teaching module redesign

New tables: semester_course_offerings, class_course_assignments,
teaching_classes, teaching_class_members, scheduling_constraints,
schedule_conflict_records. Add teaching_class_id to schedule_entries."
```

---

## Phase 2: Backend Domain Models

### Task 2: Create offering domain models (SemesterOffering, ClassCourseAssignment)

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/offering/SemesterOffering.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/offering/ClassCourseAssignment.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/SemesterOfferingRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/ClassCourseAssignmentRepository.java`

**Step 1: Write SemesterOffering aggregate root**

Following InspProject pattern: extend `AggregateRoot<Long>`, factory `create()`, `reconstruct()`, private fields + public getters.

```java
package com.school.management.domain.teaching.model.offering;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class SemesterOffering extends AggregateRoot<Long> {
    private Long semesterId;
    private Long courseId;
    private String applicableGrade;
    private Integer weeklyHours;
    private Integer totalWeeks;
    private Integer startWeek;
    private Integer endWeek;
    private Integer courseCategory;
    private Integer courseType;
    private Boolean allowCombined;
    private Integer maxCombinedClasses;
    private Boolean allowWalking;
    private Integer status; // 0draft 1confirmed 2allocated
    private String remark;
    private Long createdBy;

    protected SemesterOffering() {}

    public static SemesterOffering create(Long semesterId, Long courseId, String applicableGrade,
            Integer weeklyHours, Integer startWeek, Integer endWeek, Long createdBy) {
        SemesterOffering o = new SemesterOffering();
        o.semesterId = semesterId;
        o.courseId = courseId;
        o.applicableGrade = applicableGrade;
        o.weeklyHours = weeklyHours;
        o.startWeek = startWeek != null ? startWeek : 1;
        o.endWeek = endWeek;
        o.allowCombined = false;
        o.maxCombinedClasses = 2;
        o.allowWalking = false;
        o.status = 0;
        o.createdBy = createdBy;
        return o;
    }

    public static SemesterOffering reconstruct(Long id, Long semesterId, Long courseId,
            String applicableGrade, Integer weeklyHours, Integer totalWeeks,
            Integer startWeek, Integer endWeek, Integer courseCategory, Integer courseType,
            Boolean allowCombined, Integer maxCombinedClasses, Boolean allowWalking,
            Integer status, String remark, Long createdBy) {
        SemesterOffering o = new SemesterOffering();
        o.id = id;
        o.semesterId = semesterId;
        o.courseId = courseId;
        o.applicableGrade = applicableGrade;
        o.weeklyHours = weeklyHours;
        o.totalWeeks = totalWeeks;
        o.startWeek = startWeek;
        o.endWeek = endWeek;
        o.courseCategory = courseCategory;
        o.courseType = courseType;
        o.allowCombined = allowCombined;
        o.maxCombinedClasses = maxCombinedClasses;
        o.allowWalking = allowWalking;
        o.status = status;
        o.remark = remark;
        o.createdBy = createdBy;
        return o;
    }

    public void confirm() {
        if (this.status != 0) throw new IllegalStateException("只能确认草稿状态的开课计划");
        this.status = 1;
    }

    public void update(Integer weeklyHours, Integer startWeek, Integer endWeek,
            Integer courseCategory, Integer courseType, Boolean allowCombined,
            Integer maxCombinedClasses, Boolean allowWalking, String remark) {
        this.weeklyHours = weeklyHours;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.courseCategory = courseCategory;
        this.courseType = courseType;
        this.allowCombined = allowCombined;
        this.maxCombinedClasses = maxCombinedClasses;
        this.allowWalking = allowWalking;
        this.remark = remark;
    }
}
```

**Step 2: Write ClassCourseAssignment entity**

```java
package com.school.management.domain.teaching.model.offering;

import com.school.management.domain.shared.Entity;
import lombok.Getter;

@Getter
public class ClassCourseAssignment implements Entity<Long> {
    private Long id;
    private Long semesterId;
    private Long classId;
    private Long offeringId;
    private Long courseId;
    private Integer weeklyHours;
    private Integer studentCount;
    private Integer status; // 0pending 1confirmed

    protected ClassCourseAssignment() {}

    public static ClassCourseAssignment create(Long semesterId, Long classId, Long offeringId,
            Long courseId, Integer weeklyHours, Integer studentCount) {
        ClassCourseAssignment a = new ClassCourseAssignment();
        a.semesterId = semesterId;
        a.classId = classId;
        a.offeringId = offeringId;
        a.courseId = courseId;
        a.weeklyHours = weeklyHours;
        a.studentCount = studentCount;
        a.status = 0;
        return a;
    }

    public static ClassCourseAssignment reconstruct(Long id, Long semesterId, Long classId,
            Long offeringId, Long courseId, Integer weeklyHours, Integer studentCount, Integer status) {
        ClassCourseAssignment a = new ClassCourseAssignment();
        a.id = id;
        a.semesterId = semesterId;
        a.classId = classId;
        a.offeringId = offeringId;
        a.courseId = courseId;
        a.weeklyHours = weeklyHours;
        a.studentCount = studentCount;
        a.status = status;
        return a;
    }

    public void confirm() {
        this.status = 1;
    }

    public void updateHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
    }
}
```

**Step 3: Write repository interfaces**

```java
// SemesterOfferingRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.offering.SemesterOffering;
import java.util.List;
import java.util.Optional;

public interface SemesterOfferingRepository {
    SemesterOffering save(SemesterOffering offering);
    Optional<SemesterOffering> findById(Long id);
    List<SemesterOffering> findBySemesterId(Long semesterId);
    List<SemesterOffering> findBySemesterIdAndGrade(Long semesterId, String grade);
    void deleteById(Long id);
}
```

```java
// ClassCourseAssignmentRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import java.util.List;
import java.util.Optional;

public interface ClassCourseAssignmentRepository {
    ClassCourseAssignment save(ClassCourseAssignment assignment);
    Optional<ClassCourseAssignment> findById(Long id);
    List<ClassCourseAssignment> findBySemesterIdAndClassId(Long semesterId, Long classId);
    List<ClassCourseAssignment> findBySemesterId(Long semesterId);
    void deleteById(Long id);
    void deleteBySemesterIdAndClassId(Long semesterId, Long classId);
}
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/teaching/
git commit -m "feat(teaching): add offering domain models and repository interfaces"
```

---

### Task 3: Create TeachingClass domain models

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/teachingclass/TeachingClass.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/teachingclass/TeachingClassMember.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/teachingclass/TeachingClassType.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/TeachingClassRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/TeachingClassMemberRepository.java`

**Step 1: Write TeachingClassType enum**

```java
package com.school.management.domain.teaching.model.teachingclass;

public enum TeachingClassType {
    NORMAL(1, "普通"),
    COMBINED(2, "合堂"),
    WALKING(3, "走班");

    private final int code;
    private final String label;

    TeachingClassType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public static TeachingClassType fromCode(int code) {
        for (TeachingClassType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown TeachingClassType code: " + code);
    }
}
```

**Step 2: Write TeachingClass aggregate root**

```java
package com.school.management.domain.teaching.model.teachingclass;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class TeachingClass extends AggregateRoot<Long> {
    private Long semesterId;
    private String className;
    private String classCode;
    private Long courseId;
    private TeachingClassType classType;
    private Integer weeklyHours;
    private Integer studentCount;
    private String requiredRoomType;
    private Integer requiredCapacity;
    private Integer startWeek;
    private Integer endWeek;
    private Integer status; // 1active 0inactive
    private String remark;
    private Long createdBy;
    private List<TeachingClassMember> members = new ArrayList<>();

    protected TeachingClass() {}

    public static TeachingClass create(Long semesterId, String className, Long courseId,
            TeachingClassType classType, Integer weeklyHours, Long createdBy) {
        TeachingClass tc = new TeachingClass();
        tc.semesterId = semesterId;
        tc.className = className;
        tc.courseId = courseId;
        tc.classType = classType;
        tc.weeklyHours = weeklyHours;
        tc.studentCount = 0;
        tc.startWeek = 1;
        tc.status = 1;
        tc.createdBy = createdBy;
        return tc;
    }

    public static TeachingClass reconstruct(Long id, Long semesterId, String className,
            String classCode, Long courseId, TeachingClassType classType, Integer weeklyHours,
            Integer studentCount, String requiredRoomType, Integer requiredCapacity,
            Integer startWeek, Integer endWeek, Integer status, String remark, Long createdBy) {
        TeachingClass tc = new TeachingClass();
        tc.id = id;
        tc.semesterId = semesterId;
        tc.className = className;
        tc.classCode = classCode;
        tc.courseId = courseId;
        tc.classType = classType;
        tc.weeklyHours = weeklyHours;
        tc.studentCount = studentCount;
        tc.requiredRoomType = requiredRoomType;
        tc.requiredCapacity = requiredCapacity;
        tc.startWeek = startWeek;
        tc.endWeek = endWeek;
        tc.status = status;
        tc.remark = remark;
        tc.createdBy = createdBy;
        return tc;
    }

    public void addAdminClass(Long adminClassId, int classStudentCount) {
        TeachingClassMember member = TeachingClassMember.ofAdminClass(this.id, adminClassId);
        this.members.add(member);
        this.studentCount = (this.studentCount != null ? this.studentCount : 0) + classStudentCount;
        this.requiredCapacity = this.studentCount;
    }

    public void addStudent(Long studentId) {
        TeachingClassMember member = TeachingClassMember.ofStudent(this.id, studentId);
        this.members.add(member);
        this.studentCount = (this.studentCount != null ? this.studentCount : 0) + 1;
    }

    public void update(String className, Integer weeklyHours, String requiredRoomType,
            Integer requiredCapacity, Integer startWeek, Integer endWeek, String remark) {
        this.className = className;
        this.weeklyHours = weeklyHours;
        this.requiredRoomType = requiredRoomType;
        this.requiredCapacity = requiredCapacity;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.remark = remark;
    }

    public void deactivate() { this.status = 0; }
    public void activate() { this.status = 1; }
}
```

**Step 3: Write TeachingClassMember value object**

```java
package com.school.management.domain.teaching.model.teachingclass;

import lombok.Getter;

@Getter
public class TeachingClassMember {
    private Long id;
    private Long teachingClassId;
    private Integer memberType; // 1=admin_class, 2=student
    private Long adminClassId;
    private Long studentId;

    protected TeachingClassMember() {}

    public static TeachingClassMember ofAdminClass(Long teachingClassId, Long adminClassId) {
        TeachingClassMember m = new TeachingClassMember();
        m.teachingClassId = teachingClassId;
        m.memberType = 1;
        m.adminClassId = adminClassId;
        return m;
    }

    public static TeachingClassMember ofStudent(Long teachingClassId, Long studentId) {
        TeachingClassMember m = new TeachingClassMember();
        m.teachingClassId = teachingClassId;
        m.memberType = 2;
        m.studentId = studentId;
        return m;
    }

    public static TeachingClassMember reconstruct(Long id, Long teachingClassId,
            Integer memberType, Long adminClassId, Long studentId) {
        TeachingClassMember m = new TeachingClassMember();
        m.id = id;
        m.teachingClassId = teachingClassId;
        m.memberType = memberType;
        m.adminClassId = adminClassId;
        m.studentId = studentId;
        return m;
    }
}
```

**Step 4: Write repository interfaces**

```java
// TeachingClassRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.teachingclass.TeachingClass;
import java.util.List;
import java.util.Optional;

public interface TeachingClassRepository {
    TeachingClass save(TeachingClass teachingClass);
    Optional<TeachingClass> findById(Long id);
    List<TeachingClass> findBySemesterId(Long semesterId);
    List<TeachingClass> findBySemesterIdAndCourseId(Long semesterId, Long courseId);
    void deleteById(Long id);
}
```

```java
// TeachingClassMemberRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.teachingclass.TeachingClassMember;
import java.util.List;

public interface TeachingClassMemberRepository {
    void saveAll(List<TeachingClassMember> members);
    List<TeachingClassMember> findByTeachingClassId(Long teachingClassId);
    void deleteByTeachingClassId(Long teachingClassId);
}
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/teaching/
git commit -m "feat(teaching): add TeachingClass domain models with NORMAL/COMBINED/WALKING types"
```

---

### Task 4: Create SchedulingConstraint domain model

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/scheduling/SchedulingConstraint.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/scheduling/ConstraintLevel.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/scheduling/ConstraintType.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/model/scheduling/ScheduleConflictRecord.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/SchedulingConstraintRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/teaching/repository/ScheduleConflictRecordRepository.java`

**Step 1: Write enums**

```java
// ConstraintLevel.java
package com.school.management.domain.teaching.model.scheduling;

public enum ConstraintLevel {
    GLOBAL(1, "全局"),
    TEACHER(2, "教师"),
    CLASS(3, "班级"),
    COURSE(4, "课程");

    private final int code;
    private final String label;
    ConstraintLevel(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static ConstraintLevel fromCode(int code) {
        for (ConstraintLevel l : values()) if (l.code == code) return l;
        throw new IllegalArgumentException("Unknown ConstraintLevel: " + code);
    }
}
```

```java
// ConstraintType.java
package com.school.management.domain.teaching.model.scheduling;

public enum ConstraintType {
    // Hard constraints
    TIME_FORBIDDEN("时间禁排"),
    TIME_FIXED("时间固定"),
    MAX_DAILY("每日上限"),
    MAX_CONSECUTIVE("最大连排"),
    ROOM_REQUIRED("教室要求"),
    // Soft constraints
    TIME_PREFERRED("时间偏好"),
    TIME_AVOIDED("时间回避"),
    SPREAD_EVEN("均匀分布"),
    MORNING_PRIORITY("上午优先"),
    COMPACT_SCHEDULE("紧凑排课"),
    MIN_GAP("最小间隔"),
    ROOM_PREFERRED("教室偏好");

    private final String label;
    ConstraintType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

**Step 2: Write SchedulingConstraint aggregate root**

```java
package com.school.management.domain.teaching.model.scheduling;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class SchedulingConstraint extends AggregateRoot<Long> {
    private Long semesterId;
    private String constraintName;
    private ConstraintLevel constraintLevel;
    private Long targetId;
    private String targetName;
    private ConstraintType constraintType;
    private Boolean isHard;
    private Integer priority;
    private String params; // JSON string
    private String effectiveWeeks;
    private Boolean enabled;
    private Long createdBy;

    protected SchedulingConstraint() {}

    public static SchedulingConstraint create(Long semesterId, String constraintName,
            ConstraintLevel level, Long targetId, String targetName,
            ConstraintType type, Boolean isHard, Integer priority,
            String params, String effectiveWeeks, Long createdBy) {
        SchedulingConstraint c = new SchedulingConstraint();
        c.semesterId = semesterId;
        c.constraintName = constraintName;
        c.constraintLevel = level;
        c.targetId = targetId;
        c.targetName = targetName;
        c.constraintType = type;
        c.isHard = isHard != null ? isHard : true;
        c.priority = priority != null ? priority : 50;
        c.params = params;
        c.effectiveWeeks = effectiveWeeks;
        c.enabled = true;
        c.createdBy = createdBy;
        return c;
    }

    public static SchedulingConstraint reconstruct(Long id, Long semesterId, String constraintName,
            ConstraintLevel level, Long targetId, String targetName,
            ConstraintType type, Boolean isHard, Integer priority,
            String params, String effectiveWeeks, Boolean enabled, Long createdBy) {
        SchedulingConstraint c = new SchedulingConstraint();
        c.id = id;
        c.semesterId = semesterId;
        c.constraintName = constraintName;
        c.constraintLevel = level;
        c.targetId = targetId;
        c.targetName = targetName;
        c.constraintType = type;
        c.isHard = isHard;
        c.priority = priority;
        c.params = params;
        c.effectiveWeeks = effectiveWeeks;
        c.enabled = enabled;
        c.createdBy = createdBy;
        return c;
    }

    public void update(String constraintName, Boolean isHard, Integer priority,
            String params, String effectiveWeeks) {
        this.constraintName = constraintName;
        this.isHard = isHard;
        this.priority = priority;
        this.params = params;
        this.effectiveWeeks = effectiveWeeks;
    }

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
```

**Step 3: Write ScheduleConflictRecord entity**

```java
package com.school.management.domain.teaching.model.scheduling;

import lombok.Getter;

@Getter
public class ScheduleConflictRecord {
    private Long id;
    private Long semesterId;
    private String detectionBatch;
    private Integer conflictCategory; // 1resource 2constraint 3soft
    private String conflictType;
    private Integer severity; // 1block 2warn 3info
    private String description;
    private String detail; // JSON
    private Long entryId1;
    private Long entryId2;
    private Long constraintId;
    private Integer resolutionStatus; // 0pending 1resolved 2ignored
    private String resolutionNote;
    private Long resolvedBy;

    protected ScheduleConflictRecord() {}

    public static ScheduleConflictRecord create(Long semesterId, String detectionBatch,
            Integer conflictCategory, String conflictType, Integer severity,
            String description, String detail, Long entryId1, Long entryId2, Long constraintId) {
        ScheduleConflictRecord r = new ScheduleConflictRecord();
        r.semesterId = semesterId;
        r.detectionBatch = detectionBatch;
        r.conflictCategory = conflictCategory;
        r.conflictType = conflictType;
        r.severity = severity;
        r.description = description;
        r.detail = detail;
        r.entryId1 = entryId1;
        r.entryId2 = entryId2;
        r.constraintId = constraintId;
        r.resolutionStatus = 0;
        return r;
    }

    public void resolve(String note, Long resolvedBy) {
        this.resolutionStatus = 1;
        this.resolutionNote = note;
        this.resolvedBy = resolvedBy;
    }

    public void ignore(String note, Long resolvedBy) {
        this.resolutionStatus = 2;
        this.resolutionNote = note;
        this.resolvedBy = resolvedBy;
    }
}
```

**Step 4: Write repository interfaces**

```java
// SchedulingConstraintRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import java.util.List;
import java.util.Optional;

public interface SchedulingConstraintRepository {
    SchedulingConstraint save(SchedulingConstraint constraint);
    Optional<SchedulingConstraint> findById(Long id);
    List<SchedulingConstraint> findBySemesterId(Long semesterId);
    List<SchedulingConstraint> findBySemesterIdAndLevel(Long semesterId, ConstraintLevel level);
    List<SchedulingConstraint> findBySemesterIdAndLevelAndTargetId(Long semesterId, ConstraintLevel level, Long targetId);
    void deleteById(Long id);
}
```

```java
// ScheduleConflictRecordRepository.java
package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.scheduling.ScheduleConflictRecord;
import java.util.List;

public interface ScheduleConflictRecordRepository {
    ScheduleConflictRecord save(ScheduleConflictRecord record);
    List<ScheduleConflictRecord> findBySemesterId(Long semesterId);
    List<ScheduleConflictRecord> findByDetectionBatch(String batch);
    List<ScheduleConflictRecord> findBySemesterIdAndStatus(Long semesterId, Integer status);
    void deleteByDetectionBatch(String batch);
}
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/teaching/
git commit -m "feat(teaching): add SchedulingConstraint and ConflictRecord domain models

Four-level constraint system: GLOBAL/TEACHER/CLASS/COURSE with hard/soft
constraint types and JSON-parameterized rules."
```

---

## Phase 3: Backend Infrastructure (PO + Mapper + RepositoryImpl)

### Task 5: Create PO and Mapper for all new domain models

**Files to create (each follows the same pattern):**

For each of the 6 new tables, create:
1. `infrastructure/persistence/teaching/{sub}/XxxPO.java` — `@Data @TableName("table_name")` with `@TableId(type = IdType.AUTO)`, `@TableLogic deleted`
2. `infrastructure/persistence/teaching/{sub}/XxxMapper.java` — `extends BaseMapper<XxxPO>` with `@Mapper`
3. `infrastructure/persistence/teaching/{sub}/XxxRepositoryImpl.java` — `@Repository` implementing domain interface, with `toPO()` and `toDomain()` conversion methods

**Subdirectories:**
- `infrastructure/persistence/teaching/offering/` — SemesterOfferingPO, SemesterOfferingMapper, SemesterOfferingRepositoryImpl, ClassCourseAssignmentPO, ClassCourseAssignmentMapper, ClassCourseAssignmentRepositoryImpl
- `infrastructure/persistence/teaching/teachingclass/` — TeachingClassPO, TeachingClassMapper, TeachingClassRepositoryImpl, TeachingClassMemberPO, TeachingClassMemberMapper, TeachingClassMemberRepositoryImpl
- `infrastructure/persistence/teaching/scheduling/` — SchedulingConstraintPO, SchedulingConstraintMapper, SchedulingConstraintRepositoryImpl, ScheduleConflictRecordPO, ScheduleConflictRecordMapper, ScheduleConflictRecordRepositoryImpl

**Step 1: Create all PO classes**

Each PO follows this pattern (example: SemesterOfferingPO):
```java
package com.school.management.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("semester_course_offerings")
public class SemesterOfferingPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Long courseId;
    private String applicableGrade;
    private Integer weeklyHours;
    private Integer totalWeeks;
    private Integer startWeek;
    private Integer endWeek;
    private Integer courseCategory;
    private Integer courseType;
    private Boolean allowCombined;
    private Integer maxCombinedClasses;
    private Boolean allowWalking;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
```

Repeat for: ClassCourseAssignmentPO, TeachingClassPO, TeachingClassMemberPO (no @TableLogic), SchedulingConstraintPO (with `params` as String for JSON), ScheduleConflictRecordPO.

**Step 2: Create all Mapper interfaces**

Each extends `BaseMapper<XxxPO>` with `@Mapper`. Add custom query methods as needed (e.g., `@Select` for complex filters).

**Step 3: Create all RepositoryImpl classes**

Each `@Repository` class implements the corresponding domain repository interface, with `toPO(DomainModel)` and `toDomain(PO)` conversion methods. For SchedulingConstraintRepositoryImpl, handle enum conversions (`ConstraintLevel.fromCode()`, `ConstraintType.valueOf()`).

**Step 4: Compile and verify**

Run: `cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/persistence/teaching/
git commit -m "feat(teaching): add PO, Mapper, and RepositoryImpl for all new teaching tables"
```

---

## Phase 4: Backend Application Services & Controllers

### Task 6: Create OfferingApplicationService and OfferingController

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/OfferingApplicationService.java`
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/OfferingController.java`

**Step 1: Write application service**

Key methods:
- `listBySemester(Long semesterId)` — list offerings
- `create(Map<String, Object> data)` — create offering
- `update(Long id, Map<String, Object> data)` — update
- `delete(Long id)` — soft delete
- `confirm(Long id)` — confirm offering
- `importFromCurriculumPlan(Long semesterId, Long planId, List<Long> classIds)` — bulk import from plan
- `listClassAssignments(Long semesterId, Long classId)` — list class assignments
- `createClassAssignment(Map<String, Object> data)` — create assignment
- `batchConfirmAssignments(Long semesterId, Long classId)` — batch confirm

**Step 2: Write REST controller**

Endpoints following the API design:
```
GET    /api/teaching/offerings?semesterId=X
POST   /api/teaching/offerings
PUT    /api/teaching/offerings/{id}
DELETE /api/teaching/offerings/{id}
POST   /api/teaching/offerings/{id}/confirm
POST   /api/teaching/offerings/import-from-plan
GET    /api/teaching/class-assignments?semesterId=X&classId=Y
POST   /api/teaching/class-assignments
POST   /api/teaching/class-assignments/batch-confirm
DELETE /api/teaching/class-assignments/{id}
```

**Step 3: Compile and verify**

Run: `cd backend && mvn compile -DskipTests`

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/application/teaching/ backend/src/main/java/com/school/management/interfaces/rest/teaching/
git commit -m "feat(teaching): add offering management service and API endpoints"
```

---

### Task 7: Create TeachingClassApplicationService and TeachingClassController

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/TeachingClassApplicationService.java`
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/TeachingClassController.java`

Key methods:
- `listBySemester(Long semesterId)` — list teaching classes with members
- `create(Map data)` — create with members
- `update(Long id, Map data)` — update
- `delete(Long id)` — deactivate
- `autoGenerate(Long semesterId)` — auto-generate from class assignments (NORMAL for single-class, COMBINED for multi-class offerings)
- `addMembers(Long id, List members)` — add admin classes or students
- `removeMembers(Long id, List memberIds)` — remove members

Endpoints:
```
GET    /api/teaching/teaching-classes?semesterId=X
POST   /api/teaching/teaching-classes
PUT    /api/teaching/teaching-classes/{id}
DELETE /api/teaching/teaching-classes/{id}
POST   /api/teaching/teaching-classes/auto-generate
GET    /api/teaching/teaching-classes/{id}/members
POST   /api/teaching/teaching-classes/{id}/members
DELETE /api/teaching/teaching-classes/{id}/members
```

**Commit after compile succeeds.**

---

### Task 8: Create ConstraintApplicationService and ConstraintController

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/ConstraintApplicationService.java`
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/ConstraintController.java`

Key methods:
- `listBySemester(Long semesterId)` — all constraints
- `listByLevelAndTarget(Long semesterId, int level, Long targetId)` — filtered
- `create(Map data)` — create constraint
- `update(Long id, Map data)` — update
- `delete(Long id)` — soft delete
- `enable(Long id)` / `disable(Long id)` — toggle
- `getTimeMatrix(Long semesterId, int level, Long targetId)` — compute effective time availability matrix (7 days × N periods) considering all applicable constraints
- `batchImport(Long semesterId, List<Map> constraints)` — bulk create

Endpoints:
```
GET    /api/teaching/constraints?semesterId=X&level=Y&targetId=Z
POST   /api/teaching/constraints
PUT    /api/teaching/constraints/{id}
DELETE /api/teaching/constraints/{id}
POST   /api/teaching/constraints/{id}/enable
POST   /api/teaching/constraints/{id}/disable
GET    /api/teaching/constraints/time-matrix?semesterId=X&level=Y&targetId=Z
POST   /api/teaching/constraints/batch-import
```

The `getTimeMatrix` method is critical — it computes a 7×N boolean matrix showing which time slots are available, forbidden, preferred, or avoided for a given target. It must aggregate global constraints + target-specific constraints.

**Commit after compile succeeds.**

---

### Task 9: Create ConflictDetectionService and ConflictController

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/ConflictDetectionService.java`
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/ConflictController.java`

Key methods in ConflictDetectionService:
- `feasibilityCheck(Long semesterId)` — pre-scheduling check:
  1. For each teacher: count required weekly periods vs available time slots (after constraints). Flag if insufficient.
  2. For each pair of constraints: check mutual contradiction (e.g., teacher A says "only Tuesday" + global says "Tuesday forbidden").
  3. For each teaching class: check if required room capacity exists.
  Returns: `FeasibilityReport { List<BlockingIssue>, List<Warning> }`

- `detectConflicts(Long semesterId)` — post-scheduling check:
  1. Teacher conflicts: same teacher, same day+period, overlapping weeks
  2. Classroom conflicts: same room, same day+period, overlapping weeks
  3. Class conflicts: same admin class, same day+period (through teaching_class_members)
  4. Constraint violations: check each entry against all applicable constraints
  Returns: `List<ScheduleConflictRecord>`

Endpoints:
```
POST   /api/teaching/conflicts/feasibility-check?semesterId=X
POST   /api/teaching/conflicts/detect?semesterId=X
GET    /api/teaching/conflicts?semesterId=X&status=Y
POST   /api/teaching/conflicts/{id}/resolve
POST   /api/teaching/conflicts/{id}/ignore
```

**Commit after compile succeeds.**

---

### Task 10: Refactor existing teaching controllers to DDD

**Files to modify:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/TeachingScheduleController.java` — refactor schedule entry endpoints, remove raw JdbcTemplate, delegate to application service
- Create: `backend/src/main/java/com/school/management/application/teaching/ScheduleEntryApplicationService.java` — wraps schedule_entries table operations with proper transaction management and conflict checking

Key changes:
1. Replace all `JdbcTemplate` operations with repository/mapper calls
2. Add `@Transactional` to write operations
3. Replace `System.currentTimeMillis()` ID generation with `IdType.AUTO`
4. Replace hardcoded `applicantId=1L` with `SecurityUtils.getCurrentUserId()`
5. Schedule entries must now include `teaching_class_id` when creating/updating
6. `getByClass`, `getByTeacher`, `getByClassroom` queries should JOIN with teaching_classes for enriched data

**Note:** Keep existing endpoints working (backward compatible paths) while adding new ones. The old `course_schedules` concept (scheme list) is removed — scheduling now works directly on entries per semester.

**Commit after compile succeeds.**

---

## Phase 5: Frontend Types & API

### Task 11: Add new TypeScript types

**Files:**
- Modify: `frontend/src/types/teaching.ts` — add new types at the end

**New types to add:**

```typescript
// === Offering Management ===
export interface SemesterOffering {
  id: number
  semesterId: number
  courseId: number
  courseName?: string
  courseCode?: string
  applicableGrade: string
  weeklyHours: number
  totalWeeks?: number
  startWeek: number
  endWeek?: number
  courseCategory?: number
  courseType?: number
  allowCombined: boolean
  maxCombinedClasses: number
  allowWalking: boolean
  status: number
  remark?: string
}

export interface ClassCourseAssignment {
  id: number
  semesterId: number
  classId: number
  className?: string
  offeringId: number
  courseId: number
  courseName?: string
  weeklyHours: number
  studentCount?: number
  status: number
}

// === Teaching Class ===
export interface TeachingClass {
  id: number
  semesterId: number
  className: string
  classCode?: string
  courseId: number
  courseName?: string
  classType: 1 | 2 | 3 // 1NORMAL 2COMBINED 3WALKING
  weeklyHours: number
  studentCount: number
  requiredRoomType?: string
  requiredCapacity?: number
  startWeek: number
  endWeek?: number
  status: number
  remark?: string
  members?: TeachingClassMember[]
}

export interface TeachingClassMember {
  id: number
  teachingClassId: number
  memberType: 1 | 2 // 1admin_class 2student
  adminClassId?: number
  adminClassName?: string
  studentId?: number
  studentName?: string
}

export const TEACHING_CLASS_TYPES = [
  { value: 1, label: '普通' },
  { value: 2, label: '合堂' },
  { value: 3, label: '走班' },
]

// === Scheduling Constraints ===
export interface SchedulingConstraint {
  id: number
  semesterId: number
  constraintName: string
  constraintLevel: 1 | 2 | 3 | 4
  targetId?: number
  targetName?: string
  constraintType: string
  isHard: boolean
  priority: number
  params: Record<string, any>
  effectiveWeeks?: string
  enabled: boolean
}

export const CONSTRAINT_LEVELS = [
  { value: 1, label: '全局' },
  { value: 2, label: '教师' },
  { value: 3, label: '班级' },
  { value: 4, label: '课程' },
]

export const CONSTRAINT_TYPES = [
  { value: 'TIME_FORBIDDEN', label: '时间禁排', isHardDefault: true },
  { value: 'TIME_FIXED', label: '时间固定', isHardDefault: true },
  { value: 'MAX_DAILY', label: '每日上限', isHardDefault: true },
  { value: 'MAX_CONSECUTIVE', label: '最大连排', isHardDefault: true },
  { value: 'ROOM_REQUIRED', label: '教室要求', isHardDefault: true },
  { value: 'TIME_PREFERRED', label: '时间偏好', isHardDefault: false },
  { value: 'TIME_AVOIDED', label: '时间回避', isHardDefault: false },
  { value: 'SPREAD_EVEN', label: '均匀分布', isHardDefault: false },
  { value: 'MORNING_PRIORITY', label: '上午优先', isHardDefault: false },
  { value: 'COMPACT_SCHEDULE', label: '紧凑排课', isHardDefault: false },
  { value: 'MIN_GAP', label: '最小间隔', isHardDefault: false },
  { value: 'ROOM_PREFERRED', label: '教室偏好', isHardDefault: false },
]

// === Time Matrix (constraint visualization) ===
export interface TimeSlotStatus {
  day: number // 1-7
  period: number // 1-N
  status: 'available' | 'forbidden' | 'preferred' | 'avoided'
  reasons: string[]
}

export type TimeMatrix = TimeSlotStatus[][]

// === Conflict Detection ===
export interface ScheduleConflict {
  id: number
  semesterId: number
  detectionBatch?: string
  conflictCategory: 1 | 2 | 3
  conflictType: string
  severity: 1 | 2 | 3
  description: string
  detail?: Record<string, any>
  entryId1?: number
  entryId2?: number
  constraintId?: number
  resolutionStatus: 0 | 1 | 2
  resolutionNote?: string
}

export interface FeasibilityReport {
  blockingIssues: FeasibilityIssue[]
  warnings: FeasibilityIssue[]
  passedChecks: number
}

export interface FeasibilityIssue {
  type: string
  target: string
  description: string
  suggestion: string
}
```

**Commit after adding types.**

---

### Task 12: Add new frontend API functions

**Files:**
- Modify: `frontend/src/api/teaching.ts` — add new API groups

**Add these API objects:**

```typescript
// === Offering Management API ===
export const offeringApi = {
  list: (semesterId: number | string) =>
    request.get<SemesterOffering[]>(`${BASE_URL}/offerings`, { params: { semesterId } }),
  create: (data: Partial<SemesterOffering>) =>
    request.post<SemesterOffering>(`${BASE_URL}/offerings`, data),
  update: (id: number | string, data: Partial<SemesterOffering>) =>
    request.put<SemesterOffering>(`${BASE_URL}/offerings/${id}`, data),
  delete: (id: number | string) =>
    request.delete(`${BASE_URL}/offerings/${id}`),
  confirm: (id: number | string) =>
    request.post(`${BASE_URL}/offerings/${id}/confirm`),
  importFromPlan: (data: { semesterId: number; planId: number; classIds?: number[] }) =>
    request.post(`${BASE_URL}/offerings/import-from-plan`, data),
}

export const classAssignmentApi = {
  list: (semesterId: number | string, classId?: number | string) =>
    request.get<ClassCourseAssignment[]>(`${BASE_URL}/class-assignments`, { params: { semesterId, classId } }),
  create: (data: Partial<ClassCourseAssignment>) =>
    request.post<ClassCourseAssignment>(`${BASE_URL}/class-assignments`, data),
  delete: (id: number | string) =>
    request.delete(`${BASE_URL}/class-assignments/${id}`),
  batchConfirm: (semesterId: number | string, classId: number | string) =>
    request.post(`${BASE_URL}/class-assignments/batch-confirm`, { semesterId, classId }),
}

// === Teaching Class API ===
export const teachingClassApi = {
  list: (semesterId: number | string) =>
    request.get<TeachingClass[]>(`${BASE_URL}/teaching-classes`, { params: { semesterId } }),
  getById: (id: number | string) =>
    request.get<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`),
  create: (data: Partial<TeachingClass>) =>
    request.post<TeachingClass>(`${BASE_URL}/teaching-classes`, data),
  update: (id: number | string, data: Partial<TeachingClass>) =>
    request.put<TeachingClass>(`${BASE_URL}/teaching-classes/${id}`, data),
  delete: (id: number | string) =>
    request.delete(`${BASE_URL}/teaching-classes/${id}`),
  autoGenerate: (semesterId: number | string) =>
    request.post(`${BASE_URL}/teaching-classes/auto-generate`, { semesterId }),
  getMembers: (id: number | string) =>
    request.get<TeachingClassMember[]>(`${BASE_URL}/teaching-classes/${id}/members`),
  addMembers: (id: number | string, members: Partial<TeachingClassMember>[]) =>
    request.post(`${BASE_URL}/teaching-classes/${id}/members`, members),
  removeMembers: (id: number | string, memberIds: number[]) =>
    request.delete(`${BASE_URL}/teaching-classes/${id}/members`, { data: memberIds }),
}

// === Constraint API ===
export const constraintApi = {
  list: (params: { semesterId: number | string; level?: number; targetId?: number | string }) =>
    request.get<SchedulingConstraint[]>(`${BASE_URL}/constraints`, { params }),
  create: (data: Partial<SchedulingConstraint>) =>
    request.post<SchedulingConstraint>(`${BASE_URL}/constraints`, data),
  update: (id: number | string, data: Partial<SchedulingConstraint>) =>
    request.put<SchedulingConstraint>(`${BASE_URL}/constraints/${id}`, data),
  delete: (id: number | string) =>
    request.delete(`${BASE_URL}/constraints/${id}`),
  enable: (id: number | string) =>
    request.post(`${BASE_URL}/constraints/${id}/enable`),
  disable: (id: number | string) =>
    request.post(`${BASE_URL}/constraints/${id}/disable`),
  getTimeMatrix: (params: { semesterId: number | string; level: number; targetId?: number | string }) =>
    request.get<TimeMatrix>(`${BASE_URL}/constraints/time-matrix`, { params }),
  batchImport: (semesterId: number | string, constraints: Partial<SchedulingConstraint>[]) =>
    request.post(`${BASE_URL}/constraints/batch-import`, { semesterId, constraints }),
}

// === Conflict API ===
export const conflictApi = {
  feasibilityCheck: (semesterId: number | string) =>
    request.post<FeasibilityReport>(`${BASE_URL}/conflicts/feasibility-check`, null, { params: { semesterId } }),
  detect: (semesterId: number | string) =>
    request.post<ScheduleConflict[]>(`${BASE_URL}/conflicts/detect`, null, { params: { semesterId } }),
  list: (params: { semesterId: number | string; status?: number }) =>
    request.get<ScheduleConflict[]>(`${BASE_URL}/conflicts`, { params }),
  resolve: (id: number | string, note: string) =>
    request.post(`${BASE_URL}/conflicts/${id}/resolve`, { note }),
  ignore: (id: number | string, note: string) =>
    request.post(`${BASE_URL}/conflicts/${id}/ignore`, { note }),
}
```

**Commit.**

---

## Phase 6: Frontend Views — Offering Management

### Task 13: Create OfferingManagementView (班级开课管理)

**Files:**
- Create: `frontend/src/views/teaching/OfferingManagementView.vue`

This is the **three-step wizard** view for:
1. 学期开课计划 (Step 1)
2. 班级开课确认 (Step 2)
3. 教学班组建 & 教师分配 (Step 3)

**Key UI requirements:**
- Top tab bar switching between 3 steps
- Step 1: Table of semester offerings with "从培养方案导入" button, inline editing of weekly hours, allow-combined toggle
- Step 2: Class selector dropdown → shows that class's courses with hours, confirm button, total hours counter with warning if >30
- Step 3: Teaching class list with type badges (普通/合堂/走班), member display, teacher assignment dialog, "自动生成教学班" button

**Follow UI style patterns from design document.** Compact stat bar, no decorative icons, Tailwind CSS.

**After creating the view, add the route:**

Modify `frontend/src/router/index.ts`: add route for `/teaching/offerings` with `order: 3` (shift existing routes down).

```typescript
{
  path: '/teaching/offerings',
  name: 'TeachingOfferings',
  component: () => import('@/views/teaching/OfferingManagementView.vue'),
  meta: { title: '开课管理', order: 3 }
},
```

**Commit.**

---

## Phase 7: Frontend Views — Constraint Configuration

### Task 14: Create ConstraintConfigView (排课约束配置)

**Files:**
- Create: `frontend/src/views/teaching/scheduling/ConstraintConfigView.vue`

**Key UI requirements:**
- 4 tabs: 全局约束 / 教师约束 / 班级约束 / 课程约束
- Each tab lists constraints grouped by hard/soft
- Each constraint card shows: name, type badge, params summary, enable/disable toggle, edit/delete buttons
- Soft constraints show priority weight bar (1-100)
- **Time matrix visualization** at the bottom of each tab:
  - 7 columns (Mon-Sun) × N rows (periods)
  - Color-coded cells: green=available, red=forbidden, blue=preferred, yellow=avoided
  - Clickable to quickly toggle forbidden/preferred/avoided
- "添加约束" dialog with dynamic form based on constraint type selection
- Teacher/Class/Course tabs have target selector dropdown at top

**Commit.**

---

## Phase 8: Frontend Views — Schedule View Redesign

### Task 15: Completely rewrite ScheduleView (排课中心)

**Files:**
- Rewrite: `frontend/src/views/teaching/ScheduleView.vue` — complete replacement

**Key structure — 6 tabs:**
1. **排课总览** — progress bar, task status table, action buttons (自动排课/可行性检测/发布)
2. **课表视图** — 3-mode switcher (班级/教师/教室), timetable grid with colored course blocks, week navigation, drag-and-drop support
3. **约束配置** — embed `ConstraintConfigView` or link to it
4. **冲突中心** — conflict list with severity badges, resolve/ignore actions, feasibility report display
5. **调课管理** — existing adjustment functionality (refactored from `ScheduleAdjustmentView.vue`)
6. **导出打印** — export options (placeholder)

**Timetable grid is the core component.** Key features:
- Table: rows=periods (from class_time_configs), cols=weekdays
- Each cell can contain 0-N course blocks (colored cards with course name, teacher, room)
- Grey background for constraint-forbidden slots
- Week selector to navigate between teaching weeks
- Single/double week filter toggle
- Stats bar at bottom: "本周N节课 | M天有课 | 教室利用率X%"

**This is the largest single task.** Break the ScheduleView into sub-components:

- `frontend/src/views/teaching/scheduling/ScheduleOverviewTab.vue`
- `frontend/src/views/teaching/scheduling/TimetableTab.vue`
- `frontend/src/views/teaching/scheduling/TimetableGrid.vue` (the grid component, reusable)
- `frontend/src/views/teaching/scheduling/ConflictCenterTab.vue`
- `frontend/src/views/teaching/scheduling/AdjustmentTab.vue`

**Commit each sub-component separately.**

---

### Task 16: Create TimetableGrid component (课表格子组件)

**Files:**
- Create: `frontend/src/views/teaching/scheduling/TimetableGrid.vue`

**Props:**
```typescript
interface Props {
  entries: ScheduleEntry[]
  periods: PeriodConfig[]
  weekdays?: typeof WEEKDAYS
  constraints?: TimeMatrix // optional: show forbidden/preferred slots
  editable?: boolean // enable drag-and-drop
  highlightConflicts?: boolean
}
```

**Emits:**
```typescript
interface Emits {
  (e: 'entry-click', entry: ScheduleEntry): void
  (e: 'cell-click', day: number, period: number): void
  (e: 'entry-drop', entryId: number, newDay: number, newPeriod: number): void
}
```

**Key implementation details:**
- Use HTML table with `table-fixed` layout
- Course blocks: colored rounded cards using `bg-{color}-500` based on `courseId % 8`
- Conflict entries: red border `ring-2 ring-red-500`
- Forbidden slots: `bg-gray-100` with diagonal stripes pattern
- Preferred slots: `bg-blue-50`
- Hover tooltip: full details (course, teacher, room, weeks, student count)
- Drag: use HTML5 Drag and Drop API (`draggable`, `@dragstart`, `@dragover`, `@drop`)

**Commit.**

---

## Phase 9: Router & Navigation Updates

### Task 17: Update router and clean up old views

**Files:**
- Modify: `frontend/src/router/index.ts` — restructure teaching routes
- Delete: `frontend/src/views/teaching/ScheduleAdjustmentView.vue` (merged into scheduling tabs)

**New route structure:**
```typescript
{
  path: '/teaching',
  name: 'Teaching',
  redirect: '/teaching/calendar',
  meta: { title: '教务管理', icon: 'GraduationCap', requiresAuth: true, order: 20, group: 'operations' },
  children: [
    { path: 'calendar', name: 'TeachingCalendar', component: () => import('@/views/teaching/AcademicCalendarView.vue'), meta: { title: '校历管理', order: 1 } },
    { path: 'courses', name: 'TeachingCourses', component: () => import('@/views/teaching/CourseListView.vue'), meta: { title: '课程管理', order: 2 } },
    { path: 'curriculum-plans', name: 'TeachingCurriculum', component: () => import('@/views/teaching/CurriculumPlanView.vue'), meta: { title: '培养方案', order: 3 } },
    { path: 'offerings', name: 'TeachingOfferings', component: () => import('@/views/teaching/OfferingManagementView.vue'), meta: { title: '开课管理', order: 4 } },
    { path: 'scheduling', name: 'TeachingScheduling', component: () => import('@/views/teaching/ScheduleView.vue'), meta: { title: '排课中心', order: 5 } },
    { path: 'tasks', name: 'TeachingTasks', component: () => import('@/views/teaching/TeachingTaskView.vue'), meta: { title: '教学任务', order: 6 } },
    { path: 'examinations', name: 'TeachingExams', component: () => import('@/views/teaching/ExaminationView.vue'), meta: { title: '考试管理', order: 7 } },
    { path: 'grades', name: 'TeachingGrades', component: () => import('@/views/teaching/GradeView.vue'), meta: { title: '成绩管理', order: 8 } },
  ]
}
```

**Commit.**

---

## Phase 10: Integration & Polish

### Task 18: Backend compile test and fix all errors

**Step 1:** Run full backend compile
```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests
```

**Step 2:** Fix all compilation errors (missing imports, type mismatches, etc.)

**Step 3:** Start backend and verify endpoints respond
```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
```

Test key endpoints with curl:
```bash
curl http://localhost:8080/api/teaching/offerings?semesterId=1
curl http://localhost:8080/api/teaching/teaching-classes?semesterId=1
curl http://localhost:8080/api/teaching/constraints?semesterId=1
```

**Commit fixes.**

---

### Task 19: Frontend type-check and fix all errors

**Step 1:** Run type checker
```bash
cd frontend && npm run type-check
```

**Step 2:** Fix all TypeScript errors

**Step 3:** Run dev server and verify pages load
```bash
cd frontend && npm run dev
```

Navigate to:
- `/teaching/offerings` — should show empty offering management page
- `/teaching/scheduling` — should show redesigned schedule center

**Commit fixes.**

---

### Task 20: End-to-end smoke test

**Manual verification flow:**

1. **开课管理**: Create a semester offering → Confirm → Create class assignment → Confirm
2. **教学班**: Auto-generate teaching classes from confirmed assignments
3. **教学任务**: Create teaching task for a teaching class, assign teacher
4. **约束配置**: Add a global TIME_FORBIDDEN constraint (e.g., Tuesday afternoon)
5. **约束配置**: Add a teacher constraint (e.g., max 3 periods/day)
6. **冲突检测**: Run feasibility check — should detect issues if constraints are tight
7. **课表视图**: Manually add a schedule entry, verify it appears in timetable grid
8. **冲突检测**: Run conflict detection — verify it catches double-booked slots

**Document any issues found and fix them.**

**Final commit.**

---

## Summary: File Count Estimate

| Category | New Files | Modified Files |
|----------|-----------|----------------|
| Database migration | 1 | 0 |
| Domain models | ~12 | 0 |
| Repository interfaces | ~8 | 0 |
| Infrastructure (PO+Mapper+Impl) | ~18 | 0 |
| Application services | ~5 | 1 |
| Controllers | ~5 | 1 |
| Frontend types | 0 | 1 |
| Frontend API | 0 | 1 |
| Frontend views | ~8 | 1 (ScheduleView rewrite) |
| Router | 0 | 1 |
| **Total** | **~57** | **~6** |
