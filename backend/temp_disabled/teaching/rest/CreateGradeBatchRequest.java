package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGradeBatchRequest {
    @NotNull(message = "学期ID不能为空")
    private Long semesterId;
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotNull(message = "班级ID不能为空")
    private Long classId;
    @NotBlank(message = "批次名称不能为空")
    private String batchName;
    @NotNull(message = "成绩类型不能为空")
    private Integer gradeType;
}
