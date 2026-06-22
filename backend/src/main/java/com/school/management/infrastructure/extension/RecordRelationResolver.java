package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 接口式资源关系解析器 (统一锚定 PROVIDER 档,R3c)。
 *
 * <p>当一个数据关系的逻辑<b>无法落列、不入 record_relations、也不走主体图</b>(如"老师任课的学生"
 * 藏在排课/任课表里),插件实现本 SPI,系统调用它算出"当前主体可见的记录集"。
 * 在 {@code resource_relations} 注册时 {@code storage_kind=PROVIDER}、{@code resolver_bean=本 bean 名}。
 *
 * <p><b>双模(优先级 subquery &gt; recordIds)</b>:
 * <ul>
 *   <li>{@link #subquery} —— 主用:返回参数化子查询,引擎包成 {@code 资源表.id IN (<子查询>)},
 *       <b>下推数据库</b>走索引、规模友好。返回 null 表示改用 recordIds。</li>
 *   <li>{@link #recordIds} —— 退化:返回有界 id 集,引擎包成 {@code 资源表.id IN (...)}。</li>
 * </ul>
 *
 * <p><b>fail-closed</b>:两者都给不出(都 null)→ 引擎注入 {@code 1=0} 拒绝所有(安全优先,
 * 与 bean 不可用时一致)。recordIds 返回空集亦为"拒绝所有"(明确无可见记录)。
 *
 * <p>注:本 SPI 是<b>读向</b>的(无列可自动填)。写鉴权 (R8) 另需判定单条记录是否属当前主体,
 * 届时再补 {@code isRelated(recordId, ctx)} 默认方法,P1 不引入。
 */
public interface RecordRelationResolver {

    /** 主模式:参数化子查询 (产出"主体可见的记录 id")。返回 null = 改用 {@link #recordIds}。 */
    default SqlFragment subquery(ScopeContext ctx) {
        return null;
    }

    /** 退化模式:有界 id 集。仅当 {@link #subquery} 返回 null 时被调用。 */
    default List<Long> recordIds(ScopeContext ctx) {
        return null;
    }
}
