package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * v3 数据资源配置持久化对象 (data_resources 表)
 * 替代 v2 的 DataModulePO + data_modules 表
 */
@Data
@TableName("data_resources")
public class DataResourcePO {

    @TableId(type = IdType.INPUT)
    private String resourceCode;

    private String resourceName;
    private String domainCode;
    private String domainName;

    /** 所属行业 CORE/EDU/CARE/CUSTOM — 插件生命周期级联用 */
    private String industry;

    /** access_relations 表里对应的 resource_type(可为 NULL,表示不走关系子查询) */
    private String accessResourceType;

    private String orgUnitField;
    private String creatorField;

    private String registeredBy;
    private Integer sortOrder;
    private Integer enabled;
    private Long tenantId;

    /** 所属插件是否启用 — 由 PluginLifecycleService 级联控制, 默认 1 */
    private Boolean pluginEnabled;

    /**
     * 本模块支持的 scope 代码数组 (JSON 字符串).
     * 例: ["ALL","BY_CLASS","SELF","CUSTOM"]
     * null/空 = 兼容旧数据, 前端按 fallback 全集渲染
     */
    @TableField("allowed_scopes")
    private String allowedScopes;
}
