package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教室实体类
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classrooms")
public class Classroom extends BaseEntity {

    /**
     * 教学楼ID
     */
    @TableField("building_id")
    private Long buildingId;

    /**
     * 教室编号 (对应数据库的classroom_no字段)
     */
    @TableField("classroom_no")
    private String classroomNo;

    /**
     * 教室名称 (非数据库字段，用于兼容性)
     */
    @TableField(exist = false)
    private String classroomName;

    /**
     * 教室编号 (非数据库字段，用于兼容性)
     */
    @TableField(exist = false)
    private String classroomCode;

    /**
     * 楼层
     */
    @TableField("floor")
    private Integer floor;

    /**
     * 房间号 (非数据库字段)
     */
    @TableField(exist = false)
    private String roomNumber;

    /**
     * 容纳人数
     */
    @TableField("capacity")
    private Integer capacity;

    /**
     * 关联班级ID
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 班级名称
     */
    @TableField("class_name")
    private String className;

    /**
     * 班主任ID (非数据库字段)
     */
    @TableField(exist = false)
    private Long headTeacherId;

    /**
     * 班主任姓名(非数据库字段,通过JOIN查询获取)
     */
    @TableField(exist = false)
    private String headTeacherName;

    /**
     * 班级人数 (非数据库字段)
     */
    @TableField(exist = false)
    private Integer studentCount;

    /**
     * 教室类型
     */
    @TableField("classroom_type")
    private String classroomType;

    /**
     * 设施设备 (非数据库字段)
     */
    @TableField(exist = false)
    private String facilities;

    /**
     * 状态(0-停用,1-启用)
     */
    @TableField("is_active")
    private Integer status;

    /**
     * 教学楼名称(非数据库字段,通过JOIN查询获取)
     */
    @TableField(exist = false)
    private String buildingName;

    /**
     * 教学楼编号(非数据库字段,通过JOIN查询获取)
     */
    @TableField(exist = false)
    private String buildingNo;
}
