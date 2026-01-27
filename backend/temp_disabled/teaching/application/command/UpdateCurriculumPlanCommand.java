package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新培养方案命令
 */
@Data
@Builder
public class UpdateCurriculumPlanCommand {

    @NotNull(message = "方案ID不能为空")
    private Long id;

    private String planName;

    private Integer duration;

    private BigDecimal totalCredits;

    private BigDecimal requiredCredits;

    private BigDecimal electiveCredits;

    private BigDecimal practiceCredits;

    private String objectives;

    private String requirements;

    private String remark;

    /**
     * 课程列表(如果提供则全量更新)
     */
    private List<PlanCourseItem> courses;

    /**
     * 状态: 0-草稿 1-已发布 2-已归档
     */
    private Integer status;

    private Long operatorId;

    @Data
    @Builder
    public static class PlanCourseItem {
        private Long courseId;
        private Integer semester;
        private Integer weeklyHours;
        private String examType;
        private Boolean isRequired;
    }
}
