package com.school.management.casbin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 范围元数据实体
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("scope_metadata")
public class ScopeMetadata {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
