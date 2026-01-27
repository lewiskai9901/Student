package com.school.management.application.asset.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产盘点DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInventoryDTO {

    private Long id;

    private String inventoryCode;

    private String inventoryName;

    private String scopeType;

    private String scopeTypeDesc;

    private String scopeValue;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer status;

    private String statusDesc;

    private Integer totalCount;

    private Integer checkedCount;

    private Integer profitCount;

    private Integer lossCount;

    // 进度百分比
    private Integer progress;

    private Long createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 盘点明细
    private List<AssetInventoryDetailDTO> details;

    /**
     * 盘点明细DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetInventoryDetailDTO {

        private Long id;

        private Long inventoryId;

        private Long assetId;

        private String assetCode;

        private String assetName;

        private String locationName;

        private Integer expectedQuantity;

        private Integer actualQuantity;

        private Integer difference;

        private Integer resultType;

        private String resultTypeDesc;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime checkTime;

        private Long checkerId;

        private String checkerName;

        private String remark;
    }
}
