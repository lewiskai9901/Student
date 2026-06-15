package com.school.management.domain.access.model;

/**
 * 组织锚点 (数据范围"轴①") —— 一个角色可见组织集合的派生方式。
 *
 * <p>数据范围 (data scope) 正从扁平的 {@link DataScope} 单枚举重构为若干正交可组合的轴。
 * 本枚举是轴① (organization anchor) 的存储真相: 后续 {@code ScopeSpec} 值对象持有一个
 * {@code OrgAnchor} 字段, 并以 {@code name()} 持久化到 DB 列 {@code role_data_scopes.org_anchor}。
 * 因此 {@link #fromCode(String)} 必须与 {@code name()} 严格往返, 未知输入返回 {@code null}
 * (调用方将 {@code null} 视为"不是 core 锚点", 由插件维度等其它路径接管)。
 *
 * <ul>
 *   <li>{@link #ALL} —— 全部组织, 不做任何过滤 (绕过 interceptor)。</li>
 *   <li>{@link #SELF} —— 仅本人 (落到创建者/负责人过滤, 不锚定任何组织)。</li>
 *   <li>{@link #PRIMARY_ORG} —— 用户主属组织 (member 关系所在 org)。</li>
 *   <li>{@link #RELATION} —— 由关系派生的组织集 (如 admin 关系=我管理的组织)。</li>
 *   <li>{@link #CUSTOM_ORG} —— 管理员显式配置的指定组织集合。</li>
 *   <li>{@link #PLUGIN_DIM} —— 由插件贡献的动态维度派生 (如按专业), 走 {@code PluginDataScopeRouter}。</li>
 * </ul>
 */
public enum OrgAnchor {
    ALL, SELF, PRIMARY_ORG, RELATION, CUSTOM_ORG, PLUGIN_DIM;

    /**
     * 按 {@code name()} 严格解析, 找不到 (含 {@code null}) 返回 {@code null}。
     * 与 {@code name()} 往返一致, 供 {@code ScopeSpec} 从 DB 列反序列化。
     */
    public static OrgAnchor fromCode(String c) {
        if (c == null) return null;
        for (OrgAnchor a : values()) {
            if (a.name().equals(c)) return a;
        }
        return null;
    }
}
