package com.school.management.application.dashboard;

/**
 * 看板数据范围 — 由核心解析当前用户的数据权限后, 传给各 {@link DashboardSectionContributor}。
 *
 * <p>让插件贡献的看板分区 (如教务统计) 在不感知核心 ScopeFilter 内部实现的前提下,
 * 仍能按"全量 / 子树 / 单组织 / 拒绝"四种范围过滤自己的 SQL。
 *
 * @param kind        UNRESTRICTED(全量) / SUBTREE(本组织及以下) / SINGLE(仅本组织) / DENY(无权)
 * @param tenantId    租户
 * @param orgUnitPath SUBTREE 时的 tree_path 前缀 (含末尾 /)
 * @param orgUnitId   SINGLE 时的组织 id
 */
public record DashboardScope(String kind, Long tenantId, String orgUnitPath, Long orgUnitId) {
    public boolean unrestricted() { return "UNRESTRICTED".equals(kind); }
    public boolean subtree()      { return "SUBTREE".equals(kind); }
    public boolean single()       { return "SINGLE".equals(kind); }
    public boolean deniesAll()    { return "DENY".equals(kind); }
}
