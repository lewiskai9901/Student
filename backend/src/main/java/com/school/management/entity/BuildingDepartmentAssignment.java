package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍楼-院系分配实体
 * 支持一栋宿舍楼分配给多个院系，支持按楼层范围分配
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("building_department_assignments")
public class BuildingDepartmentAssignment extends BaseEntity {

    /**
     * 宿舍楼ID
     */
    private Long buildingId;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 起始楼层(NULL表示全楼)
     */
    private Integer floorStart;

    /**
     * 结束楼层(NULL表示全楼)
     */
    private Integer floorEnd;

    /**
     * 分配的房间数量(NULL表示不限)
     */
    private Integer allocatedRooms;

    /**
     * 分配的床位数量(NULL表示不限)
     */
    private Integer allocatedBeds;

    /**
     * 优先级(数值越大优先级越高)
     */
    private Integer priority;

    /**
     * 状态(1=有效,0=无效)
     */
    private Integer status;

    /**
     * 备注
     */
    private String notes;

    // ========== 关联字段（非数据库字段）==========

    /**
     * 宿舍楼名称
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 组织单元名称
     */
    @TableField(exist = false)
    private String orgUnitName;

    /**
     * 楼层范围描述
     */
    @TableField(exist = false)
    private String floorRangeDesc;

    /**
     * 获取楼层范围描述
     */
    public String getFloorRangeDesc() {
        if (floorStart == null && floorEnd == null) {
            return "全楼";
        } else if (floorStart != null && floorEnd != null) {
            if (floorStart.equals(floorEnd)) {
                return floorStart + "楼";
            }
            return floorStart + "-" + floorEnd + "楼";
        } else if (floorStart != null) {
            return floorStart + "楼及以上";
        } else {
            return floorEnd + "楼及以下";
        }
    }
}
