package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍楼扩展实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("building_dormitories")
public class BuildingDormitory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 宿舍类型: 1-男生宿舍楼, 2-女生宿舍楼, 3-教职工男生宿舍楼, 4-教职工女生宿舍楼, 5-教职工混合宿舍楼
     */
    private Integer dormitoryType;

    /**
     * 总房间数（统计字段）
     */
    private Integer totalRooms;

    /**
     * 已入住房间数（统计字段）
     */
    private Integer occupiedRooms;

    /**
     * 总床位数（统计字段）
     */
    private Integer totalBeds;

    /**
     * 已入住床位数（统计字段）
     */
    private Integer occupiedBeds;

    /**
     * 管理规定
     */
    private String managementRules;

    /**
     * 探访时间
     */
    private String visitingHours;

    /**
     * 设施说明
     */
    private String facilities;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 楼宇名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 楼号 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingNo;

    /**
     * 位置 (关联查询获取)
     */
    @TableField(exist = false)
    private String location;

    /**
     * 总楼层数 (关联查询获取)
     */
    @TableField(exist = false)
    private Integer totalFloors;

    /**
     * 状态 (关联查询获取)
     */
    @TableField(exist = false)
    private Integer status;

    /**
     * 宿舍类型名称 (非数据库字段)
     */
    @TableField(exist = false)
    private String dormitoryTypeName;

    /**
     * 管理员列表 (非数据库字段)
     */
    @TableField(exist = false)
    private java.util.List<Long> managerIds;

    /**
     * 管理员名称列表 (非数据库字段)
     */
    @TableField(exist = false)
    private java.util.List<String> managerNames;

    /**
     * 获取宿舍类型名称
     */
    public String getDormitoryTypeName() {
        if (dormitoryTypeName != null) {
            return dormitoryTypeName;
        }
        if (dormitoryType == null) {
            return "";
        }
        switch (dormitoryType) {
            case 1:
                return "男生宿舍楼";
            case 2:
                return "女生宿舍楼";
            case 3:
                return "教职工男生宿舍楼";
            case 4:
                return "教职工女生宿舍楼";
            case 5:
                return "教职工混合宿舍楼";
            default:
                return "未知";
        }
    }

    /**
     * 获取允许的房间性别类型
     * @return 允许的性别类型列表
     */
    public java.util.List<Integer> getAllowedGenderTypes() {
        if (dormitoryType == null) {
            return java.util.Collections.emptyList();
        }
        switch (dormitoryType) {
            case 1: // 男生宿舍楼
            case 3: // 教职工男生宿舍楼
                return java.util.Collections.singletonList(1);
            case 2: // 女生宿舍楼
            case 4: // 教职工女生宿舍楼
                return java.util.Collections.singletonList(2);
            case 5: // 教职工混合宿舍楼
                return java.util.Arrays.asList(1, 2, 3);
            default:
                return java.util.Collections.emptyList();
        }
    }
}
