package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 事件类型定义 (对应 entity_event_types 表一行)。双轨收敛: 从 MessagingDomainPlugin 提为顶层 record。
 *
 * @param typeCode            如 "DORM_CHECKIN_EVT"
 * @param typeName            中文名 "入住登记"
 * @param categoryCode        "PERSONNEL"/"ACADEMIC"/"DISCIPLINE"/"AWARD"/"TEACHING"/"PLACE"
 * @param categoryPolarity    "POSITIVE"/"NEGATIVE"/"NEUTRAL"
 * @param icon                图标标识
 * @param color               颜色 hex
 * @param applicableSubjects  适用的 subject_type 集合 [USER/ORG_UNIT/PLACE]
 */
public record EventTypeDef(
    String typeCode,
    String typeName,
    String categoryCode,
    String categoryName,
    String categoryPolarity,
    String icon,
    String color,
    List<String> applicableSubjects,
    String description
) {}
