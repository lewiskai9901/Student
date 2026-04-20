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
 * {@link com.school.management.application.access.AuthorizationService} 构造时
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
}
