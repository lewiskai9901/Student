package com.school.management.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 宿舍响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DormitoryResponse {

    /**
     * 宿舍ID
     */
    private Long id;

    /**
     * 宿舍楼ID
     */
    private Long buildingId;

    /**
     * 所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 房间号
     */
    private String dormitoryNo;

    /**
     * 房间号(前端使用,兼容字段)
     */
    private String roomNo;

    /**
     * 楼宇名称
     */
    private String buildingName;

    /**
     * 楼号
     */
    private String buildingNo;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    /**
     * 已分配的班级ID列表（逗号分隔，支持混合宿舍）
     */
    private String assignedClassIds;

    /**
     * 已分配的班级名称列表（逗号分隔，支持混合宿舍）
     */
    private String assignedClassNames;

    /**
     * 楼层
     */
    private Integer floorNumber;

    /**
     * 楼层(前端使用)
     */
    private Integer floor;

    /**
     * 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
     */
    private Integer roomUsageType;

    /**
     * 房间用途类型名称
     */
    private String roomUsageTypeName;

    /**
     * 床位容量规格: 4/6/8等数字,配电室等为0
     */
    private Integer bedCapacity;

    /**
     * 实际床位数
     */
    private Integer bedCount;

    /**
     * 已占用床位数
     */
    private Integer occupiedBeds;

    /**
     * 房间类型: 1四人间 2六人间 3八人间 (已废弃)
     */
    @Deprecated
    private Integer roomType;

    /**
     * 房间类型名称 (已废弃)
     */
    @Deprecated
    private String roomTypeName;

    /**
     * 性别类型: 1男 2女 3混合
     */
    private Integer genderType;

    /**
     * 性别类型名称
     */
    private String genderTypeName;

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
     * 宿舍类型: 1学生宿舍 2职工宿舍 3其他宿舍
     */
    private Integer dormitoryType;

    /**
     * 宿舍类型名称
     */
    private String dormitoryTypeName;

    /**
     * 最大容纳人数
     */
    private Integer maxOccupancy;

    /**
     * 最大床位数
     */
    private Integer maxCapacity;

    /**
     * 当前入住人数
     */
    private Integer currentOccupancy;

    /**
     * 当前入住人数
     */
    private Integer currentCount;

    /**
     * 宿舍内的学生列表
     */
    private List<StudentSimpleInfo> students;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 学生简单信息(用于宿舍总览)
     */
    @Data
    public static class StudentSimpleInfo {
        private Long id;
        private String studentNo;
        private String realName;
        private String bedNumber;
        private String className;
    }
}