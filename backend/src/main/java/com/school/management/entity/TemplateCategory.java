package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板检查类别关联实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_categories")
public class TemplateCategory extends BaseEntity {

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 检查类别ID(quantification_types表)
     */
    private Long categoryId;

    /**
     * 关联类型 0不关联 1关联宿舍 2关联教室
     */
    private Integer linkType;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否必检 0否 1是
     */
    private Integer isRequired;

    /**
     * 检查轮次，默认1（已废弃，保留兼容）
     */
    private Integer checkRounds;

    /**
     * 参与的轮次，逗号分隔，如"1,3"表示参与第1轮和第3轮
     */
    private String participatedRounds;
}
