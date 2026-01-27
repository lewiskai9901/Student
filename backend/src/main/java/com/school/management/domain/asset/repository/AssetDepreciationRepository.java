package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.entity.AssetDepreciation;

import java.util.List;
import java.util.Optional;

/**
 * 资产折旧仓储接口
 */
public interface AssetDepreciationRepository {

    /**
     * 保存折旧记录
     */
    void save(AssetDepreciation depreciation);

    /**
     * 批量保存折旧记录
     */
    void saveAll(List<AssetDepreciation> depreciations);

    /**
     * 根据ID查询
     */
    Optional<AssetDepreciation> findById(Long id);

    /**
     * 根据资产ID查询折旧历史
     */
    List<AssetDepreciation> findByAssetId(Long assetId);

    /**
     * 根据期间查询所有折旧记录
     */
    List<AssetDepreciation> findByPeriod(String period);

    /**
     * 查询资产最近一条折旧记录
     */
    Optional<AssetDepreciation> findLatestByAssetId(Long assetId);

    /**
     * 检查某期间是否已计提折旧
     */
    boolean existsByAssetIdAndPeriod(Long assetId, String period);

    /**
     * 统计资产累计折旧总额
     */
    java.math.BigDecimal sumDepreciationByAssetId(Long assetId);

    /**
     * 分页查询
     */
    List<AssetDepreciation> findByAssetIdWithPagination(Long assetId, int pageNum, int pageSize);

    /**
     * 统计资产折旧记录数
     */
    int countByAssetId(Long assetId);
}
