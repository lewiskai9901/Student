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
 * 组织节点不能跨学校移动策略 (BLOCK).
 *
 * <p>语义:BEFORE_UPDATE org_unit 阶段, 若 parent_id 变了, 校验新老 parent
 * 是否在同一棵 SCHOOL 树下. 不同 SCHOOL 之间移动 → 阻断.
 *
 * <p>跨校移动会导致 access_relations / role_data_scopes 等子树聚合错位,
 * 不可静默允许. 真要做 (合并办学等) 应通过显式数据迁移工具.
 *
 * <p>payload 约定: Map 含
 * <ul>
 *   <li>orgUnitId (Long): 被更新节点</li>
 *   <li>newParentId (Long): 新父节点 (null = 移到根)</li>
 * </ul>
 */
@Slf4j
@Component
public class OrgUnitCannotMoveAcrossSchoolPolicy implements Policy<Object> {

    private final JdbcTemplate jdbc;

    public OrgUnitCannotMoveAcrossSchoolPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public String code() { return "ORG_UNIT_CROSS_SCHOOL_MOVE"; }
    @Override public String name() { return "组织跨学校移动保护"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "org_unit".equals(ctx.entityType())
            && ("BEFORE_UPDATE".equals(ctx.phase())
                || "BEFORE_MOVE".equals(ctx.phase()));
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (!(ctx.payload() instanceof Map<?, ?> m)) return List.of();
        Object idObj = m.get("orgUnitId");
        if (!(idObj instanceof Long orgUnitId)) return List.of();
        Object newParentObj = m.get("newParentId");
        Long newParentId = newParentObj instanceof Long lp ? lp : null;

        try {
            // 取当前 tree_path 找到所属 SCHOOL
            String currentSchool = findRootSchool(orgUnitId);
            if (currentSchool == null) {
                return List.of();   // 当前节点不在 SCHOOL 树下, 不约束
            }

            // 取新 parent 的 SCHOOL
            String newSchool = null;
            if (newParentId != null) {
                newSchool = findRootSchool(newParentId);
            }

            if (newSchool != null && !newSchool.equals(currentSchool)) {
                return List.of(Violation.block(code(),
                    String.format("禁止跨学校移动组织 %d: 当前所属 SCHOOL=%s, 目标 SCHOOL=%s. " +
                        "合并办学等场景请走数据迁移工具.",
                        orgUnitId, currentSchool, newSchool)));
            }
        } catch (Exception e) {
            log.warn("[OrgUnitCannotMoveAcrossSchoolPolicy] check failed: {}", e.getMessage());
        }
        return List.of();
    }

    /** 沿 tree_path 找到所在 SCHOOL 节点 id (字符串, 因为 path 是 / 分隔字符串) */
    private String findRootSchool(Long orgUnitId) {
        try {
            // tree_path 格式: /school_id/department_id/...
            String path = jdbc.queryForObject(
                "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
                String.class, orgUnitId);
            if (path == null || path.isBlank()) return null;
            // 取第一段(SCHOOL id)
            String[] parts = path.split("/");
            for (String p : parts) {
                if (!p.isBlank()) return p;
            }
        } catch (Exception e) {
            log.debug("[OrgUnit] findRootSchool failed for {}: {}", orgUnitId, e.getMessage());
        }
        return null;
    }
}
