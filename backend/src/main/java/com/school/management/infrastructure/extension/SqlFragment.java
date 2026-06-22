package com.school.management.infrastructure.extension;

import java.util.Map;

/**
 * 一段参数化 SQL 片段 (R3c PROVIDER 关系用)。
 *
 * <p>{@link RecordRelationResolver#subquery} 返回它:{@code sql} 用<b>命名参数</b> {@code :name}
 * 占位,{@code params} 给出每个名字的值。引擎把它包成 {@code 资源表.id IN (<sql>)},并把命名参数
 * 按出现顺序改写为位置 {@code ?} 安全绑定 —— 插件<b>只写内层 SELECT,不拼任何用户数据</b>,无注入。
 *
 * <p>例:{@code SqlFragment.of("SELECT s.id FROM user_student s JOIN teacher_assignments ta " +
 * "ON ta.org_unit_id = s.org_unit_id WHERE ta.teacher_id = :me", Map.of("me", userId))}。
 */
public record SqlFragment(String sql, Map<String, Object> params) {

    public static SqlFragment of(String sql, Map<String, Object> params) {
        return new SqlFragment(sql, params == null ? Map.of() : params);
    }

    public static SqlFragment of(String sql) {
        return new SqlFragment(sql, Map.of());
    }
}
