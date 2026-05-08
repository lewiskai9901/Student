package com.school.management.application.access.masking;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * 默认脱敏策略.
 *
 * <p>规则 (按优先级):
 * <ol>
 *   <li>看自己: 全显 (空集合)</li>
 *   <li>有 admin / responsible_for / mentor_of / advisor_of / family_of 等管理类关系: 全显</li>
 *   <li>有 member / viewer 等同侪关系: 脱敏 phone, email</li>
 *   <li>没有任何关系: 脱敏 phone, email, idCard, realName (全部敏感字段)</li>
 * </ol>
 */
@Component
public class DefaultMaskingPolicy implements MaskingPolicy {

    /** 高级关系: 看到目标后全显字段 */
    private static final Set<String> PRIVILEGED_RELATIONS = Set.of(
        "admin", "responsible_for", "mentor_of", "advisor_of",
        "family_of", "guardian_of",
        "attending_of", "nurse_of", "deputy"
    );

    /** 中级关系: 同事/同组,部分脱敏 */
    private static final Set<String> PEER_RELATIONS = Set.of(
        "member", "viewer", "manages", "occupies", "watches", "in_ward"
    );

    @Override
    public Set<String> fieldsToMask(Long viewerId, Long targetUserId, Set<String> viewerRelations) {
        // 自己看自己 — 全显
        if (Objects.equals(viewerId, targetUserId)) return Set.of();

        if (viewerRelations == null) viewerRelations = Set.of();

        // 有任一高级关系 — 全显
        for (String r : viewerRelations) {
            if (PRIVILEGED_RELATIONS.contains(r)) return Set.of();
        }

        // 有同侪关系 — 部分脱敏
        for (String r : viewerRelations) {
            if (PEER_RELATIONS.contains(r)) return Set.of("phone", "email");
        }

        // 无关系 — 全部敏感字段脱敏 (调用方应结合权限决定是否真返回这些字段)
        return Set.of("phone", "email", "idCard", "realName");
    }
}
