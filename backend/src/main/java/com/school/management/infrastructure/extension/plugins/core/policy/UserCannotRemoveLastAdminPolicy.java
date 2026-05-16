package com.school.management.infrastructure.extension.plugins.core.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 组织最后一个管理员不可移除策略 (BLOCK).
 *
 * <p>语义:BEFORE_REMOVE_MEMBER (org_unit) 阶段, 校验组织至少保留 1 个 admin
 * relation. 移除最后一个 admin → 阻断, 防止孤儿组织.
 *
 * <p>payload 约定: Map 含
 * <ul>
 *   <li>orgUnitId (Long): 必须</li>
 *   <li>userId (Long): 要移除的用户</li>
 * </ul>
 *
 * <p>逻辑: 查 access_relations WHERE relation IN ('admin','SCHOOL_ADMIN','DEPT_ADMIN','TENANT_ADMIN')
 * AND resource_type='org_unit' AND resource_id=orgUnitId. 若该 user 是最后一个,
 * 阻断.
 */
@Slf4j
@Component
public class UserCannotRemoveLastAdminPolicy implements Policy<Object> {

    private final JdbcTemplate jdbc;

    public UserCannotRemoveLastAdminPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public String code() { return "ORG_LAST_ADMIN_PROTECTION"; }
    @Override public String name() { return "组织最后管理员保护"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "org_unit".equals(ctx.entityType())
            && ("BEFORE_REMOVE_MEMBER".equals(ctx.phase())
                || "BEFORE_REVOKE_ROLE".equals(ctx.phase()));
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (!(ctx.payload() instanceof Map<?, ?> m)) return List.of();
        Object orgObj = m.get("orgUnitId");
        Object userObj = m.get("userId");
        if (!(orgObj instanceof Long orgUnitId) || !(userObj instanceof Long userId)) {
            return List.of();
        }

        try {
            // 1. 检查这位 user 当前是否 admin
            Integer isAdminCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM access_relations " +
                "WHERE deleted = 0 AND subject_type = 'user' AND subject_id = ? " +
                "  AND resource_type = 'org_unit' AND resource_id = ? " +
                "  AND relation IN ('admin', 'SCHOOL_ADMIN', 'DEPT_ADMIN', 'TENANT_ADMIN', " +
                "                    'CLASS_TEACHER', 'GRADE_DIRECTOR', 'ACADEMIC_DIRECTOR')",
                Integer.class, userId, orgUnitId);
            if (isAdminCount == null || isAdminCount == 0) {
                // 用户不是 admin, 无关此策略
                return List.of();
            }

            // 2. 检查该组织 admin 总数(不含此 user)
            Integer otherAdminCount = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT subject_id) FROM access_relations " +
                "WHERE deleted = 0 AND subject_type = 'user' AND subject_id != ? " +
                "  AND resource_type = 'org_unit' AND resource_id = ? " +
                "  AND relation IN ('admin', 'SCHOOL_ADMIN', 'DEPT_ADMIN', 'TENANT_ADMIN', " +
                "                    'CLASS_TEACHER', 'GRADE_DIRECTOR', 'ACADEMIC_DIRECTOR')",
                Integer.class, userId, orgUnitId);

            if (otherAdminCount == null || otherAdminCount == 0) {
                return List.of(Violation.block(code(),
                    String.format("不能移除组织 %d 的最后一个管理员 user %d — 移除后该组织无人管理",
                        orgUnitId, userId)));
            }
        } catch (Exception e) {
            log.warn("[UserCannotRemoveLastAdminPolicy] check failed: {}", e.getMessage());
        }
        return List.of();
    }
}
