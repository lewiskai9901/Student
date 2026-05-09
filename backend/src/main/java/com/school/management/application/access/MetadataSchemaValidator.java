package com.school.management.application.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AccessRelation.metadata JSON Schema 校验器.
 *
 * <p>Phase 7 W7.2 — 接 networknt-json-schema-validator (draft 2020-12) 完整库.
 *
 * <p>支持两种注册方式:
 * <ul>
 *   <li>{@link #registerRequiredKeys(String, String...)} — 旧 API,
 *       仅声明 required keys, 内部转 schema 等价物</li>
 *   <li>{@link #registerSchema(String, String)} — 注册完整 JSON Schema 字符串
 *       (含 type / properties / enum / pattern / minimum / maxLength 等)</li>
 * </ul>
 *
 * <p>未注册的关系直接放行 (默认通过).
 *
 * <p>DB 列 {@code relation_types.metadata_schema} 已在 V20260509_4 预留,
 * Phase 5 启动后可由 {@code RelationTypeBootstrap} 自动注册.
 */
@Slf4j
@Component
public class MetadataSchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    /** relation_code → 编译好的 JSON Schema. */
    private final Map<String, JsonSchema> compiledSchemaIndex = new ConcurrentHashMap<>();

    public MetadataSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * 兼容旧 API — 仅声明 required keys.
     * 内部转换成 {@code {"type":"object","required":[...]}} 等价 schema.
     */
    public void registerRequiredKeys(String relationCode, String... requiredKeys) {
        if (requiredKeys == null || requiredKeys.length == 0) return;
        try {
            StringBuilder sb = new StringBuilder("{\"type\":\"object\",\"required\":[");
            for (int i = 0; i < requiredKeys.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(requiredKeys[i]).append("\"");
            }
            sb.append("]}");
            JsonSchema schema = schemaFactory.getSchema(sb.toString());
            compiledSchemaIndex.put(relationCode, schema);
            log.debug("[MetadataSchema] 注册关系 {} 必填 keys: {}",
                relationCode, String.join(",", requiredKeys));
        } catch (Exception e) {
            log.warn("[MetadataSchema] 编译 required keys schema 失败 {}: {}",
                relationCode, e.getMessage());
        }
    }

    /** 注册完整 JSON Schema. */
    public void registerSchema(String relationCode, String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) return;
        try {
            JsonSchema schema = schemaFactory.getSchema(schemaJson);
            compiledSchemaIndex.put(relationCode, schema);
            log.debug("[MetadataSchema] 注册关系 {} JSON Schema (字符数 {})",
                relationCode, schemaJson.length());
        } catch (Exception e) {
            log.warn("[MetadataSchema] 注册 schema 失败 {}: {}", relationCode, e.getMessage());
        }
    }

    /** 验证 metadata 满足关系的 schema 要求。失败抛 IllegalArgumentException。 */
    public void validate(String relationCode, Map<String, Object> metadata) {
        JsonSchema schema = compiledSchemaIndex.get(relationCode);
        if (schema == null) return;  // 未注册 → 不校验

        try {
            JsonNode node = objectMapper.valueToTree(metadata != null ? metadata : Map.of());
            Set<ValidationMessage> errors = schema.validate(node);
            if (!errors.isEmpty()) {
                String detail = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
                throw new IllegalArgumentException(
                    "关系 '" + relationCode + "' 的 metadata 校验失败: " + detail);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MetadataSchema] validate 异常 (relation={}): {}", relationCode, e.getMessage(), e);
            throw new IllegalArgumentException("metadata 校验异常: " + e.getMessage(), e);
        }
    }
}
