package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新班级请求
 */
@Data
@Schema(description = "更新班级请求")
public class UpdateClassRequest {

    @Schema(description = "班级名称", example = "2024级计算机科学1班")
    private String className;

    @Schema(description = "班级简称", example = "计科1班")
    private String shortName;

    @Schema(description = "标准班级人数", example = "50")
    private Integer standardSize;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "所属组织单元ID（部门）")
    private Long orgUnitId;

    @Schema(description = "年级ID")
    private Long gradeId;

    @Schema(description = "专业方向ID")
    private Long majorDirectionId;

    @Schema(description = "状态：PREPARING-筹建中，ACTIVE-正常招生")
    private String status;
}
