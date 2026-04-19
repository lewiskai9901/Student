package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 实体类型插件接口 — 行业垂直通过实现此接口向平台注册类型。
 *
 * 职责:
 * 1. 类型注册: 声明类型编码、名称、系统字段 (启动时)
 * 2. 生命周期: beforeCreate/afterCreate/beforeDelete 等钩子
 * 3. 校验: 自定义业务校验逻辑
 *
 * Spring 自动扫描所有 @Component 实现类并注册。
 *
 * @deprecated since 1.1.0 — 用 {@link PluginPackage#contribute()} 返回
 *   {@link Contribution.EntityTypeContribution} 替代. 旧 API 仍被
 *   {@link PluginRegistrar} 扫描, 运行时等价, 现有实现无需立即迁移.
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public interface EntityTypePlugin {

    // ========== 类型注册 ==========

    /** 实体类型: ORG_UNIT / PLACE / USER */
    String getEntityType();

    /** 类型编码: CLASS / CLASSROOM / TEACHER 等 */
    String getTypeCode();

    /** 类型显示名 */
    String getTypeName();

    /** 类型分类 */
    default String getCategory() { return null; }

    /** 父类型编码 */
    default String getParentTypeCode() { return null; }

    /** 允许的子类型编码 */
    default List<String> getAllowedChildTypeCodes() { return List.of(); }

    /** 系统字段定义（管理员不可删除） */
    List<FieldDefinition> getSystemFields();

    /**
     * 能力声明 (Feature dictionary)
     *
     * 标准能力词 (所有插件应优先使用这些):
     *   - isLearner / isStaff / isExternal              身份大类
     *   - canLogin / profileEditableBySelf              通用能力
     *   - receivesPersonalGrade / hasGuardian           学习场景
     *   - attendanceTracked / canEnroll                 教务场景
     *   - canBeAdminOfOrg / canBeResponsibleForPlace    管理能力
     *   - bookable / occupiable / hasCapacity           场所能力
     *
     * 业务代码判断 user.hasFeature("isLearner"),
     * 禁止 "STUDENT".equals(user.getType())。
     */
    default Map<String, Boolean> getFeatures() { return Map.of(); }

    /**
     * 扩展表名 (如果此类型有独立扩展表存高频字段)。
     * 命名规范: {entity}_{type} 小写,如 "user_student" / "place_classroom"。
     * 返回 null 表示此类型无独立扩展表,所有字段走 entity_attribute_values (EAV)。
     */
    default String getExtensionTable() { return null; }

    /** UI 配置 */
    default Map<String, Object> getUiConfig() { return Map.of(); }

    // ========== 生命周期钩子 ==========

    /** 优先级（多个插件时执行顺序，值小先执行） */
    default int getOrder() { return 0; }

    default void beforeCreate(ExtensionContext ctx) {}
    default void afterCreate(ExtensionContext ctx) {}
    default void beforeUpdate(ExtensionContext ctx) {}
    default void afterUpdate(ExtensionContext ctx) {}
    default void beforeDelete(ExtensionContext ctx) {}
    default void afterDelete(ExtensionContext ctx) {}

    /** 自定义校验，返回错误消息列表（空=通过） */
    default List<String> validate(ExtensionContext ctx) { return List.of(); }
}
