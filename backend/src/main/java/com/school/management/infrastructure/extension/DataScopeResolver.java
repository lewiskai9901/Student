package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 数据范围解析器 — 插件实现类在运行时提供"给定用户 + 资源类型 → 可访问的 resource_id 列表"。
 *
 * <p>这是**运行时解析契约**(Spring bean), 与声明维度的 {@link Contribution.DataScopeContribution}
 * 分属两件事 (Phase 2 双轨收敛: 从 DataScopePlugin 内提为顶层接口, 声明走 contribute(),
 * 解析仍由实现本接口的 bean 完成, 经 resolverType 全限定名解析)。
 */
public interface DataScopeResolver {
    /**
     * @param userId       当前登录用户
     * @param resourceType 要访问的资源类型 (如 'user' / 'place' / 'student')
     * @return 可访问的 resource_id 列表, 空列表=拒绝所有, null=不限(相当于 ALL)
     */
    List<Long> resolve(Long userId, String resourceType);
}
