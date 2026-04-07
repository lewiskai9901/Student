# Phase 1: 校历×排课基础设施 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 建立实况课表(schedule_instances)、作息表(period_configs)、扩展校历事件表，并实现实况生成服务和课时统计API。

**Architecture:** 三层架构 — 基准层(schedule_entries已有) → 实况层(schedule_instances新增) → 事件层(academic_event扩展+period_configs新增)。后端用 JdbcTemplate 直操作，前端 API 定义先行。

**Tech Stack:** Spring Boot 3.2 + JdbcTemplate + MySQL 5.7 + Vue 3 + TypeScript

---

### Task 1: 数据库迁移 — 创建 period_configs + schedule_instances 表，扩展 academic_event

**Files:**
- Create: `database/migrations/V20260407_3__calendar_schedule_infrastructure.sql`

**Step 1: 写迁移SQL**

```sql
-- 作息表
CREATE TABLE IF NOT EXISTS period_configs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  semester_id BIGINT NOT NULL,
  config_name VARCHAR(50) NOT NULL DEFAULT '默认作息表',
  periods_per_day INT NOT NULL DEFAULT 8,
  schedule_days JSON NOT NULL,
  periods JSON NOT NULL COMMENT '[{period,name,startTime,endTime}]',
  is_default TINYINT DEFAULT 1,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_semester (semester_id)
) COMMENT '作息表/节次配置';

-- 实况课表
CREATE TABLE IF NOT EXISTS schedule_instances (
  id BIGINT NOT NULL PRIMARY KEY,
  entry_id BIGINT COMMENT '来源基准条目',
  semester_id BIGINT NOT NULL,
  actual_date DATE NOT NULL,
  weekday TINYINT NOT NULL,
  week_number INT,
  start_slot INT NOT NULL,
  end_slot INT NOT NULL,
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  teacher_id BIGINT,
  original_teacher_id BIGINT,
  classroom_id BIGINT,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已取消 2已调走 3补课 4代课',
  cancel_reason VARCHAR(200),
  source_type TINYINT NOT NULL DEFAULT 0 COMMENT '0基准展开 1调课 2补课日 3临时',
  source_id BIGINT,
  actual_hours DECIMAL(3,1) DEFAULT 1.0,
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_sem_date (semester_id, actual_date),
  INDEX idx_teacher (teacher_id, actual_date),
  INDEX idx_class (class_id, actual_date),
  INDEX idx_classroom (classroom_id, actual_date),
  INDEX idx_entry (entry_id),
  INDEX idx_week (semester_id, week_number)
) COMMENT '实况课表';

-- 扩展 academic_event
SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_type');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_type TINYINT DEFAULT 0 COMMENT ''0无影响 1全天停课 2半天停课 3补课日 4考试周''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_scope');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_scope VARCHAR(50) DEFAULT ''all'' COMMENT ''影响范围''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='substitute_weekday');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN substitute_weekday TINYINT COMMENT ''补课日按周几上课''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @col = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='academic_event' AND COLUMN_NAME='affect_slots');
SET @s = IF(@col=0, 'ALTER TABLE academic_event ADD COLUMN affect_slots VARCHAR(50) COMMENT ''影响节次''', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
```

**Step 2: 执行迁移**

```bash
mysql -u root -p123456 student_management < database/migrations/V20260407_3__calendar_schedule_infrastructure.sql
```

**Step 3: 验证**

```bash
mysql -u root -p123456 student_management -e "SHOW TABLES LIKE 'period_configs'; SHOW TABLES LIKE 'schedule_instances'; SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='academic_event' AND COLUMN_NAME IN ('affect_type','substitute_weekday');"
```

**Step 4: Commit**

```bash
git add database/migrations/V20260407_3__calendar_schedule_infrastructure.sql
git commit -m "feat: create period_configs + schedule_instances tables, extend academic_event"
```

---

### Task 2: 后端 — 作息表 CRUD API (PeriodConfigController)

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/calendar/PeriodConfigController.java`

**Step 1: 实现 Controller**

```java
@Slf4j
@RestController
@RequestMapping("/calendar/period-configs")
@RequiredArgsConstructor
public class PeriodConfigController {
    private final JdbcTemplate jdbc;

    // GET / — 获取某学期的作息表
    // POST / — 创建作息表
    // PUT /{id} — 更新
    // DELETE /{id} — 删除
    // POST /init-from-previous — 从上一学期继承
}
```

端点:
- `GET /calendar/period-configs?semesterId=X` → 列表(通常1条)
- `POST /calendar/period-configs` → 创建 `{semesterId, configName, periodsPerDay, scheduleDays, periods}`
- `PUT /calendar/period-configs/{id}` → 更新
- `DELETE /calendar/period-configs/{id}` → 软删除

**Step 2: 编译验证**

```bash
cd backend && mvn compile -DskipTests
```

**Step 3: Commit**

```bash
git add backend/src/main/java/.../PeriodConfigController.java
git commit -m "feat: add period config CRUD API /calendar/period-configs"
```

---

### Task 3: 后端 — 实况课表生成服务 (InstanceGenerationService)

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/InstanceGenerationService.java`

**Step 1: 实现服务**

