# 校历重构：事件驱动校历表 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 让校历事件（假期、补课、考试）真正影响教学日历计算，并提供周×日网格校历表视图。

**Architecture:** 复用现有 `academic_event` 表，补齐后端 `affect_type` / `substitute_weekday` 字段使事件具备排课语义。新增 `/calendar-grid` 只读 API 将学期日期 + 事件覆盖计算为每日类型。前端新增 `CalendarGrid.vue` 组件渲染周×日网格。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus / Vue 3 + TypeScript

---

## 现状分析

### 前端已有但后端缺失
- `CalendarEvents.vue` 已有 `affectType`（0=无影响/1=全天停课/2=半天停课/3=补课日/4=考试周）
- `CalendarEvents.vue` 已有 `substituteWeekday`（补课日按周几课表）
- 这些字段前端发了，后端没存，静默丢弃

### 需要改的
1. 后端 `academic_event` 表加 2 列
2. 后端 `AcademicEvent` 领域模型加 2 字段
3. 新增只读 API：`GET /calendar/semesters/{id}/calendar-grid`
4. 前端新增 `CalendarGrid.vue` 校历表组件
5. 统计数据从 calendar-grid 计算

---

## Task 1: 数据库 — academic_event 加列

**Files:**
- Create: `database/migrations/V20260410_1__event_affect_fields.sql`
- Modify: `database/schema/teaching_academic_complete_schema.sql`

**Step 1: 写迁移脚本**

```sql
-- 给 academic_event 加排课影响字段
ALTER TABLE `academic_event`
    ADD COLUMN `affect_type` TINYINT DEFAULT 0 COMMENT '排课影响: 0=无 1=全天停课 2=半天停课 3=补课日 4=考试周' AFTER `description`,
    ADD COLUMN `substitute_weekday` TINYINT NULL COMMENT '补课日按周几课表(1=周一...5=周五)' AFTER `affect_type`,
    ADD COLUMN `affect_slots` VARCHAR(20) NULL COMMENT '半天停课节次范围(如1-4)' AFTER `substitute_weekday`;

-- 回填：eventType=1(放假/HOLIDAY) 且 affect_type=0 的 → 设为全天停课
UPDATE `academic_event`
SET `affect_type` = 1
WHERE `event_type` = 2 AND `affect_type` = 0 AND `deleted` = 0;
```

注意：eventType 枚举对照：1=OPENING, **2=HOLIDAY**, 3=EXAM, 4=ACTIVITY, 5=OTHER

**Step 2: 更新权威 schema**

在 `teaching_academic_complete_schema.sql` 的 `academic_event` 建表语句里加：
```sql
`affect_type` TINYINT DEFAULT 0 COMMENT '排课影响: 0=无 1=全天停课 2=半天停课 3=补课日 4=考试周',
`substitute_weekday` TINYINT NULL COMMENT '补课日按周几课表(1-5)',
`affect_slots` VARCHAR(20) NULL COMMENT '半天停课节次(如1-4)',
```

**Step 3: 执行迁移**

```bash
mysql -u root -p123456 student_management < database/migrations/V20260410_1__event_affect_fields.sql
```

**Step 4: 验证**

```bash
mysql -u root -p123456 student_management -e "DESCRIBE academic_event;"
```

Expected: 看到 `affect_type`, `substitute_weekday`, `affect_slots` 三列

---

## Task 2: 后端领域模型 — AcademicEvent 加字段

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/calendar/model/entity/AcademicEvent.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/calendar/AcademicEventPO.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/calendar/AcademicEventRepositoryImpl.java`

**Step 1: AcademicEvent 领域模型加字段**

加三个字段：
```java
private Integer affectType;        // 0=无 1=全天停课 2=半天停课 3=补课日 4=考试周
private Integer substituteWeekday; // 补课日按周几(1-5)
private String affectSlots;        // 半天停课节次
```

更新 `create()` 和 `reconstruct()` 方法签名，加入这三个参数。
更新 `update()` 方法签名，加入这三个参数。
加 getter。

**Step 2: AcademicEventPO 加字段**

```java
private Integer affectType;
private Integer substituteWeekday;
private String affectSlots;
```

**Step 3: AcademicEventRepositoryImpl 更新 toPO/toDomain 映射**

在 `toPO()` 中加：
```java
po.setAffectType(d.getAffectType());
po.setSubstituteWeekday(d.getSubstituteWeekday());
po.setAffectSlots(d.getAffectSlots());
```

在 `toDomain()` 的 `reconstruct()` 调用中加入对应参数。

**Step 4: 编译验证**

```bash
cd backend && mvn compile -DskipTests
```

Expected: BUILD SUCCESS

---

## Task 3: 后端 CalendarApplicationService 更新事件创建

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/calendar/command/CreateAcademicEventCommand.java`
- Modify: `backend/src/main/java/com/school/management/application/calendar/command/UpdateAcademicEventCommand.java`
- Modify: `backend/src/main/java/com/school/management/application/calendar/CalendarApplicationService.java`

