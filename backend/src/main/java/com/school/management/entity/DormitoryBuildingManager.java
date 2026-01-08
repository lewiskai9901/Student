package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍楼管理员关联实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("dormitory_building_managers")
public class DormitoryBuildingManager {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 宿舍楼ID
     */
    private Long buildingId;

    /**
     * 管理员用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 管理员姓名 (关联查询获取)
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 管理员工号 (关联查询获取)
     */
    @TableField(exist = false)
    private String employeeNo;

    /**
     * 楼宇名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingName;
}
