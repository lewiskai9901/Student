package com.school.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量生成宿舍请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DormitoryBatchCreateRequest {

    /**
     * 楼宇ID
     */
    @NotNull(message = "楼宇ID不能为空")
    private Long buildingId;

    /**
     * 起始楼层
     */
    @NotNull(message = "起始楼层不能为空")
    @Min(value = 1, message = "起始楼层必须大于0")
    private Integer startFloor;

    /**
     * 结束楼层
     */
    @NotNull(message = "结束楼层不能为空")
    @Min(value = 1, message = "结束楼层必须大于0")
    private Integer endFloor;

    /**
     * 每层房间数
     */
    @NotNull(message = "每层房间数不能为空")
    @Min(value = 1, message = "每层房间数必须大于0")
    @Max(value = 100, message = "每层房间数不能超过100")
    private Integer roomsPerFloor;

    /**
     * 房间编号格式: 1=楼层+房间号(如101,102) 2=楼层+0+房间号(如1001,1002)
     */
    @NotNull(message = "房间编号格式不能为空")
    private Integer numberFormat;

    /**
     * 房间类型: 1四人间 2六人间 3八人间
     */
    @NotNull(message = "房间类型不能为空")
    private Integer roomType;

    /**
     * 性别类型: 1男生宿舍 2女生宿舍 3混合宿舍
     */
    @NotNull(message = "性别类型不能为空")
    private Integer genderType;

    /**
     * 宿管员ID
     */
    private Long supervisorId;

    /**
     * 设施配置
     */
    private String facilities;

    /**
     * 状态: 1正常 0停用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
