package com.school.management.infrastructure.access;

/**
 * 资源侧元数据 —— {@link ScopeEvaluator} compose SQL 所需的"这张表怎么过滤"配置。
 *
 * <p>由 {@code @DataPermission} 注解 + {@code DataModulePO moduleConfig} 合并而来
 * (合并规则与 interceptor 现状一致: moduleConfig 优先, 注解兜底)。本任务 (T6) 只定义并消费
 * 该结构; <b>T7</b> 负责在 interceptor 里从 annotation+moduleConfig 构建它。
 *
 * <p>两条互斥的资源路径 (与 compose 的分支一致):
 * <ul>
 *   <li>{@code viaMembership=true} —— 主表行本身是 access_relations 的 subject(user),
 *       按 {@code member} 关系的 org 归属过滤 (如 users / user_student)。</li>
 *   <li>否则 —— org 字段直过滤 ({@code orgUnitField})。</li>
 * </ul>
 *
 * @param tableAlias             主表别名 (空串表示无别名)。已 sanitize, 不含 "." 后缀。
 * @param orgUnitField           org 字段列名 (org 字段路径用)。
 * @param creatorField           创建者列名 (SELF 在 org 字段路径用)。
 * @param viaMembership          主表行即 member 关系 subject。
 * @param membershipSubjectColumn 主表中作为 ar.subject_id 的列 (默认 id)。
 * @param typeField              类型列 (轴③, 为空则不启用类型过滤)。
 * @param resourceCode           资源码 (= data_resources.resource_code / @DataPermission.module)。
 *                               R4: 引擎据此查 record_relations + registry.relationOf 判 RECORD_RELATION /
 *                               PROVIDER grant。
 */
public record ResourceScopeMeta(
        String tableAlias,
        String orgUnitField,
        String creatorField,
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

    /**
     * 换列 (A1 per-relation COLUMN): 把 orgUnitField 与 creatorField 都换成该关系注册的列, 复用单锚点
     * compose 既有分支 —— org-set 主体走 orgUnitField 得 {@code col IN (S)} (如 inspected,target_id∈我的成员组织);
     * SELF 主体走 creatorField 得 {@code col = me} (如 reviewer,reviewer_id=me)。其余字段不变。
     * owner_org(=orgUnitField)/creator(=creatorField) 本就默认列, 不经此换。
     */
    public ResourceScopeMeta withColumn(String col) {
        return new ResourceScopeMeta(tableAlias, col, col, viaMembership,
                membershipSubjectColumn, typeField, resourceCode);
    }
}