**Step 1: CreateAcademicEventCommand 加字段**

```java
private Integer affectType;
private Integer substituteWeekday;
private String affectSlots;
```

**Step 2: UpdateAcademicEventCommand 同样加字段**

**Step 3: CalendarApplicationService.createEvent() 传递新字段**

将 `command.getAffectType()` 等传入 `AcademicEvent.create()`。

**Step 4: CalendarApplicationService.updateEvent() 传递新字段**

将新字段传入 `event.update()`。

**Step 5: 编译验证**

---

## Task 4: 后端新增 CalendarGrid 只读 API

**Files:**
- Create: `backend/src/main/java/com/school/management/application/calendar/query/CalendarDayDTO.java`
- Create: `backend/src/main/java/com/school/management/application/calendar/query/CalendarWeekDTO.java`
- Create: `backend/src/main/java/com/school/management/application/calendar/query/CalendarGridDTO.java`
- Modify: `backend/src/main/java/com/school/management/application/calendar/CalendarApplicationService.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/calendar/AcademicCalendarController.java`

**Step 1: 创建 DTO**

```java
// CalendarDayDTO.java
@Data @Builder
public class CalendarDayDTO {
    private String date;         // "2025-10-01"
    private int weekday;         // 1=周一...7=周日
    private String dayType;      // TEACHING / WEEKEND / HOLIDAY / MAKEUP / EXAM
    private String eventName;    // "国庆节" or null
    private Integer followWeekday; // 补课日按周几(1-5) or null
}

// CalendarWeekDTO.java
@Data @Builder
public class CalendarWeekDTO {
    private int weekNumber;
    private String startDate;
    private String endDate;
    private List<CalendarDayDTO> days;  // 固定7天
}

// CalendarGridDTO.java
@Data @Builder
public class CalendarGridDTO {
    private List<CalendarWeekDTO> weeks;
    private int totalTeachingDays;
    private int totalHolidayDays;
    private int totalMakeupDays;
    private int totalExamDays;
}
```

**Step 2: CalendarApplicationService 新增 buildCalendarGrid 方法**

逻辑：
1. 加载学期起止日期
2. 按7天切成周（连续编号）
3. 默认 Mon-Fri = TEACHING, Sat-Sun = WEEKEND
4. 加载学期所有事件（带 affectType）
5. 遍历事件，展开日期范围，覆盖对应天的 dayType：
   - affectType=1 → dayType=HOLIDAY
   - affectType=3 → dayType=MAKEUP, followWeekday=event.substituteWeekday
   - affectType=4 → dayType=EXAM
6. 统计各类天数

```java
@Transactional(readOnly = true)
public CalendarGridDTO buildCalendarGrid(Long semesterId) {
    Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new BusinessException("学期不存在"));
    List<AcademicEvent> events = academicEventRepository
            .findAll(null, semesterId, null);

    // 构建日期→事件覆盖 map
    Map<LocalDate, AcademicEvent> overrideMap = new LinkedHashMap<>();
    for (AcademicEvent e : events) {
        if (e.getAffectType() == null || e.getAffectType() == 0) continue;
        LocalDate d = e.getStartDate();
        LocalDate end = e.getEndDate() != null ? e.getEndDate() : d;
        while (!d.isAfter(end)) {
            overrideMap.put(d, e);
            d = d.plusDays(1);
        }
    }

    // 按7天生成周
    List<CalendarWeekDTO> weeks = new ArrayList<>();
    LocalDate weekStart = semester.getStartDate();
    int weekNum = 1;
    int teachDays = 0, holidayDays = 0, makeupDays = 0, examDays = 0;

    while (!weekStart.isAfter(semester.getEndDate())) {
        List<CalendarDayDTO> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            if (date.isAfter(semester.getEndDate())) break;

            int weekday = date.getDayOfWeek().getValue(); // 1=Mon...7=Sun
            String dayType;
            String eventName = null;
            Integer followWeekday = null;

            AcademicEvent override = overrideMap.get(date);
            if (override != null) {
                switch (override.getAffectType()) {
                    case 1: dayType = "HOLIDAY"; holidayDays++; break;
                    case 3: dayType = "MAKEUP"; makeupDays++;
                            followWeekday = override.getSubstituteWeekday(); break;
                    case 4: dayType = "EXAM"; examDays++; break;
                    default: dayType = weekday <= 5 ? "TEACHING" : "WEEKEND";
                }
                eventName = override.getEventName();
            } else {
                dayType = weekday <= 5 ? "TEACHING" : "WEEKEND";
            }
            if ("TEACHING".equals(dayType)) teachDays++;

            days.add(CalendarDayDTO.builder()
                    .date(date.toString()).weekday(weekday)
                    .dayType(dayType).eventName(eventName)
                    .followWeekday(followWeekday).build());
        }
        LocalDate weekEnd = weekStart.plusDays(6);
        if (weekEnd.isAfter(semester.getEndDate())) weekEnd = semester.getEndDate();
        weeks.add(CalendarWeekDTO.builder()
                .weekNumber(weekNum).startDate(weekStart.toString())
                .endDate(weekEnd.toString()).days(days).build());
        weekStart = weekStart.plusDays(7);
        weekNum++;
    }

    return CalendarGridDTO.builder()
            .weeks(weeks)
            .totalTeachingDays(teachDays)
            .totalHolidayDays(holidayDays)
            .totalMakeupDays(makeupDays)
            .totalExamDays(examDays)
            .build();
}
```

