package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 插件注册器 — 应用启动时扫描所有 EntityTypePlugin，注册/合并到 entity_type_configs 表。
 *
 * 合并策略:
 * - 新类型: 直接插入，metadata_schema = 插件的 systemFields
 * - 已有类型: 保留管理员自定义字段(system=false)，更新插件系统字段(system=true)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginRegistrar implements ApplicationRunner {

    private final ExtensionDispatcher dispatcher;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<EntityTypePlugin> plugins = dispatcher.getAllPlugins();
        if (plugins.isEmpty()) {
            log.info("PluginRegistrar: 无插件需要注册");
            return;
        }

        int created = 0, updated = 0;
        for (EntityTypePlugin plugin : plugins) {
            try {
                boolean isNew = registerOrMerge(plugin);
                if (isNew) created++; else updated++;
            } catch (Exception e) {
                log.error("注册插件失败: {}/{} - {}",
                    plugin.getEntityType(), plugin.getTypeCode(), e.getMessage());
            }
        }
        log.info("PluginRegistrar: 完成注册 {} 个插件 (新增: {}, 更新: {})",
            plugins.size(), created, updated);
    }

    /**
     * 注册或合并插件到 entity_type_configs
     * @return true=新注册, false=已存在(合并更新)
     */
    private boolean registerOrMerge(EntityTypePlugin plugin) throws Exception {
        String entityType = plugin.getEntityType();
        String typeCode = plugin.getTypeCode();

        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
            Long.class, entityType, typeCode);

        String childCodesJson = objectMapper.writeValueAsString(plugin.getAllowedChildTypeCodes());
        String featuresJson = objectMapper.writeValueAsString(plugin.getFeatures());
        String uiConfigJson = objectMapper.writeValueAsString(plugin.getUiConfig());

        if (exists == null || exists == 0) {
            // 新注册
            String schemaJson = buildSchemaJson(plugin.getSystemFields(), List.of());
            jdbc.update(
                "INSERT INTO entity_type_configs (entity_type, type_code, type_name, category, " +
                "parent_type_code, allowed_child_type_codes, metadata_schema, features, ui_config, " +
                "is_plugin_registered, plugin_class, is_enabled, deleted) " +
                "VALUES (?,?,?,?,?,?,?,?,?,1,?,1,0)",
                entityType, typeCode, plugin.getTypeName(), plugin.getCategory(),
                plugin.getParentTypeCode(), childCodesJson, schemaJson,
                featuresJson, uiConfigJson, plugin.getClass().getName());

            log.info("  注册新类型: {}/{} ({})", entityType, typeCode, plugin.getTypeName());
            return true;
        } else {
            // 已存在: 合并 — 保留管理员自定义字段，更新系统字段
            String currentSchemaStr = jdbc.queryForObject(
                "SELECT metadata_schema FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
                String.class, entityType, typeCode);

            List<FieldDefinition> customFields = extractCustomFields(currentSchemaStr);
            String mergedSchema = buildSchemaJson(plugin.getSystemFields(), customFields);

            jdbc.update(
                "UPDATE entity_type_configs SET type_name=?, category=?, parent_type_code=?, " +
                "allowed_child_type_codes=?, metadata_schema=?, features=?, ui_config=?, " +
                "is_plugin_registered=1, plugin_class=? " +
                "WHERE entity_type=? AND type_code=? AND deleted=0",
                plugin.getTypeName(), plugin.getCategory(), plugin.getParentTypeCode(),
                childCodesJson, mergedSchema, featuresJson, uiConfigJson,
                plugin.getClass().getName(), entityType, typeCode);

            log.info("  更新类型: {}/{} (保留 {} 个自定义字段)",
                entityType, typeCode, customFields.size());
            return false;
        }
    }

    /**
     * 构建合并后的 metadata_schema JSON
     */
    private String buildSchemaJson(List<FieldDefinition> systemFields, List<FieldDefinition> customFields)
            throws Exception {
        // 系统字段标记 system=true
        List<Map<String, Object>> allFields = new ArrayList<>();
        for (FieldDefinition f : systemFields) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", f.getKey());
            m.put("label", f.getLabel());
            m.put("type", f.getType());
            m.put("group", f.getGroup());
            m.put("required", f.isRequired());
            m.put("system", true);
            if (f.getDefaultValue() != null) m.put("defaultValue", f.getDefaultValue());
            if (f.getConfig() != null && !f.getConfig().isEmpty()) m.put("config", f.getConfig());
            allFields.add(m);
        }
        // 管理员自定义字段标记 system=false
        for (FieldDefinition f : customFields) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", f.getKey());
            m.put("label", f.getLabel());
            m.put("type", f.getType());
            m.put("group", f.getGroup() != null ? f.getGroup() : "自定义");
            m.put("required", f.isRequired());
            m.put("system", false);
            if (f.getConfig() != null) m.put("config", f.getConfig());
            allFields.add(m);
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("fields", allFields);
        return objectMapper.writeValueAsString(schema);
    }

    /**
     * 从现有 schema JSON 中提取管理员自定义字段(system=false)
     */
    @SuppressWarnings("unchecked")
    private List<FieldDefinition> extractCustomFields(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return List.of();
        try {
            Map<String, Object> schema = objectMapper.readValue(schemaJson, Map.class);
            List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", List.of());
            return fields.stream()
                .filter(f -> !Boolean.TRUE.equals(f.get("system")))
                .map(f -> FieldDefinition.builder()
                    .key((String) f.get("key"))
                    .label((String) f.get("label"))
                    .type((String) f.get("type"))
                    .group((String) f.get("group"))
                    .required(Boolean.TRUE.equals(f.get("required")))
                    .system(false)
                    .config((Map<String, Object>) f.get("config"))
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析 schema JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }
}
