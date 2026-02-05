package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.entity.AssetHistory;
import com.school.management.domain.asset.model.valueobject.ChangeType;

import java.util.List;

/**
 * 资产变更记录仓储接口
 */
public interface AssetHistoryRepository {

    /**
     * 保存变更记录
     */
    AssetHistory save(AssetHistory history);

    /**
     * 根据资产ID查询变更记录
     */
    List<AssetHistory> findByAssetId(Long assetId);

    /**
     * 根据资产ID和变更类型查询
     */
    List<AssetHistory> findByAssetIdAndChangeType(Long assetId, ChangeType changeType);

    /**
     * 查询最近的N条变更记录
     */
    List<AssetHistory> findRecentByAssetId(Long assetId, int limit);
}
