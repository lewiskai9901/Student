package com.school.management.dto.request;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 年级创建请求DTO
 * 年级为全校共享资源，不再绑定特定院系
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-29
 */
@Data
public class GradeCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年级名称
     */
    @NotBlank(message = "年级名称不能为空")
    @Size(max = 50, message = "年级名称不能超过50字符")
    private String gradeName;

    /**
     * 年级编码
     */
    @NotBlank(message = "年级编码不能为空")
    @Size(max = 50, message = "年级编码不能超过50字符")
    private String gradeCode;

    /**
     * 入学年份
     */
    @NotNull(message = "入学年份不能为空")
    @Min(value = 2000, message = "入学年份不能早于2000年")
    @Max(value = 2100, message = "入学年份不能晚于2100年")
    private Integer enrollmentYear;

    /**
     * 年级主任ID
     */
    private Long gradeDirectorId;

    /**
     * 标准班级人数
     */
    @Min(value = 1, message = "标准班级人数必须大于0")
    @Max(value = 100, message = "标准班级人数不能超过100")
    private Integer standardClassSize;

    /**
     * 状态(1=正常,0=停用)
     */
    private Integer status;
}
