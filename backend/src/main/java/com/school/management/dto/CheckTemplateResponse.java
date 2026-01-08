package com.school.management.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检查模板响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class CheckTemplateResponse {

    /**
     * 模板ID
     */
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
     * 轮次名称列表
     */
    private List<String> roundNames;

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

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查类别列表
     */
    private List<TemplateCategoryResponse> categories;

    @Data
    public static class TemplateCategoryResponse {
        /**
         * 关联记录ID
         */
        private Long id;

        /**
         * 检查类别ID
         */
        private Long categoryId;

        /**
         * 类别名称
         */
        private String categoryName;

        /**
         * 类别编码
         */
        private String categoryCode;

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
         * 检查轮次（已废弃）
         */
        private Integer checkRounds;

        /**
         * 参与的轮次，如"1,3"
         */
        private String participatedRounds;

        /**
         * 参与的轮次列表
         */
        private List<Integer> participatedRoundsList;

        /**
         * 扣分项列表
         */
        private List<DeductionItemResponse> deductionItems;
    }

    @Data
    public static class DeductionItemResponse {
        /**
         * 扣分项ID
         */
        private Long id;

        /**
         * 扣分项名称
         */
        private String itemName;

        /**
         * 扣分模式 1固定扣分 2按人数扣分 3区间扣分
         */
        private Integer deductMode;

        /**
         * 固定扣分分数(模式1)
         */
        private java.math.BigDecimal fixedScore;

        /**
         * 基础扣分分数(模式2)
         */
        private java.math.BigDecimal baseScore;

        /**
         * 每人扣分分数(模式2)
         */
        private java.math.BigDecimal perPersonScore;

        /**
         * 区间配置JSON(模式3)
         */
        private String rangeConfig;

        /**
         * 扣分项描述
         */
        private String description;

        /**
         * 排序
         */
        private Integer sortOrder;

        /**
         * 是否允许上传照片 0否 1是
         */
        private Integer allowPhoto;

        /**
         * 是否允许添加备注 0否 1是
         */
        private Integer allowRemark;

        /**
         * 是否允许添加学生 0否 1是
         */
        private Integer allowStudents;
    }
}