**Step 3: Controller 加端点**

```java
@GetMapping("/semesters/{semesterId}/calendar-grid")
@CasbinAccess(resource = "calendar", action = "view")
public Result<CalendarGridDTO> getCalendarGrid(@PathVariable Long semesterId) {
    return Result.success(calendarService.buildCalendarGrid(semesterId));
}
```

**Step 4: 编译 + 手动测试 API**

```bash
curl http://localhost:8080/api/calendar/semesters/{id}/calendar-grid
```

---

## Task 5: 前端 API + 类型

**Files:**
- Modify: `frontend/src/api/calendar.ts`
- Modify: `frontend/src/types/teaching.ts`

**Step 1: types/teaching.ts 加类型**

```typescript
export interface CalendarDay {
  date: string
  weekday: number       // 1-7
  dayType: 'TEACHING' | 'WEEKEND' | 'HOLIDAY' | 'MAKEUP' | 'EXAM'
  eventName?: string
  followWeekday?: number
}

export interface CalendarWeek {
  weekNumber: number
  startDate: string
  endDate: string
  days: CalendarDay[]
}

export interface CalendarGrid {
  weeks: CalendarWeek[]
  totalTeachingDays: number
  totalHolidayDays: number
  totalMakeupDays: number
  totalExamDays: number
}
```

**Step 2: api/calendar.ts 加方法**

在 `semesterApi` 中加：
```typescript
getCalendarGrid(semesterId: number | string): Promise<CalendarGrid> {
  return request.get(`/calendar/semesters/${semesterId}/calendar-grid`).then(r => r.data)
}
```

---

## Task 6: 前端 CalendarGrid.vue 校历表组件

**Files:**
- Create: `frontend/src/views/teaching/calendar-new/CalendarGrid.vue`

这是核心 UI 组件 — 周×日网格，颜色区分日类型。

**Props:**
```typescript
props<{ semesterId: number | string | undefined }>
```

**主要逻辑:**
1. 调用 `semesterApi.getCalendarGrid(semesterId)` 获取数据
2. 渲染 table：行=周, 列=周一~周日
3. 每个单元格显示日期 + 颜色（白=教学, 灰=休息, 红=假期, 橙=补课, 紫=考试）
4. hover 显示事件名称
5. 底部统计条：教学X天 | 假期X天 | 补课X天 | 考试X天

**样式映射:**
- TEACHING: `background: #fff; color: #111`
- WEEKEND: `background: #f3f4f6; color: #9ca3af`
- HOLIDAY: `background: #fef2f2; color: #dc2626`
- MAKEUP: `background: #fff7ed; color: #ea580c`
- EXAM: `background: #f5f3ff; color: #7c3aed`

---

## Task 7: CalendarCenter.vue 集成校历表 tab

**Files:**
- Modify: `frontend/src/views/teaching/CalendarCenter.vue`

**Step 1: 加 tab**

在 tabs 区域加一个"校历表"按钮（插在"学期总览"后面）：
```html
<button :class="['tm-tab', { active: tab === 'grid' }]" @click="tab = 'grid'">校历表</button>
```

**Step 2: 加组件引用**

```typescript
import CalendarGrid from './calendar-new/CalendarGrid.vue'
```

**Step 3: 在内容区渲染**

```html
<CalendarGrid v-else-if="tab === 'grid'" :semester-id="semesterId" />
```

---

## Task 8: SemesterOverview 统计从 CalendarGrid 数据计算

**Files:**
- Modify: `frontend/src/views/teaching/calendar-new/SemesterOverview.vue`
- Modify: `frontend/src/views/teaching/CalendarCenter.vue`

**改动:**
1. CalendarCenter 加载 calendarGrid 数据，传给 SemesterOverview
2. SemesterOverview 的统计卡片改用 grid.totalHolidayDays / totalMakeupDays 等真实数据
3. 不再从 events.length 估算

---

## 执行顺序

```
Task 1 (DB)  →  Task 2 (后端模型)  →  Task 3 (后端服务)  →  Task 4 (后端API)
                                                                    ↓
Task 8 (Overview统计) ← Task 7 (集成tab) ← Task 6 (Grid组件) ← Task 5 (前端类型+API)
```

Task 1-4 顺序执行（有依赖），Task 5-8 顺序执行。
