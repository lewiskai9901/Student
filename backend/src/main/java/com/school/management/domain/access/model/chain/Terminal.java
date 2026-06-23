package com.school.management.domain.access.model.chain;

import java.util.List;

/**
 * 关系链的"终端锚点" (统一锚定 P0) —— 数据记录怎么挂到链末实体集上。
 *
 * <p><b>关键约束</b> (用户要求): 每个 anchorRelation 必须 ∈ 该资源在 {@code resource_relations}
 * 注册表里声明的关系 (owner_org/creator/owner_place/PROVIDER/...) —— "链接最后只能是模块数据
 * 支持的锚点"。这正是把无限图遍历收敛成可强制执行 SQL 的边界。校验见 {@code ChainValidator}。
 *
 * @param anchorRelations 终端锚点关系码集 (每个 ∈ resource_relations[resource]); 非空
 * @param combine         多锚点组合: AND=各锚点谓词叠加 / OR=并联
 */
public record Terminal(
        List<String> anchorRelations,
        Combine combine
) {
    public Terminal {
        anchorRelations = anchorRelations == null ? List.of() : List.copyOf(anchorRelations);
        combine = combine == null ? Combine.OR : combine;
    }
}
