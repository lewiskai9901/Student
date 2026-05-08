package com.school.management.application.access.masking;

import java.util.Set;

/**
 * 决定:某 viewer 看某 target user 时, 哪些字段需要脱敏.
 *
 * <p>策略由 {@link DefaultMaskingPolicy} 默认实现, 也可由插件覆写注册行业专属规则.
 */
public interface MaskingPolicy {

    /**
     * 给定 viewer 与 target user 之间的关系列表, 返回要脱敏的字段名集合.
     *
     * @param viewerId          当前用户 (查阅者) ID
     * @param targetUserId      目标用户 (被查) ID
     * @param viewerRelations   viewer 对 target 的所有关系码列表 (如 ["admin","member"]); 自己看自己时含 "self"; 无关系空列表
     * @return 应脱敏的字段名集合 (如 {"phone", "email"}); 空集合表示全显
     */
    Set<String> fieldsToMask(Long viewerId, Long targetUserId, Set<String> viewerRelations);
}
