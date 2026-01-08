package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分配教师请求
 */
@Data
@Schema(description = "分配教师请求")
public class AssignTeacherRequest {

    @NotNull(message = "教师ID不能为空")
    @Schema(description = "教师ID", required = true)
    private Long teacherId;

    @NotBlank(message = "教师姓名不能为空")
    @Schema(description = "教师姓名", required = true, example = "张老师")
    private String teacherName;
}
