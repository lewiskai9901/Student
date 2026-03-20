package com.school.management.domain.access.model.entity;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色数据权限实体 (V5)
 * 表示角色对某个模块的数据访问范围配置
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 获取数据范围枚举
     */
    public DataScope getScope() {
        return DataScope.fromCode(scopeCode);
    }

    /**
     * 判断是否为自定义范围
     */
    public boolean isCustomScope() {
        return DataScope.CUSTOM.getCode().equals(scopeCode);
    }

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
