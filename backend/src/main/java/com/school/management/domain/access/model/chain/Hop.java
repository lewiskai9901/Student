package com.school.management.domain.access.model.chain;

import java.util.List;

/**
 * 关系链中的一"跳" (统一锚定 P0) —— 从上一实体 (起点恒为当前用户) 经一组关系到达下一实体集。
 *
 * <p>实例对应 {@code access_relations} 上的一层遍历。例: {@code 我 经[管理] 到达 组织} =
 * {@code Hop(["admin"], OR, "org_unit", true)}。
 *
 * @param relations 这一跳的关系码集 (来自 relation_types, 如 ["member","admin"]); 非空
 * @param combine   同级多关系组合: AND=交集 / OR=并集
 * @param toType    这一跳到达的实体类型: user | org_unit | place
 * @param subtree   org_unit 跳是否含下级 (子树展开)
 * @param direction 遍历方向: FORWARD=subject→resource (默认) / REVERSE=resource→subject
 */
public record Hop(
        List<String> relations,
        Combine combine,
        String toType,
        boolean subtree,
        Direction direction
) {
    public Hop {
        relations = relations == null ? List.of() : List.copyOf(relations);
        combine = combine == null ? Combine.OR : combine;
        // 回兼: 旧 grant JSON 无 direction → null → 默认正向 (现有配置字节等价, 金标准安全)
        direction = direction == null ? Direction.FORWARD : direction;
    }

    /** 4-arg 回兼构造 (现有调用/测试不带 direction → 默认 FORWARD)。 */
    public Hop(List<String> relations, Combine combine, String toType, boolean subtree) {
        this(relations, combine, toType, subtree, Direction.FORWARD);
    }
}
