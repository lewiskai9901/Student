package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建班级请求
 */
@Data
@Schema(description = "创建班级请求")
public class CreateClassRequest {

    @NotBlank(message = "班级编码不能为空")
    @Schema(description = "班级编码", required = true, example = "2024-CS-01")
    private String classCode;

    @NotBlank(message = "班级名称不能为空")
    @Schema(description = "班级名称", required = true, example = "2024级计算机科学1班")
    private String className;

    @Schema(description = "班级简称", example = "计科1班")
    private String shortName;

    @NotNull(message = "所属组织单元ID不能为空")
    @Schema(description = "所属组织单元ID", required = true)
    private Long orgUnitId;

    @NotNull(message = "入学年份不能为空")
    @Schema(description = "入学年份", required = true, example = "2024")
    private Integer enrollmentYear;

    @Schema(description = "年级级别", example = "1")
    private Integer gradeLevel;

    @Schema(description = "专业方向ID")
    private Long majorDirectionId;

    @Schema(description = "学制（年）", example = "4")
    private Integer schoolingYears;

    @Schema(description = "标准班级人数", example = "50")
    private Integer standardSize;

    @Schema(description = "班级状态 (0=筹建中, 1=在读中, 2=已毕业, 3=已撤销)", example = "1")
    private Integer status;
}
