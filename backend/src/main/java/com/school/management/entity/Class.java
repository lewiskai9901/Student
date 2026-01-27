package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Year;

/**
 * 班级实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classes")
public class Class extends BaseEntity {

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级编码
     */
    private String classCode;

    /**
     * 年级
     */
    private Integer gradeLevel;

    /**
     * 所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 副班主任ID
     */
    private Long assistantTeacherId;

    /**
     * 学生数量
     */
    private Integer studentCount;

    /**
     * 教室位置
     */
    private String classroomLocation;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 毕业年份
     */
    private Integer graduationYear;

    /**
     * 年级(格式化字段,非数据库字段)
     */
    @TableField(exist = false)
    private Integer grade;

    /**
     * 班级类型: 1普通班 2重点班 3实验班
     */
    private Integer classType;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    // ========== 关联字段（非数据库字段） ==========

    /**
     * 组织单元名称(关联查询)
     */
    @TableField(exist = false)
    private String orgUnitName;

    /**
     * 组织单元信息(关联查询)
     */
    @TableField(exist = false)
    private Department orgUnit;

    /**
     * 专业信息(关联查询)
     */
    @TableField(exist = false)
    private Major major;

    /**
     * 专业名称(关联查询)
     */
    @TableField(exist = false)
    private String majorName;

    /**
     * 班主任姓名(关联查询)
     */
    @TableField(exist = false)
    private String teacherName;

    /**
     * 班主任信息(关联查询)
     */
    @TableField(exist = false)
    private User teacher;

    /**
     * 副班主任姓名(关联查询)
     */
    @TableField(exist = false)
    private String assistantTeacherName;

    /**
     * 副班主任信息(关联查询)
     */
    @TableField(exist = false)
    private User assistantTeacher;
}