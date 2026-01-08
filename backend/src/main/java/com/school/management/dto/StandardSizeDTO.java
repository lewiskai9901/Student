package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标准班级人数DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class StandardSizeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 年级级别(10=高一,11=高二,12=高三,null=全部年级)
     */
    private Integer gradeLevel;

    /**
     * 年级级别描述
     */
    private String gradeLevelDesc;

    /**
     * 标准人数
     */
    private Integer standardSize;

    /**
     * 是否锁定(1=固定不变,0=实时计算)
     */
    private Boolean locked;

    /**
     * 锁定状态描述
     */
    private String lockedDesc;

    /**
     * 计算方法(MANUAL=手动设置,AUTO=自动计算,SYSTEM=系统默认)
     */
    private String calculationMethod;

    /**
     * 计算方法描述
     */
    private String calculationMethodDesc;

    /**
     * 锁定时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedAt;

    /**
     * 锁定人ID
     */
    private Long lockedBy;

    /**
     * 锁定人姓名
     */
    private String lockedByName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 获取年级级别描述
     */
    public String getGradeLevelDesc() {
        if (gradeLevel == null) {
            return "全部年级";
        }
        switch (gradeLevel) {
            case 10:
                return "高一";
            case 11:
                return "高二";
            case 12:
                return "高三";
            default:
                return "未知";
        }
    }

    /**
     * 获取锁定状态描述
     */
    public String getLockedDesc() {
        if (locked == null) {
            return "未知";
        }
        return locked ? "已锁定(固定)" : "未锁定(动态)";
    }

    /**
     * 获取计算方法描述
     */
    public String getCalculationMethodDesc() {
        if (calculationMethod == null) {
            return "未知";
        }
        switch (calculationMethod) {
            case "MANUAL":
                return "手动设置";
            case "AUTO":
                return "自动计算";
            case "SYSTEM":
                return "系统默认";
            default:
                return "未知";
        }
    }
}
