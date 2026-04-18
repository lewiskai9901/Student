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

    /** access_relations 表里对应的 resource_type(可为 NULL,表示不走关系子查询) */
    private String accessResourceType;

    private String orgUnitField;
    private String creatorField;

    private String registeredBy;
    private Integer sortOrder;
    private Integer enabled;
    private Long tenantId;
}
