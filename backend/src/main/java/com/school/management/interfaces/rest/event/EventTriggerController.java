package com.school.management.interfaces.rest.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.event.TriggerService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCase;
import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 事件触发器管理 API
 */
@RestController
@RequestMapping("/event/triggers")
@Tag(name = "事件触发器管理", description = "事件触发器配置 API")
@RequiredArgsConstructor
public class EventTriggerController {

    private final JdbcTemplate jdbcTemplate;
    private final TriggerService triggerService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "获取触发器列表")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String pointCode,
            @RequestParam(required = false) String eventType) {
        StringBuilder sql = new StringBuilder(
            "SELECT t.*, tp.point_name AS trigger_point_name, tp.module_code, tp.module_name " +
            "FROM event_triggers t " +
            "LEFT JOIN trigger_points tp ON t.trigger_point_code = tp.point_code AND tp.deleted = 0 " +
            "WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (pointCode != null && !pointCode.isBlank()) {
            sql.append(" AND t.trigger_point_code = ?");
            params.add(pointCode);
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND t.event_type_code = ?");
            params.add(eventType);
        }
        sql.append(" ORDER BY t.sort_order, t.id");

        return Result.success(toCamelCaseList(jdbcTemplate.queryForList(sql.toString(), params.toArray())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取触发器详情")
    @CasbinAccess(resource = "event-trigger", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT t.*, tp.point_name AS trigger_point_name, tp.module_code, tp.module_name " +
            "FROM event_triggers t " +
            "LEFT JOIN trigger_points tp ON t.trigger_point_code = tp.point_code AND tp.deleted = 0 " +
            "WHERE t.id = ? AND t.deleted = 0", id);
        if (rows.isEmpty()) {
            return Result.error("触发器不存在");
        }
        return Result.success(toCamelCase(rows.get(0)));
    }

    @PostMapping
    @Operation(summary = "创建触发器")
    @CasbinAccess(resource = "event-trigger", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        String conditionJson = null;
        if (body.get("conditionJson") != null) {
            try {
                conditionJson = objectMapper.writeValueAsString(body.get("conditionJson"));
            } catch (Exception e) {
                conditionJson = body.get("conditionJson").toString();
            }
        }
        String relatedSources = null;
        if (body.get("relatedSources") != null) {
            try {
                relatedSources = objectMapper.writeValueAsString(body.get("relatedSources"));
            } catch (Exception e) {
                relatedSources = body.get("relatedSources").toString();
            }
        }
        String payloadFields = null;
        if (body.get("payloadFields") != null) {
            try {
                payloadFields = objectMapper.writeValueAsString(body.get("payloadFields"));
            } catch (Exception e) {
                payloadFields = body.get("payloadFields").toString();
            }
        }

        jdbcTemplate.update(
            "INSERT INTO event_triggers (name, trigger_point_code, condition_json, " +
            "event_type_mode, event_type_code, event_type_source, " +
            "subject_type, subject_source, subject_name_source, " +
            "related_sources, payload_fields, description, is_enabled, sort_order, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
            body.get("name"), body.get("triggerPointCode"), conditionJson,
            body.getOrDefault("eventTypeMode", "FIXED"),
            body.get("eventTypeCode"), body.get("eventTypeSource"),
            body.getOrDefault("subjectType", "USER"),
            body.get("subjectSource"), body.get("subjectNameSource"),
            relatedSources, payloadFields,
            body.get("description"),
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String conditionJson = null;
        if (body.get("conditionJson") != null) {
            try {
                conditionJson = objectMapper.writeValueAsString(body.get("conditionJson"));
            } catch (Exception e) {
                conditionJson = body.get("conditionJson").toString();
            }
        }
        String relatedSources = null;
        if (body.get("relatedSources") != null) {
            try {
                relatedSources = objectMapper.writeValueAsString(body.get("relatedSources"));
            } catch (Exception e) {
                relatedSources = body.get("relatedSources").toString();
            }
        }
        String payloadFields = null;
        if (body.get("payloadFields") != null) {
            try {
                payloadFields = objectMapper.writeValueAsString(body.get("payloadFields"));
            } catch (Exception e) {
                payloadFields = body.get("payloadFields").toString();
            }
        }

        jdbcTemplate.update(
            "UPDATE event_triggers SET name = ?, trigger_point_code = ?, condition_json = ?, " +
            "event_type_mode = ?, event_type_code = ?, event_type_source = ?, " +
            "subject_type = ?, subject_source = ?, subject_name_source = ?, " +
            "related_sources = ?, payload_fields = ?, description = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("name"), body.get("triggerPointCode"), conditionJson,
            body.getOrDefault("eventTypeMode", "FIXED"),
            body.get("eventTypeCode"), body.get("eventTypeSource"),
            body.getOrDefault("subjectType", "USER"),
            body.get("subjectSource"), body.get("subjectNameSource"),
            relatedSources, payloadFields,
            body.get("description"),
            body.getOrDefault("sortOrder", 0),
            id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除触发器")
    @CasbinAccess(resource = "event-trigger", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE event_triggers SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE event_triggers SET is_enabled = 1 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用触发器")
    @CasbinAccess(resource = "event-trigger", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE event_triggers SET is_enabled = 0 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    @PostMapping("/test")
    @Operation(summary = "测试触发器匹配")
    @CasbinAccess(resource = "event-trigger", action = "view")
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> test(@RequestBody Map<String, Object> body) {
        String pointCode = (String) body.get("pointCode");
        Map<String, Object> context = (Map<String, Object>) body.get("context");
        if (pointCode == null || context == null) {
            return Result.error("pointCode和context不能为空");
        }
        return Result.success(toCamelCaseList(triggerService.testFire(pointCode, context)));
    }
}
