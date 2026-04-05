package com.school.management.interfaces.rest.event;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 触发点管理 API
 */
@RestController
@RequestMapping("/event/trigger-points")
@Tag(name = "触发点管理", description = "事件触发点配置 API")
@RequiredArgsConstructor
public class TriggerPointController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "获取触发点列表")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String module) {
        String sql = "SELECT * FROM trigger_points WHERE deleted = 0";
        if (module != null && !module.isBlank()) {
            sql += " AND module_code = '" + module.replace("'", "") + "'";
        }
        sql += " ORDER BY module_code, sort_order";
        return Result.success(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取触发点详情")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM trigger_points WHERE id = ? AND deleted = 0", id);
        if (rows.isEmpty()) {
            return Result.error("触发点不存在");
        }
        return Result.success(rows.get(0));
    }

    @PostMapping
    @Operation(summary = "创建触发点")
    @CasbinAccess(resource = "event-trigger", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO trigger_points (module_code, module_name, point_code, point_name, " +
            "description, context_schema, is_enabled, sort_order, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
            body.get("moduleCode"), body.get("moduleName"),
            body.get("pointCode"), body.get("pointName"),
            body.get("description"),
            body.get("contextSchema") != null ? body.get("contextSchema").toString() : null,
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE trigger_points SET module_code = ?, module_name = ?, point_code = ?, " +
            "point_name = ?, description = ?, context_schema = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("moduleCode"), body.get("moduleName"),
            body.get("pointCode"), body.get("pointName"),
            body.get("description"),
            body.get("contextSchema") != null ? body.get("contextSchema").toString() : null,
            body.getOrDefault("sortOrder", 0),
            id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除触发点")
    @CasbinAccess(resource = "event-trigger", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE trigger_points SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE trigger_points SET is_enabled = 1 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用触发点")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE trigger_points SET is_enabled = 0 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }
}
