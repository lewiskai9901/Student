package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("data_modules")
public class DataModulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String moduleCode;
    private String moduleName;
    private String domainCode;
    private String domainName;
    /** 所属行业 CORE/EDU/HEALTH/CARE/CUSTOM — 从 data_resources.industry 透传 */
    private String industry;
    private String resourceType;
    private String orgUnitField;
    private String creatorField;
    private Integer sortOrder;
    private Boolean enabled;
    /** 所属插件是否启用 — 从 data_resources.plugin_enabled 透传 */
    private Boolean pluginEnabled;
}
