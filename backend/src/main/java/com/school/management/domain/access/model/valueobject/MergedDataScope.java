package com.school.management.domain.access.model.valueobject;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 合并后的数据范围值对象 (V5)
 * 用于多角色权限合并的结果表示
 *
 * 合并策略：
 * 1. 如果任一角色有ALL权限，最终结果为ALL
 * 2. 非ALL权限按优先级取最大（DEPARTMENT_AND_BELOW > DEPARTMENT > CUSTOM > SELF）
 * 3. CUSTOM范围的scopeItems取并集
 * 4. SELF范围单独处理（只能看自己创建的）
 */
@Data
@Builder
public class MergedDataScope {

    /**
     * 模块代码
     */
    private String moduleCode;

    /**
     * 合并后的最终范围
     */
    private DataScope effectiveScope;

    /**
     * 合并后的自定义范围项（当effectiveScope为CUSTOM时有效）
     * Key: itemTypeCode, Value: scopeId列表
     */
    @Builder.Default
    private Map<String, Set<Long>> mergedScopeItems = new HashMap<>();

    /**
     * 包含子级的组织单元ID集合
     */
    @Builder.Default
    private Set<Long> orgUnitsWithChildren = new HashSet<>();

    /**
     * 原始权限来源（用于调试和审计）
     */
    @Builder.Default
    private List<Long> sourceRoleIds = new ArrayList<>();

    /**
     * 是否有SELF权限（需要额外过滤创建者）
     */
    @Builder.Default
    private boolean hasSelfScope = false;

    /**
     * 合并多个角色的数据权限
     */
    public static MergedDataScope merge(String moduleCode, List<RoleDataPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            // 无权限配置，默认SELF
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .build();
        }

        List<Long> roleIds = permissions.stream()
                .map(RoleDataPermission::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // 检查是否有ALL权限
        boolean hasAll = permissions.stream()
                .anyMatch(p -> DataScope.ALL.getCode().equals(p.getScopeCode()));

        if (hasAll) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.ALL)
                    .sourceRoleIds(roleIds)
                    .build();
        }

        // 检查是否有SELF权限
        boolean hasSelf = permissions.stream()
                .anyMatch(p -> DataScope.SELF.getCode().equals(p.getScopeCode()));

        // 找出最高优先级的非ALL范围
        DataScope maxScope = permissions.stream()
                .map(RoleDataPermission::getScope)
                .filter(s -> s != DataScope.ALL && s != DataScope.SELF)
                .reduce(null, DataScope::max);

        // 合并CUSTOM范围项
        Map<String, Set<Long>> mergedItems = new HashMap<>();
        Set<Long> orgUnitsWithChildren = new HashSet<>();

        permissions.stream()
                .filter(RoleDataPermission::isCustomScope)
                .flatMap(p -> p.getScopeItems().stream())
                .forEach(item -> {
                    mergedItems.computeIfAbsent(item.getItemTypeCode(), k -> new HashSet<>())
                            .add(item.getScopeId());
                    if (Boolean.TRUE.equals(item.getIncludeChildren()) && item.isOrgUnitType()) {
                        orgUnitsWithChildren.add(item.getScopeId());
                    }
                });

        // 确定最终范围
        DataScope effectiveScope;
        if (maxScope != null) {
            effectiveScope = maxScope;
        } else if (!mergedItems.isEmpty()) {
            effectiveScope = DataScope.CUSTOM;
        } else if (hasSelf) {
            effectiveScope = DataScope.SELF;
        } else {
            effectiveScope = DataScope.SELF; // 默认
        }

        return MergedDataScope.builder()
                .moduleCode(moduleCode)
                .effectiveScope(effectiveScope)
                .mergedScopeItems(mergedItems)
                .orgUnitsWithChildren(orgUnitsWithChildren)
                .sourceRoleIds(roleIds)
                .hasSelfScope(hasSelf)
                .build();
    }

    /**
     * 是否需要过滤数据
     */
    public boolean needsFiltering() {
        return effectiveScope != DataScope.ALL;
    }

    /**
     * 是否为ALL范围（无需过滤）
     */
    public boolean isAllScope() {
        return effectiveScope == DataScope.ALL;
    }

    /**
     * 是否为SELF范围
     */
    public boolean isSelfScope() {
        return effectiveScope == DataScope.SELF;
    }

    /**
     * 是否为CUSTOM范围
     */
    public boolean isCustomScope() {
        return effectiveScope == DataScope.CUSTOM;
    }

    /**
     * 获取指定类型的范围ID集合
     */
    public Set<Long> getScopeIds(String itemTypeCode) {
        return mergedScopeItems.getOrDefault(itemTypeCode, Collections.emptySet());
    }

    /**
     * 获取组织单元ID集合
     */
    public Set<Long> getOrgUnitIds() {
        return getScopeIds("ORG_UNIT");
    }

    /**
     * 判断指定组织单元是否包含子级
     */
    public boolean includesChildren(Long orgUnitId) {
        return orgUnitsWithChildren.contains(orgUnitId);
    }
}
