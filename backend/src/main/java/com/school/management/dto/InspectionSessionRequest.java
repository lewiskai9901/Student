package com.school.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建检查批次请求DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class InspectionSessionRequest {

    /**
     * 检查日期
     */
    @NotNull(message = "检查日期不能为空")
    private LocalDate inspectionDate;

    /**
     * 检查时间
     */
    private LocalTime inspectionTime;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 检查目标列表
     */
    @NotEmpty(message = "检查目标不能为空")
    @Valid
    private List<InspectionTargetRequest> targets;

    @Data
    public static class InspectionTargetRequest {
        /**
         * 目标类型:class-班级,dorm-宿舍
         */
        private String targetType = "class";

        /**
         * 目标ID
         */
        @NotNull(message = "目标ID不能为空")
        private Long targetId;

        /**
         * 目标名称
         */
        @NotNull(message = "目标名称不能为空")
        private String targetName;

        /**
         * 备注
         */
        private String remarks;

        /**
         * 检查类别列表
         */
        @Valid
        private List<InspectionCategoryRequest> categories;
    }

    @Data
    public static class InspectionCategoryRequest {
        /**
         * 量化类型ID
         */
        @NotNull(message = "量化类型ID不能为空")
        private Long typeId;

        /**
         * 类别名称
         */
        @NotNull(message = "类别名称不能为空")
        private String typeName;

        /**
         * 类别代码
         */
        @NotNull(message = "类别代码不能为空")
        private String typeCode;

        /**
         * 证据图片
         */
        private String evidenceImages;

        /**
         * 备注
         */
        private String remarks;

        /**
         * 扣分项列表
         */
        @Valid
        private List<InspectionDeductionItemRequest> items;
    }

    @Data
    public static class InspectionDeductionItemRequest {
        /**
         * 扣分项名称
         */
        @NotNull(message = "扣分项名称不能为空")
        private String itemName;

        /**
         * 扣分分值
         */
        @NotNull(message = "扣分分值不能为空")
        private java.math.BigDecimal deductScore;

        /**
         * 涉及人数
         */
        private Integer personCount = 0;

        /**
         * 扣分原因详情
         */
        private String deductReason;

        /**
         * 证据图片
         */
        private String evidenceImages;
    }
}
