package com.school.management.domain.access.model.chain;

import java.util.List;

/**
 * 一条关系链 (统一锚定 P0) —— 当前用户经 0..N 个中间跳到达链末实体集, 数据再经终端锚点挂上去。
 *
 * <p>例 (用户原例, 用户管理/角色A):
 * <pre>
 *   Chain(
 *     hops     = [ Hop(["responsible_for","admin"], AND, "org_unit", true) ],   // 我 负责且管理 的组织(含下级)
 *     terminal = Terminal(["member","responsible_for"], AND),                    // 数据(用户) 属于且负责 该组织
 *     typeFilter = ["STUDENT"]                                                   // 且类型=学生
 *   )
 * </pre>
 *
 * <p>1 跳退化 = hops 为空 + 单终端锚点 (= 旧 relation_grant)。
 *
 * @param hops       中间跳 (0..N, 走 access_relations 图); 起点恒为当前用户
 * @param terminal   终端锚点 (必填, ∈ resource_relations)
 * @param typeFilter 轴③ 类型过滤码 (可空)
 */
public record Chain(
        List<Hop> hops,
        Terminal terminal,
        List<String> typeFilter
) {
    public Chain {
        hops = hops == null ? List.of() : List.copyOf(hops);
        typeFilter = typeFilter == null ? List.of() : List.copyOf(typeFilter);
    }
}
