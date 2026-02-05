package com.school.management.domain.asset.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产折旧记录实体
 * 记录每次折旧计提的详细信息
 */
@Data
@NoArgsConstructor
public class AssetDepreciation implements Entity<Long> {

    private Long id;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 资产编码
     */
    private String assetCode;

    /**
     * 折旧期间（年月，如：202601）
     */
    private String depreciationPeriod;

    /**
     * 期初原值
     */
    private BigDecimal beginningValue;

    /**
     * 期初累计折旧
     */
    private BigDecimal beginningAccumulatedDepreciation;

    /**
     * 期初净值
     */
    private BigDecimal beginningNetValue;

    /**
     * 本期折旧额
     */
    private BigDecimal depreciationAmount;

    /**
     * 期末累计折旧
     */
    private BigDecimal endingAccumulatedDepreciation;

    /**
     * 期末净值
     */
    private BigDecimal endingNetValue;

    /**
     * 已使用月数（截止本期）
     */
    private Integer usedMonths;

    /**
     * 剩余使用月数
     */
    private Integer remainingMonths;

    /**
     * 折旧方法
     */
    private Integer depreciationMethod;

    /**
     * 计提日期
     */
    private LocalDate depreciationDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 备注
     */
    private String remark;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getDepreciationPeriod() {
        return depreciationPeriod;
    }

    public void setDepreciationPeriod(String depreciationPeriod) {
        this.depreciationPeriod = depreciationPeriod;
    }

    public BigDecimal getBeginningValue() {
        return beginningValue;
    }

    public void setBeginningValue(BigDecimal beginningValue) {
        this.beginningValue = beginningValue;
    }

    public BigDecimal getBeginningAccumulatedDepreciation() {
        return beginningAccumulatedDepreciation;
    }

    public void setBeginningAccumulatedDepreciation(BigDecimal beginningAccumulatedDepreciation) {
        this.beginningAccumulatedDepreciation = beginningAccumulatedDepreciation;
    }

    public BigDecimal getBeginningNetValue() {
        return beginningNetValue;
    }

    public void setBeginningNetValue(BigDecimal beginningNetValue) {
        this.beginningNetValue = beginningNetValue;
    }

    public BigDecimal getDepreciationAmount() {
        return depreciationAmount;
    }

    public void setDepreciationAmount(BigDecimal depreciationAmount) {
        this.depreciationAmount = depreciationAmount;
    }

    public BigDecimal getEndingAccumulatedDepreciation() {
        return endingAccumulatedDepreciation;
    }

    public void setEndingAccumulatedDepreciation(BigDecimal endingAccumulatedDepreciation) {
        this.endingAccumulatedDepreciation = endingAccumulatedDepreciation;
    }

    public BigDecimal getEndingNetValue() {
        return endingNetValue;
    }

    public void setEndingNetValue(BigDecimal endingNetValue) {
        this.endingNetValue = endingNetValue;
    }

    public Integer getUsedMonths() {
        return usedMonths;
    }

    public void setUsedMonths(Integer usedMonths) {
        this.usedMonths = usedMonths;
    }

    public Integer getRemainingMonths() {
        return remainingMonths;
    }

    public void setRemainingMonths(Integer remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    public Integer getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(Integer depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public LocalDate getDepreciationDate() {
        return depreciationDate;
    }

    public void setDepreciationDate(LocalDate depreciationDate) {
        this.depreciationDate = depreciationDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 创建折旧记录
     */
    public static AssetDepreciation create(
            Long assetId,
            String assetCode,
            String period,
            BigDecimal beginningValue,
            BigDecimal beginningAccumulatedDepreciation,
            BigDecimal depreciationAmount,
            int usedMonths,
            int remainingMonths,
            int depreciationMethod
    ) {
        AssetDepreciation record = new AssetDepreciation();
        record.assetId = assetId;
        record.assetCode = assetCode;
        record.depreciationPeriod = period;
        record.beginningValue = beginningValue;
        record.beginningAccumulatedDepreciation = beginningAccumulatedDepreciation;
        record.beginningNetValue = beginningValue.subtract(beginningAccumulatedDepreciation);
        record.depreciationAmount = depreciationAmount;
        record.endingAccumulatedDepreciation = beginningAccumulatedDepreciation.add(depreciationAmount);
        record.endingNetValue = beginningValue.subtract(record.endingAccumulatedDepreciation);
        record.usedMonths = usedMonths;
        record.remainingMonths = remainingMonths;
        record.depreciationMethod = depreciationMethod;
        record.depreciationDate = LocalDate.now();
        record.createdAt = LocalDateTime.now();
        return record;
    }
}
