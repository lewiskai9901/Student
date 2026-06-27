package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * EntityType 插件 Registrar — 扫描所有 {@link EntityTypePlugin},
 * 注册/合并到 {@code entity_type_configs} 表.
 *
 * 继承 {@link AbstractPluginRegistrar}. 比其他 Registrar 复杂之处:
 *  - metadata_schema 需要合并 (保留 admin 自定义字段 system=false)
 *  - 声明是"一插件一类型" (EntityTypePlugin 的方法直接返回 typeCode/typeName)
 *
 * @Order(100): 最早运行的数据型 Registrar.
 */
@Slf4j
@Component
@Order(100)
public class PluginRegistrar extends AbstractPluginRegistrar<EntityTypePlugin, EntityTypePlugin> {

    private final ExtensionDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public PluginRegistrar(ExtensionDispatcher dispatcher,
                            JdbcTemplate jdbc,
                            PluginPackageRegistrar packageRegistrar,
                            ObjectMapper objectMapper) {
        super(jdbc, packageRegistrar);
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @Override protected List<EntityTypePlugin> getPluginList() { return dispatcher.getAllPlugins(); }

    /** EntityTypePlugin 本身即声明 — 一个插件只声明一个类型 */
    @Override protected List<EntityTypePlugin> extractDefs(EntityTypePlugin p) {
        return List.of(p);
    }

    @Override protected String describeDef(EntityTypePlugin p) {
        return p.getEntityType() + "/" + p.getTypeCode();
    }

    @Override
    protected UpsertResult upsertOne(EntityTypePlugin plugin, EntityTypePlugin def,
                                      String industry, String pluginClass) throws Exception {
        String entityType = plugin.getEntityType();
        String typeCode = plugin.getTypeCode();

        // 守护: 插件声明的 category 必须 ∈ 对应实体枚举 (或 null=未归类)。分类是封闭枚举, 非法值会
        // 静默失效 (categoryDefaults valueOf 落 catch) → 这里启动期 fail-fast, 逼插件作者用合法分类。
        if (!EntityTypeCategories.isValid(entityType, plugin.getCategory())) {
            throw new IllegalStateException(String.format(
                "插件 %s 声明了非法类型分类: %s/%s category='%s' (合法值: %s, 或留 null 表示未归类)",
                plugin.getClass().getSimpleName(), entityType, typeCode, plugin.getCategory(),
                EntityTypeCategories.validValues(entityType)));
        }

        if (isCustomProtected(
                "SELECT industry FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
                entityType, typeCode)) {
            return UpsertResult.SKIPPED;
        }
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
            Long.class, entityType, typeCode);

        String childCodesJson = objectMapper.writeValueAsString(plugin.getAllowedChildTypeCodes());
        // 分类默认特性 (能力模板) 作基线, 插件显式 features 覆盖 → 让 ROOM 真 bookable/occupiable 等落到 features 列
        // (此前 PLACE/ORG 类型 features={} 时这些能力全 false, 是悬空缺陷)。USER 分类默认现为空, 不受影响。
        java.util.Map<String, Boolean> mergedFeatures =
                new java.util.LinkedHashMap<>(EntityTypeCategories.defaultFeatures(entityType, plugin.getCategory()));
        mergedFeatures.putAll(plugin.getFeatures());
        String featuresJson = objectMapper.writeValueAsString(mergedFeatures);
        String uiConfigJson = objectMapper.writeValueAsString(plugin.getUiConfig());
        List<String> pluginDefaultRoles = plugin.getDefaultRoleCodes();
        String defaultRolesJson = objectMapper.writeValueAsString(pluginDefaultRoles);

        String origin = resolveOrigin(plugin);
        if (exists == null || exists == 0) {
            String schemaJson = buildSchemaJson(plugin.getSystemFields(), List.of());
            jdbc.update(
                "INSERT INTO entity_type_configs (entity_type, type_code, type_name, category, " +
                "parent_type_code, allowed_child_type_codes, metadata_schema, features, ui_config, " +
                "default_role_codes, is_plugin_registered, plugin_class, industry, origin, is_enabled, deleted) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,1,?,?,?,1,0)",
                entityType, typeCode, plugin.getTypeName(), plugin.getCategory(),
                plugin.getParentTypeCode(), childCodesJson, schemaJson,
                featuresJson, uiConfigJson, defaultRolesJson, pluginClass, industry, origin);
            return UpsertResult.CREATED;
        }
        // 合并: 保留 admin 自定义字段
        Map<String, Object> existingRow = jdbc.queryForMap(
            "SELECT metadata_schema, type_name, category, ui_config, features, " +
            "parent_type_code, allowed_child_type_codes, default_role_codes, overridden_fields " +
            "FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
            entityType, typeCode);
        String currentSchemaStr = (String) existingRow.get("metadata_schema");
        List<FieldDefinition> customFields = extractCustomFields(currentSchemaStr);
        String mergedSchema = buildSchemaJson(plugin.getSystemFields(), customFields);

        // ── 字段级合并: 被管理员覆写的字段保留现值, 其他跟插件声明走 ──
        // overridden_fields 是 JSON 数组, 如 ["typeName","features","parentTypeCode"]
        // 每个可覆写字段独立判断 (features 不再与 category 耦合 — 修复 admin 单改 features 重启被覆盖)。
        Set<String> overridden = parseOverriddenFields(existingRow.get("overridden_fields"));

        String finalTypeName = overridden.contains("typeName")
                ? (String) existingRow.get("type_name")
                : plugin.getTypeName();
        String finalCategory = overridden.contains("category")
                ? (String) existingRow.get("category")
                : plugin.getCategory();
        String finalUiConfig = overridden.contains("uiConfig")
                ? (String) existingRow.get("ui_config")
                : uiConfigJson;
        String finalFeatures = overridden.contains("features")
                ? (String) existingRow.get("features")
                : featuresJson;
        String finalParent = overridden.contains("parentTypeCode")
                ? (String) existingRow.get("parent_type_code")
                : plugin.getParentTypeCode();
        String finalChildCodes = overridden.contains("allowedChildTypeCodes")
                ? (String) existingRow.get("allowed_child_type_codes")
                : childCodesJson;
        // default_role_codes 是部署绑定: 插件空集合 = 无意见 → 保留 seed/admin 现值, 不覆盖。
        String finalDefaultRoles = (overridden.contains("defaultRoleCodes") || pluginDefaultRoles.isEmpty())
                ? (String) existingRow.get("default_role_codes")
                : defaultRolesJson;

        jdbc.update(
            "UPDATE entity_type_configs SET type_name=?, category=?, parent_type_code=?, " +
            "allowed_child_type_codes=?, metadata_schema=?, features=?, ui_config=?, default_role_codes=?, " +
            "is_plugin_registered=1, plugin_class=?, industry=?, origin=? " +
            "WHERE entity_type=? AND type_code=? AND deleted=0",
            finalTypeName, finalCategory, finalParent,
            finalChildCodes, mergedSchema, finalFeatures, finalUiConfig, finalDefaultRoles,
            pluginClass, industry, origin, entityType, typeCode);
        if (!overridden.isEmpty()) {
            log.info("[PluginRegistrar] {}/{} 有管理员覆写字段: {}", entityType, typeCode, overridden);
        }
        return UpsertResult.UPDATED;
    }

