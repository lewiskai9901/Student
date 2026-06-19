package com.school.management.domain.access.model;

/**
 * 关系授予的 subject 范围 (统一锚定 R3) —— 一条 relation_grant 里"subject 取哪些主体"。
 *
 * <p>取代 M1 扁平 {@link OrgAnchor} 作为 grant 内的范围维度: M1 一行只有一个 OrgAnchor,
 * R3 一行有 N 条 {@code {relation, subject}} 授予 (OR 叠加), subject 用本枚举 + param 表达。
 *
 * <ul>
 *   <li>{@link #SELF} —— 当前用户 (列锚资源=creator 列=我; 成员资源=member-self)。</li>
 *   <li>{@link #MY_ORG} —— 我的主属组织 (配 subtree)。</li>
 *   <li>{@link #RELATION} —— 经某关系到达的组织集 (subjectParam=关系码, 如 admin)。</li>
 *   <li>{@link #CUSTOM} —— 管理员显式指定组织集 (orgIds)。</li>
 *   <li>{@link #PLUGIN_DIM} —— 插件维度派生 (subjectParam=维度码, 如 BY_CLASS)。</li>
 *   <li>{@link #ALL} —— 无界 (该关系不约束)。</li>
 * </ul>
 */
public enum SubjectScope {
    SELF, MY_ORG, RELATION, CUSTOM, PLUGIN_DIM, ALL
}
