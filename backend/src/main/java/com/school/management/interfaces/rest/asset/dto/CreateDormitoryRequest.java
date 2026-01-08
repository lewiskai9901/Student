package com.school.management.interfaces.rest.asset.dto;

import lombok.Data;

/**
 * 创建宿舍请求DTO
 */
@Data
public class CreateDormitoryRequest {

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 宿舍号
     */
    private String dormitoryNo;

    /**
     * 楼层号
     */
    private Integer floorNumber;

    /**
     * 房间用途类型
     */
    private Integer roomUsageType;

    /**
     * 床位容量
     */
    private Integer bedCapacity;

    /**
     * 性别类型 (1-男, 2-女, 0-混合)
     */
    private Integer genderType;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 设施信息
     */
    private String facilities;

    /**
     * 备注
     */
    private String notes;
}
