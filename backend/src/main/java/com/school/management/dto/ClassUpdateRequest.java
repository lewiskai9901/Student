package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 班级更新请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class ClassUpdateRequest {

    /**
     * 班级ID
     */
    @NotNull(message = "班级ID不能为空")
    private Long id;

    /**
     * 班级名称
     */
    @NotBlank(message = "班级名称不能为空")
    private String className;

    /**
     * 班级编码
     */
    @NotBlank(message = "班级编码不能为空")
    private String classCode;

    /**
     * 年级
     */
    @NotNull(message = "年级不能为空")
    private Integer gradeLevel;

    /**
     * 所属组织单元ID
     */
    @NotNull(message = "所属组织单元不能为空")
    private Long orgUnitId;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 副班主任ID
     */
    private Long assistantTeacherId;

    /**
     * 教室位置
     */
    private String classroomLocation;

    /**
     * 入学年份
     */
    @NotNull(message = "入学年份不能为空")
    private Integer enrollmentYear;

    /**
     * 毕业年份
     */
    @NotNull(message = "毕业年份不能为空")
    private Integer graduationYear;

    /**
     * 班级类型: 1普通班 2重点班 3实验班
     */
    @NotNull(message = "班级类型不能为空")
    private Integer classType;

    /**
     * 状态: 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}