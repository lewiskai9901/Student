package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 楼宇DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "楼宇信息")
public class BuildingDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "楼号", required = true)
    @NotBlank(message = "楼号不能为空")
    @Size(max = 50, message = "楼号不能超过50字符")
    private String buildingNo;

    @Schema(description = "楼宇名称", required = true)
    @NotBlank(message = "楼宇名称不能为空")
    @Size(max = 100, message = "楼宇名称不能超过100字符")
    private String buildingName;

    @Schema(description = "楼宇类型: 1-教学楼, 2-宿舍楼, 3-办公楼", required = true)
    @NotNull(message = "楼宇类型不能为空")
    @Min(value = 1, message = "楼宇类型无效")
    @Max(value = 3, message = "楼宇类型无效")
    private Integer buildingType;

    @Schema(description = "总楼层数", required = true)
    @NotNull(message = "总楼层数不能为空")
    @Min(value = 1, message = "楼层数至少为1")
    @Max(value = 100, message = "楼层数不能超过100")
    private Integer totalFloors;

    @Schema(description = "地理位置")
    @Size(max = 200, message = "地理位置不能超过200字符")
    private String location;

    @Schema(description = "建造年份")
    @Min(value = 1900, message = "建造年份不能早于1900年")
    @Max(value = 2100, message = "建造年份无效")
    private Integer constructionYear;

    @Schema(description = "楼宇描述")
    @Size(max = 500, message = "楼宇描述不能超过500字符")
    private String description;

    @Schema(description = "状态: 0-停用, 1-启用")
    private Integer status;

    @Schema(description = "关联组织单元ID列表")
    private List<Long> orgUnitIds;

    // ========== 关联字段 ==========

    @Schema(description = "楼宇类型名称")
    private String buildingTypeName;

    @Schema(description = "关联组织单元名称列表")
    private List<String> orgUnitNames;

    @Schema(description = "房间数量")
    private Integer roomCount;
}
