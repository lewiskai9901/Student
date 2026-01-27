package com.school.management.domain.asset.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.asset.model.entity.AssetInventory;
import com.school.management.domain.asset.model.entity.AssetInventoryDetail;
import com.school.management.domain.asset.model.valueobject.InventoryStatus;
import com.school.management.infrastructure.persistence.asset.AssetInventoryDetailPO;
import com.school.management.infrastructure.persistence.asset.AssetInventoryPO;

import java.util.List;
import java.util.Optional;

/**
 * 资产盘点仓储接口
 */
public interface AssetInventoryRepository {

    /**
     * 保存盘点任务
     */
    AssetInventory save(AssetInventory inventory);

    /**
     * 根据ID查找盘点任务
     */
    Optional<AssetInventory> findById(Long id);

    /**
     * 根据盘点单号查找
     */
    Optional<AssetInventory> findByInventoryCode(String inventoryCode);

    /**
     * 查询所有盘点任务
     */
    List<AssetInventory> findAll();

    /**
     * 根据状态查询
     */
    List<AssetInventory> findByStatus(InventoryStatus status);

    /**
     * 生成盘点单号
     */
    String generateInventoryCode();

    // ============ 盘点明细相关 ============

    /**
     * 保存盘点明细
     */
    AssetInventoryDetail saveDetail(AssetInventoryDetail detail);

    /**
     * 批量保存盘点明细
     */
    void saveDetails(List<AssetInventoryDetail> details);

    /**
     * 根据盘点ID查询明细列表
     */
    List<AssetInventoryDetail> findDetailsByInventoryId(Long inventoryId);

    /**
     * 根据ID查找明细
     */
    Optional<AssetInventoryDetail> findDetailById(Long id);

    /**
     * 更新盘点明细
     */
    void updateDetail(AssetInventoryDetail detail);

    /**
     * 分页查询盘点任务
     */
    IPage<AssetInventoryPO> selectPage(Page<AssetInventoryPO> page, Integer status, String keyword);

    /**
     * 根据盘点ID查询明细列表(PO)
     */
    List<AssetInventoryDetailPO> findDetailPOsByInventoryId(Long inventoryId);

    /**
     * 统计指定状态的盘点数量
     */
    int countByStatus(InventoryStatus status);
}
