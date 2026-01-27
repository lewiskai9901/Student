package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建培养方案请求
 */
@Data
public class CreateCurriculumPlanRequest {

    @NotBlank(message = "方案代码不能为空")
    private String planCode;

    @NotBlank(message = "方案名称不能为空")
    private String planName;

    @NotNull(message = "专业ID不能为空")
    private Long majorId;

    @NotNull(message = "入学年份不能为空")
    private Integer enrollYear;

    @NotNull(message = "学制不能为空")
    private Integer duration;

    private BigDecimal totalCredits;

    private BigDecimal requiredCredits;

    private BigDecimal electiveCredits;

    private BigDecimal practiceCredits;

    private String objectives;

    private String requirements;

    private String remark;

    private List<PlanCourseItem> courses;

    @Data
    public static class PlanCourseItem {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;

        @NotNull(message = "开课学期不能为空")
        private Integer semester;

        private Integer weeklyHours;

        private String examType;

        private Boolean isRequired;
    }
}
