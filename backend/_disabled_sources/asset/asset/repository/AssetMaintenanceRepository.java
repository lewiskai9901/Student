package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.entity.AssetMaintenance;
import com.school.management.domain.asset.model.valueobject.MaintenanceStatus;

import java.util.List;
import java.util.Optional;

/**
 * 资产维修保养仓储接口
 */
public interface AssetMaintenanceRepository {

    /**
     * 保存维修记录
     */
    AssetMaintenance save(AssetMaintenance maintenance);

    /**
     * 根据ID查找
     */
    Optional<AssetMaintenance> findById(Long id);

    /**
     * 根据资产ID查询维修记录
     */
    List<AssetMaintenance> findByAssetId(Long assetId);

    /**
     * 根据状态查询
     */
    List<AssetMaintenance> findByStatus(MaintenanceStatus status);

    /**
     * 查询资产的进行中维修记录
     */
    Optional<AssetMaintenance> findInProgressByAssetId(Long assetId);
}
