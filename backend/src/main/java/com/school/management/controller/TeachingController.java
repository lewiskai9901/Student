package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.entity.teaching.AcademicEvent;
import com.school.management.service.teaching.AcademicEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教务管理控制器
 * 校历事件已接入数据库，其他模块暂用模拟数据
 */
@Slf4j
@RestController
@RequestMapping("/v2/teaching")
@Tag(name = "教务管理", description = "教务管理相关接口")
@RequiredArgsConstructor
public class TeachingController {

    private final AcademicEventService academicEventService;

    // ==================== 学年管理 ====================

    @GetMapping("/academic-years")
    @Operation(summary = "获取学年列表")
    public Result<List<AcademicYearDTO>> listAcademicYears() {
        List<AcademicYearDTO> years = new ArrayList<>();

        AcademicYearDTO year1 = new AcademicYearDTO();
        year1.setId(1L);
        year1.setYearName("2025-2026学年");
        year1.setStartDate(LocalDate.of(2025, 9, 1));
        year1.setEndDate(LocalDate.of(2026, 7, 31));
        year1.setIsCurrent(true);
        year1.setStatus(1);
        years.add(year1);

        AcademicYearDTO year2 = new AcademicYearDTO();
        year2.setId(2L);
        year2.setYearName("2024-2025学年");
        year2.setStartDate(LocalDate.of(2024, 9, 1));
        year2.setEndDate(LocalDate.of(2025, 7, 31));
        year2.setIsCurrent(false);
        year2.setStatus(1);
        years.add(year2);

        return Result.success(years);
    }

    @GetMapping("/academic-years/current")
    @Operation(summary = "获取当前学年")
    public Result<AcademicYearDTO> getCurrentAcademicYear() {
        AcademicYearDTO year = new AcademicYearDTO();
        year.setId(1L);
        year.setYearName("2025-2026学年");
        year.setStartDate(LocalDate.of(2025, 9, 1));
        year.setEndDate(LocalDate.of(2026, 7, 31));
        year.setIsCurrent(true);
        year.setStatus(1);
        return Result.success(year);
    }

    @GetMapping("/academic-years/{id}")
    @Operation(summary = "获取学年详情")
    public Result<AcademicYearDTO> getAcademicYear(@PathVariable Long id) {
        AcademicYearDTO year = new AcademicYearDTO();
        year.setId(id);
        year.setYearName("2025-2026学年");
        year.setStartDate(LocalDate.of(2025, 9, 1));
        year.setEndDate(LocalDate.of(2026, 7, 31));
        year.setIsCurrent(true);
        year.setStatus(1);
        return Result.success(year);
    }

