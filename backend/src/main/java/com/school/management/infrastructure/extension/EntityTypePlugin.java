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
 * <h3>双轨收敛收官后的定位 (2026-06-13)</h3>
 * 其余 6 个声明型 SPI (RelationType/DataScope/RolePreset/Menu/Permission/MessagingDomain)
 * 已全部收敛到 {@link PluginPackage#contribute()} 并删除。<b>本接口是唯一保留的旧 SPI</b> —
 * 因为它不只是声明: ClassPlugin/SuperAdminPlugin/DormitoryPlugin 重写了 beforeCreate/
 * afterCreate/validate <b>生命周期钩子, 携带行为</b>, 是合法的 bean SPI (同 Policy /
 * DataScopeResolver / TargetModeResolver), 不适合退化为纯数据 contribute() 声明。
 *
 * <p>仍标 {@code @Deprecated} 仅为提示"纯声明部分推荐走 contribute()"; 不计划删除
 * ({@code forRemoval=false})。{@link Contribution.EntityTypeContribution} permit 作为
 * 未来可选迁移通道保留, 当前无人 emit。
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

    /**
     * 该类型用户创建时默认分配的角色编码 (仅 entity_type=USER 有意义)。
     *
     * <p>语义为"部署绑定": 角色编码取决于具体部署的角色目录, 因此默认空。
     * 返回非空时, {@link PluginRegistrar} 在<b>新建</b>类型行时写入 default_role_codes;
     * 合并已存在行时, 空集合视为"无意见", 保留 seed / 管理员设定值, 不覆盖。
     * 被 {@link com.school.management.application.user.UserTypeDefaultProvisioner} 消费。
     */
    default List<String> getDefaultRoleCodes() { return List.of(); }

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
