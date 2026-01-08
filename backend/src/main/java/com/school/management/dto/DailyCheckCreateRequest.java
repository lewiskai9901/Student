package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建日常检查请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DailyCheckCreateRequest {

    /**
     * 所属检查计划ID (可选)
     */
    private Long planId;

    /**
     * 检查日期
     */
    @NotNull(message = "检查日期不能为空")
    private LocalDate checkDate;

    /**
     * 检查名称
     */
    @NotBlank(message = "检查名称不能为空")
    private String checkName;

    /**
     * 使用的模板ID (可选,如果不选择模板则手动配置)
     */
    private Long templateId;

    /**
     * 检查类型 1日常检查 2专项检查
     */
    private Integer checkType;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称列表
     */
    private List<String> roundNames;

    /**
     * 检查说明
     */
    private String description;

    /**
     * 检查目标列表
     */
    @NotNull(message = "检查目标不能为空")
    private List<CheckTargetItem> targets;

    /**
     * 检查类别列表 (如果使用模板则可为空)
     */
    private List<CheckCategoryItem> categories;

    /**
     * 排除的目标列表 (可选，用于记录被排除的班级及原因)
     */
    private List<ExcludedTargetItem> excludedTargets;

    @Data
    public static class ExcludedTargetItem {
        /**
         * 班级ID
         */
        private Long classId;

        /**
         * 班级名称
         */
        private String className;

        /**
         * 排除原因
         */
        private String reason;
    }

    @Data
    public static class CheckTargetItem {
        /**
         * 目标类型 1班级 2年级 3院系
         */
        @NotNull(message = "目标类型不能为空")
        private Integer targetType;

        /**
         * 目标ID
         */
        @NotNull(message = "目标ID不能为空")
        private Long targetId;

        /**
         * 目标名称
         */
        private String targetName;
    }

    @Data
    public static class CheckCategoryItem {
        /**
         * 检查类别ID
         */
        @NotNull(message = "类别ID不能为空")
        private Long categoryId;

        /**
         * 类别名称
         */
        private String categoryName;

        /**
         * 关联类型 0不关联 1关联宿舍 2关联教室
         */
        private Integer linkType;

        /**
         * 是否必检
         */
        private Integer isRequired;

        /**
         * 排序
         */
        private Integer sortOrder;

        /**
         * 检查轮次数（已废弃）
         */
        private Integer checkRounds;

        /**
         * 参与的轮次，如"1,3"
         */
        private String participatedRounds;
    }
}
