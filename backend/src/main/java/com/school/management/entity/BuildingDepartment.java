package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼宇部门关联实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("building_departments")
public class BuildingDepartment {

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
     * 部门ID
     */
    private Long departmentId;

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
     * 部门名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String departmentName;

    /**
     * 楼宇名称 (关联查询获取)
     */
    @TableField(exist = false)
    private String buildingName;
}
