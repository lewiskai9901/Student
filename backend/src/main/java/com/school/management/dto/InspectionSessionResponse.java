package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 检查批次响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class InspectionSessionResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * 检查批次编号
     */
    private String sessionCode;

    /**
     * 检查日期
     */
    private LocalDate inspectionDate;

    /**
     * 检查时间
     */
    private LocalTime inspectionTime;

    /**
     * 检查人ID
     */
    private Long inspectorId;

    /**
     * 检查人姓名
     */
    private String inspectorName;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 检查目标总数
     */
    private Integer totalTargets;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductions;

    /**
     * 状态:1草稿 2待审核 3已发布
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核人姓名
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查目标列表(详情时返回)
     */
    private List<InspectionTargetResponse> targets;

    @Data
    public static class InspectionTargetResponse {
        private Long id;
        private String targetType;
        private Long targetId;
        private String targetName;
        private BigDecimal totalDeductions;
        private Integer categoryCount;
        private Integer itemCount;
        private String remarks;
        private List<InspectionCategoryResponse> categories;
    }

    @Data
    public static class InspectionCategoryResponse {
        private Long id;
        private Long typeId;
        private String typeName;
        private String typeCode;
        private BigDecimal categoryDeductions;
        private Integer itemCount;
        private String evidenceImages;
        private String remarks;
        private List<InspectionDeductionItemResponse> items;
    }

    @Data
    public static class InspectionDeductionItemResponse {
        private Long id;
        private String itemName;
        private BigDecimal deductScore;
        private Integer personCount;
        private String deductReason;
        private String evidenceImages;
    }
}
