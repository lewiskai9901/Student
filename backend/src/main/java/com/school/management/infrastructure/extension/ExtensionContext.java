package com.school.management.infrastructure.extension;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * SPI 扩展上下文 — 传递给插件生命周期钩子的参数
 */
@Data
@Builder
public class ExtensionContext {
    private String entityType;                    // ORG_UNIT / PLACE / USER
    private String typeCode;                      // CLASS / CLASSROOM / TEACHER
    private Long entityId;
    private String entityName;
    private Long parentId;
    private Map<String, Object> attributes;
    private Map<String, Object> oldAttributes;
    private Long operatorId;

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOld(String key) {
        return oldAttributes != null ? (T) oldAttributes.get(key) : null;
    }
}
