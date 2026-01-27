package com.school.management.dto.request;

import com.school.management.exception.BusinessException;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 宿舍楼-院系分配创建请求DTO
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
@Data
public class BuildingDepartmentAssignmentCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 宿舍楼ID
     */
    @NotNull(message = "宿舍楼ID不能为空")
    private Long buildingId;

    /**
     * 组织单元ID
     */
    @NotNull(message = "组织单元ID不能为空")
    private Long orgUnitId;

    /**
     * 起始楼层(null表示全楼)
     */
    @Min(value = 1, message = "起始楼层必须大于0")
    private Integer floorStart;

    /**
     * 结束楼层(null表示全楼)
     */
    @Min(value = 1, message = "结束楼层必须大于0")
    private Integer floorEnd;

    /**
     * 分配的房间数量(null表示不限)
     */
    @Min(value = 1, message = "分配房间数必须大于0")
    private Integer allocatedRooms;

    /**
     * 分配的床位数量(null表示不限)
     */
    @Min(value = 1, message = "分配床位数必须大于0")
    private Integer allocatedBeds;

    /**
     * 优先级(数值越大优先级越高)
     */
    @Min(value = 0, message = "优先级不能为负数")
    @Max(value = 100, message = "优先级不能超过100")
    private Integer priority;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500字符")
    private String notes;

    /**
     * 验证楼层范围
     */
    public void validateFloorRange() {
        if (floorStart != null && floorEnd != null && floorStart > floorEnd) {
            throw new BusinessException("起始楼层不能大于结束楼层");
        }
    }
}
