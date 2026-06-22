package com.school.management.infrastructure.extension;

/**
 * 解析上下文 —— 传给 {@link RecordRelationResolver} 的最小信息。
 *
 * <p>解耦 SPI 与引擎内部 UserContext:resolver 只拿到"当前主体是谁、在查哪个资源",
 * 据此算出该主体可见的记录集。
 *
 * @param userId       当前登录用户 id (= 主体 id)
 * @param subjectType  主体类型 (一般 "USER")
 * @param resourceCode 正在过滤的资源码 (= @DataPermission.module)
 * @param tenantId     租户 id (单租户恒 1)
 */
public record ScopeContext(Long userId, String subjectType, String resourceCode, Long tenantId) {
}
