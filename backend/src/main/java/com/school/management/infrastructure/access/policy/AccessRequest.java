package com.school.management.infrastructure.access.policy;

import java.util.Map;
import java.util.Set;

/**
 * 一次授权护栏检查的请求上下文：主体 × 动作 × 目标资源 × 属性。
 *
 * <p>主体字段是一等公民；目标资源的属性放在通用 {@code attrs} 包里（键见 {@link PolicyAttrs}），
 * 由 {@code UserManagementGuard} 等"解析器"在调用前填好，规则只读不查库 —— 保证规则是
 * 纯函数、可脱库单测。
 *
 * @param subjectUserId        操作者用户 id
 * @param subjectIsSuperAdmin  操作者是否超管
 * @param subjectManagedOrgIds 操作者持 admin 关系的组织 id 集（"他管理哪些组织"）
 * @param action               {@link AuthActions} 之一
 * @param resourceType         目标资源类型，如 "user"
 * @param resourceId           目标资源 id（如被操作的用户 id）
 * @param attrs                目标属性包，键见 {@link PolicyAttrs}
 */
public record AccessRequest(
        Long subjectUserId,
        boolean subjectIsSuperAdmin,
        Set<Long> subjectManagedOrgIds,
        String action,
        String resourceType,
        Long resourceId,
        Map<String, Object> attrs
) {
    /** 取布尔属性，缺失/非布尔按 false。 */
    public boolean attrBool(String key) {
        return attrs != null && attrs.get(key) instanceof Boolean b && b;
    }

    /** 取 Long 集合属性，缺失按空集。 */
    @SuppressWarnings("unchecked")
    public Set<Long> attrLongSet(String key) {
        Object v = attrs == null ? null : attrs.get(key);
        return v instanceof Set<?> s ? (Set<Long>) s : Set.of();
    }
}
