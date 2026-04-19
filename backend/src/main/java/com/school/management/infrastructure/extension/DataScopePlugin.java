package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 数据权限维度插件 SPI.
 *
 * 通用核心已有 5 种维度: ALL / DEPARTMENT_AND_BELOW / DEPARTMENT / SELF / CUSTOM.
 * 插件可声明新维度(如教育的"按专业"、"按年级"),供管理员在角色数据权限配置 UI 中选择,
 * 并在数据查询时由 {@link DataScopeResolver} 解析为可访问的 resource_id 列表.
 *
 * 典型实现:
 * <pre>
 *   &#64;Component
 *   public class EducationDataScopePlugin implements DataScopePlugin {
 *       public List&lt;DimensionDef&gt; getDimensions() {
 *           return List.of(
 *               new DimensionDef("BY_MAJOR", "按专业",
 *                   "按照用户所在专业的所有学生/课程",
 *                   majorResolver)
 *           );
 *       }
 *   }
 * </pre>
 *
 * Registrar (@Order 600) 启动时 UPSERT 到 data_scope_dims 表.
 */
public interface DataScopePlugin {

    /** 业务域码,用于分组展示 */
    String getDomainCode();

    /** 声明本插件贡献的数据维度 */
    List<DimensionDef> getDimensions();

    /**
     * 数据维度定义.
     *
     * @param code         维度码,如 "BY_MAJOR"
     * @param name         中文名 "按专业"
     * @param description  业务说明
     * @param resolverType resolver 实现类全限定名,由 Registrar 在运行时通过 ApplicationContext 解析
     */
    record DimensionDef(
        String code,
        String name,
        String description,
        String resolverType
    ) {}

    /**
     * 数据范围解析器 — 插件实现类在运行时提供"给定用户 + 资源类型 → 可访问的 resource_id 列表".
     */
    interface DataScopeResolver {
        /**
         * @param userId       当前登录用户
         * @param resourceType 要访问的资源类型 (如 'user' / 'place' / 'student')
         * @return 可访问的 resource_id 列表,空列表=拒绝所有,null=不限(相当于 ALL)
         */
        List<Long> resolve(Long userId, String resourceType);
    }
}
