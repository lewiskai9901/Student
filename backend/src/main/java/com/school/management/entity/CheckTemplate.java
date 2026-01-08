package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检查模板实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_templates")
public class CheckTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称数组 JSON格式 如["早检","午检","晚检"]
     */
    private String roundNames;

    /**
     * 是否默认模板 0否 1是
     */
    private Integer isDefault;

    /**
     * 状态 0禁用 1启用
     */
    private Integer status;

    /**
     * 创建人ID
     * 重写BaseEntity中的字段,在此表中该字段实际存在
     */
    @TableField(value = "created_by", exist = true)
    private Long createdBy;
}
