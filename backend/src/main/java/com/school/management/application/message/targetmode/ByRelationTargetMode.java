package com.school.management.application.message.targetmode;

import com.school.management.application.access.AccessRelationService;
import com.school.management.infrastructure.extension.TargetModeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BY_RELATION — 基于 access_relations 统一关系查询, 展开 implied 派生.
 *
 * config JSON 格式:
 *   {
 *     "relation": "admin",            // 必填 - 关系码
 *     "resource_type": "org_unit",    // 必填 - subject 面向的 resource 类型
 *     "direction": "inward",          // 可选 - inward(默认) = 找"以 subject 为 resource 的 user"
 *                                     //         outward = 找"subject 关联的 resources"
 *     "include_deputies": true        // 可选 - 同时纳入 deputy 关系
 *   }
 *
 * 典型场景:
 *   - 成绩发布 (subject=班级 ORG_UNIT):
 *     {relation:"admin", resource_type:"org_unit"}        → 通知该班班主任
 *   - 学生扣分 (subject=学生 USER):
 *     {relation:"guardian_of", resource_type:"user", direction:"outward"}  → 通知该学生的家长
 *
 * inward 走 AccessRelationService.findSubjectsWithRelation(expandImplied=true).
 * outward 走裸 SQL (当前 MVP 暂不覆盖派生, 需反向 BFS 后续扩展).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ByRelationTargetMode implements TargetModeResolver {

    private final AccessRelationService accessRelationService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String modeCode() {
        return "BY_RELATION";
    }

    @Override
    public String displayName() {
        return "按关系";
    }

    @Override
    public boolean supportsPreview() { return false; }

    @Override
    public List<Long> resolve(Map<String, Object> config, Map<String, Object> event) {
        try {
            if (event == null || event.isEmpty()) return List.of();
            Object subjectIdObj = event.get("subjectId");
            if (subjectIdObj == null) return List.of();
            long subjectId = ((Number) subjectIdObj).longValue();
            String subjectType = (String) event.get("subjectType");

            if (config == null) config = Map.of();
            String relation = (String) config.get("relation");
            String resourceType = (String) config.get("resource_type");
            String direction = (String) config.getOrDefault("direction", "inward");
            boolean includeDeputies = Boolean.TRUE.equals(config.get("include_deputies"));

            if (relation == null || relation.isBlank()) {
                log.warn("[BY_RELATION] 缺少 relation 参数, config={}", config);
                return List.of();
            }

            List<String> relations = new ArrayList<>();
            relations.add(relation);
            if (includeDeputies) relations.add("deputy");

            if ("outward".equalsIgnoreCase(direction)) {
                String placeholders = relations.stream().map(r -> "?").collect(Collectors.joining(","));
                String sql = "SELECT DISTINCT ar.resource_id FROM access_relations ar " +
                        "WHERE ar.subject_type = ? AND ar.subject_id = ? " +
                        "  AND ar.resource_type = 'user' " +
                        "  AND ar.relation IN (" + placeholders + ") " +
                        "  AND ar.deleted = 0 " +
                        "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW())";
                Object[] params = new Object[relations.size() + 2];
                params[0] = subjectType != null ? subjectType.toLowerCase() : "user";
                params[1] = subjectId;
                for (int i = 0; i < relations.size(); i++) params[i + 2] = relations.get(i);
                List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, params);
                return new ArrayList<>(new LinkedHashSet<>(ids));
            }

            // inward (默认): subject 是 resource, 找它的 user 们, 展开 implied.
            String rt = resourceType != null ? resourceType.toLowerCase()
                    : (subjectType != null ? subjectType.toLowerCase() : "org_unit");
            Set<Long> result = new LinkedHashSet<>();
            for (String rel : relations) {
                List<Long> ids = accessRelationService.findSubjectsWithRelation(
                    rt, subjectId, rel, /* subjectTypeFilter= */ "user", /* expandImplied= */ true);
                result.addAll(ids);
            }
            return new ArrayList<>(result);
        } catch (Exception e) {
            log.warn("[BY_RELATION] 查询失败: {}, config={}", e.getMessage(), config);
            return List.of();
        }
    }
}
