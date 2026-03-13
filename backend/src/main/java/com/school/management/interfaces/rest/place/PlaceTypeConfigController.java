package com.school.management.interfaces.rest.space;

import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 场所类型配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/space-type-configs")
@RequiredArgsConstructor
@Tag(name = "场所类型配置", description = "管理可配置的场所类型")
public class SpaceTypeConfigController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "获取所有场所类型配置")
    public Result<List<Map<String, Object>>> getAll() {
        String sql = "SELECT id, type_code, type_name, parent_type, has_capacity, has_occupancy, " +
                "has_gender, icon, color, sort_order, enabled, created_at " +
                "FROM space_type_config WHERE deleted = 0 ORDER BY sort_order";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return Result.success(list);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取启用的场所类型配置")
    public Result<List<Map<String, Object>>> getEnabled() {
        String sql = "SELECT id, type_code, type_name, parent_type, has_capacity, has_occupancy, " +
                "has_gender, icon, color, sort_order " +
                "FROM space_type_config WHERE deleted = 0 AND enabled = 1 ORDER BY sort_order";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return Result.success(list);
    }

    @GetMapping("/room-types")
    @Operation(summary = "获取房间类型配置", description = "获取parent_type为ROOM的类型配置")
    public Result<List<Map<String, Object>>> getRoomTypes() {
        String sql = "SELECT id, type_code, type_name, has_capacity, has_occupancy, " +
                "has_gender, icon, color, sort_order " +
                "FROM space_type_config WHERE deleted = 0 AND enabled = 1 AND parent_type = 'ROOM' ORDER BY sort_order";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return Result.success(list);
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用类型配置")
    @PreAuthorize("hasAuthority('space:config')")
    public Result<Void> toggleEnabled(@PathVariable Long id) {
        String sql = "UPDATE space_type_config SET enabled = NOT enabled WHERE id = ?";
        jdbcTemplate.update(sql, id);
        return Result.success();
    }

    @PutMapping("/{id}/sort")
    @Operation(summary = "更新排序")
    @PreAuthorize("hasAuthority('space:config')")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam int sortOrder) {
        String sql = "UPDATE space_type_config SET sort_order = ? WHERE id = ?";
        jdbcTemplate.update(sql, sortOrder, id);
        return Result.success();
    }
}
