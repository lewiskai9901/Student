package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.valueobject.AssetLocation;
import com.school.management.domain.asset.model.valueobject.AssetStatus;
import com.school.management.domain.asset.model.valueobject.LocationType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {

    private final AssetMapper assetMapper;

    @Override
    public Asset save(Asset asset) {
        AssetPO po = toPO(asset);
        if (po.getId() == null) {
            assetMapper.insert(po);
        } else {
            assetMapper.updateById(po);
        }
        asset.setId(po.getId());
        return asset;
    }

    @Override
    public Optional<Asset> findById(Long id) {
        AssetPO po = assetMapper.selectByIdWithCategory(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<Asset> findByAssetCode(String assetCode) {
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetPO::getAssetCode, assetCode);
        AssetPO po = assetMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Asset> findByCategoryId(Long categoryId) {
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetPO::getCategoryId, categoryId);
        return assetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByStatus(AssetStatus status) {
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetPO::getStatus, status.getCode());
        return assetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByLocation(LocationType locationType, Long locationId) {
        return assetMapper.selectByLocation(locationType.getCode(), locationId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        assetMapper.deleteById(id);
    }

    @Override
    public boolean existsByAssetCode(String assetCode) {
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetPO::getAssetCode, assetCode);
        return assetMapper.selectCount(wrapper) > 0;
    }

    @Override
    public String generateAssetCode(String categoryCode) {
        // 格式: ZC-{分类编码前缀}-{日期}-{序号}
        // 例如: ZC-TEACH-20260122-0001
        String prefix = "ZC-" + (categoryCode != null ? categoryCode.split("-")[0] : "OTH") + "-";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fullPrefix = prefix + dateStr + "-";

        String maxCode = assetMapper.getMaxAssetCode(fullPrefix);
        int nextSeq = 1;
        if (maxCode != null && maxCode.length() > fullPrefix.length()) {
            try {
                nextSeq = Integer.parseInt(maxCode.substring(fullPrefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return fullPrefix + String.format("%04d", nextSeq);
    }

    @Override
    public int countByCategoryId(Long categoryId) {
        return assetMapper.countByCategoryId(categoryId);
    }

    @Override
    public int countByStatus(AssetStatus status) {
        return assetMapper.countByStatus(status.getCode());
    }

    @Override
    public int countAll() {
        return assetMapper.countAll();
    }

    @Override
    public List<Asset> batchSave(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return assets;
        }
        List<AssetPO> poList = assets.stream().map(this::toPO).collect(Collectors.toList());
        // MyBatis Plus 批量插入
        for (AssetPO po : poList) {
            assetMapper.insert(po);
        }
        // 回填ID
        for (int i = 0; i < assets.size(); i++) {
            assets.get(i).setId(poList.get(i).getId());
        }
        return assets;
    }

    @Override
    public List<String> generateAssetCodes(String categoryCode, int count) {
        // 格式: ZC-{分类编码前缀}-{日期}-{序号}
        String prefix = "ZC-" + (categoryCode != null ? categoryCode.split("-")[0] : "OTH") + "-";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fullPrefix = prefix + dateStr + "-";

        String maxCode = assetMapper.getMaxAssetCode(fullPrefix);
        int startSeq = 1;
        if (maxCode != null && maxCode.length() > fullPrefix.length()) {
            try {
                startSeq = Integer.parseInt(maxCode.substring(fullPrefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }

        List<String> codes = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(fullPrefix + String.format("%04d", startSeq + i));
        }
        return codes;
    }

    @Override
    public List<Asset> findWarrantyExpiringWithin(int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(AssetPO::getWarrantyDate)
                .le(AssetPO::getWarrantyDate, endDate)
                .ge(AssetPO::getWarrantyDate, LocalDate.now())
                .ne(AssetPO::getStatus, AssetStatus.SCRAPPED.getCode());
        return assetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findByManagementMode(ManagementMode managementMode) {
        LambdaQueryWrapper<AssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetPO::getManagementMode, managementMode.getCode());
        return assetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findAll() {
        return assetMapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ============ 转换方法 ============

    private Asset toDomain(AssetPO po) {
        if (po == null) return null;

        Asset asset = Asset.builder()
                .assetCode(po.getAssetCode())
                .assetName(po.getAssetName())
                .categoryId(po.getCategoryId())
                .categoryName(po.getCategoryName())
                .brand(po.getBrand())
                .model(po.getModel())
                .unit(po.getUnit())
                .quantity(po.getQuantity())
                .originalValue(po.getOriginalValue())
                .netValue(po.getNetValue())
                .purchaseDate(po.getPurchaseDate())
                .warrantyDate(po.getWarrantyDate())
                .supplier(po.getSupplier())
                .status(po.getStatus() != null ? AssetStatus.fromCode(po.getStatus()) : null)
                .managementMode(po.getManagementMode() != null ?
                        ManagementMode.fromCode(po.getManagementMode()) : ManagementMode.SINGLE_ITEM)
                .location(AssetLocation.of(
                        LocationType.fromCode(po.getLocationType()),
                        po.getLocationId(),
                        po.getLocationName()
                ))
                .responsibleUserId(po.getResponsibleUserId())
                .responsibleUserName(po.getResponsibleUserName())
                .categoryType(po.getCategoryType())
                .depreciationMethod(po.getDepreciationMethod())
                .residualValue(po.getResidualValue())
                .accumulatedDepreciation(po.getAccumulatedDepreciation())
                .usefulLife(po.getUsefulLife())
                .stockWarningThreshold(po.getStockWarningThreshold())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
        asset.setId(po.getId());
        return asset;
    }

    private AssetPO toPO(Asset asset) {
        AssetLocation location = asset.getLocation();
        return AssetPO.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .assetName(asset.getAssetName())
                .categoryId(asset.getCategoryId())
                .brand(asset.getBrand())
                .model(asset.getModel())
                .unit(asset.getUnit())
                .quantity(asset.getQuantity())
                .originalValue(asset.getOriginalValue())
                .netValue(asset.getNetValue())
                .purchaseDate(asset.getPurchaseDate())
                .warrantyDate(asset.getWarrantyDate())
                .supplier(asset.getSupplier())
                .status(asset.getStatus() != null ? asset.getStatus().getCode() : null)
                .managementMode(asset.getManagementMode() != null ?
                        asset.getManagementMode().getCode() : ManagementMode.SINGLE_ITEM.getCode())
                .locationType(location != null && location.getLocationType() != null ?
                        location.getLocationType().getCode() : null)
                .locationId(location != null ? location.getLocationId() : null)
                .locationName(location != null ? location.getLocationName() : null)
                .responsibleUserId(asset.getResponsibleUserId())
                .responsibleUserName(asset.getResponsibleUserName())
                .categoryType(asset.getCategoryType())
                .depreciationMethod(asset.getDepreciationMethod())
                .residualValue(asset.getResidualValue())
                .accumulatedDepreciation(asset.getAccumulatedDepreciation())
                .usefulLife(asset.getUsefulLife())
                .stockWarningThreshold(asset.getStockWarningThreshold())
                .remark(asset.getRemark())
                .createdBy(asset.getCreatedBy())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
