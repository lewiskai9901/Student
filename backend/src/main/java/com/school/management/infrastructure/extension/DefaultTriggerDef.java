package com.school.management.infrastructure.extension;

/**
 * 默认触发器定义 (对应 event_triggers 表一行)。双轨收敛: 从 MessagingDomainPlugin 提为顶层 record。
 * 把触发点默认映射到事件类型, admin 可在 UI 里覆盖。
 *
 * @param pointCode      触发点码
 * @param eventTypeCode  事件类型码
 * @param subjectsExpr   JSON 表达式, 如 [{"type":"USER","id":"{{occupantId}}"}]
 */
public record DefaultTriggerDef(
    String pointCode,
    String eventTypeCode,
    Object subjectsExpr
) {}
