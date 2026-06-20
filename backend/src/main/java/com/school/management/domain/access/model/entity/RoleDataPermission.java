package com.school.management.domain.access.model.entity;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.RelationGrant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 角色数据权限实体 —— 角色对某资源的可组合数据范围配置。
 *
 * <p>存储真相是三组正交轴 (轴① org anchor / 轴② subject-relation / 轴③ type filter),
 * 见 {@code role_data_scopes} 表与 {@link com.school.management.domain.access.model.valueobject.ScopeSpec}。
 * {@code scopeCode} 是面向 UI 的命名范围码 (由轴① 反推, 非独立持久化列)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDataPermission implements Entity<Long> {

    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 模块代码
     */
    private String moduleCode;

    /**
     * 数据范围代码
     */
    private String scopeCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 自定义范围明细项（仅当scopeCode为CUSTOM时有效）
     */
    @Builder.Default
    private List<DataScopeItem> scopeItems = new ArrayList<>();

    /**
     * 类型过滤（闸2/2b）：与组织范围 AND 组合的类型码集合。
     * 例：scope=MANAGED_ORGS + typeFilter=[STUDENT] → "我管理的组织里、只看学生类型的用户"。
     * null/空 = 不做类型过滤。仅对配置了 type_field 的资源(如 user)生效。
     */
    private List<String> typeFilter;

    // ── 可组合数据范围轴 (composable ScopeSpec axes) ──
    // 上层 (T8) 迁移后会直接填这些轴; 在此之前 saveRolePermission 从 scopeCode 翻译填充。

    /** apply_to: 该规格治理的动作类 ("READ"/"WRITE"/"BOTH")。null → 默认 BOTH。 */
    private String applyTo;

    /** 轴① org anchor (可见组织派生方式)。null → 由 scopeCode 翻译。 */
    private OrgAnchor orgAnchor;

    /** 轴① 参数: RELATION 时为关系码, PLUGIN_DIM 时为维度码。 */
    private String anchorParam;

    /** 轴① 是否含子树。 */
    private boolean includeSubtree;

    /** 轴① CUSTOM_ORG 时的指定组织 id 集合。 */
    private Set<Long> customOrgIds;

    /** 轴② subject-relation include。 */
    private Set<String> subjectRelInclude;

    /** 轴② subject-relation exclude。 */
    private Set<String> subjectRelExclude;

    /**
     * R3/R4: 显式关系授予数组 (多锚点)。非空时 saveRolePermission 直接持久化为 relation_grants,
     * 跳过"从 scopeCode 派生单 grant"。供多 grant 配置 (R3c UI / API)。
     */
    private List<RelationGrant> relationGrants;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 添加自定义范围项
     */
    public void addScopeItem(DataScopeItem item) {
        if (this.scopeItems == null) {
            this.scopeItems = new ArrayList<>();
        }
        item.setRoleDataPermissionId(this.id);
        this.scopeItems.add(item);
    }

    /**
     * 清除所有自定义范围项
     */
    public void clearScopeItems() {
        if (this.scopeItems != null) {
            this.scopeItems.clear();
        }
    }

    /**
     * 创建默认权限配置（ALL范围）
     */
    public static RoleDataPermission createDefault(Long roleId, String moduleCode) {
        return RoleDataPermission.builder()
                .roleId(roleId)
                .moduleCode(moduleCode)
                .scopeCode(DataScope.ALL.getCode())
                .description("默认配置")
                .build();
    }

    /**
     * 创建自定义范围权限
     */
    public static RoleDataPermission createCustom(Long roleId, String moduleCode, List<DataScopeItem> items) {
        RoleDataPermission permission = RoleDataPermission.builder()
                .roleId(roleId)
                .moduleCode(moduleCode)
                .scopeCode(DataScope.CUSTOM.getCode())
                .description("自定义范围")
                .scopeItems(items != null ? items : new ArrayList<>())
                .build();
        return permission;
    }
}
