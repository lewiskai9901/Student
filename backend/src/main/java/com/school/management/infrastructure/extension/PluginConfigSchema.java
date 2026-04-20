package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * Phase 7.5: 插件配置 schema SPI — 声明插件接受哪些运行时配置项.
 *
 * 管理员 UI 可按这个 schema 自动渲染配置表单, 并调用
 * {@code TenantPluginService.setConfig()} 存到 tenant_plugin_enablement.config_json.
 *
 * 插件代码读配置: {@code TenantPluginService.getConfig(tenantId, code, key)}.
 *
 * 典型用法:
 * <pre>
 * public class HealthcareManifest implements PluginPackage {
 *   &#64;Override
 *   public PluginConfigSchema configSchema() {
 *       return new PluginConfigSchema(List.of(
 *           PluginConfigSchema.Field.stringField("admissionWardPrefix", "入院病区前缀", "A-", false),
 *           PluginConfigSchema.Field.booleanField("autoAssignBed", "自动床位分配", true)
 *       ));
 *   }
 * }
 * </pre>
 *
 * @param fields 配置字段清单 (可空)
 */
public record PluginConfigSchema(List<Field> fields) {

    public static PluginConfigSchema empty() {
        return new PluginConfigSchema(List.of());
    }

    public boolean isEmpty() {
        return fields == null || fields.isEmpty();
    }

    /**
     * 配置字段定义 — 驱动管理员 UI 表单控件渲染.
     *
     * @param key          配置键 (snake_case, 如 admission_ward_prefix)
     * @param label        中文显示名 (入院病区前缀)
     * @param type         "string" | "boolean" | "number" | "enum"
     * @param required    是否必填
     * @param defaultValue 默认值 (字符串表示)
     * @param description  业务说明 (UI 悬浮提示)
     * @param enumValues   type=enum 时的枚举候选 (null 其他 type)
     */
    public record Field(String key, String label, String type, boolean required,
                        Object defaultValue, String description, List<String> enumValues) {

        public static Field stringField(String key, String label, String defaultValue, boolean required) {
            return new Field(key, label, "string", required, defaultValue, null, null);
        }

        public static Field booleanField(String key, String label, boolean defaultValue) {
            return new Field(key, label, "boolean", false, defaultValue, null, null);
        }

        public static Field numberField(String key, String label, Number defaultValue, boolean required) {
            return new Field(key, label, "number", required, defaultValue, null, null);
        }

        public static Field enumField(String key, String label, List<String> values, String defaultValue) {
            return new Field(key, label, "enum", false, defaultValue, null, values);
        }

        public Field withDescription(String desc) {
            return new Field(key, label, type, required, defaultValue, desc, enumValues);
        }
    }
}
