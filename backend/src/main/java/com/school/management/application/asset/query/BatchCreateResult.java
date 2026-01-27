package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量入库结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateResult {

    /**
     * 入库总数
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 首个资产编号
     */
    private String firstAssetCode;

    /**
     * 末个资产编号
     */
    private String lastAssetCode;

    /**
     * 生成的资产ID列表
     */
    private List<Long> assetIds;

    /**
     * 总价值
     */
    private BigDecimal totalValue;
}
