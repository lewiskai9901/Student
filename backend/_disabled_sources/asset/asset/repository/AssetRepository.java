package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.valueobject.AssetStatus;
import com.school.management.domain.asset.model.valueobject.LocationType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;

import java.util.List;
import java.util.Optional;

/**
 * 资产仓储接口
 */
public interface AssetRepository {

    /**
     * 保存资产
     */
    Asset save(Asset asset);

    /**
     * 根据ID查找资产
     */
    Optional<Asset> findById(Long id);

    /**
     * 根据资产编号查找
     */
    Optional<Asset> findByAssetCode(String assetCode);

    /**
     * 根据分类ID查询资产列表
     */
    List<Asset> findByCategoryId(Long categoryId);

    /**
     * 根据状态查询资产列表
     */
    List<Asset> findByStatus(AssetStatus status);

    /**
     * 根据位置查询资产列表
     */
    List<Asset> findByLocation(LocationType locationType, Long locationId);

    /**
     * 删除资产(逻辑删除)
     */
    void delete(Long id);

    /**
     * 检查资产编号是否存在
     */
    boolean existsByAssetCode(String assetCode);

    /**
     * 生成资产编号
     */
    String generateAssetCode(String categoryCode);

    /**
     * 统计指定分类下的资产数量
     */
    int countByCategoryId(Long categoryId);

    /**
     * 按状态统计资产数量
     */
    int countByStatus(AssetStatus status);

    /**
     * 统计全部资产数量
     */
    int countAll();

    /**
     * 批量保存资产
     */
    List<Asset> batchSave(List<Asset> assets);

    /**
     * 批量生成资产编号
     * @param categoryCode 分类编码
     * @param count 数量
     * @return 资产编号列表
     */
    List<String> generateAssetCodes(String categoryCode, int count);

    /**
     * 查找保修即将到期的资产
     * @param days 天数
     */
    List<Asset> findWarrantyExpiringWithin(int days);

    /**
     * 根据管理模式查找资产
     */
    List<Asset> findByManagementMode(ManagementMode managementMode);

    /**
     * 查询所有资产
     */
    List<Asset> findAll();
}