    /**
     * 解析 overridden_fields JSON 数组; 异常或空值返回空集合 (等同"无覆写").
     */
    private Set<String> parseOverriddenFields(Object raw) {
        if (raw == null) return Collections.emptySet();
        String str = raw.toString();
        if (str.isBlank()) return Collections.emptySet();
        try {
            List<String> list = objectMapper.readValue(str, new TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            log.warn("[PluginRegistrar] 解析 overridden_fields 失败: {} - {}", str, e.getMessage());
            return Collections.emptySet();
        }
    }

    // ═══════════════ schema 合并辅助 ═══════════════

    private String buildSchemaJson(List<FieldDefinition> systemFields, List<FieldDefinition> customFields)
            throws Exception {
        List<Map<String, Object>> allFields = new ArrayList<>();
        for (FieldDefinition f : systemFields) allFields.add(toMap(f, true));
        for (FieldDefinition f : customFields) allFields.add(toMap(f, false));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("fields", allFields);
        return objectMapper.writeValueAsString(schema);
    }

    private Map<String, Object> toMap(FieldDefinition f, boolean system) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", f.getKey());
        m.put("label", f.getLabel());
        m.put("type", f.getType());
        m.put("group", system ? f.getGroup() : (f.getGroup() != null ? f.getGroup() : "自定义"));
        m.put("required", f.isRequired());
        m.put("system", system);
        if (system && f.getDefaultValue() != null) m.put("defaultValue", f.getDefaultValue());
        if (f.getConfig() != null && !f.getConfig().isEmpty()) m.put("config", f.getConfig());
        return m;
    }

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
