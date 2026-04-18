package com.school.management.infrastructure.persistence.access;

import lombok.Data;

/**
 * @deprecated v3 已不使用 scope_item_types 表 (CUSTOM scope 改用 custom_org_unit_ids JSON)。
 *             保留类定义作为 API DTO 兼容层,相关查询总是返回空列表。
 */
@Deprecated
@Data
public class ScopeItemTypePO {
    private Long id;
    private Long tenantId;
    private String itemTypeCode;
    private String itemTypeName;
    private Integer sortOrder;

    /** 是否支持 include_children (v2 遗留字段, v3 默认支持) */
    private Boolean supportChildren;

    /** 关联的业务表 (v2 遗留字段, v3 不再使用) */
    private String refTable;
    private String refIdField;
    private String refNameField;
}
