package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产折旧记录持久化对象
 */
@TableName("asset_depreciation")
public class AssetDepreciationPO {

    @TableId(type = IdType.ASSIGN_ID)
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
     * 已使用月数
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 备注
     */
    private String remark;

    // Getters and Setters
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
}
