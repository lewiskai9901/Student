package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.entity.AssetDepreciation;
import com.school.management.domain.asset.repository.AssetDepreciationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产折旧仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetDepreciationRepositoryImpl implements AssetDepreciationRepository {

    private final AssetDepreciationMapper mapper;

    @Override
    public void save(AssetDepreciation depreciation) {
        AssetDepreciationPO po = toPO(depreciation);
        if (po.getId() == null) {
            mapper.insert(po);
            depreciation.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
    }

    @Override
    public void saveAll(List<AssetDepreciation> depreciations) {
        for (AssetDepreciation depreciation : depreciations) {
            save(depreciation);
        }
    }

    @Override
    public Optional<AssetDepreciation> findById(Long id) {
        AssetDepreciationPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AssetDepreciation> findByAssetId(Long assetId) {
        LambdaQueryWrapper<AssetDepreciationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDepreciationPO::getAssetId, assetId)
                .orderByDesc(AssetDepreciationPO::getDepreciationPeriod);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetDepreciation> findByPeriod(String period) {
        LambdaQueryWrapper<AssetDepreciationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDepreciationPO::getDepreciationPeriod, period);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssetDepreciation> findLatestByAssetId(Long assetId) {
        AssetDepreciationPO po = mapper.selectLatestByAssetId(assetId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean existsByAssetIdAndPeriod(Long assetId, String period) {
        return mapper.countByAssetIdAndPeriod(assetId, period) > 0;
    }

    @Override
    public BigDecimal sumDepreciationByAssetId(Long assetId) {
        return mapper.sumDepreciationByAssetId(assetId);
    }

    @Override
    public List<AssetDepreciation> findByAssetIdWithPagination(Long assetId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return mapper.selectByAssetIdWithPagination(assetId, offset, pageSize).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countByAssetId(Long assetId) {
        LambdaQueryWrapper<AssetDepreciationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDepreciationPO::getAssetId, assetId);
        return Math.toIntExact(mapper.selectCount(wrapper));
    }

    // ==================== 映射方法 ====================

    private AssetDepreciationPO toPO(AssetDepreciation domain) {
        AssetDepreciationPO po = new AssetDepreciationPO();
        po.setId(domain.getId());
        po.setAssetId(domain.getAssetId());
        po.setAssetCode(domain.getAssetCode());
        po.setDepreciationPeriod(domain.getDepreciationPeriod());
        po.setBeginningValue(domain.getBeginningValue());
        po.setBeginningAccumulatedDepreciation(domain.getBeginningAccumulatedDepreciation());
        po.setBeginningNetValue(domain.getBeginningNetValue());
        po.setDepreciationAmount(domain.getDepreciationAmount());
        po.setEndingAccumulatedDepreciation(domain.getEndingAccumulatedDepreciation());
        po.setEndingNetValue(domain.getEndingNetValue());
        po.setUsedMonths(domain.getUsedMonths());
        po.setRemainingMonths(domain.getRemainingMonths());
        po.setDepreciationMethod(domain.getDepreciationMethod());
        po.setDepreciationDate(domain.getDepreciationDate());
        po.setCreatedAt(domain.getCreatedAt());
        po.setRemark(domain.getRemark());
        return po;
    }

    private AssetDepreciation toDomain(AssetDepreciationPO po) {
        AssetDepreciation domain = new AssetDepreciation();
        domain.setId(po.getId());
        domain.setAssetId(po.getAssetId());
        domain.setAssetCode(po.getAssetCode());
        domain.setDepreciationPeriod(po.getDepreciationPeriod());
        domain.setBeginningValue(po.getBeginningValue());
        domain.setBeginningAccumulatedDepreciation(po.getBeginningAccumulatedDepreciation());
        domain.setBeginningNetValue(po.getBeginningNetValue());
        domain.setDepreciationAmount(po.getDepreciationAmount());
        domain.setEndingAccumulatedDepreciation(po.getEndingAccumulatedDepreciation());
        domain.setEndingNetValue(po.getEndingNetValue());
        domain.setUsedMonths(po.getUsedMonths());
        domain.setRemainingMonths(po.getRemainingMonths());
        domain.setDepreciationMethod(po.getDepreciationMethod());
        domain.setDepreciationDate(po.getDepreciationDate());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setRemark(po.getRemark());
        return domain;
    }
}
