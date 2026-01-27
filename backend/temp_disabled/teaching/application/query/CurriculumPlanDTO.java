package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 培养方案查询DTO
 */
@Data
@Builder
public class CurriculumPlanDTO {

    private Long id;

    private String planCode;

    private String planName;

    private Long majorId;

    private String majorName;

    private Integer enrollYear;

    private Integer duration;

    private Integer version;

    private BigDecimal totalCredits;

    private BigDecimal requiredCredits;

    private BigDecimal electiveCredits;

    private BigDecimal practiceCredits;

    private String objectives;

    private String requirements;

    private String remark;

    /**
     * 状态: 0-草稿 1-已发布 2-已归档
     */
    private Integer status;

    private String statusName;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    /**
     * 课程列表
     */
    private List<PlanCourseDTO> courses;

    /**
     * 获取状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已归档";
            default -> "";
        };
    }
}
