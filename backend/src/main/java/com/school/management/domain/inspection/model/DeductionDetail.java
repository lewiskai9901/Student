package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DeductionDetail entity.
 * Represents a single deduction recorded against a class.
 */
@Builder
public class DeductionDetail extends Entity<Long> {

    private Long id;
    private Long classScoreId;
    private Long deductionItemId;
    private String itemName;
    private Integer count;
    private BigDecimal deductionAmount;
    private String remark;
    @Builder.Default
    private List<String> evidenceUrls = new ArrayList<>();
    @Builder.Default
    private LocalDateTime recordedAt = LocalDateTime.now();
    private Long recordedBy;

    // Adjustment fields (for appeals)
    private BigDecimal adjustedAmount;
    private String adjustmentReason;
    private Long adjustedBy;
    private LocalDateTime adjustedAt;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassScoreId() {
        return classScoreId;
    }

    public Long getDeductionItemId() {
        return deductionItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getCount() {
        return count;
    }

    public BigDecimal getDeductionAmount() {
        // Return adjusted amount if available, otherwise original
        return adjustedAmount != null ? adjustedAmount : deductionAmount;
    }

    public BigDecimal getOriginalDeductionAmount() {
        return deductionAmount;
    }

    public String getRemark() {
        return remark;
    }

    public List<String> getEvidenceUrls() {
        return Collections.unmodifiableList(evidenceUrls);
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public BigDecimal getAdjustedAmount() {
        return adjustedAmount;
    }

    public String getAdjustmentReason() {
        return adjustmentReason;
    }

    public Long getAdjustedBy() {
        return adjustedBy;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    /**
     * Checks if this deduction has been adjusted.
     */
    public boolean isAdjusted() {
        return adjustedAmount != null;
    }

    /**
     * Adjusts the deduction amount (typically from an approved appeal).
     */
    public void adjust(BigDecimal newAmount, String reason, Long adjustedBy) {
        this.adjustedAmount = newAmount;
        this.adjustmentReason = reason;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = LocalDateTime.now();
    }

    /**
     * Calculates the adjustment difference.
     */
    public BigDecimal getAdjustmentDifference() {
        if (adjustedAmount == null) {
            return BigDecimal.ZERO;
        }
        return deductionAmount.subtract(adjustedAmount);
    }
}
