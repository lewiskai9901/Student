package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.asset.model.entity.AssetInventory;
import com.school.management.domain.asset.model.entity.AssetInventoryDetail;
import com.school.management.domain.asset.model.valueobject.InventoryStatus;
import com.school.management.domain.asset.repository.AssetInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产盘点仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetInventoryRepositoryImpl implements AssetInventoryRepository {

    private final AssetInventoryMapper inventoryMapper;
    private final AssetInventoryDetailMapper detailMapper;

    @Override
    public AssetInventory save(AssetInventory inventory) {
        AssetInventoryPO po = toPO(inventory);
        if (po.getId() == null) {
            inventoryMapper.insert(po);
        } else {
            inventoryMapper.updateById(po);
        }
        inventory.setId(po.getId());
        return inventory;
    }

    @Override
    public Optional<AssetInventory> findById(Long id) {
        AssetInventoryPO po = inventoryMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<AssetInventory> findByInventoryCode(String inventoryCode) {
        LambdaQueryWrapper<AssetInventoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetInventoryPO::getInventoryCode, inventoryCode);
        AssetInventoryPO po = inventoryMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AssetInventory> findAll() {
        LambdaQueryWrapper<AssetInventoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AssetInventoryPO::getCreatedAt);
        return inventoryMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetInventory> findByStatus(InventoryStatus status) {
        return inventoryMapper.selectByStatus(status.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public String generateInventoryCode() {
        // 格式: PD-{日期}-{序号}
        // 例如: PD-20260122-0001
        String prefix = "PD-";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fullPrefix = prefix + dateStr + "-";

        String maxCode = inventoryMapper.getMaxInventoryCode(fullPrefix);
        int nextSeq = 1;
        if (maxCode != null && maxCode.length() > fullPrefix.length()) {
            try {
                nextSeq = Integer.parseInt(maxCode.substring(fullPrefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return fullPrefix + String.format("%04d", nextSeq);
    }

    // ============ 盘点明细相关 ============

    @Override
    public AssetInventoryDetail saveDetail(AssetInventoryDetail detail) {
        AssetInventoryDetailPO po = toDetailPO(detail);
        if (po.getId() == null) {
            detailMapper.insert(po);
        } else {
            detailMapper.updateById(po);
        }
        detail.setId(po.getId());
        return detail;
    }

    @Override
    public void saveDetails(List<AssetInventoryDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<AssetInventoryDetailPO> poList = details.stream()
                .map(this::toDetailPO)
                .collect(Collectors.toList());
        detailMapper.batchInsert(poList);
    }

    @Override
    public List<AssetInventoryDetail> findDetailsByInventoryId(Long inventoryId) {
        return detailMapper.selectByInventoryIdWithAsset(inventoryId).stream()
                .map(this::toDetailDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssetInventoryDetail> findDetailById(Long id) {
        AssetInventoryDetailPO po = detailMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDetailDomain);
    }

    @Override
    public void updateDetail(AssetInventoryDetail detail) {
        AssetInventoryDetailPO po = toDetailPO(detail);
        detailMapper.updateById(po);
    }

    @Override
    public IPage<AssetInventoryPO> selectPage(Page<AssetInventoryPO> page, Integer status, String keyword) {
        LambdaQueryWrapper<AssetInventoryPO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AssetInventoryPO::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(AssetInventoryPO::getInventoryCode, keyword)
                    .or()
                    .like(AssetInventoryPO::getInventoryName, keyword)
            );
        }
        wrapper.orderByDesc(AssetInventoryPO::getCreatedAt);
        return inventoryMapper.selectPage(page, wrapper);
    }

    @Override
    public List<AssetInventoryDetailPO> findDetailPOsByInventoryId(Long inventoryId) {
        return detailMapper.selectByInventoryIdWithAsset(inventoryId);
    }

    @Override
    public int countByStatus(InventoryStatus status) {
        LambdaQueryWrapper<AssetInventoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetInventoryPO::getStatus, status.getCode());
        return Math.toIntExact(inventoryMapper.selectCount(wrapper));
    }

    // ============ 转换方法 ============

    private AssetInventory toDomain(AssetInventoryPO po) {
        if (po == null) return null;

        AssetInventory inventory = AssetInventory.builder()
                .inventoryCode(po.getInventoryCode())
                .inventoryName(po.getInventoryName())
                .scopeType(po.getScopeType())
                .scopeValue(po.getScopeValue())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .status(po.getStatus() != null ?
                        InventoryStatus.fromCode(po.getStatus()) : null)
                .totalCount(po.getTotalCount())
                .checkedCount(po.getCheckedCount())
                .profitCount(po.getProfitCount())
                .lossCount(po.getLossCount())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
        inventory.setId(po.getId());
        return inventory;
    }

    private AssetInventoryPO toPO(AssetInventory inventory) {
        return AssetInventoryPO.builder()
                .id(inventory.getId())
                .inventoryCode(inventory.getInventoryCode())
                .inventoryName(inventory.getInventoryName())
                .scopeType(inventory.getScopeType())
                .scopeValue(inventory.getScopeValue())
                .startDate(inventory.getStartDate())
                .endDate(inventory.getEndDate())
                .status(inventory.getStatus() != null ?
                        inventory.getStatus().getCode() : null)
                .totalCount(inventory.getTotalCount())
                .checkedCount(inventory.getCheckedCount())
                .profitCount(inventory.getProfitCount())
                .lossCount(inventory.getLossCount())
                .createdBy(inventory.getCreatedBy())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private AssetInventoryDetail toDetailDomain(AssetInventoryDetailPO po) {
        if (po == null) return null;

        AssetInventoryDetail detail = AssetInventoryDetail.builder()
                .inventoryId(po.getInventoryId())
                .assetId(po.getAssetId())
                .expectedQuantity(po.getExpectedQuantity())
                .actualQuantity(po.getActualQuantity())
                .difference(po.getDifference())
                .resultType(po.getResultType())
                .checkTime(po.getCheckTime())
                .checkerId(po.getCheckerId())
                .checkerName(po.getCheckerName())
                .remark(po.getRemark())
                .assetCode(po.getAssetCode())
                .assetName(po.getAssetName())
                .locationName(po.getLocationName())
                .build();
        detail.setId(po.getId());
        return detail;
    }

    private AssetInventoryDetailPO toDetailPO(AssetInventoryDetail detail) {
        return AssetInventoryDetailPO.builder()
                .id(detail.getId())
                .inventoryId(detail.getInventoryId())
                .assetId(detail.getAssetId())
                .expectedQuantity(detail.getExpectedQuantity())
                .actualQuantity(detail.getActualQuantity())
                .difference(detail.getDifference())
                .resultType(detail.getResultType())
                .checkTime(detail.getCheckTime())
                .checkerId(detail.getCheckerId())
                .checkerName(detail.getCheckerName())
                .remark(detail.getRemark())
                .build();
    }
}
