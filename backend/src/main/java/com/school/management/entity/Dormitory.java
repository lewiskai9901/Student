package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dormitories")
public class Dormitory extends BaseEntity {

    /**
     * 楼宇ID (外键关联buildings表)
     */
    private Long buildingId;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 房间号 (唯一)
     */
    private String dormitoryNo;

    /**
     * 楼层号
     */
    private Integer floorNumber;

    /**
     * 房间用途类型: 1学生宿舍 2教职工宿舍 3配电室 4卫生间 5杂物间 6其他
     */
    private Integer roomUsageType;

    /**
     * 床位容量规格: 4/6/8等数字,配电室等为0
     */
    private Integer bedCapacity;

    /**
     * 房间类型: 1四人间 2六人间 3八人间 (已废弃,请使用roomUsageType和bedCapacity)
     */
    @Deprecated
    private Integer roomType;

    /**
     * 实际床位数(通常等于bedCapacity,配电室等为0)
     */
    private Integer bedCount;

    /**
     * 已占用床位数
     */
    private Integer occupiedBeds;

    /**
     * 性别类型(从宿舍楼自动继承): 1男 2女 3混合
     */
    private Integer genderType;

    /**
     * 设施配置(JSON或文本)
     */
    private String facilities;

    /**
     * 备注
     */
    private String notes;

    /**
     * 状态: 1正常 0停用
     */
    private Integer status;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 楼宇名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 楼宇编号 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingNo;

    /**
     * 部门名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String departmentName;
}
