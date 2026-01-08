package com.school.management.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 班级加权计算结果DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class ClassWeightResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 实际班级人数
     */
    private Integer actualClassSize;

    /**
     * 标准班级人数
     */
    private Integer standardClassSize;

    /**
     * 权重系数
     */
    private BigDecimal weightFactor;

    /**
     * 原始扣分
     */
    private BigDecimal originalScore;

    /**
     * 加权后扣分
     */
    private BigDecimal weightedScore;

    /**
     * 分数差值(加权后 - 原始)
     */
    private BigDecimal scoreDifference;

    /**
     * 是否应用了加权
     */
    private Boolean weightApplied;

    /**
     * 加权模式(STANDARD=标准人数,PER_CAPITA=人均,SEGMENT=分段,NONE=不加权)
     */
    private String weightMode;

    /**
     * 加权模式描述
     */
    private String weightModeDesc;

    /**
     * 是否受限(达到上下限)
     */
    private Boolean limited;

    /**
     * 限制类型(MIN=下限,MAX=上限,NONE=未限制)
     */
    private String limitType;

    /**
     * 最小权重限制
     */
    private BigDecimal minWeight;

    /**
     * 最大权重限制
     */
    private BigDecimal maxWeight;

    /**
     * 快照ID(使用的人数快照)
     */
    private Long snapshotId;

    /**
     * 是否使用快照
     */
    private Boolean useSnapshot;

    /**
     * 计算说明
     */
    private String calculationNote;

    /**
     * 获取加权模式描述
     */
    public String getWeightModeDesc() {
        if (weightMode == null) {
            return "未知";
        }
        switch (weightMode) {
            case "STANDARD":
                return "标准人数加权";
            case "PER_CAPITA":
                return "人均加权";
            case "SEGMENT":
                return "分段加权";
            case "NONE":
                return "不加权";
            default:
                return "未知";
        }
    }

    /**
     * 获取计算说明
     */
    public String getCalculationNote() {
        if (!weightApplied) {
            return "未启用加权";
        }

        StringBuilder note = new StringBuilder();
        note.append(String.format("班级人数: %d, 标准人数: %d, ", actualClassSize, standardClassSize));
        note.append(String.format("权重系数: %.2f", weightFactor));

        if (limited) {
            if ("MIN".equals(limitType)) {
                note.append(String.format(" (受下限%.2f限制)", minWeight));
            } else if ("MAX".equals(limitType)) {
                note.append(String.format(" (受上限%.2f限制)", maxWeight));
            }
        }

        note.append(String.format(", 原始分: %.2f, 加权分: %.2f", originalScore, weightedScore));

        if (scoreDifference.compareTo(BigDecimal.ZERO) > 0) {
            note.append(String.format(" (多扣%.2f分)", scoreDifference));
        } else if (scoreDifference.compareTo(BigDecimal.ZERO) < 0) {
            note.append(String.format(" (少扣%.2f分)", scoreDifference.abs()));
        }

        return note.toString();
    }

    /**
     * 是否对班级有利(加权后扣分更少)
     */
    public Boolean isFavorable() {
        if (scoreDifference == null) {
            return null;
        }
        return scoreDifference.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 是否对班级不利(加权后扣分更多)
     */
    public Boolean isUnfavorable() {
        if (scoreDifference == null) {
            return null;
        }
        return scoreDifference.compareTo(BigDecimal.ZERO) > 0;
    }
}
