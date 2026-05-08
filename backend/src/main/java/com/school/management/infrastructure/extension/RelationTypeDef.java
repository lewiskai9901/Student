package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 关系类型定义 (对应 relation_types 表一行).
 *
 * <p>Phase 2 W2.2: 从 {@code RelationTypePlugin.RelationTypeDef} 内嵌 record
 * 提到顶层独立文件, 与 {@code RelationTypePlugin} SPI 解耦, 让 PluginPackage
 * 路径 ({@link Contribution.RelationTypeContribution}) 不再依赖已废弃接口.
 *
 * <p>关系链推导 (implied relations) 语义见 {@link Implied} record.
 *
 * @param relationCode      如 "guardian_of"
 * @param fromType          subject_type 限制,如 "user"
 * @param toType            resource_type 限制,如 "user"
 * @param relationName      中文名,如 "监护"
 * @param isTransitive      是否沿组织树传递
 * @param category          OWNERSHIP / MEMBERSHIP / ASSOCIATION / DELEGATION / SUBSCRIPTION
 * @param description       关系含义说明
 * @param capacityBound     受资源容量限制(如 place.capacity 限制入住人数)
 * @param maxPerResource    每个 resource 最多允许的 subject 数,全局默认 (null=无限)
 * @param maxBySubtype      按资源子类型(如 org_unit 的 typeCode)覆盖上限,优先于 maxPerResource
 * @param impliedRelations  关系链推导 — 本关系额外"派生"出哪些隐含关系。空列表表示不派生其它关系。
 */
public record RelationTypeDef(
    String relationCode,
    String fromType,
    String toType,
    String relationName,
    boolean isTransitive,
    String category,
    String description,
    boolean capacityBound,
    Integer maxPerResource,
    Map<String, Integer> maxBySubtype,
    List<Implied> impliedRelations
) {
    public static RelationTypeDef of(String code, String from, String to, String name,
                                     String category, String description) {
        return new RelationTypeDef(code, from, to, name, false, category, description,
            false, null, null, List.of());
    }

    public RelationTypeDef transitive() {
        return new RelationTypeDef(relationCode, fromType, toType, relationName, true, category, description,
            capacityBound, maxPerResource, maxBySubtype, impliedRelations);
    }

    public RelationTypeDef withCapacityBound() {
        return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description,
            true, maxPerResource, maxBySubtype, impliedRelations);
    }

    public RelationTypeDef withMaxPerResource(int max) {
        return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description,
            capacityBound, max, maxBySubtype, impliedRelations);
    }

    /** 按资源子类型(如班级 CLASS / 年级 GRADE)细化上限 */
    public RelationTypeDef withMaxBySubtype(Map<String, Integer> bySubtype) {
        return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description,
            capacityBound, maxPerResource, bySubtype, impliedRelations);
    }

    /** 追加/覆盖关系链推导规则 */
    public RelationTypeDef withImplied(List<Implied> implied) {
        return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description,
            capacityBound, maxPerResource, maxBySubtype,
            implied == null ? List.of() : List.copyOf(implied));
    }

    /**
     * 关系链推导规则 — 描述一条"派生"。
     *
     * <p>典型场景: 用户 U 对 place P 有 {@code dorm_head} 关系
     *   → 自动派生 U 对 P 内所有 {@code occupant} 关系 user 有 {@code viewer} 关系。
     *
     * <p>在 {@code AuthorizationService.check(...)} 里会展开这些推导。
     *
     * @param targetType     派生目标的 resource_type (如 "user")
     * @param relation       派生得到的关系 code (如 "viewer")
     * @param discoveryRule  如何从本关系的 resource 定位派生目标 id 列表
     *                       (见下方常量; 对应 {@code RelationDiscoveryRule.code()})
     */
    public record Implied(String targetType, String relation, String discoveryRule) {
        /** 从 place 出发找所有 occupants (用户) — 用于宿舍头领看到室友 */
        public static final String OCCUPANTS_OF_PLACE = "OCCUPANTS_OF_PLACE";
        /** 从 org_unit 出发找所有 members — 用于主管看到下属 */
        public static final String MEMBERS_OF_ORG     = "MEMBERS_OF_ORG";
        /** 从 org_unit 出发找所有子孙 org_unit (组织树) */
        public static final String DESCENDANTS_OF_ORG = "DESCENDANTS_OF_ORG";
    }
}
