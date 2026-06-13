package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 单个数据资源的 scope 声明 (Phase 1 双轨收敛: 从已删的 DataResourceProvider SPI 提为顶层 record)。
 *
 * <p>data_resources 表里每一行对应一个"数据模块"(例: student / grade_batch / place),
 * 不同模块支持的数据权限 scope 不同。行记录本身由早期 migration 落库, 插件只通过
 * {@link Contribution.DataResourceContribution} 声明并 UPDATE 该行的 {@code allowed_scopes} 列;
 * 若声明的 resourceCode 在表里不存在则跳过 (见 {@link DataResourceUpserter})。
 *
 * @param resourceCode  对应 data_resources.resource_code (主键)
 * @param allowedScopes 本模块允许的 scope 代码集合 (至少 1 个)
 */
public record DataResourceDef(String resourceCode, List<String> allowedScopes) {

    public DataResourceDef {
        if (resourceCode == null || resourceCode.isBlank()) {
            throw new IllegalArgumentException("resourceCode 不能为空");
        }
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            throw new IllegalArgumentException("allowedScopes 至少声明 1 个 scope: " + resourceCode);
        }
    }

    public static DataResourceDef of(String resourceCode, String... scopes) {
        return new DataResourceDef(resourceCode, List.of(scopes));
    }
}
