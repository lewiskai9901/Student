package com.school.management.domain.access.model.chain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 一个角色对一个资源的多级关系链数据范围 (统一锚定 P0) —— 顶层多链 = OR (满足任一链即可见)。
 *
 * <p>持久化: {@code role_data_scopes.relation_grants} JSON (无数据兼容, 形状从旧 List&lt;RelationGrant&gt;
 * 重定义为 List&lt;Chain&gt;)。读写共用同一 spec (compileSpec), 语义一致。
 *
 * <p>设计: docs/plans/2026-06-23-multi-level-chain-FINAL-plan.md。
 *
 * @param chains 多条链, OR 叠加 (可见集 = 各链结果并集)
 */
public record ScopeChainSpec(
        List<Chain> chains
) {
    public ScopeChainSpec {
        chains = chains == null ? List.of() : List.copyOf(chains);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return chains.isEmpty();
    }
}
