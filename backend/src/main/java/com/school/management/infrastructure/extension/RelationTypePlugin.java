package com.school.management.infrastructure.extension;

import java.util.List;

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
 */
public interface RelationTypePlugin {

    /** 来源标识 (对应 relation_types.registered_by),通常是插件类名或行业标识 */
    String getSourceName();

    /** 插件层级: CORE / COMMON_EXT / DOMAIN — 影响 UI 分组和删除保护 */
    default String getTier() { return "DOMAIN"; }

    /** 声明本插件引入的所有关系类型 */
    List<RelationTypeDef> getRelationTypes();

    /**
     * 关系类型定义 (对应 relation_types 表一行).
     */
    record RelationTypeDef(
        String relationCode,      // 如 "guardian_of"
        String fromType,          // subject_type 限制,如 "user"
        String toType,            // resource_type 限制,如 "user"
        String relationName,      // 中文名,如 "监护"
        boolean isTransitive,     // 是否沿组织树传递
        String category,          // OWNERSHIP / MEMBERSHIP / ASSOCIATION / DELEGATION / SUBSCRIPTION
        String description
    ) {
        public static RelationTypeDef of(String code, String from, String to, String name,
                                         String category, String description) {
            return new RelationTypeDef(code, from, to, name, false, category, description);
        }
        public RelationTypeDef transitive() {
            return new RelationTypeDef(relationCode, fromType, toType, relationName, true, category, description);
        }
    }
}
