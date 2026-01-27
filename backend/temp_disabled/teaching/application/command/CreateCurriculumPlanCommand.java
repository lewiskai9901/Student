package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建培养方案命令
 */
@Data
@Builder
public class CreateCurriculumPlanCommand {

    @NotBlank(message = "方案代码不能为空")
    private String planCode;

    @NotBlank(message = "方案名称不能为空")
    private String planName;

    @NotNull(message = "专业ID不能为空")
    private Long majorId;

    /**
     * 适用入学年份
     */
    @NotNull(message = "入学年份不能为空")
    private Integer enrollYear;

    /**
     * 学制(年)
     */
    @NotNull(message = "学制不能为空")
    private Integer duration;

    /**
     * 总学分要求
     */
    private BigDecimal totalCredits;

    /**
     * 必修学分
     */
    private BigDecimal requiredCredits;

    /**
     * 选修学分
     */
    private BigDecimal electiveCredits;

    /**
     * 实践学分
     */
    private BigDecimal practiceCredits;

    /**
     * 培养目标
     */
    private String objectives;

    /**
     * 毕业要求
     */
    private String requirements;

    /**
     * 备注
     */
    private String remark;

    /**
     * 课程列表
     */
    private List<PlanCourseItem> courses;

    /**
     * 操作人
     */
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
