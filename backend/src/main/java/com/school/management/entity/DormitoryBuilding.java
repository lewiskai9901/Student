package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍楼实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dormitory_buildings")
public class DormitoryBuilding extends BaseEntity {

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
     * 备注
     */
    private String notes;

    /**
     * 状态: 1正常 2维修 3停用
     */
    private Integer status;
}
