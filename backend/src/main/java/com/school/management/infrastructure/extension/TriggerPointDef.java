package com.school.management.infrastructure.extension;

import java.util.Map;

/**
 * 触发点定义 (对应 trigger_points 表一行)。双轨收敛: 从 MessagingDomainPlugin 提为顶层 record。
 *
 * @param pointCode      如 "DORM_CHECKIN"
 * @param pointName      中文名 "宿舍入住"
 * @param contextSchema  期望的 context 字段 schema, 如 {"occupantId":"Long","placeId":"Long"}
 * @param description    业务描述
 */
public record TriggerPointDef(
    String pointCode,
    String pointName,
    Map<String, String> contextSchema,
    String description
) {
    public static TriggerPointDef of(String code, String name, Map<String, String> schema) {
        return new TriggerPointDef(code, name, schema, null);
    }
    public TriggerPointDef withDescription(String desc) {
        return new TriggerPointDef(pointCode, pointName, contextSchema, desc);
    }
}
