package com.school.management.application.asset.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {

    private Long id;

    private String assetCode;

    private String assetName;

    private Long categoryId;

    private String categoryName;

    private String categoryCode;

    private String brand;

    private String model;

    private String unit;

    private Integer quantity;

    private BigDecimal originalValue;

    private BigDecimal netValue;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyDate;

    private String supplier;

    private Integer status;

    private String statusDesc;

    private Integer managementMode;

    private String managementModeDesc;

    // 批量管理时的可用库存数量
    private Integer availableQuantity;

    // 位置信息
    private String locationType;

    private String locationTypeDesc;

    private Long locationId;

    private String locationName;

    // 责任人
    private Long responsibleUserId;

    private String responsibleUserName;

    private String remark;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 是否在保修期内
    private Boolean underWarranty;
}
