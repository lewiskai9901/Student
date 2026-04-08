package com.school.management.interfaces.rest.extension;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 统一实体类型配置 API
 */
@Slf4j
@RestController
@RequestMapping("/entity-type-configs")
@RequiredArgsConstructor
public class EntityTypeConfigController {

    private final JdbcTemplate jdbc;

    private static final String SELECT_COLS =
        "id, entity_type AS entityType, type_code AS typeCode, type_name AS typeName, " +
        "category, parent_type_code AS parentTypeCode, allowed_child_type_codes AS allowedChildTypeCodes, " +
        "metadata_schema AS metadataSchema, features, ui_config AS uiConfig, " +
        "is_plugin_registered AS isPluginRegistered, is_enabled AS isEnabled, sort_order AS sortOrder";

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam String entityType,
            @RequestParam(required = false) String category) {
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLS +
            " FROM entity_type_configs WHERE entity_type = ? AND deleted = 0 AND is_enabled = 1");
        List<Object> params = new ArrayList<>();
        params.add(entityType);
        if (category != null) { sql.append(" AND category = ?"); params.add(category); }
        sql.append(" ORDER BY sort_order, type_name");
        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(
            @RequestParam String entityType,
            @RequestParam String typeCode) {
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT " + SELECT_COLS + " FROM entity_type_configs WHERE entity_type = ? AND type_code = ? AND deleted = 0",
            entityType, typeCode);
        return Result.success(row);
    }

    @GetMapping("/allowed-children")
    public Result<List<Map<String, Object>>> allowedChildren(
            @RequestParam String entityType,
            @RequestParam String parentTypeCode) {
        // 查父类型的 allowed_child_type_codes
        String childCodesJson;
        try {
            childCodesJson = jdbc.queryForObject(
                "SELECT allowed_child_type_codes FROM entity_type_configs WHERE entity_type = ? AND type_code = ? AND deleted = 0",
                String.class, entityType, parentTypeCode);
        } catch (Exception e) {
            return Result.success(List.of());
        }

        if (childCodesJson == null || childCodesJson.equals("[]") || childCodesJson.equals("null")) {
            return Result.success(List.of());
        }

        // 解析 JSON 数组
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> childCodes = om.readValue(childCodesJson, List.class);
            if (childCodes.isEmpty()) return Result.success(List.of());

            String placeholders = String.join(",", Collections.nCopies(childCodes.size(), "?"));
            List<Object> params = new ArrayList<>();
            params.add(entityType);
            params.addAll(childCodes);

            List<Map<String, Object>> children = jdbc.queryForList(
                "SELECT " + SELECT_COLS + " FROM entity_type_configs WHERE entity_type = ? AND type_code IN (" + placeholders + ") AND deleted = 0 AND is_enabled = 1 ORDER BY sort_order",
                params.toArray());
            return Result.success(children);
        } catch (Exception e) {
            log.warn("解析 allowed_child_type_codes 失败: {}", e.getMessage());
            return Result.success(List.of());
        }
    }

    @PostMapping
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            jdbc.update(
                "INSERT INTO entity_type_configs (entity_type, type_code, type_name, category, " +
                "parent_type_code, allowed_child_type_codes, metadata_schema, features, " +
                "is_plugin_registered, is_enabled, deleted) VALUES (?,?,?,?,?,?,?,?,0,1,0)",
                data.get("entityType"), data.get("typeCode"), data.get("typeName"),
                data.get("category"),
                data.get("parentTypeCode"),
                om.writeValueAsString(data.getOrDefault("allowedChildTypeCodes", List.of())),
                data.getOrDefault("metadataSchema", "{\"fields\":[]}"),
                data.getOrDefault("features", "{}"));
            return Result.success(Map.of("message", "created"));
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            jdbc.update(
                "UPDATE entity_type_configs SET type_name=?, category=?, parent_type_code=?, " +
                "allowed_child_type_codes=? WHERE id=? AND deleted=0",
                data.get("typeName"), data.get("category"), data.get("parentTypeCode"),
                om.writeValueAsString(data.getOrDefault("allowedChildTypeCodes", List.of())),
                id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        // 不允许删除插件注册的类型
        try {
            Integer isPlugin = jdbc.queryForObject(
                "SELECT is_plugin_registered FROM entity_type_configs WHERE id=? AND deleted=0",
                Integer.class, id);
            if (isPlugin != null && isPlugin == 1) {
                return Result.error("插件注册的类型不能删除");
            }
            jdbc.update("UPDATE entity_type_configs SET deleted=1 WHERE id=?", id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/custom-fields")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> addCustomField(@PathVariable Long id, @RequestBody Map<String, Object> field) {
        // 读取当前 schema，追加自定义字段
        String schemaStr = jdbc.queryForObject(
            "SELECT metadata_schema FROM entity_type_configs WHERE id = ? AND deleted = 0", String.class, id);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> schema = om.readValue(schemaStr, Map.class);
            List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", new ArrayList<>());
            field.put("system", false);
            fields.add(field);
            schema.put("fields", fields);
            jdbc.update("UPDATE entity_type_configs SET metadata_schema = ? WHERE id = ?",
                om.writeValueAsString(schema), id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("添加自定义字段失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/custom-fields/{fieldKey}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> removeCustomField(@PathVariable Long id, @PathVariable String fieldKey) {
        String schemaStr = jdbc.queryForObject(
            "SELECT metadata_schema FROM entity_type_configs WHERE id = ? AND deleted = 0", String.class, id);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> schema = om.readValue(schemaStr, Map.class);
            List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", new ArrayList<>());
            // 只能删除 system=false 的字段
            fields.removeIf(f -> fieldKey.equals(f.get("key")) && !Boolean.TRUE.equals(f.get("system")));
            schema.put("fields", fields);
            jdbc.update("UPDATE entity_type_configs SET metadata_schema = ? WHERE id = ?",
                om.writeValueAsString(schema), id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除自定义字段失败: " + e.getMessage());
        }
    }
}
