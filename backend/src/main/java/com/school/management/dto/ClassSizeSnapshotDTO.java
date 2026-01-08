package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级人数快照DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class ClassSizeSnapshotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 快照ID
     */
    private Long id;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查编号
     */
    private String checkCode;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 快照日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate snapshotDate;

    /**
     * 班级人数
     */
    private Integer studentCount;

    /**
     * 标准人数
     */
    private Integer standardSize;

    /**
     * 人数差异
     */
    private Integer sizeDifference;

    /**
     * 人数差异率
     */
    private Double sizeDifferenceRate;

    /**
     * 快照来源(CHECK=检查发布,SCHEDULED=定时任务,MANUAL=手动创建)
     */
    private String snapshotSource;

    /**
     * 快照来源描述
     */
    private String snapshotSourceDesc;

    /**
     * 是否已使用(被检查记录引用)
     */
    private Boolean isUsed;

    /**
     * 使用次数
     */
    private Integer usageCount;

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
     * 获取人数差异(实际 - 标准)
     */
    public Integer getSizeDifference() {
        if (studentCount == null || standardSize == null) {
            return null;
        }
        return studentCount - standardSize;
    }

    /**
     * 获取人数差异率
     */
    public Double getSizeDifferenceRate() {
        if (studentCount == null || standardSize == null || standardSize == 0) {
            return null;
        }
        return (studentCount - standardSize) * 100.0 / standardSize;
    }

    /**
     * 获取快照来源描述
     */
    public String getSnapshotSourceDesc() {
        if (snapshotSource == null) {
            return "未知";
        }
        switch (snapshotSource) {
            case "CHECK":
                return "检查发布";
            case "SCHEDULED":
                return "定时任务";
            case "MANUAL":
                return "手动创建";
            default:
                return "未知";
        }
    }

    /**
     * 获取人数差异描述
     */
    public String getSizeDifferenceDesc() {
        Integer diff = getSizeDifference();
        if (diff == null) {
            return "无数据";
        }
        if (diff > 0) {
            return String.format("超过标准%d人", diff);
        } else if (diff < 0) {
            return String.format("少于标准%d人", Math.abs(diff));
        } else {
            return "符合标准";
        }
    }

    /**
     * 人数是否正常(差异在±10%以内)
     */
    public Boolean isSizeNormal() {
        Double rate = getSizeDifferenceRate();
        if (rate == null) {
            return null;
        }
        return Math.abs(rate) <= 10;
    }
}
