package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 教师任职响应
 */
@Data
@Schema(description = "教师任职响应")
public class TeacherAssignmentResponse {

    @Schema(description = "教师ID")
    private Long teacherId;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "任职角色: HEAD_TEACHER, DEPUTY_HEAD_TEACHER, SUBJECT_TEACHER, COUNSELOR")
    private String role;

    @Schema(description = "任职开始日期")
    private LocalDate startDate;

    @Schema(description = "任职结束日期")
    private LocalDate endDate;

    @Schema(description = "是否当前任职")
    private boolean current;
}
