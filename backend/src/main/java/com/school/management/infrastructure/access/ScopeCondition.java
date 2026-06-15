package com.school.management.infrastructure.access;

import org.apache.ibatis.type.JdbcType;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个数据范围规格 ({@link com.school.management.domain.access.model.valueobject.ScopeSpec})
 * compose 出的 SQL 片段 + 位置绑定参数。
 *
 * <p>这是 {@link ScopeEvaluator#toSqlCondition} 的输出值对象, 与
 * {@code DataPermissionInterceptor} 内部私有的 {@code ParameterizedCondition} /
 * {@code AdditionalParam} 同构 —— T7 由 interceptor 桥接消费 (把 {@link Param} 翻成
 * ibatis {@code ParameterMapping} 并 {@code setAdditionalParameter})。
 *
 * <p>独立类: 本任务 (T6) 不修改 interceptor 内的那两个内部类。
 *
 * <p>参数顺序约定: {@link #params} 中的元素严格按 SQL 字符串里 {@code ?} 出现的先后排列
 * (orgSet → 轴② → 轴③), 位置绑定 — 这是历史 bug 的重灾区, 必须精确。
 */
public class ScopeCondition {

    /** compose 出的 SQL 谓词。空串表示"无过滤"(放行全量)。 */
    public String sql = "";

    /** 与 {@link #sql} 中 {@code ?} 一一对应的参数, 严格按位置顺序。 */
    public final List<Param> params = new ArrayList<>();

    /** 追加一个位置绑定参数 (按调用顺序入列, 必须与 SQL 中 {@code ?} 顺序一致)。 */
    public void addParam(String property, Object value, Class<?> javaType, JdbcType jdbcType) {
        Param p = new Param();
        p.property = property;
        p.value = value;
        p.javaType = javaType;
        p.jdbcType = jdbcType;
        params.add(p);
    }

    /** 单个位置绑定参数 (mirror interceptor 内部 AdditionalParam)。 */
    public static class Param {
        public String property;
        public Object value;
        public Class<?> javaType;
        public JdbcType jdbcType;
    }
}