核心方法:
```java
@Service
public class InstanceGenerationService {
    // 1. generateInstances(semesterId) — 从基准展开全学期实况
    //    读 schedule_entries × academic_weeks → 生成 schedule_instances
    //    然后叠加 academic_event 的影响

    // 2. applyCalendarEvent(eventId) — 校历事件变更时更新实况
    //    affect_type=1: 全天取消
    //    affect_type=2: 半天取消
    //    affect_type=3: 生成补课日实例

    // 3. applyAdjustment(adjustmentId) — 调课执行时更新实况

    // 4. regenerateWeek(semesterId, weekNumber) — 重新生成某一周
}
```

关键逻辑:
- 读 academic_weeks 获取每周的 start_date/end_date
- 对每条 schedule_entry，按 weekday 计算 actual_date
- 处理 week_type (0每周/1单周/2双周) 过滤
- actual_hours = end_slot - start_slot + 1

**Step 2: 编译验证**

**Step 3: Commit**

---

### Task 4: 后端 — 实况课表 REST API

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/TeachingScheduleController.java`

**Step 1: 添加端点**

```
GET  /teaching/instances?semesterId=X&date=2026-04-07          → 按日期查
GET  /teaching/instances?semesterId=X&weekNumber=5             → 按教学周查
GET  /teaching/instances?semesterId=X&teacherId=X&weekNumber=5 → 教师某周
GET  /teaching/instances?semesterId=X&classId=X&weekNumber=5   → 班级某周
GET  /teaching/instances?semesterId=X&classroomId=X&date=X     → 场所某天
POST /teaching/instances/generate     → {semesterId} 生成全学期实况
POST /teaching/instances/apply-event  → {eventId} 应用校历事件
```

**Step 2: Commit**

---

### Task 5: 后端 — 课时统计 API

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/teaching/HoursStatisticsController.java`

**Step 1: 实现**

```
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher
GET /teaching/statistics/hours?semesterId=X&groupBy=class
GET /teaching/statistics/hours?semesterId=X&groupBy=course
GET /teaching/statistics/hours?semesterId=X&groupBy=classroom
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher&period=week&weekNumber=5
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher&period=month&month=3
```

核心 SQL:
```sql
SELECT 
  teacher_id,
  SUM(CASE WHEN status IN (0,3,4) THEN actual_hours ELSE 0 END) AS actual_hours,
  SUM(CASE WHEN status = 1 THEN actual_hours ELSE 0 END) AS cancelled_hours,
  SUM(CASE WHEN status = 3 THEN actual_hours ELSE 0 END) AS substitute_hours,
  SUM(CASE WHEN status = 4 THEN actual_hours ELSE 0 END) AS proxy_hours,
  COUNT(*) AS total_instances
FROM schedule_instances
WHERE semester_id = ? AND deleted = 0
GROUP BY teacher_id
```

**Step 2: Commit**

---

### Task 6: 前端 — API 定义 + TypeScript 类型

**Files:**
- Modify: `frontend/src/api/calendar.ts` — 添加 periodConfigApi
- Modify: `frontend/src/api/teaching.ts` — 添加 instanceApi, hoursApi
- Modify: `frontend/src/types/teaching.ts` — 添加 ScheduleInstance, HoursStatItem 类型

**Step 1: 类型定义**

```typescript
export interface ScheduleInstance {
  id: number | string
  entryId?: number | string
  semesterId: number | string
  actualDate: string        // YYYY-MM-DD
  weekday: number
  weekNumber: number
  startSlot: number
  endSlot: number
  courseId: number | string
  courseName?: string
  classId: number | string
  className?: string
  teacherId?: number | string
  teacherName?: string
  originalTeacherId?: number | string
  classroomId?: number | string
  classroomName?: string
  status: number            // 0正常 1取消 2调走 3补课 4代课
  cancelReason?: string
  sourceType: number
  actualHours: number
}

export interface HoursStatItem {
  id: number | string
  name: string
  totalHours: number
  actualHours: number
  cancelledHours: number
  substituteHours: number
  proxyHours: number
  proxiedHours: number
  attendanceRate: string
}
```

**Step 2: API 定义**

```typescript
export const instanceApi = {
  listByDate: (semesterId, date) => http.get('/teaching/instances', { params: { semesterId, date } }),
  listByWeek: (semesterId, weekNumber, filters?) => http.get('/teaching/instances', { params: { semesterId, weekNumber, ...filters } }),
  generate: (semesterId) => http.post('/teaching/instances/generate', { semesterId }),
  applyEvent: (eventId) => http.post('/teaching/instances/apply-event', { eventId }),
}

export const hoursApi = {
  getStatistics: (params) => http.get('/teaching/statistics/hours', { params }),
}
```

**Step 3: Commit**

---

### Task 7: 验证 — 用 curl 跑通全链路

**Step 1: 创建作息表**
```bash
curl -X POST /calendar/period-configs -d '{semesterId, periodsPerDay:8, ...}'
```

**Step 2: 生成实况课表**
```bash
curl -X POST /teaching/instances/generate -d '{semesterId}'
```

**Step 3: 查询实况**
```bash
curl /teaching/instances?semesterId=X&weekNumber=1
```

**Step 4: 查询课时统计**
```bash
curl /teaching/statistics/hours?semesterId=X&groupBy=teacher
```

**Step 5: Commit all remaining changes**

---

## Phase 1 完成标志

- [x] period_configs 表可 CRUD
- [x] schedule_instances 表可查询
- [x] 从基准课表生成实况(20条 entries → ~400条 instances)
- [x] 校历事件可影响实况(放假→批量取消)
- [x] 课时统计按教师/班级/课程/场所维度可查
- [x] 前端 TypeScript 类型和 API 定义就绪
