package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级人数标准实体类
 * 用于存储固定的标准班级人数(按学期、院系、年级)
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("class_size_standards")
public class ClassSizeStandard {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 院系ID
     */
    private Long orgUnitId;

    /**
     * 年级等级
     */
    private Integer gradeLevel;

    /**
     * 标准班级人数
     */
    private Integer standardSize;

    /**
     * 计算方式(MANUAL=手动设置,AUTO=自动计算)
     */
    private String calculationMethod;

    /**
     * 计算日期
     */
    private LocalDate calculationDate;

    /**
     * 计算基准(用于计算的班级数)
     */
    private Integer calculationBaseCount;

    /**
     * 标准人数来源(FIXED=固定标准,DYNAMIC=实时平均)
     * - FIXED: 使用手动设置的固定标准人数
     * - DYNAMIC: 自动计算实际平均班级人数
     * 注意:所有检查记录都会自动保存快照,确保历史数据准确性
     */
    @TableField("standard_mode")
    private String standardMode;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 锁定人ID
     */
    private Long lockedBy;

    /**
     * 备注说明
     */
    private String remarks;

    /**
     * 模式说明
     */
    private String modeDescription;

    /**
     * 最后一次模式切换时间
     */
    private LocalDateTime lastModeChangeAt;

    /**
     * 最后一次模式切换操作人
     */
    private Long lastModeChangeBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 学期名称
     */
    @TableField(exist = false)
    private String semesterName;

    /**
     * 院系名称
     */
    @TableField(exist = false)
    private String orgUnitName;

    /**
     * 年级名称
     */
    @TableField(exist = false)
    private String gradeName;

    /**
     * 锁定人姓名
     */
    @TableField(exist = false)
    private String lockedByName;

    /**
     * 锁定状态描述
     */
    @TableField(exist = false)
    private String lockedDesc;

    /**
     * 实时计算的平均班级人数(非数据库字段)
     */
    @TableField(exist = false)
    private Integer actualAverageSize;

    /**
     * 实时计算的参考班级数量(非数据库字段)
     */
    @TableField(exist = false)
    private Integer referenceClassCount;

    /**
     * 人数差异(实时-标准)
     */
    @TableField(exist = false)
    private Integer sizeDifference;

    /**
     * 差异百分比
     */
    @TableField(exist = false)
    private String differencePercentage;

    /**
     * 获取标准来源描述
     */
    public String getLockedDesc() {
        if (standardMode == null) {
            return "未知";
        }
        return "FIXED".equals(standardMode) ? "固定标准" : "实时平均";
    }

    /**
     * 获取模式枚举
     */
    public com.school.management.enums.StandardSizeMode getStandardModeEnum() {
        return com.school.management.enums.StandardSizeMode.fromCode(standardMode);
    }

    /**
     * 判断是否为固定模式
     */
    public boolean isFixedMode() {
        return "FIXED".equals(standardMode);
    }

    /**
     * 判断是否为动态模式
     */
    public boolean isDynamicMode() {
        return "DYNAMIC".equals(standardMode);
    }

    /**
     * 兼容旧版: getLocked
     */
    @Deprecated
    public Integer getLocked() {
        return "FIXED".equals(standardMode) ? 1 : 0;
    }

    /**
     * 兼容旧版: setLocked
     */
    @Deprecated
    public void setLocked(Integer locked) {
        this.standardMode = (locked != null && locked == 1) ? "FIXED" : "DYNAMIC";
    }

    /**
     * 计算并设置差异信息
     */
    public void calculateDifference() {
        if (actualAverageSize != null && standardSize != null && standardSize > 0) {
            this.sizeDifference = actualAverageSize - standardSize;
            double percentage = ((double) sizeDifference / standardSize) * 100;
            this.differencePercentage = String.format("%.1f%%", percentage);
        }
    }

    // ========== 别名方法(兼容性) ==========

    /**
     * 设置锁定日期(别名)
     */
    public void setLockedDate(LocalDateTime lockedDate) {
        this.lockedAt = lockedDate;
    }

    /**
     * 获取锁定日期(别名)
     */
    public LocalDateTime getLockedDate() {
        return this.lockedAt;
    }
}
