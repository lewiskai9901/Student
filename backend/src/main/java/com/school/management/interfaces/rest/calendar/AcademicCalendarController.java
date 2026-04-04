package com.school.management.interfaces.rest.calendar;

import com.school.management.application.calendar.CalendarApplicationService;
import com.school.management.application.calendar.command.*;
import com.school.management.common.result.Result;
import com.school.management.domain.calendar.model.aggregate.AcademicYear;
import com.school.management.domain.calendar.model.aggregate.Semester;
import com.school.management.domain.calendar.model.entity.AcademicEvent;
import com.school.management.domain.calendar.model.entity.TeachingWeek;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 校历管理 REST Controller (共享资源)
 *
 * 管理学年、学期、教学周、校历事件的 CRUD。
 * 委托 CalendarApplicationService (DDD) 完成所有业务操作。
 */
@Slf4j
@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class AcademicCalendarController {

    private final CalendarApplicationService calendarService;

    // ==================== 学年管理 ====================

    @GetMapping("/academic-years")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<AcademicYear>> listAcademicYears() {
        return Result.success(calendarService.listAcademicYears());
    }

    @GetMapping("/academic-years/{id}")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<AcademicYear> getAcademicYear(@PathVariable Long id) {
        return calendarService.getAcademicYear(id)
                .map(Result::success)
                .orElse(Result.error("学年不存在"));
    }

    @GetMapping("/academic-years/current")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<AcademicYear> getCurrentAcademicYear() {
        return Result.success(calendarService.getCurrentAcademicYear().orElse(null));
    }

    @PostMapping("/academic-years")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<AcademicYear> createAcademicYear(@RequestBody CreateAcademicYearCommand command) {
        return Result.success(calendarService.createAcademicYear(command));
    }

    @PutMapping("/academic-years/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<AcademicYear> updateAcademicYear(@PathVariable Long id,
                                                    @RequestBody UpdateAcademicYearCommand command) {
        return Result.success(calendarService.updateAcademicYear(id, command));
    }

    @DeleteMapping("/academic-years/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> deleteAcademicYear(@PathVariable Long id) {
        calendarService.deleteAcademicYear(id);
        return Result.success();
    }

    @PostMapping("/academic-years/{id}/set-current")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> setCurrentAcademicYear(@PathVariable Long id) {
        calendarService.setCurrentAcademicYear(id);
        return Result.success();
    }

    // ==================== 学期管理 ====================

    @GetMapping("/semesters")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<Semester>> listSemesters(@RequestParam(required = false) Long yearId) {
        if (yearId != null) {
            return Result.success(calendarService.listSemestersByYear(yearId));
        }
        return Result.success(calendarService.listSemesters());
    }

    @GetMapping("/semesters/{id}")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<Semester> getSemester(@PathVariable Long id) {
        return calendarService.getSemester(id)
                .map(Result::success)
                .orElse(Result.error("学期不存在"));
    }

    @GetMapping("/semesters/current")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<Semester> getCurrentSemester() {
        return Result.success(calendarService.getCurrentSemester().orElse(null));
    }

    @PostMapping("/semesters")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Semester> createSemester(@RequestBody CreateSemesterCommand command) {
        return Result.success(calendarService.createSemester(command));
    }

    @PutMapping("/semesters/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Semester> updateSemester(@PathVariable Long id,
                                            @RequestBody UpdateSemesterCommand command) {
        return Result.success(calendarService.updateSemester(id, command));
    }

    @DeleteMapping("/semesters/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        calendarService.deleteSemester(id);
        return Result.success();
    }

    @PostMapping("/semesters/{id}/set-current")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Semester> setCurrentSemester(@PathVariable Long id) {
        return Result.success(calendarService.setCurrentSemester(id));
    }

    @PostMapping("/semesters/{id}/end")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Semester> endSemester(@PathVariable Long id) {
        return Result.success(calendarService.endSemester(id));
    }

    @PostMapping("/semesters/{id}/reactivate")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Semester> reactivateSemester(@PathVariable Long id) {
        return Result.success(calendarService.reactivateSemester(id));
    }

    @GetMapping("/semesters/generate-code")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<String> generateSemesterCode(@RequestParam Integer startYear,
                                                @RequestParam Integer semesterType) {
        return Result.success(calendarService.generateSemesterCode(startYear, semesterType));
    }

    // ==================== 教学周 ====================

    @GetMapping("/semesters/{semesterId}/weeks")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<TeachingWeek>> getWeeks(@PathVariable Long semesterId) {
        return Result.success(calendarService.getWeeks(semesterId));
    }

    @PostMapping("/semesters/{semesterId}/generate-weeks")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<List<TeachingWeek>> generateWeeks(@PathVariable Long semesterId) {
        return Result.success(calendarService.generateWeeks(semesterId));
    }

    // ==================== 校历事件 ====================

    @GetMapping("/events")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<AcademicEvent>> listEvents(
            @RequestParam(required = false) Long yearId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer eventType) {
        return Result.success(calendarService.listEvents(yearId, semesterId, eventType));
    }

    @GetMapping("/events/{id}")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<AcademicEvent> getEvent(@PathVariable Long id) {
        return calendarService.getEvent(id)
                .map(Result::success)
                .orElse(Result.error("校历事件不存在"));
    }

    @PostMapping("/events")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<AcademicEvent> createEvent(@RequestBody CreateAcademicEventCommand command) {
        return Result.success(calendarService.createEvent(command));
    }

    @PutMapping("/events/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<AcademicEvent> updateEvent(@PathVariable Long id,
                                              @RequestBody UpdateAcademicEventCommand command) {
        return Result.success(calendarService.updateEvent(id, command));
    }

    @DeleteMapping("/events/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        calendarService.deleteEvent(id);
        return Result.success();
    }
}
