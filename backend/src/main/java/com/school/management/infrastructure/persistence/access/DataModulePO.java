package com.school.management.infrastructure.persistence.access;

import lombok.Data;

/**
 * 数据模块配置 — <b>API 契约 DTO, 不对应物理表</b>。
 *
 * <p>v3 单轨后真相源是 {@code data_resources} 表, 本对象由
 * {@code DynamicModuleService.toDataModulePO} 从 DataResourcePO 内存构造,
 * 供拦截器/Controller 消费。曾经映射的 {@code data_modules} 表已是死表,
 * 于 V20260612_1 DROP — 故不再带 @TableName/@TableId。
 */
@Data
public class DataModulePO {

    /** 由 resource_code hash 派生的展示 id (非物理主键) */
    private Long id;

    private Long tenantId;
    private String moduleCode;
    private String moduleName;
    private String domainCode;
    private String domainName;
    /** 所属行业 CORE/EDU/CARE/CUSTOM — 从 data_resources.industry 透传 */
    private String industry;
    private String resourceType;
    private String orgUnitField;
    private String creatorField;
    /** 类型过滤字段, 如 user→user_type_code; NULL=该资源不支持类型过滤 */
    private String typeField;
    /** 类型选项来源实体 USER/PLACE/ORG_UNIT — 供配置 UI 拉取可选类型 */
    private String typeEntity;
    /** 是否支持②结果关系过滤(成员型资源, 如 user) — 从 data_resources.subject_relation_filterable 透传 */
    private Boolean subjectRelationFilterable;
    private Integer sortOrder;
    private Boolean enabled;
    /** 所属插件是否启用 — 从 data_resources.plugin_enabled 透传 */
    private Boolean pluginEnabled;

    /**
     * 本模块支持的 scope 代码数组 (从 data_resources.allowed_scopes 解析).
     * null = 未配置, 前端按默认全集渲染.
     */
    private java.util.List<String> allowedScopes;
}
