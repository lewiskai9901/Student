package com.school.management.application.access;

import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.infrastructure.extension.ApproverFinderPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 审批人路由:按 pending 的关系码找最匹配的 {@link ApproverFinderPlugin}.
 *
 * <p>选择规则:
 * <ol>
 *   <li>relation 命中 plugin.applicableRelations() (非 null) → 取 order 最小的那个</li>
 *   <li>否则取 fallback (applicableRelations()==null) → order 最小的那个</li>
 *   <li>都没有 → 返回空列表</li>
 * </ol>
 *
 * <p>Phase 7 W7.4.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverResolver {

    private final List<ApproverFinderPlugin> finders;

    public List<Long> resolveApprovers(PendingRelationApproval p) {
        // 1. 找匹配 relation 的具体 finder
        List<ApproverFinderPlugin> matched = finders.stream()
            .filter(f -> f.applicableRelations() != null
                && f.applicableRelations().contains(p.getRelation()))
            .sorted(Comparator.comparingInt(ApproverFinderPlugin::order))
            .toList();
        if (!matched.isEmpty()) {
            return matched.get(0).findApprovers(p);
        }

        // 2. fallback finder (applicableRelations()==null)
        return finders.stream()
            .filter(f -> f.applicableRelations() == null)
            .sorted(Comparator.comparingInt(ApproverFinderPlugin::order))
            .findFirst()
            .map(f -> f.findApprovers(p))
            .orElse(List.of());
    }
}
