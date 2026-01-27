package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建成绩批次命令
 */
@Data
@Builder
public class CreateGradeBatchCommand {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /**
     * 成绩类型: 1-百分制 2-五级制 3-二级制
     */
    @NotNull(message = "成绩类型不能为空")
    private Integer gradeType;

    /**
     * 成绩组成(JSON格式)
     */
    private List<GradeComposition> compositions;

    private LocalDateTime deadline;

    private Long operatorId;

    @Data
    @Builder
    public static class GradeComposition {
        private String name;
        private Integer weight;
    }
}
