package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Persistence object for inspection templates.
 * Maps to the existing 'check_templates' table.
 */
@Data
@TableName("check_templates")
public class InspectionTemplatePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 轮次名称数组 JSON格式
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
     */
    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
