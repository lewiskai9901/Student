package com.school.management.domain.access.model;

import com.school.management.domain.access.model.valueobject.ScopeSpec;

/**
 * 数据范围预设目录 (ScopePreset) —— 面向 UI 的"命名范围"糖衣 (全部/本组织/我管理的组织/…)。
 *
 * <p>数据范围模型正从扁平的单枚举 {@link DataScope} 重构为若干<b>正交可组合的轴</b>。
 * 本枚举把那批熟悉的命名范围保留为 UI 预设, 每个预设<b>只映射到轴① (org anchor + param + subtree)</b>;
 * apply_to 与轴② (subject-relation filter)、轴③ (type filter) 由调用方/存储层另行叠加, 预设不碰。
 * 真正的存储真相是可组合的列, 预设只是便利入口。
 *
 * <p>UI-facing catalog of the familiar named scopes. Each preset maps to <b>axis ① only</b>
 * (anchor + param + subtree); applyTo and axes ②③ are layered separately by the caller.
 * Plugin dimensions (BY_CLASS / BY_MAJOR / …) are <b>NOT</b> presets — see
 * {@link #fromLegacyScopeType(String)} which returns {@code null} for them so the caller can branch.
 *
 * <p>{@code level} 与现有 {@link DataScope} 的等级保持一致, 以保留既有排序。
 */
public enum ScopePreset {

    /** 全部数据。 */
    ALL("全部数据", 100, OrgAnchor.ALL, null, false),
    /** 仅本人。 */
    SELF("仅本人", 20, OrgAnchor.SELF, null, false),
    /** 本组织。 */
    DEPARTMENT("本组织", 60, OrgAnchor.PRIMARY_ORG, null, false),
    /** 本组织及以下。 */
    DEPARTMENT_AND_BELOW("本组织及以下", 80, OrgAnchor.PRIMARY_ORG, null, true),
    /** 我管理的组织 (admin 关系派生)。 */
    MANAGED_ORGS("我管理的组织", 65, OrgAnchor.RELATION, "admin", false),
    /** 我管理的组织及以下 (admin 关系派生 + 子树)。 */
    MANAGED_ORGS_AND_BELOW("我管理的组织及以下", 75, OrgAnchor.RELATION, "admin", true),
    /** 自定义 (管理员显式指定组织集合)。 */
    CUSTOM("自定义", 40, OrgAnchor.CUSTOM_ORG, null, false);

    private final String displayName;
    private final int level;
    private final OrgAnchor orgAnchor;
    private final String anchorParam;
    private final boolean includeSubtree;

    ScopePreset(String displayName, int level, OrgAnchor orgAnchor, String anchorParam, boolean includeSubtree) {
        this.displayName = displayName;
        this.level = level;
        this.orgAnchor = orgAnchor;
        this.anchorParam = anchorParam;
        this.includeSubtree = includeSubtree;
    }

    /** UI 展示名 (中文)。 */
    public String getDisplayName() {
        return displayName;
    }

    /** 排序等级 (与 {@link DataScope} 对齐)。 */
    public int getLevel() {
        return level;
    }

    /**
     * 派生轴① 规格 —— <b>只</b>填 org anchor + param + subtree。
     * applyTo 与轴②③ 留 {@code null}, 由调用方叠加。
     */
    public ScopeSpec toSpec() {
        return ScopeSpec.builder()
                .orgAnchor(orgAnchor)
                .anchorParam(anchorParam)
                .includeSubtree(includeSubtree)
                .build();
    }

    /**
     * 把旧 {@code role_data_scopes.scope_type} 字符串翻译为预设。
     *
     * <p>旧 {@link DataScope} 码与本枚举常量名同名 (ALL/SELF/DEPARTMENT/DEPARTMENT_AND_BELOW/
     * MANAGED_ORGS/MANAGED_ORGS_AND_BELOW/CUSTOM) —— 精确命中即返回对应预设。
     * 其它一切 (插件维度 BY_CLASS/BY_MAJOR/BY_GRADE、未知码、空串、{@code null}) 返回 {@code null}:
     * 调用方将 {@code null} 视为"非预设 → 插件维度", 由 PLUGIN_DIM 路径接管 (不在本类职责内)。
     *
     * @param scopeType 旧 scope_type 字符串, 可为 {@code null}
     * @return 对应预设, 或 {@code null} (未知/插件维度/null)
     */
    public static ScopePreset fromLegacyScopeType(String scopeType) {
        if (scopeType == null) {
            return null;
        }
        for (ScopePreset p : values()) {
            if (p.name().equals(scopeType)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 反向: 由已持久化的轴① (anchor + param + subtree) 还原对应预设的命名码。
     *
     * <p>T9 删除 {@code scope_type} 列后, 读路径需要把三轴反推回 UI 熟悉的命名范围码
     * (ALL/SELF/DEPARTMENT/.../CUSTOM), 作为 {@code RoleDataPermission.scopeCode} 回填前端。
     * 与 {@link #toSpec()} 严格往返。
     *
     * <p>插件维度 (anchor=PLUGIN_DIM) 不是预设 → 返回 {@code null}; 调用方应直接用
     * {@code anchorParam} (维度码 BY_CLASS…) 作为 scopeCode。anchor 为 {@code null} 同样返回
     * {@code null} (无配置)。
     *
     * @return 命中预设的 {@code name()}; 插件维度/未知/null → {@code null}
     */
    public static String scopeCodeFromAxes(OrgAnchor anchor, String anchorParam, boolean includeSubtree) {
        if (anchor == null) {
            return null;
        }
        for (ScopePreset p : values()) {
            if (p.orgAnchor == anchor
                    && java.util.Objects.equals(p.anchorParam, anchorParam)
                    && p.includeSubtree == includeSubtree) {
                return p.name();
            }
        }
        return null;
    }
}
