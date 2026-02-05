package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Assign teacher request
 */
@Schema(description = "Assign teacher request")
public class AssignTeacherRequest {

    @NotNull(message = "Teacher ID required")
    @Schema(description = "Teacher ID", required = true)
    private Long teacherId;

    @NotBlank(message = "Teacher name required")
    @Schema(description = "Teacher name", required = true, example = "Teacher Zhang")
    private String teacherName;

    // Getters
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }

    // Setters
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}
