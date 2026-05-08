package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AccessRelation.metadata JSON Schema 校验器 (Phase 4 W4.3 骨架).
 *
 * <p>当前最小实现: 支持插件通过 {@link #registerRequiredKeys(String, String...)}
 * 注册 schema, {@link #validate(String, java.util.Map)} 时校验。
 *
 * <p>暂未集成 everit-json-schema 库 (避免新依赖) — 当前实现只做基础校验:
 * <ul>
 *   <li>未注册 schema 的关系不校验 (默认通过)</li>
 *   <li>注册了 schema 但是空内容 → 通过</li>
 *   <li>注册了 schema 且 metadata 为 null → 失败 (required 字段缺失)</li>
 *   <li>注册了 required 字段列表 → metadata 必须含这些 key</li>
 * </ul>
 *
 * <p>未来 (Phase 5) 集成 everit / networknt 库可在 {@link #validate} 中引入完整
 * JSON Schema 校验, DB 列 {@code relation_types.metadata_schema} 已在 V20260509_4 预留。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataSchemaValidator {

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    /** relation_code → simplified schema (required keys list) */
    private final Map<String, String[]> requiredKeysIndex = new ConcurrentHashMap<>();

    /**
     * 注册某关系的必要 metadata key。
     * 启动期由插件调用, 运行时静态。
     */
    public void registerRequiredKeys(String relationCode, String... requiredKeys) {
        if (requiredKeys != null && requiredKeys.length > 0) {
            requiredKeysIndex.put(relationCode, requiredKeys);
            log.debug("[MetadataSchema] 注册关系 {} 必填 keys: {}",
                relationCode, String.join(",", requiredKeys));
        }
    }

    /** 验证 metadata 满足关系的 schema 要求。失败抛 IllegalArgumentException。 */
    public void validate(String relationCode, Map<String, Object> metadata) {
        String[] required = requiredKeysIndex.get(relationCode);
        if (required == null) return;  // 未注册 schema, 跳过
        for (String key : required) {
            if (metadata == null || !metadata.containsKey(key)) {
                throw new IllegalArgumentException(
                    "关系 '" + relationCode + "' 的 metadata 缺少必填字段: " + key);
            }
        }
    }
}
