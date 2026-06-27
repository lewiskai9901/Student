package com.school.management.application.extension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.infrastructure.extension.EntityTypeCategories;
import com.school.management.domain.place.model.valueobject.BaseCategory;
import com.school.management.domain.user.model.entity.UserCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 统一实体类型配置 - 应用服务.
 *
 * 承载 EntityTypeConfigController 的全部数据访问逻辑 (SQL/jdbc),
 * 控制器仅保留 HTTP 映射、权限注解、请求/响应绑定与纯展示逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityTypeConfigApplicationService {

    private final JdbcTemplate jdbc;
    private final ApplicationContext appCtx;

    /**
     * 内部 JSON 编解码用。刻意用 vanilla ObjectMapper 而非 Spring 注入的全局 bean —
     * 全局 JacksonConfig 把所有 Long 序列化为 string (前端 53-bit 精度防丢), 那套 web 序列化策略
     * 不应渗进这里的配置 blob (features/allowed_child_type_codes/overridden_fields 等) 读写。
     * ObjectMapper 配置完成后线程安全, 单例复用即可, 不必每个方法 new 一个。
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SELECT_COLS =
        "id, entity_type AS entityType, type_code AS typeCode, type_name AS typeName, " +
        "category, parent_type_code AS parentTypeCode, allowed_child_type_codes AS allowedChildTypeCodes, " +
        "metadata_schema AS metadataSchema, features, ui_config AS uiConfig, " +
        "is_plugin_registered AS isPluginRegistered, is_enabled AS isEnabled, " +
        "plugin_enabled AS pluginEnabled, sort_order AS sortOrder, " +
        "plugin_class AS pluginClass, industry, origin, " +
        "overridden_fields AS overriddenFields";

    /**
     * 可被管理员覆写的字段白名单。
     * 必须与 {@link #update} 追踪进 overridden_fields 的键集 + {@link #resetField} 支持恢复的键集三者一致,
     * 否则会出现"可覆写却不可重置"的单向锁死 (features 等)。
     */
    public static final Set<String> OVERRIDABLE_FIELDS = Set.of(
            "typeName", "category", "uiConfig", "features", "parentTypeCode", "allowedChildTypeCodes");

    /**
     * 查询某实体类型下的所有类型配置。
     *
     * @param admin true=管理员视角(含被禁类型), false=仅启用且插件启用
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String entityType, String category, boolean admin) {
        StringBuilder sql = new StringBuilder("SELECT " + SELECT_COLS +
            " FROM entity_type_configs WHERE entity_type = ? AND deleted = 0");
        if (!admin) {
            sql.append(" AND is_enabled = 1 AND plugin_enabled = 1");
        }
        List<Object> params = new ArrayList<>();
        params.add(entityType);
        if (category != null) { sql.append(" AND category = ?"); params.add(category); }
        sql.append(" ORDER BY sort_order, type_name");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /** 查询单个类型配置详情。 */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(String entityType, String typeCode) {
        return jdbc.queryForMap(
            "SELECT " + SELECT_COLS + " FROM entity_type_configs WHERE entity_type = ? AND type_code = ? AND deleted = 0",
            entityType, typeCode);
    }

    /**
     * 查询父类型允许的子类型列表。
     *
     * @return 子类型配置列表，解析失败或无子类型时返回空列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> allowedChildren(String entityType, String parentTypeCode) {
        String childCodesJson;
        try {
            childCodesJson = jdbc.queryForObject(
                "SELECT allowed_child_type_codes FROM entity_type_configs WHERE entity_type = ? AND type_code = ? AND deleted = 0",
                String.class, entityType, parentTypeCode);
        } catch (Exception e) {
            return List.of();
        }

        if (childCodesJson == null || childCodesJson.equals("[]") || childCodesJson.equals("null")) {
            return List.of();
        }

        try {
            ObjectMapper om = MAPPER;
            @SuppressWarnings("unchecked")
            List<String> childCodes = om.readValue(childCodesJson, List.class);
            if (childCodes.isEmpty()) return List.of();

            String placeholders = String.join(",", Collections.nCopies(childCodes.size(), "?"));
            List<Object> params = new ArrayList<>();
            params.add(entityType);
            params.addAll(childCodes);

            return jdbc.queryForList(
                "SELECT " + SELECT_COLS + " FROM entity_type_configs WHERE entity_type = ? AND type_code IN (" + placeholders + ") AND deleted = 0 AND is_enabled = 1 AND plugin_enabled = 1 ORDER BY sort_order",
                params.toArray());
        } catch (Exception e) {
            log.warn("解析 allowed_child_type_codes 失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 允许的实体类型 (USER/ORG_UNIT/PLACE)。 */
    private static final Set<String> VALID_ENTITY_TYPES = Set.of("USER", "ORG_UNIT", "PLACE");

    /** 创建自定义类型配置。入参校验 + typeCode 唯一性预检 (避免裸抛唯一键 SQLException)。 */
    @Transactional
    public void create(Map<String, Object> data) throws Exception {
        String entityType = str(data.get("entityType"));
        String typeCode = str(data.get("typeCode"));
        String typeName = str(data.get("typeName"));
        if (entityType == null || !VALID_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("entityType 必须是 USER / ORG_UNIT / PLACE 之一");
        }
        if (typeCode == null || typeCode.isBlank()) {
            throw new IllegalArgumentException("typeCode 不能为空");
        }
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("typeName 不能为空");
        }
        if (!EntityTypeCategories.isValid(entityType, str(data.get("category")))) {
            throw new IllegalArgumentException("非法类型分类 '" + str(data.get("category")) + "': 实体 "
                + entityType + " 的合法分类为 [" + EntityTypeCategories.validValues(entityType) + "] 或留空");
        }
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM entity_type_configs WHERE entity_type = ? AND type_code = ? AND deleted = 0",
            Integer.class, entityType, typeCode);
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("类型编码已存在: " + typeCode);
        }
        ObjectMapper om = MAPPER;
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
    }

    /**
     * 更新类型配置。插件类型会自动维护 overridden_fields。
     *
     * @throws IllegalArgumentException features 子集校验失败
     */
    @Transactional
    public void update(Long id, Map<String, Object> data) throws Exception {
        ObjectMapper om = MAPPER;

        // 读当前行: 判断是否插件类型 + 当前值 + 已有覆写集合
        Map<String, Object> current = jdbc.queryForMap(
            "SELECT is_plugin_registered, type_name, category, ui_config, features, " +
            "parent_type_code, allowed_child_type_codes, overridden_fields " +
            "FROM entity_type_configs WHERE id=? AND deleted=0", id);
        boolean isPlugin = toBool(current.get("is_plugin_registered"));
        Set<String> overridden = parseOverriddenFields(current.get("overridden_fields"), om);

        // 新分类必须合法 (空=不改, 不校验)
        String newCategory = str(data.get("category"));
        if (newCategory != null && !newCategory.isBlank()
                && !EntityTypeCategories.isValid(existingEntityType(id), newCategory)) {
            throw new IllegalArgumentException("非法类型分类 '" + newCategory + "': 合法分类为 ["
                + EntityTypeCategories.validValues(existingEntityType(id)) + "] 或留空");
        }
        // features 子集校验: 不允许启用不在 category 默认集内的 feature。
        // category 缺省 (只改 features 的 partial-update) 时回退当前行 category, 否则校验被绕过。
        String effectiveCategory = (newCategory != null && !newCategory.isBlank())
                ? newCategory : str(current.get("category"));
        Object featuresObj = data.get("features");
        if (featuresObj != null && effectiveCategory != null && !effectiveCategory.isBlank()) {
            validateFeaturesSubset(featuresObj, effectiveCategory, existingEntityType(id));
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
            // features / parentTypeCode / allowedChildTypeCodes 同样可被管理员覆写 —
            // 必须独立追踪, 否则 PluginRegistrar 重启合并时会用插件声明覆盖管理员改动。
            if (featuresObj != null) {
                String newFeatures = featuresObj instanceof String
                        ? (String) featuresObj : om.writeValueAsString(featuresObj);
                String curFeatures = str(current.get("features"));
                if (!Objects.equals(newFeatures, curFeatures)) overridden.add("features");
            }
            if (data.containsKey("parentTypeCode")) {
                String newParent = str(data.get("parentTypeCode"));
                String curParent = str(current.get("parent_type_code"));
                if (!Objects.equals(newParent, curParent)) overridden.add("parentTypeCode");
            }
            if (data.containsKey("allowedChildTypeCodes")) {
                String newChildren = om.writeValueAsString(data.getOrDefault("allowedChildTypeCodes", List.of()));
                String curChildren = str(current.get("allowed_child_type_codes"));
                if (!Objects.equals(newChildren, curChildren)) overridden.add("allowedChildTypeCodes");
            }
        }

        // 动态列更新: 只 SET 请求里实际出现的字段, 避免 partial-update 把未传字段清空
        // (此前无条件写全 7 列, 只改 typeName 的请求会把 category/parent/features/ui_config 抹成 NULL、
        //  children 重置 []。改为按 data.containsKey 动态拼列)。
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (data.containsKey("typeName")) { setClauses.add("type_name=?"); params.add(data.get("typeName")); }
        if (data.containsKey("category")) { setClauses.add("category=?"); params.add(newCategory); }
        if (data.containsKey("parentTypeCode")) { setClauses.add("parent_type_code=?"); params.add(data.get("parentTypeCode")); }
        if (data.containsKey("allowedChildTypeCodes")) {
            setClauses.add("allowed_child_type_codes=?");
            params.add(om.writeValueAsString(data.getOrDefault("allowedChildTypeCodes", List.of())));
        }
        if (featuresObj != null) {
            setClauses.add("features=?");
            params.add(featuresObj instanceof String ? (String) featuresObj : om.writeValueAsString(featuresObj));
        }
        if (data.containsKey("uiConfig")) {
            setClauses.add("ui_config=?");
            params.add(data.get("uiConfig") instanceof String ? (String) data.get("uiConfig")
                    : (data.get("uiConfig") != null ? om.writeValueAsString(data.get("uiConfig")) : null));
        }
        // overridden_fields 始终随插件类型同步 (反映本次覆写追踪结果)
        if (isPlugin) {
            setClauses.add("overridden_fields=?");
            params.add(om.writeValueAsString(new ArrayList<>(overridden)));
        }
        if (setClauses.isEmpty()) {
            return; // 无任何字段需要更新
        }
        params.add(id);
        jdbc.update(
            "UPDATE entity_type_configs SET " + String.join(", ", setClauses) + " WHERE id=? AND deleted=0",
            params.toArray());
    }

    /**
     * 恢复某个字段为插件默认值。
     *
     * @return null=成功; 非 null=错误消息
     */
    @Transactional
    public String resetField(Long id, String field) throws Exception {
        ObjectMapper om = MAPPER;
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT is_plugin_registered, plugin_class, overridden_fields " +
            "FROM entity_type_configs WHERE id=? AND deleted=0", id);
        if (!toBool(row.get("is_plugin_registered"))) {
            return "非插件类型无需恢复";
        }
        String pluginClass = str(row.get("plugin_class"));
        if (pluginClass == null) {
            return "无插件类, 无法恢复默认值";
        }
        // 从 classpath 反射插件 bean 拿原始值
        Object original = resolvePluginOriginal(pluginClass, field);
        if (original == null) {
            return "插件 " + pluginClass + " 未提供字段 [" + field + "] 的声明";
        }
        Set<String> overridden = parseOverriddenFields(row.get("overridden_fields"), om);
        overridden.remove(field);
        // category 恢复会连带把 features 也写回插件默认 (见下方 switch), 故 features 的覆写标记
        // 也必须一并清除, 否则 overridden 残留 "features" 会让下次 PluginRegistrar 合并保留已被冲掉的值。
        if ("category".equals(field)) {
            overridden.remove("features");
        }
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
            case "features":
                String featJson = original instanceof String ? (String) original : om.writeValueAsString(original);
                jdbc.update("UPDATE entity_type_configs SET features=?, overridden_fields=? WHERE id=? AND deleted=0",
                        featJson, overriddenJson, id);
                break;
            case "parentTypeCode":
                jdbc.update("UPDATE entity_type_configs SET parent_type_code=?, overridden_fields=? WHERE id=? AND deleted=0",
                        original, overriddenJson, id);
                break;
            case "allowedChildTypeCodes":
                String childJson = original instanceof String ? (String) original : om.writeValueAsString(original);
                jdbc.update("UPDATE entity_type_configs SET allowed_child_type_codes=?, overridden_fields=? WHERE id=? AND deleted=0",
                        childJson, overriddenJson, id);
                break;
        }
        return null;
    }

    /**
     * 统计某实体类型(typeCode)当前被多少存量实体在用 (deleted=0)。
     *
     * <p>实体类型→宿主表: USER→users.user_type_code / ORG_UNIT→org_units.type_code /
     * PLACE→places.type_code。未知 entityType 返回 0。用于删除保护与前端"实例数"展示。
     */
    @Transactional(readOnly = true)
    public long countEntitiesUsingType(String entityType, String typeCode) {
        if (entityType == null || typeCode == null || typeCode.isBlank()) return 0;
        String sql;
        switch (entityType) {
            case "USER":     sql = "SELECT COUNT(*) FROM users WHERE user_type_code = ? AND deleted = 0"; break;
            case "ORG_UNIT": sql = "SELECT COUNT(*) FROM org_units WHERE type_code = ? AND deleted = 0"; break;
            case "PLACE":    sql = "SELECT COUNT(*) FROM places WHERE type_code = ? AND deleted = 0"; break;
            default: return 0;
        }
        Long n = jdbc.queryForObject(sql, Long.class, typeCode);
        return n == null ? 0 : n;
    }

    /**
     * 按类型配置 id 统计其被多少存量实体在用 (前端 usage-count 端点用)。
     */
    @Transactional(readOnly = true)
    public long usageCount(Long id) {
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT entity_type, type_code FROM entity_type_configs WHERE id=? AND deleted=0", id);
        return countEntitiesUsingType(str(row.get("entity_type")), str(row.get("type_code")));
    }

    /**
     * 逻辑删除类型配置。插件类型不可删; 仍有实体在用的类型不可删 (防孤儿化)。
     *
     * @return null=成功; 非 null=错误消息
     */
    @Transactional
    public String delete(Long id) {
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT is_plugin_registered, entity_type, type_code FROM entity_type_configs WHERE id=? AND deleted=0", id);
        if (toBool(row.get("is_plugin_registered"))) {
            return "插件注册的类型不能删除";
        }
        // 删除保护: 仍有实体引用该 type_code 时拒绝, 否则存量实体类型指针孤儿化
        long inUse = countEntitiesUsingType(str(row.get("entity_type")), str(row.get("type_code")));
        if (inUse > 0) {
            return "该类型正在被 " + inUse + " 个实体使用，不能删除";
        }
        jdbc.update("UPDATE entity_type_configs SET deleted=1 WHERE id=?", id);
        return null;
    }

    /**
     * 向类型配置的 metadata_schema 追加自定义字段。
     *
     * @return null=成功; 非 null=错误消息
     */
    @Transactional
    public String addCustomField(Long id, Map<String, Object> field, String key) throws Exception {
        String schemaStr = jdbc.queryForObject(
            "SELECT metadata_schema FROM entity_type_configs WHERE id = ? AND deleted = 0", String.class, id);
        ObjectMapper om = MAPPER;
        Map<String, Object> schema = om.readValue(schemaStr, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", new ArrayList<>());

        // 检查 key 重复
        for (Map<String, Object> existing : fields) {
            if (key.equals(existing.get("key"))) {
                return "字段 key 已存在: " + key;
            }
        }

        // 标记为非系统字段
        field.put("system", false);

        // 追加到 fields
        fields.add(field);
        schema.put("fields", fields);
        jdbc.update("UPDATE entity_type_configs SET metadata_schema = ? WHERE id = ?",
            om.writeValueAsString(schema), id);
        return null;
    }

    /** 从类型配置的 metadata_schema 移除指定自定义字段 (仅 system=false)。 */
    @Transactional
    public void removeCustomField(Long id, String fieldKey) throws Exception {
        String schemaStr = jdbc.queryForObject(
            "SELECT metadata_schema FROM entity_type_configs WHERE id = ? AND deleted = 0", String.class, id);
        ObjectMapper om = MAPPER;
        Map<String, Object> schema = om.readValue(schemaStr, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) schema.getOrDefault("fields", new ArrayList<>());
        // 只能删除 system=false 的字段
        fields.removeIf(f -> fieldKey.equals(f.get("key")) && !Boolean.TRUE.equals(f.get("system")));
        schema.put("fields", fields);
        jdbc.update("UPDATE entity_type_configs SET metadata_schema = ? WHERE id = ?",
            om.writeValueAsString(schema), id);
    }

    // ===== 内部辅助 (含 SQL 与插件反射) =====

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
                case "parentTypeCode": return invoke(bean, "getParentTypeCode");
                case "allowedChildTypeCodes": return invoke(bean, "getAllowedChildTypeCodes");
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

    private Set<String> parseOverriddenFields(Object raw, ObjectMapper om) {
        if (raw == null) return new HashSet<>();
        String s = raw.toString();
        if (s.isBlank()) return new HashSet<>();
        try {
            List<String> list = om.readValue(s, new TypeReference<List<String>>() {});
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
     * features 只校验 category 已声明的那 5 个通用 capability。
     */
    private void validateFeaturesSubset(Object featuresObj, String categoryCode, String entityType) {
        try {
            ObjectMapper om = MAPPER;
            Map<String, Boolean> features = featuresObj instanceof String
                    ? om.readValue((String) featuresObj, new TypeReference<Map<String, Boolean>>() {})
                    : om.convertValue(featuresObj, new TypeReference<Map<String, Boolean>>() {});
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
}
