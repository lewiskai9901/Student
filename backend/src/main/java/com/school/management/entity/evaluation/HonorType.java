package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 荣誉类型字典实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("honor_types")
public class HonorType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 类型编码
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 类别: COMPETITION/CERTIFICATE/TITLE/ACTIVITY/PUBLICATION/OTHER
     */
    private String category;

    /**
     * 影响维度
     */
    private String evaluationDimension;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否必须上传附件
     */
    private Integer requiredAttachments;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用, 0禁用
     */
    private Integer status;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 类别常量 ====================

    public static final String CATEGORY_COMPETITION = "COMPETITION";  // 竞赛
    public static final String CATEGORY_CERTIFICATE = "CERTIFICATE";  // 证书
    public static final String CATEGORY_TITLE = "TITLE";              // 称号
    public static final String CATEGORY_ACTIVITY = "ACTIVITY";        // 活动
    public static final String CATEGORY_PUBLICATION = "PUBLICATION";  // 学术成果
    public static final String CATEGORY_OTHER = "OTHER";              // 其他
}
