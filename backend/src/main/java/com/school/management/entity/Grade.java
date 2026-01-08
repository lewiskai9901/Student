package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 年级实体类
 * 年级为全校共享资源，不再绑定特定院系
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grades")
public class Grade extends BaseEntity {

    /**
     * 年级名称(如:2024级高一)
     */
    private String gradeName;

    /**
     * 年级编码(如:GRADE_2024_10)
     */
    private String gradeCode;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 年级主任ID
     */
    private Long gradeDirectorId;

    /**
     * 年级辅导员ID
     */
    private Long gradeCounselorId;

    /**
     * 班级总数
     */
    private Integer totalClasses;

    /**
     * 学生总数
     */
    private Integer totalStudents;

    /**
     * 标准班级人数(用于加权计算)
     */
    private Integer standardClassSize;

    /**
     * 状态(1=在读,2=已毕业,3=停招)
     */
    private Integer status;

    /**
     * 预计毕业年份
     */
    private Integer graduationYear;

    /**
     * 年级描述
     */
    @TableField(exist = false)
    private String description;

    /**
     * 排序号
     */
    @TableField(exist = false)
    private Integer sortOrder;

    // ========== 关联字段（非数据库字段） ==========

    /**
     * 年级主任姓名
     */
    @TableField(exist = false)
    private String gradeDirectorName;

    /**
     * 年级主任信息
     */
    @TableField(exist = false)
    private User gradeDirector;

    /**
     * 年级辅导员姓名
     */
    @TableField(exist = false)
    private String gradeCounselorName;

    /**
     * 年级辅导员信息
     */
    @TableField(exist = false)
    private User gradeCounselor;

    /**
     * 平均班级人数
     */
    @TableField(exist = false)
    private Double avgStudentsPerClass;

    /**
     * 班级数量(非数据库字段,用于统计)
     */
    @TableField(exist = false)
    private Integer classCount;

    /**
     * 学生数量(非数据库字段,用于统计)
     */
    @TableField(exist = false)
    private Integer studentCount;

    /**
     * 平均班级规模(非数据库字段,用于统计)
     */
    @TableField(exist = false)
    private Double averageClassSize;

    /**
     * 状态描述
     */
    @TableField(exist = false)
    private String statusDesc;

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "在读";
            case 2:
                return "已毕业";
            case 3:
                return "停招";
            default:
                return "未知";
        }
    }

    /**
     * 计算平均班级人数
     */
    public Double getAvgStudentsPerClass() {
        if (totalClasses == null || totalClasses == 0) {
            return 0.0;
        }
        if (totalStudents == null) {
            return 0.0;
        }
        return Math.round(totalStudents * 100.0 / totalClasses) / 100.0;
    }
}
