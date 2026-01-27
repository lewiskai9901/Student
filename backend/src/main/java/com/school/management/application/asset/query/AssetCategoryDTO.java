package com.school.management.application.asset.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产分类DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategoryDTO {

    private Long id;

    private Long parentId;

    private String categoryCode;

    private String categoryName;

    private Integer categoryType;

    private String categoryTypeDesc;

    private Integer defaultManagementMode;

    private String defaultManagementModeDesc;

    // 是否支持批量管理
    private Boolean supportsBatchManagement;

    private Integer depreciationYears;

    private String unit;

    private Integer sortOrder;

    private String remark;

    // 资产数量
    private Integer assetCount;

    // 子分类列表
    private List<AssetCategoryDTO> children;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
