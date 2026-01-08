package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 宿舍楼DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "宿舍楼信息")
public class BuildingDormitoryDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "楼宇ID", required = true)
    @NotNull(message = "楼宇ID不能为空")
    private Long buildingId;

    @Schema(description = "宿舍类型: 1-男生宿舍楼, 2-女生宿舍楼, 3-教职工男生宿舍楼, 4-教职工女生宿舍楼, 5-教职工混合宿舍楼")
    @Min(value = 1, message = "宿舍类型无效")
    @Max(value = 5, message = "宿舍类型无效")
    private Integer dormitoryType;

    @Schema(description = "总房间数")
    private Integer totalRooms;

    @Schema(description = "已入住房间数")
    private Integer occupiedRooms;

    @Schema(description = "管理规定")
    @Size(max = 5000, message = "管理规定不能超过5000字")
    private String managementRules;

    @Schema(description = "探访时间")
    @Size(max = 100, message = "探访时间不能超过100字")
    private String visitingHours;

    @Schema(description = "设施说明")
    @Size(max = 2000, message = "设施说明不能超过2000字")
    private String facilities;

    @Schema(description = "管理员ID列表")
    @Size(max = 5, message = "最多添加5个管理员")
    private List<Long> managerIds;

    // ========== 关联字段 ==========

    @Schema(description = "楼宇名称")
    private String buildingName;

    @Schema(description = "宿舍类型名称")
    private String dormitoryTypeName;

    @Schema(description = "管理员名称列表")
    private List<String> managerNames;
}
