package com.school.management.dto.request;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 年级更新请求DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class GradeUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年级ID
     */
    @NotNull(message = "年级ID不能为空")
    private Long id;

    /**
     * 年级名称
     */
    @Size(max = 50, message = "年级名称不能超过50字符")
    private String gradeName;

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
