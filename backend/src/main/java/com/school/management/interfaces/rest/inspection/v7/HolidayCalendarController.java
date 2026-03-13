package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.HolidayCalendarApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.platform.HolidayCalendar;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v7/insp/holiday-calendars")
@RequiredArgsConstructor
public class HolidayCalendarController {

    private final HolidayCalendarApplicationService holidayCalendarService;

    @PostMapping
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<HolidayCalendar> create(@RequestBody CreateHolidayCalendarRequest request) {
        return Result.success(holidayCalendarService.create(
                request.getCalendarName(), request.getYear(), request.getHolidays(),
                request.getWorkdays(), request.getIsDefault()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<HolidayCalendar> update(@PathVariable Long id,
                                           @RequestBody UpdateHolidayCalendarRequest request) {
        return Result.success(holidayCalendarService.update(
                id, request.getCalendarName(), request.getYear(), request.getHolidays(),
                request.getWorkdays(), request.getIsDefault()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        holidayCalendarService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<HolidayCalendar> findById(@PathVariable Long id) {
        return Result.success(holidayCalendarService.findById(id));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<HolidayCalendar>> list() {
        return Result.success(holidayCalendarService.findAll());
    }

    @GetMapping("/by-year")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<List<HolidayCalendar>> findByYear(@RequestParam Integer year) {
        return Result.success(holidayCalendarService.findByYear(year));
    }

    @GetMapping("/default")
    @CasbinAccess(resource = "insp:platform", action = "view")
    public Result<HolidayCalendar> findDefault() {
        return Result.success(holidayCalendarService.findDefault());
    }

    @Data
    public static class CreateHolidayCalendarRequest {
        private String calendarName;
        private Integer year;
        private String holidays;
        private String workdays;
        private Boolean isDefault;
    }

    @Data
    public static class UpdateHolidayCalendarRequest {
        private String calendarName;
        private Integer year;
        private String holidays;
        private String workdays;
        private Boolean isDefault;
    }
}
