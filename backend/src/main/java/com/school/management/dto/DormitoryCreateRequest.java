package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 宿舍创建请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DormitoryCreateRequest {

    /**
     * 房间号
     */
    @NotBlank(message = "房间号不能为空")
    private String dormitoryNo;

    /**
     * 楼宇ID
     */
    @NotNull(message = "楼宇ID不能为空")
    private Long buildingId;

    /**
     * 楼层
     */
    @NotNull(message = "楼层不能为空")
    private Integer floorNumber;

    /**
     * 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
     */
    @NotNull(message = "房间用途类型不能为空")
    private Integer roomUsageType;

    /**
     * 床位容量规格: 4/6/8等数字,配电室等为0
     */
    @NotNull(message = "床位容量不能为空")
    private Integer bedCapacity;

    /**
     * 房间类型: 1四人间 2六人间 3八人间 (已废弃)
     */
    @Deprecated
    private Integer roomType;

    /**
     * 实际床位数(通常等于bedCapacity)
     */
    private Integer bedCount;

    /**
     * 性别类型: 1男 2女 3混合 (从宿舍楼自动继承,前端不需要传)
     */
    private Integer genderType;

    /**
     * 设施说明
     */
    private String facilities;

    /**
     * 备注
     */
    private String notes;

    /**
     * 状态: 1正常 2维修 3停用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}