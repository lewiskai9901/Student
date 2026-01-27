package com.school.management.application.asset.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量调拨结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTransferResult {

    /**
     * 调拨总数
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 成功调拨的资产ID列表
     */
    private List<Long> successAssetIds;

    /**
     * 失败的资产信息
     */
    private List<FailedAsset> failedAssets;

    /**
     * 目标位置名称
     */
    private String targetLocationName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedAsset {
        private Long assetId;
        private String assetCode;
        private String assetName;
        private String reason;
    }
}
