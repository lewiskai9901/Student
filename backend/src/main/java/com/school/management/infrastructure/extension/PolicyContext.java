package com.school.management.infrastructure.extension;

/**
 * 策略检查上下文 — 封装 (被检查实体类型, 操作阶段, payload).
 *
 * 常用 phase 约定:
 *   BEFORE_CREATE / AFTER_CREATE
 *   BEFORE_UPDATE / AFTER_UPDATE
 *   BEFORE_DELETE
 *   BEFORE_CHECKIN / AFTER_CHECKIN   (场所)
 *   BEFORE_ADD_MEMBER / AFTER_ADD_MEMBER (组织)
 *
 * entityType 用领域码 (与 entity_type_configs.entity_type 对齐):
 *   "place" / "org_unit" / "user" / "role" / 及插件贡献的自定义类型
 *
 * @param entityType 实体类型码
 * @param phase      操作阶段码
 * @param payload    本次操作的上下文对象 (Command / Entity / Map / 都可)
 */
public record PolicyContext<T>(String entityType, String phase, T payload) {}
