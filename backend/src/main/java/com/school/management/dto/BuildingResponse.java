package com.school.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍楼响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class BuildingResponse {

    /**
     * 宿舍楼ID
     */
    private Long id;

    /**
     * 宿舍楼名称
     */
    private String buildingName;

    /**
     * 楼栋编号
     */
    private String buildingNo;

    /**
     * 楼层数
     */
    private Integer floorCount;

    /**
     * 房间总数
     */
    private Integer roomCount;

    /**
     * 床位总数
     */
    private Integer bedCount;

    /**
     * 已入住人数
     */
    private Integer occupiedCount;

    /**
     * 宿管员ID
     */
    private Long supervisorId;

    /**
     * 宿管员姓名
     */
    private String supervisorName;

    /**
     * 性别类型: 1男 2女 3混合
     */
    private Integer genderType;

    /**
     * 性别类型名称
     */
    private String genderTypeName;

    /**
     * 位置/地址
     */
    private String location;

    /**
     * 备注
     */
    private String notes;

    /**
     * 状态: 1正常 2维修 3停用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
