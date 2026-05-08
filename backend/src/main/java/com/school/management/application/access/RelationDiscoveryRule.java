package com.school.management.application.access;

import java.util.List;

/**
 * 关系链推导 — 发现规则 SPI.
 *
 * <p>每个实现类给定一个"来源 resource" (subject 对其有某个 relation),
 * 返回派生目标 resource 的 id 列表。
 *
 * <p>实现举例: 用户是宿舍的 {@code dorm_head},
 * {@link OccupantsOfPlaceDiscovery} 返回该宿舍内所有 {@code occupant} 用户 id,
 * 配合 {@code RelationTypeDef.Implied.relation="viewer"} 即完成
 * "宿舍头领对室友拥有 viewer 关系" 的推导。
 *
 * <p>注册方式: 实现类加 {@code @Component}, Spring DI 会自动收集,
 * {@link com.school.management.application.access.AccessRelationService} 构造时
 * 按 {@link #code()} 建索引并调用。
 *
 * @see com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied
 */
public interface RelationDiscoveryRule {

    /** 规则 code — 对应 Implied.discoveryRule 字段值 */
    String code();

    /**
     * 给定一条"来源" (fromResourceType + fromResourceId),
     * 返回派生目标 resource id 列表。
     *
     * <p>实现可 free-form 查任何业务表, 不限于 access_relations。
     *
     * @param fromResourceType 来源关系的 resource_type (如 "place")
     * @param fromResourceId   来源关系的 resource_id
     * @return 派生目标 id 列表; 若无则返回空列表 (不要 null)
     */
    List<Long> discover(String fromResourceType, Long fromResourceId);

    /**
     * 反向推导 — 给定派生目标 (targetResourceType + targetResourceId),
     * 返回能派生到它的"来源" resource id 列表 (fromResource 类型由具体规则决定).
     *
     * <p>用于 {@link AuthorizationService#findSubjectsWithRelation} — 给定"谁对此 resource
     * 有 relation", 需要先反查"哪些 source resource 能派生到此 target",
     * 再用 source resource 查 access_relations 的 subject.
     *
     * <p>默认实现返回空列表并记警告 — 子类应按需覆写. 未覆写的规则在 implied 反查时被跳过.
     *
     * @param targetResourceType 派生目标的 resource_type (如 "user")
     * @param targetResourceId   派生目标的 resource_id
     * @return 能派生到此目标的 source resource id 列表; 若无/不支持则返回空列表
     */
    default List<Long> reverseDiscover(String targetResourceType, Long targetResourceId) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .debug("[RelationDiscoveryRule:{}] reverseDiscover 未实现, 将被跳过 (target={}#{})",
                code(), targetResourceType, targetResourceId);
        return List.of();
    }
}
