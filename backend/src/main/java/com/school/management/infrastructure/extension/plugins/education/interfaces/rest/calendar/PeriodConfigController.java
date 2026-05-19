package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.calendar;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.application.calendar.PeriodConfigApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 作息表/节次配置 Controller. 每个学期可独立配置上几节课、每节什么时间.
 *
 * <p>M3.2.1 (2026-05-20): 9 处直 jdbc 已下沉到 {@link PeriodConfigApplicationService}.
 */
@Slf4j
@RestController
@RequestMapping("/calendar/period-configs")
@RequiredArgsConstructor
public class PeriodConfigController {

    private final PeriodConfigApplicationService periodConfigService;

    @GetMapping
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<Map<String, Object>>> list(@RequestParam Long semesterId) {
        return Result.success(periodConfigService.listBySemester(semesterId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> row = periodConfigService.findById(id);
        if (row == null) return Result.error("作息配置不存在");
        return Result.success(row);
    }

    @PostMapping
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        Long newId = periodConfigService.create(data);
        return Result.success(Map.of("id", newId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        periodConfigService.update(id, data);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        periodConfigService.softDelete(id);
        return Result.success();
    }

    @PostMapping("/init-from-previous")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Map<String, Object>> initFromPrevious(@RequestBody Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String message = periodConfigService.initFromPrevious(semesterId);
        return Result.success(Map.of("message", message));
    }
}
