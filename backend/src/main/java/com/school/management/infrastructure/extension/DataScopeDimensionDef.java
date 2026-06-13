package com.school.management.infrastructure.extension;

/**
 * 数据权限维度定义 (Phase 2 双轨收敛: 从已删的 DataScopePlugin SPI 提为顶层 record)。
 *
 * <p>通用核心已有 5 种维度 (ALL / DEPARTMENT_AND_BELOW / DEPARTMENT / SELF / CUSTOM);
 * 插件通过 {@link Contribution.DataScopeContribution} 声明新维度 (如教育的 BY_MAJOR/BY_GRADE/BY_CLASS),
 * 供 admin 在角色数据权限配置 UI 中选用, 查询时由 {@link DataScopeResolver} 解析为 resource_id 列表。
 *
 * @param code         维度码, 如 "BY_MAJOR"
 * @param name         中文名 "按专业"
 * @param description  业务说明
 * @param resolverType resolver 实现类全限定名, 运行时由 ApplicationContext 解析
 */
public record DataScopeDimensionDef(
    String code,
    String name,
    String description,
    String resolverType
) {}
