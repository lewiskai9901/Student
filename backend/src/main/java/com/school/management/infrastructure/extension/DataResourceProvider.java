package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 数据资源 SPI — 插件声明自己贡献的数据模块及其支持的 scope 集合.
 *
 * data_resources 表里每一行对应一个"数据模块"(例: student / grade_batch / place).
 * 不同模块支持的数据权限 scope 不同 — 例如:
 *   - CORE 组织向模块 (user/org_unit): ALL / DEPARTMENT_AND_BELOW / DEPARTMENT / SELF / CUSTOM
 *   - EDU 学生类 (student/attendance): + BY_GRADE / BY_CLASS / BY_MAJOR
 *   - 行业插件类资源: + 自定义维度 (插件用 DataScopeProvider SPI 贡献)
 *
 * 本 SPI 让每个插件维护自己这块语义, 不用再改 SQL migration.
 *
 * 行记录本身仍由早期 migration 落库, 本 SPI 只 UPDATE `allowed_scopes` 列 —
 * 若 provider 声明的 resourceCode 在表里不存在会跳过并记警告, 不阻塞启动.
 *
 * Registrar 在 PermissionRegistrar(400) / DataScopeRegistrar(600) 之后跑 (@Order 700),
 * 不需事件通知 (前端按请求实时读 data_resources).
 */
public interface DataResourceProvider {

    /** 声明本插件维护的数据资源 scope 配置 */
    List<DataResourceDef> getDataResources();

    /**
     * 单个数据资源的 scope 声明.
     *
     * @param resourceCode  对应 data_resources.resource_code (主键)
     * @param allowedScopes 本模块允许的 scope 代码集合 (至少 1 个)
     */
    record DataResourceDef(String resourceCode, List<String> allowedScopes) {

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
}
