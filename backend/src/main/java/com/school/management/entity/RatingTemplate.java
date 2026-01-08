package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评级模板实体类
 * 对应表: rating_templates
 * 
 * 关键: 绑定检查模板 (check_template_id)
 * 不同检查模板有不同的评级标准,不能混用
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rating_templates")
public class RatingTemplate extends BaseEntity {

    /**
     * 评级模板名称: "综合检查评级模板"
     */
    private String templateName;

    /**
     * 评级模板编码: "RATING_COMPREHENSIVE"
     */
    private String templateCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 绑定的检查模板ID (关键!)
     */
    private Long checkTemplateId;

    /**
     * 是否默认
     */
    private Integer isDefault;

    /**
     * 状态: 1=启用, 0=禁用
     */
    private Integer status;
}
