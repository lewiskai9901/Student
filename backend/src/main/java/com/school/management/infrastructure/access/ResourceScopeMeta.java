package com.school.management.infrastructure.access;

/**
 * 资源侧元数据 —— {@link ScopeEvaluator} compose SQL 所需的"这张表怎么过滤"配置。
 *
 * <p>由 {@code @DataPermission} 注解 + {@code DataModulePO moduleConfig} 合并而来
 * (合并规则与 interceptor 现状一致: moduleConfig 优先, 注解兜底)。本任务 (T6) 只定义并消费
 * 该结构; <b>T7</b> 负责在 interceptor 里从 annotation+moduleConfig 构建它。
 *
 * <p>三条互斥的资源路径 (与 {@code buildSingleRoleCondition} 的分支一致):
 * <ul>
 *   <li>{@code viaMembership=true} —— 主表行本身是 access_relations 的 subject(user),
 *       按 {@code member} 关系的 org 归属过滤 (如 users / user_student)。</li>
 *   <li>{@code resourceType} 非空 —— 走 access_relations 子查询 (resource 侧, 如 student)。</li>
 *   <li>否则 —— org 字段直过滤 ({@code orgUnitField})。</li>
 * </ul>
 *
 * @param tableAlias             主表别名 (空串表示无别名)。已 sanitize, 不含 "." 后缀。
 * @param orgUnitField           org 字段列名 (org 字段路径用; access_relation 路径的直过滤 OR 也用)。
 * @param creatorField           创建者列名 (SELF 在 org 字段路径用)。
 * @param resourceType           [R4 后无读者] 旧 access_relation 路径 (accessRelationSelect) 已删; 来源恒空
 *                               (无注解设, data_resources.access_resource_type 全 NULL, 经核实); 字段暂留待专项移除。
 * @param viaMembership          主表行即 member 关系 subject。
 * @param membershipSubjectColumn 主表中作为 ar.subject_id 的列 (默认 id)。
 * @param typeField              类型列 (轴③, 为空则不启用类型过滤)。
 * @param resourceCode           资源码 (= data_resources.resource_code / @DataPermission.module)。
 *                               R4: 引擎据此查 record_relations + registry.relationOf 判 RECORD_RELATION
 *                               grant。<b>区别于 {@code resourceType}</b> (后者是 access_relation 类型, 多 NULL)。
 */
public record ResourceScopeMeta(
        String tableAlias,
        String orgUnitField,
        String creatorField,
        String resourceType,
        boolean viaMembership,
        String membershipSubjectColumn,
        String typeField,
        String resourceCode
) {

    /** 别名前缀: 有别名时返回 {@code "alias."}, 否则空串 (与 interceptor 拼法一致)。 */
    public String aliasPrefix() {
        return tableAlias == null || tableAlias.isEmpty() ? "" : tableAlias + ".";
    }

    /** 轴③是否可用 (声明了 type 列)。 */
    public boolean hasTypeField() {
        return typeField != null && !typeField.isEmpty();
    }

    /**
     * membership 路径作为 {@code ar.subject_id} 的列, 缺省 {@code "id"}。
     * <p>用于 membershipSelect / subjectRelFilter 两处。注意 plugin-dim 自降级 SELF 的 membership
     * 分支<b>不</b>用这个缺省 (那里缺省为 {@code "user_id"}, 端口自 interceptor 的既有口径), 故不收敛。
     */
    public String membershipSubjectColumnOrDefault() {
        return membershipSubjectColumn == null || membershipSubjectColumn.isEmpty() ? "id" : membershipSubjectColumn;
    }

    /** 创建者列, 缺省 {@code "created_by"} (SELF 在 org 字段路径 / plugin-dim 降级用)。 */
    public String creatorFieldOrDefault() {
        return creatorField == null || creatorField.isEmpty() ? "created_by" : creatorField;
    }
}
