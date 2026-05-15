package com.school.management.config.openapi;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * springdoc ModelConverter — 把所有 Long(int64) 字段在 schema 中改为 string.
 *
 * <p>背景: {@code com.school.management.config.JacksonConfig} 全局注册了
 * {@code ToStringSerializer} for {@code Long.class} 和 {@code Long.TYPE}, 把所有
 * Long 字段在 HTTP JSON 输出里序列化为 string (防 JS 53-bit 大数精度丢失).
 *
 * <p>但 springdoc 默认生成的 OpenAPI schema 不知道这件事, 把 Long 字段标为
 * {@code {"type": "integer", "format": "int64"}}. 这导致 OpenAPI 文档与实际
 * wire format 不一致 — 下游 SDK codegen 会生成错误的 number 类型.
 *
 * <p>本 Converter 拦截 schema 生成, 把 Long 字段改为
 * {@code {"type": "string", "format": "int64", "x-typescript-type": "LongId"}}.
 * 前端 codegen (@hey-api/openapi-ts) 读到 x-typescript-type 后会自动映射成
 * LongId 类型 (= string alias), 实现 100% 类型契约对齐.
 */
@Component
public class LongAsStringModelConverter implements ModelConverter {

    private static final String LONG_ID_EXTENSION_KEY = "x-typescript-type";
    private static final String LONG_ID_EXTENSION_VALUE = "LongId";

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            Schema<?> resolved = chain.next().resolve(type, context, chain);
            if (resolved != null) {
                rewriteInt64ToString(resolved);
            }
            return resolved;
        }
        return null;
    }

    /**
     * 递归扫 schema, 把 type=integer/format=int64 改为 type=string/format=int64.
     */
    private void rewriteInt64ToString(Schema<?> schema) {
        if (schema == null) return;

        // 自身是 int64
        if ("integer".equals(schema.getType()) && "int64".equals(schema.getFormat())) {
            schema.setType("string");
            // 保留 format=int64 让 codegen 能识别这是 Long
            schema.addExtension(LONG_ID_EXTENSION_KEY, LONG_ID_EXTENSION_VALUE);
        }

        // 递归 properties
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, prop) -> rewriteInt64ToString(prop));
        }
        // additionalProperties (e.g. Map<String, Long>)
        if (schema.getAdditionalProperties() instanceof Schema) {
            rewriteInt64ToString((Schema<?>) schema.getAdditionalProperties());
        }
        // items (array)
        if (schema.getItems() != null) {
            rewriteInt64ToString(schema.getItems());
        }
        // oneOf / anyOf / allOf
        if (schema.getOneOf() != null) schema.getOneOf().forEach(this::rewriteInt64ToString);
        if (schema.getAnyOf() != null) schema.getAnyOf().forEach(this::rewriteInt64ToString);
        if (schema.getAllOf() != null) schema.getAllOf().forEach(this::rewriteInt64ToString);
    }
}
