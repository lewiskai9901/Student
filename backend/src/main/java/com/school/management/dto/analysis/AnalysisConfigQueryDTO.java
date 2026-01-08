package com.school.management.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统计分析配置查询DTO
 *
 * @author Claude
 * @since 2025-12-05
 */
@Data
@Schema(description = "统计分析配置查询条件")
public class AnalysisConfigQueryDTO {

    @Schema(description = "配置名称（模糊查询）")
    private String name;

    @Schema(description = "分析目标类型: TEMPLATE/CATEGORY/DEDUCTION_ITEM/SINGLE_CHECK/ORGANIZATION")
    private String targetType;

    @Schema(description = "状态: 0-禁用 1-启用")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;
}
