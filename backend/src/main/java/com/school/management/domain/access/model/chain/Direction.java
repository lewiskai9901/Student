package com.school.management.domain.access.model.chain;

/**
 * 关系链一跳的遍历方向 (有向关系链 P-M1)。
 *
 * <p>关系本身强有向 ({@code relation_types.from_type → to_type})。一跳可正读或倒读同一条
 * {@code access_relations} 边 —— 不新增存储, 只换 SQL 匹配的边 (见 ChainHopResolver)。
 *
 * <ul>
 *   <li>{@link #FORWARD}: 沿 {@code subject → resource} (= 关系定义 from→to)。默认, = 历史行为。</li>
 *   <li>{@link #REVERSE}: 沿 {@code resource → subject} (把关系倒过来用, 如 组织 →[反·成员]→ 用户)。</li>
 * </ul>
 */
public enum Direction {
    FORWARD,
    REVERSE
}
