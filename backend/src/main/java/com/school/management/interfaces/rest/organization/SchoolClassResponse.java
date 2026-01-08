package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班级响应
 */
@Data
@Schema(description = "班级响应")
public class SchoolClassResponse {

    @Schema(description = "班级ID")
    private Long id;

    @Schema(description = "班级编码")
    private String classCode;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "班级简称")
    private String shortName;

    @Schema(description = "所属组织单元ID")
    private Long orgUnitId;

    @Schema(description = "所属组织单元名称")
    private String orgUnitName;

    @Schema(description = "入学年份")
    private Integer enrollmentYear;

    @Schema(description = "年级级别")
    private Integer gradeLevel;

    @Schema(description = "专业方向ID")
    private Long majorDirectionId;

    @Schema(description = "专业方向名称")
    private String majorDirectionName;

    @Schema(description = "学制（年）")
    private Integer schoolingYears;

    @Schema(description = "标准班级人数")
    private Integer standardSize;

    @Schema(description = "当前实际人数")
    private Integer currentSize;

    @Schema(description = "班级状态")
    private String status;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "预计毕业年份")
    private Integer expectedGraduationYear;

    @Schema(description = "空余名额")
    private Integer availableSlots;

    @Schema(description = "班主任ID")
    private Long headTeacherId;

    @Schema(description = "班主任姓名")
    private String headTeacherName;

    @Schema(description = "教师任职列表")
    private List<TeacherAssignmentResponse> teacherAssignments;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
