package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新培养方案请求
 */
@Data
public class UpdateCurriculumPlanRequest {

    private String planName;

    private Integer duration;

    private BigDecimal totalCredits;

    private BigDecimal requiredCredits;

    private BigDecimal electiveCredits;

    private BigDecimal practiceCredits;

    private String objectives;

    private String requirements;

    private String remark;

    private Integer status;

    private List<PlanCourseItem> courses;

    @Data
    public static class PlanCourseItem {
        private Long courseId;
        private Integer semester;
        private Integer weeklyHours;
        private String examType;
        private Boolean isRequired;
    }
}
