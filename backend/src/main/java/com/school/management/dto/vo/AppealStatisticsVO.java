package com.school.management.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 申诉统计VO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 统计范围(OVERALL=总体,GRADE=年级,CLASS=班级,USER=个人)
     */
    private String scope;

    /**
     * 范围ID
     */
    private Long scopeId;

    /**
     * 范围名称
     */
    private String scopeName;

    /**
     * 统计周期(DAY=日,WEEK=周,MONTH=月,SEMESTER=学期,YEAR=年)
     */
    private String period;

    /**
     * 总申诉数
     */
    private Integer totalAppeals;

    /**
     * 待审核数
     */
    private Integer pendingAppeals;

    /**
     * 审核通过数
     */
    private Integer approvedAppeals;

    /**
     * 审核驳回数
     */
    private Integer rejectedAppeals;

    /**
     * 已撤销数
     */
    private Integer withdrawnAppeals;

    /**
     * 已过期数
     */
    private Integer expiredAppeals;

    /**
     * 公示中数
     */
    private Integer publicityAppeals;

    /**
     * 已生效数
     */
    private Integer effectiveAppeals;

    /**
     * 总数量(别名)
     */
    public Integer getTotalCount() {
        return totalAppeals;
    }

    /**
     * 设置总数量(别名)
     */
    public void setTotalCount(Integer totalCount) {
        this.totalAppeals = totalCount;
    }

    /**
     * 待审核数量(别名)
     */
    public Integer getPendingCount() {
        return pendingAppeals;
    }

    /**
     * 设置待审核数量(别名)
     */
    public void setPendingCount(Integer pendingCount) {
        this.pendingAppeals = pendingCount;
    }

    /**
     * 通过数量(别名)
     */
    public Integer getApprovedCount() {
        return approvedAppeals;
    }

    /**
     * 设置通过数量(别名)
     */
    public void setApprovedCount(Integer approvedCount) {
        this.approvedAppeals = approvedCount;
    }

    /**
     * 驳回数量(别名)
     */
    public Integer getRejectedCount() {
        return rejectedAppeals;
    }

    /**
     * 设置驳回数量(别名)
     */
    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedAppeals = rejectedCount;
    }

    /**
     * 已撤销数量(别名)
     */
    public Integer getWithdrawnCount() {
        return withdrawnAppeals;
    }

    /**
     * 设置已撤销数量(别名)
     */
    public void setWithdrawnCount(Integer withdrawnCount) {
        this.withdrawnAppeals = withdrawnCount;
    }

    /**
     * 已过期数量(别名)
     */
    public Integer getExpiredCount() {
        return expiredAppeals;
    }

    /**
     * 设置已过期数量(别名)
     */
    public void setExpiredCount(Integer expiredCount) {
        this.expiredAppeals = expiredCount;
    }

    /**
     * 公示中数量(别名)
     */
    public Integer getPublicityCount() {
        return publicityAppeals;
    }

    /**
     * 设置公示中数量(别名)
     */
    public void setPublicityCount(Integer publicityCount) {
        this.publicityAppeals = publicityCount;
    }

    /**
     * 已生效数量(别名)
     */
    public Integer getEffectiveCount() {
        return effectiveAppeals;
    }

    /**
     * 设置已生效数量(别名)
     */
    public void setEffectiveCount(Integer effectiveCount) {
        this.effectiveAppeals = effectiveCount;
    }

    /**
     * 成功率(别名)
     */
    public Double getSuccessRate() {
        if (approvalRate == null) {
            return null;
        }
        return approvalRate.doubleValue();
    }

    /**
     * 设置成功率(别名)
     */
    public void setSuccessRate(Double successRate) {
        if (successRate == null) {
            this.approvalRate = null;
        } else {
            this.approvalRate = BigDecimal.valueOf(successRate);
        }
    }

    /**
     * 审核通过率
     */
    private BigDecimal approvalRate;

    /**
     * 平均审批耗时(小时)
     */
    private Double averageApprovalHours;

    /**
     * 超时审批数
     */
    private Integer timeoutApprovals;

    /**
     * 超时率
     */
    private BigDecimal timeoutRate;

    /**
     * 总分数调整
     */
    private BigDecimal totalScoreAdjustment;

    /**
     * 平均分数调整
     */
    private BigDecimal averageScoreAdjustment;

    /**
     * 最大分数调整
     */
    private BigDecimal maxScoreAdjustment;

    /**
     * 最小分数调整
     */
    private BigDecimal minScoreAdjustment;

    /**
     * 按申诉类型统计
     */
    private java.util.Map<String, Integer> appealTypeStats;

    /**
     * 按状态统计
     */
    private java.util.Map<String, Integer> statusStats;

    /**
     * 按审批人统计
     */
    private java.util.List<ApproverStatDTO> approverStats;

    /**
     * 本周趋势(7天数据)
     */
    private java.util.List<DailyTrendDTO> weeklyTrend;

    /**
     * 本月趋势(30天数据)
     */
    private java.util.List<DailyTrendDTO> monthlyTrend;

    /**
     * 获取审核通过率百分比
     */
    public String getApprovalRatePercent() {
        if (approvalRate == null) {
            return "0%";
        }
        return approvalRate.multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * 获取超时率百分比
     */
    public String getTimeoutRatePercent() {
        if (timeoutRate == null) {
            return "0%";
        }
        return timeoutRate.multiply(BigDecimal.valueOf(100))
                .setScale(1, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * 审批人统计DTO
     */
    @Data
    public static class ApproverStatDTO implements Serializable {
        private Long approverId;
        private String approverName;
        private Integer totalCount;
        private Integer approvedCount;
        private Integer rejectedCount;
        private Double averageHours;
        private BigDecimal approvalRate;
    }

    /**
     * 每日趋势DTO
     */
    @Data
    public static class DailyTrendDTO implements Serializable {
        private String date;
        private Integer totalAppeals;
        private Integer approvedAppeals;
        private Integer rejectedAppeals;
        private BigDecimal approvalRate;
    }
}
