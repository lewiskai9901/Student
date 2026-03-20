package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("scope_item_types")
public class ScopeItemTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String itemTypeCode;
    private String itemTypeName;
    private String refTable;
    private String refIdField;
    private String refNameField;
    private String refParentField;
    private Boolean supportChildren;
    private Integer sortOrder;
}