    @PostMapping("/academic-years")
    @Operation(summary = "创建学年")
    public Result<AcademicYearDTO> createAcademicYear(@RequestBody AcademicYearDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @PutMapping("/academic-years/{id}")
    @Operation(summary = "更新学年")
    public Result<AcademicYearDTO> updateAcademicYear(@PathVariable Long id, @RequestBody AcademicYearDTO dto) {
        dto.setId(id);
        return Result.success(dto);
    }

    @DeleteMapping("/academic-years/{id}")
    @Operation(summary = "删除学年")
    public Result<Void> deleteAcademicYear(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/academic-years/{id}/set-current")
    @Operation(summary = "设置当前学年")
    public Result<Void> setCurrentAcademicYear(@PathVariable Long id) {
        return Result.success();
    }

    // ==================== 学期管理 ====================

    @GetMapping("/semesters")
    @Operation(summary = "获取学期列表")
    public Result<List<SemesterDTO>> listSemesters(@RequestParam(required = false) Long yearId) {
        List<SemesterDTO> semesters = new ArrayList<>();

        SemesterDTO sem1 = new SemesterDTO();
        sem1.setId(1L);
        sem1.setYearId(1L);
        sem1.setSemesterName("2025-2026学年第一学期");
        sem1.setSemesterType(1);
        sem1.setStartDate(LocalDate.of(2025, 9, 1));
        sem1.setEndDate(LocalDate.of(2026, 1, 15));
        sem1.setWeekCount(20);
        sem1.setIsCurrent(false);
        sem1.setStatus(1);
        semesters.add(sem1);

        SemesterDTO sem2 = new SemesterDTO();
        sem2.setId(2L);
        sem2.setYearId(1L);
        sem2.setSemesterName("2025-2026学年第二学期");
        sem2.setSemesterType(2);
        sem2.setStartDate(LocalDate.of(2026, 2, 17));
        sem2.setEndDate(LocalDate.of(2026, 7, 10));
        sem2.setWeekCount(20);
        sem2.setIsCurrent(true);
        sem2.setStatus(1);
        semesters.add(sem2);

        return Result.success(semesters);
    }

    @GetMapping("/semesters/current")
    @Operation(summary = "获取当前学期")
    public Result<SemesterDTO> getCurrentSemester() {
        SemesterDTO sem = new SemesterDTO();
        sem.setId(2L);
        sem.setYearId(1L);
        sem.setSemesterName("2025-2026学年第二学期");
        sem.setSemesterType(2);
        sem.setStartDate(LocalDate.of(2026, 2, 17));
        sem.setEndDate(LocalDate.of(2026, 7, 10));
        sem.setWeekCount(20);
        sem.setIsCurrent(true);
        sem.setStatus(1);
        return Result.success(sem);
    }

    @GetMapping("/semesters/{id}")
    @Operation(summary = "获取学期详情")
    public Result<SemesterDTO> getSemester(@PathVariable Long id) {
        SemesterDTO sem = new SemesterDTO();
        sem.setId(id);
        sem.setYearId(1L);
        sem.setSemesterName("2025-2026学年第二学期");
        sem.setSemesterType(2);
        sem.setStartDate(LocalDate.of(2026, 2, 17));
        sem.setEndDate(LocalDate.of(2026, 7, 10));
        sem.setWeekCount(20);
        sem.setIsCurrent(true);
        sem.setStatus(1);
        return Result.success(sem);
    }

    @PostMapping("/semesters")
    @Operation(summary = "创建学期")
    public Result<SemesterDTO> createSemester(@RequestBody SemesterDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @PutMapping("/semesters/{id}")
    @Operation(summary = "更新学期")
    public Result<SemesterDTO> updateSemester(@PathVariable Long id, @RequestBody SemesterDTO dto) {
        dto.setId(id);
        return Result.success(dto);
    }

    @DeleteMapping("/semesters/{id}")
    @Operation(summary = "删除学期")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/semesters/{id}/set-current")
    @Operation(summary = "设置当前学期")
    public Result<Void> setCurrentSemester(@PathVariable Long id) {
        return Result.success();
    }

    @GetMapping("/semesters/{semesterId}/weeks")
    @Operation(summary = "获取学期教学周")
    public Result<List<TeachingWeekDTO>> getSemesterWeeks(@PathVariable Long semesterId) {
        List<TeachingWeekDTO> weeks = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2026, 2, 17);
        for (int i = 1; i <= 20; i++) {
            TeachingWeekDTO week = new TeachingWeekDTO();
            week.setId((long) i);
            week.setSemesterId(semesterId);
            week.setWeekNumber(i);
            week.setStartDate(startDate.plusWeeks(i - 1));
            week.setEndDate(startDate.plusWeeks(i).minusDays(1));
            week.setWeekType(1);
            weeks.add(week);
        }
        return Result.success(weeks);
    }

    @PostMapping("/semesters/{semesterId}/generate-weeks")
    @Operation(summary = "生成教学周")
    public Result<List<TeachingWeekDTO>> generateWeeks(@PathVariable Long semesterId) {
        return getSemesterWeeks(semesterId);
    }

    // ==================== 校历事件 (已接入数据库) ====================

    @GetMapping("/events")
    @Operation(summary = "获取校历事件列表")
    public Result<List<AcademicEventDTO>> listEvents(
            @RequestParam(required = false) Long yearId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer eventType) {
        List<AcademicEvent> events = academicEventService.listEvents(yearId, semesterId, eventType);
        List<AcademicEventDTO> dtos = events.stream()
                .map(this::toEventDTO)
                .collect(Collectors.toList());
        return Result.success(dtos);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "获取事件详情")
    public Result<AcademicEventDTO> getEvent(@PathVariable Long id) {
        AcademicEvent event = academicEventService.getById(id);
        if (event == null) {
            return Result.error("事件不存在");
        }
        return Result.success(toEventDTO(event));
    }

    @PostMapping("/events")
    @Operation(summary = "创建事件")
    public Result<AcademicEventDTO> createEvent(@RequestBody AcademicEventDTO dto) {
        AcademicEvent event = toEventEntity(dto);
        AcademicEvent created = academicEventService.createEvent(event);
        return Result.success(toEventDTO(created));
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "更新事件")
    public Result<AcademicEventDTO> updateEvent(@PathVariable Long id, @RequestBody AcademicEventDTO dto) {
        AcademicEvent event = toEventEntity(dto);
        AcademicEvent updated = academicEventService.updateEvent(id, event);
        return Result.success(toEventDTO(updated));
    }

    @DeleteMapping("/events/{id}")
    @Operation(summary = "删除事件")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        academicEventService.deleteEvent(id);
        return Result.success();
    }

    private AcademicEventDTO toEventDTO(AcademicEvent entity) {
        AcademicEventDTO dto = new AcademicEventDTO();
        dto.setId(entity.getId());
        dto.setYearId(entity.getYearId());
        dto.setSemesterId(entity.getSemesterId());
        dto.setEventName(entity.getEventName());
        dto.setEventType(entity.getEventType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private AcademicEvent toEventEntity(AcademicEventDTO dto) {
        AcademicEvent entity = new AcademicEvent();
        entity.setYearId(dto.getYearId());
        entity.setSemesterId(dto.getSemesterId());
        entity.setEventName(dto.getEventName());
        entity.setEventType(dto.getEventType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setDescription(dto.getDescription());
        entity.setAllDay(true);
        return entity;
    }

    // ==================== 课程管理 ====================

    @GetMapping("/courses")
    @Operation(summary = "获取课程列表")
    public Result<PageResultDTO<CourseDTO>> listCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer courseType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<CourseDTO> courses = new ArrayList<>();

        CourseDTO course1 = new CourseDTO();
        course1.setId(1L);
        course1.setCode("CS101");
        course1.setName("计算机基础");
        course1.setCourseType(1);
        course1.setCredits(3.0);
        course1.setTotalHours(48);
        course1.setTheoryHours(32);
        course1.setPracticeHours(16);
        course1.setStatus(1);
        courses.add(course1);

        CourseDTO course2 = new CourseDTO();
        course2.setId(2L);
        course2.setCode("CS102");
        course2.setName("程序设计");
        course2.setCourseType(1);
        course2.setCredits(4.0);
        course2.setTotalHours(64);
        course2.setTheoryHours(48);
        course2.setPracticeHours(16);
        course2.setStatus(1);
        courses.add(course2);

        CourseDTO course3 = new CourseDTO();
        course3.setId(3L);
        course3.setCode("MA101");
        course3.setName("高等数学");
        course3.setCourseType(1);
        course3.setCredits(5.0);
        course3.setTotalHours(64);
        course3.setTheoryHours(64);
        course3.setPracticeHours(0);
        course3.setStatus(1);
        courses.add(course3);

        PageResultDTO<CourseDTO> result = new PageResultDTO<>();
        result.setContent(courses);
        result.setTotalElements(3L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @GetMapping("/courses/all")
    @Operation(summary = "获取所有课程")
    public Result<List<CourseDTO>> listAllCourses() {
        return Result.success(listCourses(null, null, null, 1, 100).getData().getRecords());
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "获取课程详情")
    public Result<CourseDTO> getCourse(@PathVariable Long id) {
        CourseDTO course = new CourseDTO();
        course.setId(id);
        course.setCode("CS101");
        course.setName("计算机基础");
        course.setCourseType(1);
        course.setCredits(3.0);
        course.setTotalHours(48);
        course.setTheoryHours(32);
        course.setPracticeHours(16);
        course.setStatus(1);
        return Result.success(course);
    }

    @PostMapping("/courses")
    @Operation(summary = "创建课程")
    public Result<CourseDTO> createCourse(@RequestBody CourseDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @PutMapping("/courses/{id}")
    @Operation(summary = "更新课程")
    public Result<CourseDTO> updateCourse(@PathVariable Long id, @RequestBody CourseDTO dto) {
        dto.setId(id);
        return Result.success(dto);
    }

    @DeleteMapping("/courses/{id}")
    @Operation(summary = "删除课程")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        return Result.success();
    }

    @PatchMapping("/courses/{id}/status")
    @Operation(summary = "更新课程状态")
    public Result<Void> updateCourseStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return Result.success();
    }

    // ==================== 培养方案 ====================

    @GetMapping("/curriculum-plans")
    @Operation(summary = "获取培养方案列表")
    public Result<PageResultDTO<CurriculumPlanDTO>> listCurriculumPlans(
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer enrollYear,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<CurriculumPlanDTO> plans = new ArrayList<>();

        CurriculumPlanDTO plan1 = new CurriculumPlanDTO();
        plan1.setId(1L);
        plan1.setName("计算机科学与技术专业2024级培养方案");
        plan1.setMajorId(1L);
        plan1.setMajorName("计算机科学与技术");
        plan1.setGradeYear(2024);
        plan1.setVersion("v1.0");
        plan1.setDuration(4);
        plan1.setTotalCredits(160.0);
        plan1.setObjectives("培养具备计算机科学与技术专业知识的高级人才");
        plan1.setStatus(1);
        plans.add(plan1);

        PageResultDTO<CurriculumPlanDTO> result = new PageResultDTO<>();
        result.setContent(plans);
        result.setTotalElements(1L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @GetMapping("/curriculum-plans/{id}")
    @Operation(summary = "获取培养方案详情")
    public Result<CurriculumPlanDTO> getCurriculumPlan(@PathVariable Long id) {
        CurriculumPlanDTO plan = new CurriculumPlanDTO();
        plan.setId(id);
        plan.setMajorId(1L);
        plan.setMajorName("计算机科学与技术");
        plan.setGradeYear(2024);
        plan.setDuration(4);
        plan.setTotalCredits(160.0);
        plan.setObjectives("培养具备计算机科学与技术专业知识的高级人才");
        plan.setStatus(1);
        return Result.success(plan);
    }

    @PostMapping("/curriculum-plans")
    @Operation(summary = "创建培养方案")
    public Result<CurriculumPlanDTO> createCurriculumPlan(@RequestBody CurriculumPlanDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @PutMapping("/curriculum-plans/{id}")
    @Operation(summary = "更新培养方案")
    public Result<CurriculumPlanDTO> updateCurriculumPlan(@PathVariable Long id, @RequestBody CurriculumPlanDTO dto) {
        dto.setId(id);
        return Result.success(dto);
    }

    @DeleteMapping("/curriculum-plans/{id}")
    @Operation(summary = "删除培养方案")
    public Result<Void> deleteCurriculumPlan(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/curriculum-plans/{id}/publish")
    @Operation(summary = "发布培养方案")
    public Result<Void> publishCurriculumPlan(@PathVariable Long id) {
        return Result.success();
    }

    @GetMapping("/curriculum-plans/{planId}/courses")
    @Operation(summary = "获取培养方案课程")
    public Result<List<PlanCourseDTO>> getPlanCourses(@PathVariable Long planId) {
        List<PlanCourseDTO> courses = new ArrayList<>();

        PlanCourseDTO pc1 = new PlanCourseDTO();
        pc1.setId(1L);
        pc1.setPlanId(planId);
        pc1.setCourseId(1L);
        pc1.setCourseName("计算机基础");
        pc1.setCourseCode("CS101");
        pc1.setSemester(1);
        pc1.setCredits(3.0);
        pc1.setIsRequired(true);
        courses.add(pc1);

        PlanCourseDTO pc2 = new PlanCourseDTO();
        pc2.setId(2L);
        pc2.setPlanId(planId);
        pc2.setCourseId(2L);
        pc2.setCourseName("程序设计");
        pc2.setCourseCode("CS102");
        pc2.setSemester(1);
        pc2.setCredits(4.0);
        pc2.setIsRequired(true);
        courses.add(pc2);

        return Result.success(courses);
    }

    @PostMapping("/curriculum-plans/{planId}/courses")
    @Operation(summary = "添加培养方案课程")
    public Result<PlanCourseDTO> addPlanCourse(@PathVariable Long planId, @RequestBody PlanCourseDTO dto) {
        dto.setId(System.currentTimeMillis());
        dto.setPlanId(planId);
        return Result.success(dto);
    }

    @DeleteMapping("/curriculum-plans/{planId}/courses/{courseId}")
    @Operation(summary = "移除培养方案课程")
    public Result<Void> removePlanCourse(@PathVariable Long planId, @PathVariable Long courseId) {
        return Result.success();
    }

    // ==================== 教学任务 ====================

    @GetMapping("/tasks")
    @Operation(summary = "获取教学任务列表")
    public Result<PageResultDTO<TeachingTaskDTO>> listTasks(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<TeachingTaskDTO> tasks = new ArrayList<>();

        TeachingTaskDTO task1 = new TeachingTaskDTO();
        task1.setId(1L);
        task1.setSemesterId(2L);
        task1.setSemesterName("2025-2026学年第二学期");
        task1.setCourseId(1L);
        task1.setCourseName("计算机基础");
        task1.setCourseCode("CS101");
        task1.setClassId(1L);
        task1.setClassName("2024级计算机1班");
        task1.setTeacherId(1L);
        task1.setTeacherName("张老师");
        task1.setWeeklyHours(4);
        task1.setTotalHours(64);
        task1.setStatus(1);
        tasks.add(task1);

        TeachingTaskDTO task2 = new TeachingTaskDTO();
        task2.setId(2L);
        task2.setSemesterId(2L);
        task2.setSemesterName("2025-2026学年第二学期");
        task2.setCourseId(2L);
        task2.setCourseName("程序设计");
        task2.setCourseCode("CS102");
        task2.setClassId(1L);
        task2.setClassName("2024级计算机1班");
        task2.setTeacherId(2L);
        task2.setTeacherName("李老师");
        task2.setWeeklyHours(6);
        task2.setTotalHours(96);
        task2.setStatus(1);
        tasks.add(task2);

        PageResultDTO<TeachingTaskDTO> result = new PageResultDTO<>();
        result.setContent(tasks);
        result.setTotalElements(2L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "获取教学任务详情")
    public Result<TeachingTaskDTO> getTask(@PathVariable Long id) {
        TeachingTaskDTO task = new TeachingTaskDTO();
        task.setId(id);
        task.setSemesterId(2L);
        task.setSemesterName("2025-2026学年第二学期");
        task.setCourseId(1L);
        task.setCourseName("计算机基础");
        task.setCourseCode("CS101");
        task.setClassId(1L);
        task.setClassName("2024级计算机1班");
        task.setTeacherId(1L);
        task.setTeacherName("张老师");
        task.setWeeklyHours(4);
        task.setTotalHours(64);
        task.setStatus(1);
        return Result.success(task);
    }

    @PostMapping("/tasks")
    @Operation(summary = "创建教学任务")
    public Result<TeachingTaskDTO> createTask(@RequestBody TeachingTaskDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "更新教学任务")
    public Result<TeachingTaskDTO> updateTask(@PathVariable Long id, @RequestBody TeachingTaskDTO dto) {
        dto.setId(id);
        return Result.success(dto);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除教学任务")
    public Result<Void> deleteTask(@PathVariable Long id) {
        return Result.success();
    }

    // ==================== 排课管理 ====================

    @GetMapping("/schedules")
    @Operation(summary = "获取课表列表")
    public Result<List<CourseScheduleDTO>> listSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        List<CourseScheduleDTO> schedules = new ArrayList<>();

        CourseScheduleDTO schedule1 = new CourseScheduleDTO();
        schedule1.setId(1L);
        schedule1.setSemesterId(2L);
        schedule1.setSemesterName("2025-2026学年第二学期");
        schedule1.setScheduleName("2025-2026第二学期总课表");
        schedule1.setStatus(1);
        schedules.add(schedule1);

        return Result.success(schedules);
    }

    @GetMapping("/schedules/{id}")
    @Operation(summary = "获取课表详情")
    public Result<CourseScheduleDTO> getSchedule(@PathVariable Long id) {
        CourseScheduleDTO schedule = new CourseScheduleDTO();
        schedule.setId(id);
        schedule.setSemesterId(2L);
        schedule.setSemesterName("2025-2026学年第二学期");
        schedule.setScheduleName("2025-2026第二学期总课表");
        schedule.setStatus(1);
        return Result.success(schedule);
    }

    @PostMapping("/schedules")
    @Operation(summary = "创建课表")
    public Result<CourseScheduleDTO> createSchedule(@RequestBody CourseScheduleDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @GetMapping("/schedules/{scheduleId}/entries")
    @Operation(summary = "获取课表条目")
    public Result<List<ScheduleEntryDTO>> getScheduleEntries(@PathVariable Long scheduleId) {
        List<ScheduleEntryDTO> entries = new ArrayList<>();

        ScheduleEntryDTO entry1 = new ScheduleEntryDTO();
        entry1.setId(1L);
        entry1.setScheduleId(scheduleId);
        entry1.setTaskId(1L);
        entry1.setCourseName("计算机基础");
        entry1.setTeacherName("张老师");
        entry1.setClassName("2024级计算机1班");
        entry1.setClassroomName("教学楼A101");
        entry1.setDayOfWeek(1);
        entry1.setPeriodStart(1);
        entry1.setPeriodEnd(2);
        entry1.setWeekStart(1);
        entry1.setWeekEnd(16);
        entries.add(entry1);

        ScheduleEntryDTO entry2 = new ScheduleEntryDTO();
        entry2.setId(2L);
        entry2.setScheduleId(scheduleId);
        entry2.setTaskId(2L);
        entry2.setCourseName("程序设计");
        entry2.setTeacherName("李老师");
        entry2.setClassName("2024级计算机1班");
        entry2.setClassroomName("教学楼A102");
        entry2.setDayOfWeek(2);
        entry2.setPeriodStart(3);
        entry2.setPeriodEnd(4);
        entry2.setWeekStart(1);
        entry2.setWeekEnd(16);
        entries.add(entry2);

        return Result.success(entries);
    }

    @PostMapping("/schedules/{scheduleId}/entries")
    @Operation(summary = "添加课表条目")
    public Result<ScheduleEntryDTO> addScheduleEntry(@PathVariable Long scheduleId, @RequestBody ScheduleEntryDTO dto) {
        dto.setId(System.currentTimeMillis());
        dto.setScheduleId(scheduleId);
        return Result.success(dto);
    }

    @GetMapping("/schedules/by-class/{classId}")
    @Operation(summary = "按班级查询课表")
    public Result<List<ScheduleEntryDTO>> getScheduleByClass(@PathVariable Long classId, @RequestParam Long semesterId) {
        return getScheduleEntries(1L);
    }

    // ==================== 调课管理 ====================

    @GetMapping("/adjustments")
    @Operation(summary = "获取调课申请列表")
    public Result<PageResultDTO<ScheduleAdjustmentDTO>> listAdjustments(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<ScheduleAdjustmentDTO> adjustments = new ArrayList<>();

        ScheduleAdjustmentDTO adj1 = new ScheduleAdjustmentDTO();
        adj1.setId(1L);
        adj1.setEntryId(1L);
        adj1.setCourseName("计算机基础");
        adj1.setTeacherName("张老师");
        adj1.setAdjustmentType(1);
        adj1.setOriginalDate(LocalDate.of(2026, 3, 2));
        adj1.setNewDate(LocalDate.of(2026, 3, 9));
        adj1.setReason("教师出差");
        adj1.setStatus(0);
        adj1.setApplicantName("张老师");
        adjustments.add(adj1);

        PageResultDTO<ScheduleAdjustmentDTO> result = new PageResultDTO<>();
        result.setContent(adjustments);
        result.setTotalElements(1L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @PostMapping("/adjustments")
    @Operation(summary = "申请调课")
    public Result<ScheduleAdjustmentDTO> applyAdjustment(@RequestBody ScheduleAdjustmentDTO dto) {
        dto.setId(System.currentTimeMillis());
        dto.setStatus(0);
        return Result.success(dto);
    }

    @PostMapping("/adjustments/{id}/approve")
    @Operation(summary = "审批通过")
    public Result<Void> approveAdjustment(@PathVariable Long id) {
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/reject")
    @Operation(summary = "审批拒绝")
    public Result<Void> rejectAdjustment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success();
    }

    @GetMapping("/adjustments/my-applications")
    @Operation(summary = "获取我的调课申请")
    public Result<PageResultDTO<ScheduleAdjustmentDTO>> getMyApplications(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 返回空列表（模拟当前用户没有申请）
        PageResultDTO<ScheduleAdjustmentDTO> result = new PageResultDTO<>();
        result.setContent(new ArrayList<>());
        result.setTotalElements(0L);
        result.setTotalPages(0);
        result.setNumber(page);
        result.setSize(size);
        return Result.success(result);
    }

    @GetMapping("/adjustments/pending-approvals")
    @Operation(summary = "获取待审批的调课申请")
    public Result<PageResultDTO<ScheduleAdjustmentDTO>> getPendingApprovals(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        // 返回空列表（模拟没有待审批的申请）
        PageResultDTO<ScheduleAdjustmentDTO> result = new PageResultDTO<>();
        result.setContent(new ArrayList<>());
        result.setTotalElements(0L);
        result.setTotalPages(0);
        result.setNumber(page);
        result.setSize(size);
        return Result.success(result);
    }

    // ==================== 考试管理 ====================

    @GetMapping("/examinations/batches")
    @Operation(summary = "获取考试批次列表")
    public Result<PageResultDTO<ExamBatchDTO>> listExamBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer examType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<ExamBatchDTO> batches = new ArrayList<>();

        ExamBatchDTO batch1 = new ExamBatchDTO();
        batch1.setId(1L);
        batch1.setSemesterId(2L);
        batch1.setSemesterName("2025-2026学年第二学期");
        batch1.setBatchName("期中考试");
        batch1.setExamType(1);
        batch1.setStartDate(LocalDate.of(2026, 4, 13));
        batch1.setEndDate(LocalDate.of(2026, 4, 17));
        batch1.setStatus(1);
        batches.add(batch1);

        PageResultDTO<ExamBatchDTO> result = new PageResultDTO<>();
        result.setContent(batches);
        result.setTotalElements(1L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @GetMapping("/examinations/batches/{id}")
    @Operation(summary = "获取考试批次详情")
    public Result<ExamBatchDTO> getExamBatch(@PathVariable Long id) {
        ExamBatchDTO batch = new ExamBatchDTO();
        batch.setId(id);
        batch.setSemesterId(2L);
        batch.setSemesterName("2025-2026学年第二学期");
        batch.setBatchName("期中考试");
        batch.setExamType(1);
        batch.setStartDate(LocalDate.of(2026, 4, 13));
        batch.setEndDate(LocalDate.of(2026, 4, 17));
        batch.setStatus(1);
        return Result.success(batch);
    }

    @PostMapping("/examinations/batches")
    @Operation(summary = "创建考试批次")
    public Result<ExamBatchDTO> createExamBatch(@RequestBody ExamBatchDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @GetMapping("/examinations/batches/{batchId}/arrangements")
    @Operation(summary = "获取考试安排")
    public Result<List<ExamArrangementDTO>> getExamArrangements(@PathVariable Long batchId) {
        List<ExamArrangementDTO> arrangements = new ArrayList<>();

        ExamArrangementDTO arr1 = new ExamArrangementDTO();
        arr1.setId(1L);
        arr1.setBatchId(batchId);
        arr1.setCourseId(1L);
        arr1.setCourseName("计算机基础");
        arr1.setExamDate(LocalDate.of(2026, 4, 14));
        arr1.setStartTime("09:00");
        arr1.setEndTime("11:00");
        arr1.setDuration(120);
        arr1.setClassroomName("教学楼A101");
        arrangements.add(arr1);

        return Result.success(arrangements);
    }

    @PostMapping("/examinations/batches/{batchId}/arrangements")
    @Operation(summary = "创建考试安排")
    public Result<ExamArrangementDTO> createExamArrangement(@PathVariable Long batchId, @RequestBody ExamArrangementDTO dto) {
        dto.setId(System.currentTimeMillis());
        dto.setBatchId(batchId);
        return Result.success(dto);
    }

    // ==================== 成绩管理 ====================

    @GetMapping("/grades/batches")
    @Operation(summary = "获取成绩批次列表")
    public Result<PageResultDTO<GradeBatchDTO>> listGradeBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        List<GradeBatchDTO> batches = new ArrayList<>();

        GradeBatchDTO batch1 = new GradeBatchDTO();
        batch1.setId(1L);
        batch1.setSemesterId(2L);
        batch1.setSemesterName("2025-2026学年第二学期");
        batch1.setCourseId(1L);
        batch1.setCourseName("计算机基础");
        batch1.setClassId(1L);
        batch1.setClassName("2024级计算机1班");
        batch1.setTeacherName("张老师");
        batch1.setStatus(1);
        batch1.setStudentCount(30);
        batch1.setRecordedCount(28);
        batches.add(batch1);

        PageResultDTO<GradeBatchDTO> result = new PageResultDTO<>();
        result.setContent(batches);
        result.setTotalElements(1L);
        result.setTotalPages(1);
        result.setNumber(page);
        result.setSize(size);

        return Result.success(result);
    }

    @GetMapping("/grades/batches/{id}")
    @Operation(summary = "获取成绩批次详情")
    public Result<GradeBatchDTO> getGradeBatch(@PathVariable Long id) {
        GradeBatchDTO batch = new GradeBatchDTO();
        batch.setId(id);
        batch.setSemesterId(2L);
        batch.setSemesterName("2025-2026学年第二学期");
        batch.setCourseId(1L);
        batch.setCourseName("计算机基础");
        batch.setClassId(1L);
        batch.setClassName("2024级计算机1班");
        batch.setTeacherName("张老师");
        batch.setStatus(1);
        batch.setStudentCount(30);
        batch.setRecordedCount(28);
        return Result.success(batch);
    }

    @PostMapping("/grades/batches")
    @Operation(summary = "创建成绩批次")
    public Result<GradeBatchDTO> createGradeBatch(@RequestBody GradeBatchDTO dto) {
        dto.setId(System.currentTimeMillis());
        return Result.success(dto);
    }

    @GetMapping("/grades/batches/{batchId}/grades")
    @Operation(summary = "获取成绩列表")
    public Result<List<StudentGradeDTO>> getBatchGrades(@PathVariable Long batchId) {
        List<StudentGradeDTO> grades = new ArrayList<>();

        StudentGradeDTO grade1 = new StudentGradeDTO();
        grade1.setId(1L);
        grade1.setBatchId(batchId);
        grade1.setStudentId(1L);
        grade1.setStudentName("张三");
        grade1.setStudentNo("2024001");
        grade1.setTotalScore(85.0);
        grade1.setGradeLevel("良好");
        grade1.setStatus(1);
        grades.add(grade1);

        StudentGradeDTO grade2 = new StudentGradeDTO();
        grade2.setId(2L);
        grade2.setBatchId(batchId);
        grade2.setStudentId(2L);
        grade2.setStudentName("李四");
        grade2.setStudentNo("2024002");
        grade2.setTotalScore(92.0);
        grade2.setGradeLevel("优秀");
        grade2.setStatus(1);
        grades.add(grade2);

        return Result.success(grades);
    }

    @PostMapping("/grades/batches/{batchId}/grades")
    @Operation(summary = "录入成绩")
    public Result<StudentGradeDTO> recordGrade(@PathVariable Long batchId, @RequestBody StudentGradeDTO dto) {
        dto.setId(System.currentTimeMillis());
        dto.setBatchId(batchId);
        return Result.success(dto);
    }

    @GetMapping("/grades/statistics")
    @Operation(summary = "获取成绩统计")
    public Result<GradeStatisticsDTO> getGradeStatistics(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId) {
        GradeStatisticsDTO stats = new GradeStatisticsDTO();
        stats.setTotalCount(30);
        stats.setRecordedCount(28);
        stats.setPassCount(26);
        stats.setPassRate(92.86);
        stats.setAverageScore(78.5);
        stats.setMaxScore(98.0);
        stats.setMinScore(45.0);
        stats.setExcellentCount(8);
        stats.setGoodCount(12);
        stats.setMediumCount(6);
        stats.setPassedCount(2);
        stats.setFailedCount(2);
        return Result.success(stats);
    }

    // ==================== 内部DTO类 ====================

    @Data
    public static class AcademicYearDTO {
        private Long id;
        private String yearName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private Integer status;
    }

    @Data
    public static class SemesterDTO {
        private Long id;
        private Long yearId;
        private String semesterName;
        private Integer semesterType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer weekCount;
        private Boolean isCurrent;
        private Integer status;
    }

    @Data
    public static class TeachingWeekDTO {
        private Long id;
        private Long semesterId;
        private Integer weekNumber;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer weekType;
    }

    @Data
    public static class AcademicEventDTO {
        private Long id;
        private Long yearId;
        private Long semesterId;
        private String eventName;
        private Integer eventType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
    }

    @Data
    public static class CourseDTO {
        private Long id;
        private String code;       // 前端期望 code
        private String name;       // 前端期望 name
        private String englishName;
        private Integer courseType;
        private Double credits;
        private Integer totalHours; // 前端期望 totalHours
        private Integer theoryHours;
        private Integer practiceHours;
        private String description;
        private Integer status;
    }

    @Data
    public static class CurriculumPlanDTO {
        private Long id;
        private String name;         // 方案名称
        private Long majorId;
        private String majorName;
        private Integer gradeYear;   // 适用年级（前端期望 gradeYear）
        private String version;      // 版本号
        private Integer duration;
        private Double totalCredits;
        private String objectives;
        private String requirements;
        private String remark;
        private Integer status;
    }

    @Data
    public static class PlanCourseDTO {
        private Long id;
        private Long planId;
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Integer semester;
        private Double credits;
        private Boolean isRequired;
    }

    @Data
    public static class TeachingTaskDTO {
        private Long id;
        private Long semesterId;
        private String semesterName;
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Long classId;
        private String className;
        private Long teacherId;
        private String teacherName;
        private Integer weeklyHours;
        private Integer totalHours;
        private Integer status;
    }

    @Data
    public static class CourseScheduleDTO {
        private Long id;
        private Long semesterId;
        private String semesterName;
        private String scheduleName;
        private Integer status;
    }

    @Data
    public static class ScheduleEntryDTO {
        private Long id;
        private Long scheduleId;
        private Long taskId;
        private String courseName;
        private String teacherName;
        private String className;
        private Long classroomId;
        private String classroomName;
        private Integer dayOfWeek;
        private Integer periodStart;
        private Integer periodEnd;
        private Integer weekStart;
        private Integer weekEnd;
    }

    @Data
    public static class ScheduleAdjustmentDTO {
        private Long id;
        private Long entryId;
        private String courseName;
        private String teacherName;
        private Integer adjustmentType;
        private LocalDate originalDate;
        private LocalDate newDate;
        private String reason;
        private Integer status;
        private String applicantName;
        private String approverName;
        private String remark;
    }

    @Data
    public static class ExamBatchDTO {
        private Long id;
        private Long semesterId;
        private String semesterName;
        private String batchName;
        private Integer examType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer status;
    }

    @Data
    public static class ExamArrangementDTO {
        private Long id;
        private Long batchId;
        private Long courseId;
        private String courseName;
        private LocalDate examDate;
        private String startTime;
        private String endTime;
        private Integer duration;
        private String classroomName;
    }

    @Data
    public static class GradeBatchDTO {
        private Long id;
        private Long semesterId;
        private String semesterName;
        private Long courseId;
        private String courseName;
        private Long classId;
        private String className;
        private String teacherName;
        private Integer status;
        private Integer studentCount;
        private Integer recordedCount;
    }

    @Data
    public static class StudentGradeDTO {
        private Long id;
        private Long batchId;
        private Long studentId;
        private String studentName;
        private String studentNo;
        private Double totalScore;
        private String gradeLevel;
        private Integer status;
        private String remark;
    }

    @Data
    public static class GradeStatisticsDTO {
        private Integer totalCount;
        private Integer recordedCount;
        private Integer passCount;
        private Double passRate;
        private Double averageScore;
        private Double maxScore;
        private Double minScore;
        private Integer excellentCount;
        private Integer goodCount;
        private Integer mediumCount;
        private Integer passedCount;
        private Integer failedCount;
    }

    @Data
    public static class PageResultDTO<T> {
        private List<T> records;  // 前端期望 records
        private Long total;       // 前端期望 total
        private Integer totalPages;
        private Integer number;
        private Integer size;

        // 兼容方法
        public void setContent(List<T> content) {
            this.records = content;
        }
        public void setTotalElements(Long totalElements) {
            this.total = totalElements;
        }
    }
}
