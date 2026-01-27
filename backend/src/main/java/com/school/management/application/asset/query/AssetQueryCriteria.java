package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资产查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetQueryCriteria {

    private Long categoryId;

    private Integer status;

    private String locationType;

    private Long locationId;

    private String keyword;

    // 分页参数
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
