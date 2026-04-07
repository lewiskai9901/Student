package com.school.management.infrastructure.extension;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 扩展字段定义 — 描述一个动态表单字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    private String key;           // 字段key，存入 attributes JSON
    private String label;         // 显示名
    private String type;          // text|number|date|boolean|select|relation|user|textarea|tags
    private String group;         // 分组名
    private boolean required;     // 是否必填
    @Builder.Default
    private boolean system = true; // true=系统字段(插件定义), false=管理员自定义
    private Object defaultValue;  // 默认值
    private Map<String, Object> config;  // 额外配置(options, target, min, max, role 等)

    public static FieldDefinition of(String key, String label, String type, String group,
                                      boolean required, Map<String, Object> config) {
        return FieldDefinition.builder()
            .key(key).label(label).type(type).group(group)
            .required(required).system(true).config(config)
            .build();
    }
}
