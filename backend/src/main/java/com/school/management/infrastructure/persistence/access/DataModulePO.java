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
    private String resourceType;
    private String orgUnitField;
    private String creatorField;
    private Integer sortOrder;
    private Boolean enabled;
}
