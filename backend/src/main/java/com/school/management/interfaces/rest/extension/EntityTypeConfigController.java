package com.school.management.interfaces.rest.extension;

import com.school.management.common.result.Result;
import com.school.management.common.util.PluginEnabledGuard;
import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.user.model.entity.UserCategory;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * 统一实体类型配置 API
 */
@Slf4j
@RestController
@RequestMapping("/entity-type-configs")
@RequiredArgsConstructor
public class EntityTypeConfigController {

    private final JdbcTemplate jdbc;
    private final PluginEnabledGuard pluginEnabledGuard;

    private static final String SELECT_COLS =
        "id, entity_type AS entityType, type_code AS typeCode, type_name AS typeName, " +
        "category, parent_type_code AS parentTypeCode, allowed_child_type_codes AS allowedChildTypeCodes, " +
        "metadata_schema AS metadataSchema, features, ui_config AS uiConfig, " +
        "is_plugin_registered AS isPluginRegistered, is_enabled AS isEnabled, " +
        "plugin_enabled AS pluginEnabled, sort_order AS sortOrder, " +
        "plugin_class AS pluginClass, industry, origin, " +
        "overridden_fields AS overriddenFields";

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam String entityType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        // includeDisabled=true: 管理员视角, 返回所属插件被禁的类型 (灰显)
        boolean admin = Boolean.TRUE.equals(includeDisabled);
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLS +
            " FROM entity_type_configs WHERE entity_type = ? AND deleted = 0");
        if (!admin) {
            sql.append(" AND is_enabled = 1 AND plugin_enabled = 1");
        }
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

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories(@RequestParam String entityType) {
        List<Map<String, Object>> list;
        switch (entityType) {
            case "ORG_UNIT":
                list = Stream.of(OrgCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            case "USER":
                list = Stream.of(UserCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            case "PLACE":
                list = Stream.of(BaseCategory.values())
                        .map(c -> Map.<String, Object>of(
                                "code", c.name(),
                                "label", c.getLabel(),
                                "allowedChildCodes", c.getAllowedChildCategories(),
                                "defaultFeatures", c.getDefaultFeatures()))
                        .toList();
                break;
            default:
                return Result.error("未知的 entityType: " + entityType);
        }
        return Result.success(list);
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
                "SELECT " + SELECT_COLS + " FROM entity_type_configs WHERE entity_type = ? AND type_code IN (" + placeholders + ") AND deleted = 0 AND is_enabled = 1 AND plugin_enabled = 1 ORDER BY sort_order",
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
                "is_plugin_registered, is_enabled, deleted, industry) VALUES (?,?,?,?,?,?,?,?,0,1,0,'CUSTOM')",
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

    /** 可被管理员覆写的字段白名单 — PluginRegistrar 启动时会读 overridden_fields 决定保留哪些字段 */
    private static final Set<String> OVERRIDABLE_FIELDS = Set.of("typeName", "category", "uiConfig");

    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        pluginEnabledGuard.check("entity_type_configs", id);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

            // 读当前行: 判断是否插件类型 + 当前值 + 已有覆写集合
            Map<String, Object> current = jdbc.queryForMap(
                "SELECT is_plugin_registered, type_name, category, ui_config, overridden_fields " +
                "FROM entity_type_configs WHERE id=? AND deleted=0", id);
            boolean isPlugin = toBool(current.get("is_plugin_registered"));
            Set<String> overridden = parseOverriddenFields(current.get("overridden_fields"), om);

            // features 子集校验: 不允许启用不在 category 默认集内的 feature
            String newCategory = str(data.get("category"));
            Object featuresObj = data.get("features");
            if (featuresObj != null && newCategory != null && !newCategory.isBlank()) {
                validateFeaturesSubset(featuresObj, newCategory, str(current.get("is_plugin_registered")) != null
                        ? existingEntityType(id) : null);
            }

            if (isPlugin) {
                // 对比每个可覆写字段, 有变化则加到 overridden_fields
                String newTypeName = str(data.get("typeName"));
                String curTypeName = str(current.get("type_name"));
                if (newTypeName != null && !Objects.equals(newTypeName, curTypeName)) {
                    overridden.add("typeName");
                }
                String curCategory = str(current.get("category"));
                if (newCategory != null && !Objects.equals(newCategory, curCategory)) {
                    overridden.add("category");
                }
                Object newUiConfig = data.get("uiConfig");
                if (newUiConfig != null) {
                    String newUi = newUiConfig instanceof String
                            ? (String) newUiConfig : om.writeValueAsString(newUiConfig);
                    String curUi = str(current.get("ui_config"));
                    if (!Objects.equals(newUi, curUi)) overridden.add("uiConfig");
                }
            }

            // 更新本身 + 同步 overridden_fields (插件类型才需要)
            String overriddenJson = isPlugin ? om.writeValueAsString(new ArrayList<>(overridden)) : null;
            jdbc.update(
                "UPDATE entity_type_configs SET type_name=?, category=?, parent_type_code=?, " +
                "allowed_child_type_codes=?, features=?, ui_config=?, overridden_fields=? " +
                "WHERE id=? AND deleted=0",
                data.get("typeName"), newCategory, data.get("parentTypeCode"),
                om.writeValueAsString(data.getOrDefault("allowedChildTypeCodes", List.of())),
                featuresObj instanceof String ? (String) featuresObj
                        : (featuresObj != null ? om.writeValueAsString(featuresObj) : null),
                data.get("uiConfig") instanceof String ? (String) data.get("uiConfig")
                        : (data.get("uiConfig") != null ? om.writeValueAsString(data.get("uiConfig")) : null),
                overriddenJson,
                id);
            return Result.success();
        } catch (IllegalArgumentException iae) {
            return Result.error(iae.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 恢复某个字段为插件默认值 — 从 overridden_fields 移除该字段并回填插件声明值.
     * PluginRegistrar 下次启动会重新按插件声明写, 这里立即生效而不用等重启.
     */
    @PostMapping("/{id}/reset-field")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> resetField(@PathVariable Long id, @RequestParam String field) {
        pluginEnabledGuard.check("entity_type_configs", id);
        if (!OVERRIDABLE_FIELDS.contains(field)) {
            return Result.error("字段 [" + field + "] 不在可恢复白名单内: " + OVERRIDABLE_FIELDS);
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_plugin_registered, plugin_class, overridden_fields " +
                "FROM entity_type_configs WHERE id=? AND deleted=0", id);
            if (!toBool(row.get("is_plugin_registered"))) {
                return Result.error("非插件类型无需恢复");
            }
            String pluginClass = str(row.get("plugin_class"));
            if (pluginClass == null) {
                return Result.error("无插件类, 无法恢复默认值");
            }
            // 从 classpath 反射插件 bean 拿原始值
            Object original = resolvePluginOriginal(pluginClass, field);
            if (original == null) {
                return Result.error("插件 " + pluginClass + " 未提供字段 [" + field + "] 的声明");
            }
            Set<String> overridden = parseOverriddenFields(row.get("overridden_fields"), om);
            overridden.remove(field);
            String overriddenJson = om.writeValueAsString(new ArrayList<>(overridden));

            switch (field) {
                case "typeName":
                    jdbc.update("UPDATE entity_type_configs SET type_name=?, overridden_fields=? WHERE id=? AND deleted=0",
                            original, overriddenJson, id);
                    break;
                case "category":
                    // category 恢复时 features 也一并回到默认
                    String featuresJson = om.writeValueAsString(resolvePluginOriginal(pluginClass, "features"));
                    jdbc.update("UPDATE entity_type_configs SET category=?, features=?, overridden_fields=? WHERE id=? AND deleted=0",
                            original, featuresJson, overriddenJson, id);
                    break;
                case "uiConfig":
                    String uiJson = original instanceof String ? (String) original : om.writeValueAsString(original);
                    jdbc.update("UPDATE entity_type_configs SET ui_config=?, overridden_fields=? WHERE id=? AND deleted=0",
                            uiJson, overriddenJson, id);
                    break;
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    /** 从 Spring context 查 EntityTypePlugin bean, 读其原始字段声明. */
    private Object resolvePluginOriginal(String pluginClass, String field) {
        try {
            Class<?> clazz = Class.forName(pluginClass);
            Object bean = appCtx.getBean(clazz);
            switch (field) {
                case "typeName": return invoke(bean, "getTypeName");
                case "category": return invoke(bean, "getCategory");
                case "uiConfig":  return invoke(bean, "getUiConfig");
                case "features":  return invoke(bean, "getFeatures");
                default: return null;
            }
        } catch (Exception e) {
            log.warn("resolvePluginOriginal failed: class={} field={} err={}", pluginClass, field, e.getMessage());
            return null;
        }
    }

    private Object invoke(Object bean, String method) throws Exception {
        return bean.getClass().getMethod(method).invoke(bean);
    }

    private Set<String> parseOverriddenFields(Object raw, com.fasterxml.jackson.databind.ObjectMapper om) {
        if (raw == null) return new HashSet<>();
        String s = raw.toString();
        if (s.isBlank()) return new HashSet<>();
        try {
            List<String> list = om.readValue(s, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return new HashSet<>(list);
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private boolean toBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        return "1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString());
    }

    private String str(Object v) { return v == null ? null : v.toString(); }

    /**
     * features 只校验 category 已声明的那 5 个通用 capability —
     * 管理员不能把 category 默认为 false 的能力翻成 true (会违反该分类的业务语义).
     *
     * 插件自扩展的额外 feature (如 CLASS 的 hasStudents/hasTimetable) 不受约束.
     */
    private void validateFeaturesSubset(Object featuresObj, String categoryCode, String entityType) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Boolean> features = featuresObj instanceof String
                    ? om.readValue((String) featuresObj, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {})
                    : om.convertValue(featuresObj, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {});
            if (features == null || features.isEmpty()) return;

            Map<String, Boolean> allowed = categoryDefaults(categoryCode, entityType);
            if (allowed == null || allowed.isEmpty()) return;

            for (Map.Entry<String, Boolean> e : features.entrySet()) {
                // 只管 category 声明过的 capability; 插件自扩展的 feature 放行
                if (!allowed.containsKey(e.getKey())) continue;
                // category 默认 false 的能力, 管理员不能翻成 true
                if (Boolean.TRUE.equals(e.getValue()) && Boolean.FALSE.equals(allowed.get(e.getKey()))) {
                    throw new IllegalArgumentException("分类 [" + categoryCode + "] 不允许启用能力 ["
                            + e.getKey() + "] (该分类默认关闭此项)");
                }
            }
        } catch (IllegalArgumentException iae) { throw iae; }
          catch (Exception ignore) { /* 解析异常不阻塞 */ }
    }

    private String existingEntityType(Long id) {
        try {
            return jdbc.queryForObject("SELECT entity_type FROM entity_type_configs WHERE id=?", String.class, id);
        } catch (Exception e) { return null; }
    }

    private Map<String, Boolean> categoryDefaults(String categoryCode, String entityType) {
        if (categoryCode == null) return null;
        try {
            if ("USER".equals(entityType)) return UserCategory.valueOf(categoryCode).getDefaultFeatures();
            if ("PLACE".equals(entityType)) return BaseCategory.valueOf(categoryCode).getDefaultFeatures();
            return OrgCategory.valueOf(categoryCode).getDefaultFeatures();
        } catch (Exception e) {
            return null;
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext appCtx;

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        pluginEnabledGuard.check("entity_type_configs", id);
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

    private static final Set<String> ALLOWED_CUSTOM_FIELD_TYPES = Set.of(
        "text", "number", "date", "datetime", "boolean", "select", "multiselect", "textarea", "radio");

    @PostMapping("/{id}/custom-fields")
    @CasbinAccess(resource = "system:config", action = "edit")
    public Result<Void> addCustomField(@PathVariable Long id, @RequestBody Map<String, Object> field) {
        pluginEnabledGuard.check("entity_type_configs", id);
        // 1. 验证必填字段
        Object keyObj = field.get("key");
        Object labelObj = field.get("label");
        Object typeObj = field.get("type");

        String key = keyObj instanceof String ? (String) keyObj : null;
        String label = labelObj instanceof String ? (String) labelObj : null;
        String type = typeObj instanceof String ? (String) typeObj : null;

        if (key == null || !key.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return Result.error("字段 key 只能包含字母、数字、下划线，且不能以数字开头");
        }
        if (label == null || label.isEmpty() || label.length() > 100) {
            return Result.error("字段名称长度须在 1-100 之间");
        }
        if (type == null || !ALLOWED_CUSTOM_FIELD_TYPES.contains(type)) {
            return Result.error("不支持的字段类型: " + type);
        }

        // 读取当前 schema，追加自定义字段
        String schemaStr = jdbc.queryForObject(
            "SELECT metadata_schema FROM entity_type_configs WHERE id = ? AND deleted = 0", String.class, id);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> schema = om.readValue(schemaStr, Map.class);
            List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", new ArrayList<>());

            // 2. 检查 key 重复
            for (Map<String, Object> existing : fields) {
                if (key.equals(existing.get("key"))) {
                    return Result.error("字段 key 已存在: " + key);
                }
            }

            // 3. 标记为非系统字段
            field.put("system", false);

            // 4. 追加到 fields
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
        pluginEnabledGuard.check("entity_type_configs", id);
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
