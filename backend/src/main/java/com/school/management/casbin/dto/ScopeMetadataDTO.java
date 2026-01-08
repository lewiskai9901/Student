package com.school.management.casbin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 范围元数据DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScopeMetadataDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 范围表达式
     */
    private String scopeExpression;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 范围类型
     */
    private String scopeType;

    /**
     * 范围类型名称
     */
    private String scopeTypeName;

    /**
     * 关联实体ID
     */
    private Long refId;

    /**
     * 关联实体类型
     */
    private String refType;

    /**
     * 父范围表达式
     */
    private String parentScope;

    /**
     * 排序
     */
    private Integer sortOrder;
}
