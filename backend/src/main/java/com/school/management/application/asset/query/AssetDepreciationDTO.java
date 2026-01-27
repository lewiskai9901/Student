package com.school.management.application.asset.query;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产折旧DTO
 */
@Data
public class AssetDepreciationDTO {
    private Long id;
    private Long assetId;
    private String assetCode;
    private String depreciationPeriod;
    private BigDecimal beginningValue;
    private BigDecimal beginningAccumulatedDepreciation;
    private BigDecimal beginningNetValue;
    private BigDecimal depreciationAmount;
    private BigDecimal endingAccumulatedDepreciation;
    private BigDecimal endingNetValue;
    private Integer usedMonths;
    private Integer remainingMonths;
    private Integer depreciationMethod;
    private String depreciationMethodName;
    private LocalDate depreciationDate;
    private LocalDateTime createdAt;
    private String remark;
}
