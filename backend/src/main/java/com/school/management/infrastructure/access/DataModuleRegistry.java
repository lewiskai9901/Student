package com.school.management.infrastructure.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据模块注册表 (V5)
 * 从数据库加载模块配置，提供运行时查询
 */
@Slf4j
@Component
public class DataModuleRegistry {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 模块配置缓存
     * Key: moduleCode
     */
    private final Map<String, ModuleConfig> moduleConfigs = new ConcurrentHashMap<>();

    /**
     * 范围类型缓存
     * Key: scopeCode
     */
    private final Map<String, ScopeTypeConfig> scopeTypes = new ConcurrentHashMap<>();

    /**
     * 范围项类型缓存
     * Key: itemTypeCode
     */
    private final Map<String, ScopeItemTypeConfig> scopeItemTypes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadModuleConfigs();
        loadScopeTypes();
        loadScopeItemTypes();
        log.info("DataModuleRegistry initialized: {} modules, {} scope types, {} item types",
                moduleConfigs.size(), scopeTypes.size(), scopeItemTypes.size());
    }

    /**
     * 从数据库加载模块配置
     */
    private void loadModuleConfigs() {
        try {
            String sql = "SELECT module_code, module_name, domain_code, domain_name, " +
                    "filter_fields, main_table, enable_data_permission, sort_order " +
                    "FROM data_modules WHERE is_enabled = 1";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> row : rows) {
                ModuleConfig config = new ModuleConfig();
                config.setModuleCode((String) row.get("module_code"));
                config.setModuleName((String) row.get("module_name"));
                config.setDomainCode((String) row.get("domain_code"));
                config.setDomainName((String) row.get("domain_name"));
                config.setMainTable((String) row.get("main_table"));
                config.setEnableDataPermission(getIntValue(row, "enable_data_permission") == 1);
                config.setSortOrder(getIntValue(row, "sort_order"));

                // 解析filterFields JSON
                String filterFieldsJson = (String) row.get("filter_fields");
                if (filterFieldsJson != null && !filterFieldsJson.isEmpty()) {
                    try {
                        Map<String, String> filterFields = objectMapper.readValue(
                                filterFieldsJson, new TypeReference<Map<String, String>>() {});
                        config.setFilterFields(filterFields);
                    } catch (Exception e) {
                        log.warn("Failed to parse filter_fields for module {}: {}", config.getModuleCode(), e.getMessage());
                        config.setFilterFields(new HashMap<>());
                    }
                } else {
                    config.setFilterFields(new HashMap<>());
                }

                moduleConfigs.put(config.getModuleCode(), config);
            }
        } catch (Exception e) {
            log.error("Failed to load module configs", e);
        }
    }

    /**
     * 从数据库加载范围类型配置
     */
    private void loadScopeTypes() {
        try {
            String sql = "SELECT scope_code, scope_name, scope_level, calc_type, description " +
                    "FROM data_scope_types WHERE is_enabled = 1 ORDER BY sort_order";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> row : rows) {
                ScopeTypeConfig config = new ScopeTypeConfig();
                config.setScopeCode((String) row.get("scope_code"));
                config.setScopeName((String) row.get("scope_name"));
                config.setScopeLevel(getIntValue(row, "scope_level"));
                config.setCalcType((String) row.get("calc_type"));
                config.setDescription((String) row.get("description"));
                scopeTypes.put(config.getScopeCode(), config);
            }
        } catch (Exception e) {
            log.error("Failed to load scope types", e);
        }
    }

    /**
     * 从数据库加载范围项类型配置
     */
    private void loadScopeItemTypes() {
        try {
            String sql = "SELECT item_type_code, item_type_name, ref_table, ref_id_field, " +
                    "ref_name_field, ref_parent_field, filter_field_key, support_children " +
                    "FROM scope_item_types WHERE is_enabled = 1 ORDER BY sort_order";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> row : rows) {
                ScopeItemTypeConfig config = new ScopeItemTypeConfig();
                config.setItemTypeCode((String) row.get("item_type_code"));
                config.setItemTypeName((String) row.get("item_type_name"));
                config.setRefTable((String) row.get("ref_table"));
                config.setRefIdField((String) row.get("ref_id_field"));
                config.setRefNameField((String) row.get("ref_name_field"));
                config.setRefParentField((String) row.get("ref_parent_field"));
                config.setFilterFieldKey((String) row.get("filter_field_key"));
                config.setSupportChildren(getIntValue(row, "support_children") == 1);
                scopeItemTypes.put(config.getItemTypeCode(), config);
            }
        } catch (Exception e) {
            log.error("Failed to load scope item types", e);
        }
    }

    private int getIntValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    /**
     * 获取模块配置
     */
    public ModuleConfig getModuleConfig(String moduleCode) {
        return moduleConfigs.get(moduleCode);
    }

    /**
     * 获取所有模块配置
     */
    public Map<String, ModuleConfig> getAllModuleConfigs() {
        return new HashMap<>(moduleConfigs);
    }

    /**
     * 获取范围类型配置
     */
    public ScopeTypeConfig getScopeTypeConfig(String scopeCode) {
        return scopeTypes.get(scopeCode);
    }

    /**
     * 获取所有范围类型配置
     */
    public Map<String, ScopeTypeConfig> getAllScopeTypes() {
        return new HashMap<>(scopeTypes);
    }

    /**
     * 获取范围项类型配置
     */
    public ScopeItemTypeConfig getScopeItemTypeConfig(String itemTypeCode) {
        return scopeItemTypes.get(itemTypeCode);
    }

    /**
     * 获取所有范围项类型配置
     */
    public Map<String, ScopeItemTypeConfig> getAllScopeItemTypes() {
        return new HashMap<>(scopeItemTypes);
    }

    /**
     * 刷新缓存
     */
    public void refresh() {
        moduleConfigs.clear();
        scopeTypes.clear();
        scopeItemTypes.clear();
        init();
    }

    // ==================== 配置类 ====================

    @Data
    public static class ModuleConfig {
        private String moduleCode;
        private String moduleName;
        private String domainCode;
        private String domainName;
        private Map<String, String> filterFields;
        private String mainTable;
        private boolean enableDataPermission;
        private int sortOrder;

        public String getFilterField(String key) {
            return filterFields != null ? filterFields.get(key) : null;
        }

        public String getOrgUnitField() {
            return getFilterField("org_unit");
        }

        public String getClassField() {
            return getFilterField("class");
        }

        public String getCreatorField() {
            return getFilterField("creator");
        }
    }

    @Data
    public static class ScopeTypeConfig {
        private String scopeCode;
        private String scopeName;
        private int scopeLevel;
        private String calcType;
        private String description;
    }

    @Data
    public static class ScopeItemTypeConfig {
        private String itemTypeCode;
        private String itemTypeName;
        private String refTable;
        private String refIdField;
        private String refNameField;
        private String refParentField;
        private String filterFieldKey;
        private boolean supportChildren;

        public boolean isTreeStructure() {
            return refParentField != null && !refParentField.isEmpty();
        }
    }
}
