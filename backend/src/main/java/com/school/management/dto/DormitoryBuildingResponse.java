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
public class DormitoryBuildingResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 楼号
     */
    private String buildingNo;

    /**
     * 楼名
     */
    private String buildingName;

    /**
     * 楼类型: 1男生宿舍 2女生宿舍 3混合宿舍 4教职工宿舍
     */
    private Integer buildingType;

    /**
     * 楼层数
     */
    private Integer floorCount;

    /**
     * 每层房间数
     */
    private Integer roomsPerFloor;

    /**
     * 房间号格式(如: {floor}{room})
     */
    private String roomNumberFormat;

    /**
     * 位置
     */
    private String location;

    /**
     * 宿管员ID
     */
    private Long supervisorId;

    /**
     * 宿管员姓名
     */
    private String supervisorName;

    /**
     * 备注
     */
    private String notes;

    /**
     * 状态: 1正常 2维修 3停用
     */
    private Integer status;

    /**
     * 总房间数
     */
    private Integer totalRooms;

    /**
     * 总床位数
     */
    private Integer totalBeds;

    /**
     * 已占用床位数
     */
    private Integer occupiedBeds;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
