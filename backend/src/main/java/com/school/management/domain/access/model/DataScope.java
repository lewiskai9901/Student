package com.school.management.domain.access.model;

/**
 * 数据权限范围枚举 (v3 通用化,去教育术语"部门")
 * 定义用户可访问的数据范围级别.
 *
 * <p><strong>注意:此 enum 是 {@code AccessRelation}
 * (位于 {@code domain.access.model.entity.AccessRelation}) 的宏观快捷表达,
 * 不是与之竞争的独立体系</strong>。详见
 * {@code backend/docs/design/access/ADR-001-datascope-as-access-relation-macro.md}。
 *
 * <p>映射关系简要:
 * <ul>
 *   <li>{@code SELF} → AccessRelation 中 subject_id=me (落到 created_by 过滤)</li>
 *   <li>{@code DEPARTMENT} → access_relations 中 member(user=me, org)</li>
 *   <li>{@code DEPARTMENT_AND_BELOW} → DEPARTMENT + tree_path LIKE 子树递归</li>
 *   <li>{@code CUSTOM} → 通过 access_relations / role_custom_scope 自定义授权</li>
 *   <li>{@code ALL} → 绕过 interceptor,不映射任何 AccessRelation</li>
 * </ul>
 *
 * <p>插件维度 (如 BY_MAJOR) 不进此 enum, 走 {@code PluginDataScopeRouter}.
 */
public enum DataScope {
    ALL("ALL", "全部数据", 100, CalcType.NONE, "可访问系统中所有数据"),
    DEPARTMENT_AND_BELOW("DEPARTMENT_AND_BELOW", "本组织及下级", 80, CalcType.USER_ORG_TREE, "可访问用户所属组织及下级组织的数据"),
    DEPARTMENT("DEPARTMENT", "仅本组织", 60, CalcType.USER_ORG, "只能访问用户所属组织的数据"),
    CUSTOM("CUSTOM", "自定义范围", 40, CalcType.CUSTOM_CONFIG, "管理员配置的指定范围"),
    SELF("SELF", "仅本人", 20, CalcType.CREATOR, "只能访问自己创建或负责的数据");

    private final String code;
    private final String displayName;
    private final int level;
    private final CalcType calcType;
    private final String description;

    DataScope(String code, String displayName, int level, CalcType calcType, String description) {
        this.code = code;
        this.displayName = displayName;
        this.level = level;
        this.calcType = calcType;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public CalcType getCalcType() {
        return calcType;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 计算类型枚举
     */
    public enum CalcType {
        NONE,           // 无需计算，直接返回所有
        USER_ORG,       // 基于用户所属组织
        USER_ORG_TREE,  // 基于用户所属组织及下级
        CUSTOM_CONFIG,  // 基于自定义配置
        CREATOR         // 基于创建者
    }

    public static DataScope fromCode(String code) {
        if (code == null) {
            return ALL;
        }
        for (DataScope scope : values()) {
            if (scope.code.equalsIgnoreCase(code)) {
                return scope;
            }
        }
        return ALL;
    }

    /**
     * 按 code 严格匹配 hardcoded enum, 找不到返回 null (不 fallback).
     *
     * 用于区分 core 的 5 种 hardcoded 维度 vs 插件贡献的动态维度(如 BY_MAJOR).
     * 调用方 (如 DataPermissionInterceptor) 拿到 null 后应路由到 PluginDataScopeRouter
     * 解析 data_scope_dims 表里的插件维度.
     */
    public static DataScope fromCodeStrict(String code) {
        if (code == null) return null;
        for (DataScope scope : values()) {
            if (scope.code.equalsIgnoreCase(code)) {
                return scope;
            }
        }
        return null;
    }

    /**
     * 比较两个范围的优先级，返回更高优先级的范围
     * 用于多角色权限合并时取最大范围
     */
    public static DataScope max(DataScope a, DataScope b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.level >= b.level ? a : b;
    }

    /**
     * 判断当前范围是否大于等于目标范围
     */
    public boolean isGreaterOrEqual(DataScope other) {
        return this.level >= other.level;
    }
}
