package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 学生住宿记录实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student_dormitory")
public class StudentDormitory extends BaseEntity {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 入住日期
     */
    private LocalDate checkInDate;

    /**
     * 退宿日期(NULL表示当前在住)
     */
    private LocalDate checkOutDate;

    /**
     * 状态: 1在住 2已退宿 3调换中
     */
    private Integer status;

    /**
     * 变动原因
     */
    private String changeReason;

    /**
     * 备注
     */
    private String remark;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 学生学号
     */
    @TableField(exist = false)
    private String studentNo;

    /**
     * 学生姓名
     */
    @TableField(exist = false)
    private String studentName;

    /**
     * 班级名称
     */
    @TableField(exist = false)
    private String className;

    /**
     * 楼宇ID
     */
    @TableField(exist = false)
    private Long buildingId;

    /**
     * 楼号
     */
    @TableField(exist = false)
    private String buildingNo;

    /**
     * 楼宇名称
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 房间号
     */
    @TableField(exist = false)
    private String dormitoryNo;

    /**
     * 楼层
     */
    @TableField(exist = false)
    private Integer floorNumber;
}
