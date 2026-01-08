package com.school.management.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日常检查响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DailyCheckResponse {

    private Long id;
    private LocalDate checkDate;
    private String checkName;
    private Long templateId;
    private String templateName;
    private Integer checkType;
    private Integer status;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称列表
     */
    private List<String> roundNames;

    /**
     * 检查目标列表
     */
    private List<CheckTargetResponse> targets;

    /**
     * 检查类别列表
     */
    private List<CheckCategoryResponse> categories;

    @Data
    public static class CheckTargetResponse {
        private Long id;
        private Integer targetType;
        private Long targetId;
        private String targetName;
    }

    @Data
    public static class CheckCategoryResponse {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private Integer linkType;
        private Integer isRequired;
        private Integer sortOrder;
        /**
         * 检查轮次数（已废弃）
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
    }
}
