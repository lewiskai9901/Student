package com.school.management.infrastructure.extension.plugins.education.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 学生用户必须关联班级策略 (BLOCK).
 *
 * <p>语义:BEFORE_CREATE / AFTER_CREATE / BEFORE_UPDATE user 阶段, 若用户
 * 类型是 STUDENT, 必须在 user_student 表有记录且 user_student.org_unit_id
 * 是 CLASS 类型的 org_unit. 没班级 → 阻断.
 *
 * <p>EDU 插件贡献的 production policy — 防止"游离学生" (admin 误操作改 user_type
 * 为 STUDENT 但忘了关联班级, 导致后续 BY_CLASS / DEPARTMENT_AND_BELOW
 * 数据权限都失效).
 *
 * <p>payload 约定: Map 含
 * <ul>
 *   <li>userId (Long)</li>
 *   <li>userTypeCode (String): 可选, 不传时跳过</li>
 * </ul>
 */
@Slf4j
@Component
public class StudentMustHaveClassPolicy implements Policy<Object> {

    private final JdbcTemplate jdbc;

    public StudentMustHaveClassPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public String code() { return "STUDENT_MUST_HAVE_CLASS"; }
    @Override public String name() { return "学生必须关联班级"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "user".equals(ctx.entityType())
            && (
                "AFTER_CREATE".equals(ctx.phase())
                || "AFTER_UPDATE".equals(ctx.phase())
                || "BEFORE_DELETE".equals(ctx.phase())     // 防止留下孤儿学生
            );
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (!(ctx.payload() instanceof Map<?, ?> m)) return List.of();
        Object userIdObj = m.get("userId");
        if (!(userIdObj instanceof Long userId)) return List.of();
        Object typeObj = m.get("userTypeCode");
        String userType = typeObj instanceof String s ? s : null;
        if (userType == null) {
            // 从 users 表读
            try {
                userType = jdbc.queryForObject(
                    "SELECT user_type_code FROM users WHERE id = ? AND deleted = 0",
                    String.class, userId);
            } catch (Exception e) {
                return List.of();   // 用户不存在, 不归此策略管
            }
        }
        if (!"STUDENT".equals(userType)) return List.of();

        // BEFORE_DELETE: 学生在删除时不强制有 class (允许清理)
        if ("BEFORE_DELETE".equals(ctx.phase())) return List.of();

        try {
            // 查 user_student 是否存在 + org_unit_id 是否 CLASS 类型
            Integer ok = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_student us " +
                "JOIN org_units ou ON ou.id = us.org_unit_id AND ou.deleted = 0 " +
                "WHERE us.user_id = ? AND us.deleted = 0 AND us.org_unit_id IS NOT NULL " +
                "  AND EXISTS (SELECT 1 FROM entity_type_configs etc " +
                "              WHERE etc.entity_type = 'ORG_UNIT' AND etc.type_code = 'CLASS' " +
                "                AND etc.deleted = 0)",
                Integer.class, userId);
            if (ok == null || ok == 0) {
                return List.of(Violation.block(code(),
                    String.format("学生 user %d 未关联到任何 CLASS 类型的班级 — 请先在 user_student 表配置 org_unit_id",
                        userId)));
            }
        } catch (Exception e) {
            log.warn("[StudentMustHaveClassPolicy] check failed: {}", e.getMessage());
        }
        return List.of();
    }
}
