package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 关系类型插件 SPI.
 *
 * 每个实现类在启动时被 {@link RelationTypePluginRegistrar} 扫描,
 * 自动 UPSERT 到 relation_types 表.
 *
 * 规范:
 *  - 关系是强类型语义,不允许管理员运行时自定义 (参见业界 Zanzibar/AWS IAM/Casbin 共识)
 *  - 平台自带的通用关系 → {@code CoreRelationsPlugin}
 *  - 行业特有关系 → 各行业插件 (如 EducationRelationsPlugin)
 *  - 业务代码引用关系应使用 Java 常量,不用裸字符串
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.RelationTypeContribution} 替代. 旧 API 仍被
 *   {@link RelationTypePluginRegistrar} 扫描, 运行时等价, 现有实现无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface RelationTypePlugin {

    /** 来源标识 (对应 relation_types.registered_by),通常是插件类名或行业标识 */
    String getSourceName();

    /** 插件层级: CORE / COMMON_EXT / DOMAIN — 影响 UI 分组和删除保护 */
    default String getTier() { return "DOMAIN"; }

    /** 声明本插件引入的所有关系类型 */
    List<RelationTypeDef> getRelationTypes();

    /**
     * 关系类型定义 (对应 relation_types 表一行).
     *
     * 关系链推导 (implied relations) 语义见 {@link Implied} record。
     */
    record RelationTypeDef(
        String relationCode,      // 如 "guardian_of"
        String fromType,          // subject_type 限制,如 "user"
        String toType,            // resource_type 限制,如 "user"
        String relationName,      // 中文名,如 "监护"
        boolean isTransitive,     // 是否沿组织树传递
        String category,          // OWNERSHIP / MEMBERSHIP / ASSOCIATION / DELEGATION / SUBSCRIPTION
        String description,
        /** 受资源容量限制(如 place.capacity 限制入住人数) */
        boolean capacityBound,
        /** 每个 resource 最多允许的 subject 数,全局默认 (null=无限) */
        Integer maxPerResource,
        /** 按资源子类型(如 org_unit 的 typeCode)覆盖上限,优先于 maxPerResource */
        Map<String, Integer> maxBySubtype,
        /**
         * 关系链推导 — 本关系额外"派生"出哪些隐含关系。
         *
         * 典型场景: 用户 U 对 place P 有 {@code dorm_head} 关系
         *   → 自动派生 U 对 P 内所有 {@code occupant} 关系 user 有 {@code viewer} 关系。
         *
         * 在 {@code AuthorizationService.check(...)} 里会展开这些推导。
         * 空列表表示本关系不派生其它关系 (大多数情况)。
         */
        List<Implied> impliedRelations
    ) {
        public static RelationTypeDef of(String code, String from, String to, String name,
                                         String category, String description) {
            return new RelationTypeDef(code, from, to, name, false, category, description, false, null, null, List.of());
        }
        public RelationTypeDef transitive() {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, true, category, description, capacityBound, maxPerResource, maxBySubtype, impliedRelations);
        }
        public RelationTypeDef withCapacityBound() {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description, true, maxPerResource, maxBySubtype, impliedRelations);
        }
        public RelationTypeDef withMaxPerResource(int max) {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description, capacityBound, max, maxBySubtype, impliedRelations);
        }
        /** 按资源子类型(如班级 CLASS / 年级 GRADE)细化上限 */
        public RelationTypeDef withMaxBySubtype(Map<String, Integer> bySubtype) {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description, capacityBound, maxPerResource, bySubtype, impliedRelations);
        }
        /** 追加/覆盖关系链推导规则 */
        public RelationTypeDef withImplied(List<Implied> implied) {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, isTransitive, category, description, capacityBound, maxPerResource, maxBySubtype,
                implied == null ? List.of() : List.copyOf(implied));
        }

        /**
         * 关系链推导规则 — 描述一条"派生"。
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
}
