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
 * 资产维修保养记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMaintenanceDTO {

    private Long id;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Integer maintenanceType;

    private String maintenanceTypeDesc;

    private String faultDesc;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private BigDecimal cost;

    private String maintainer;

    private String result;

    private Integer status;

    private String statusDesc;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
