package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.asset.command.CreateInventoryCommand;
import com.school.management.application.asset.query.AssetInventoryDTO;
import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.entity.AssetInventory;
import com.school.management.domain.asset.model.entity.AssetInventoryDetail;
import com.school.management.domain.asset.model.valueobject.AssetStatus;
import com.school.management.domain.asset.model.valueobject.InventoryStatus;
import com.school.management.domain.asset.repository.AssetInventoryRepository;
import com.school.management.domain.asset.repository.AssetRepository;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.persistence.asset.AssetInventoryDetailPO;
import com.school.management.infrastructure.persistence.asset.AssetInventoryPO;
import com.school.management.infrastructure.persistence.asset.AssetMapper;
import com.school.management.infrastructure.persistence.asset.AssetPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产盘点应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInventoryApplicationService {

    private final AssetInventoryRepository inventoryRepository;
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    /**
     * 创建盘点任务
     */
    @Transactional
    public Long createInventory(CreateInventoryCommand command) {
        // 生成盘点单号
        String inventoryCode = inventoryRepository.generateInventoryCode();

        // 根据范围获取需要盘点的资产
        List<Asset> assets = getAssetsInScope(command.getScopeType(), command.getScopeValue());
        if (assets.isEmpty()) {
            throw new BusinessException("所选范围内没有可盘点的资产");
        }

        // 创建盘点任务
        AssetInventory inventory = AssetInventory.create(
                inventoryCode,
                command.getInventoryName(),
                command.getScopeType(),
                command.getScopeValue(),
                command.getStartDate(),
                command.getEndDate(),
                assets.size(),
                command.getOperatorId()
        );

        inventoryRepository.save(inventory);

        // 创建盘点明细
        for (Asset asset : assets) {
            AssetInventoryDetail detail = AssetInventoryDetail.create(
                    inventory.getId(),
                    asset.getId(),
                    asset.getQuantity()
            );
            inventoryRepository.saveDetail(detail);
        }

        log.info("Created inventory task: {} with {} assets", inventoryCode, assets.size());
        return inventory.getId();
    }

    /**
     * 获取盘点列表
     */
    public IPage<AssetInventoryDTO> queryInventories(Integer status, String keyword, int pageNum, int pageSize) {
        Page<AssetInventoryPO> page = new Page<>(pageNum, pageSize);
        IPage<AssetInventoryPO> result = inventoryRepository.selectPage(page, status, keyword);
        return result.convert(this::toInventoryDTO);
    }

    /**
     * 获取盘点详情
     */
    public AssetInventoryDTO getInventory(Long id) {
        AssetInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("盘点任务不存在"));

        AssetInventoryDTO dto = toInventoryDTO(inventory);

        // 加载明细
        List<AssetInventoryDetailPO> detailPOs = inventoryRepository.findDetailPOsByInventoryId(id);
        dto.setDetails(detailPOs.stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * 更新盘点明细
     */
    @Transactional
    public void updateInventoryDetail(Long detailId, Integer actualQuantity, String remark,
                                       Long checkerId, String checkerName) {
        AssetInventoryDetail detail = inventoryRepository.findDetailById(detailId)
                .orElseThrow(() -> new BusinessException("盘点明细不存在"));

        AssetInventory inventory = inventoryRepository.findById(detail.getInventoryId())
                .orElseThrow(() -> new BusinessException("盘点任务不存在"));

        if (!inventory.isEditable()) {
            throw new BusinessException("该盘点任务已结束，不能修改");
        }

        // 更新明细
        detail.check(actualQuantity, checkerId, checkerName, remark);
        inventoryRepository.saveDetail(detail);

        // 更新盘点任务进度
        updateInventoryProgress(inventory.getId());

        log.info("Updated inventory detail: {} with actualQuantity: {}", detailId, actualQuantity);
    }

    /**
     * 完成盘点
     */
    @Transactional
    public void completeInventory(Long id) {
        AssetInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("盘点任务不存在"));

        if (!inventory.isEditable()) {
            throw new BusinessException("该盘点任务已结束");
        }

        // 统计盘点结果
        List<AssetInventoryDetailPO> details = inventoryRepository.findDetailPOsByInventoryId(id);
        int checkedCount = 0;
        int profitCount = 0;
        int lossCount = 0;

        for (AssetInventoryDetailPO detail : details) {
            if (detail.getActualQuantity() != null) {
                checkedCount++;
                if (detail.getResultType() != null) {
                    if (detail.getResultType() == 2) profitCount++;
                    if (detail.getResultType() == 3) lossCount++;
                }
            }
        }

        inventory.complete(checkedCount, profitCount, lossCount);
        inventoryRepository.save(inventory);

        log.info("Completed inventory: {} with checked:{}, profit:{}, loss:{}",
                inventory.getInventoryCode(), checkedCount, profitCount, lossCount);
    }

    /**
     * 取消盘点
     */
    @Transactional
    public void cancelInventory(Long id) {
        AssetInventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("盘点任务不存在"));

        if (!inventory.isEditable()) {
            throw new BusinessException("该盘点任务已结束，不能取消");
        }

        inventory.cancel();
        inventoryRepository.save(inventory);

        log.info("Cancelled inventory: {}", inventory.getInventoryCode());
    }

    /**
     * 获取盘点统计
     */
    public InventoryStatistics getStatistics() {
        int inProgressCount = inventoryRepository.countByStatus(InventoryStatus.IN_PROGRESS);
        int completedCount = inventoryRepository.countByStatus(InventoryStatus.COMPLETED);
        return InventoryStatistics.builder()
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .build();
    }

    // ============ 私有方法 ============

    private List<Asset> getAssetsInScope(String scopeType, String scopeValue) {
        List<AssetPO> assetPOs;

        if (scopeType == null || "all".equals(scopeType)) {
            // 全部资产（非报废）
            assetPOs = assetMapper.selectByStatusNot(AssetStatus.SCRAPPED.getCode());
        } else if ("category".equals(scopeType)) {
            // 按分类
            Long categoryId = Long.parseLong(scopeValue);
            assetPOs = assetMapper.selectByCategoryIdAndStatusNot(categoryId, AssetStatus.SCRAPPED.getCode());
        } else if ("location".equals(scopeType)) {
            // 按位置
            String[] parts = scopeValue.split(":");
            String locationType = parts[0];
            Long locationId = Long.parseLong(parts[1]);
            assetPOs = assetMapper.selectByLocationAndStatusNot(locationType, locationId, AssetStatus.SCRAPPED.getCode());
        } else {
            assetPOs = new ArrayList<>();
        }

        return assetPOs.stream()
                .map(this::poToAsset)
                .collect(Collectors.toList());
    }

    private void updateInventoryProgress(Long inventoryId) {
        List<AssetInventoryDetailPO> details = inventoryRepository.findDetailPOsByInventoryId(inventoryId);
        int checkedCount = 0;
        int profitCount = 0;
        int lossCount = 0;

        for (AssetInventoryDetailPO detail : details) {
            if (detail.getActualQuantity() != null) {
                checkedCount++;
                if (detail.getResultType() != null) {
                    if (detail.getResultType() == 2) profitCount++;
                    if (detail.getResultType() == 3) lossCount++;
                }
            }
        }

        AssetInventory inventory = inventoryRepository.findById(inventoryId).orElse(null);
        if (inventory != null) {
            inventory.updateProgress(checkedCount, profitCount, lossCount);
            inventoryRepository.save(inventory);
        }
    }

    private Asset poToAsset(AssetPO po) {
        Asset asset = new Asset();
        asset.setId(po.getId());
        asset.setAssetCode(po.getAssetCode());
        asset.setAssetName(po.getAssetName());
        asset.setQuantity(po.getQuantity());
        return asset;
    }

    private AssetInventoryDTO toInventoryDTO(AssetInventoryPO po) {
        int progress = 0;
        if (po.getTotalCount() != null && po.getTotalCount() > 0 && po.getCheckedCount() != null) {
            progress = (int) ((po.getCheckedCount() * 100.0) / po.getTotalCount());
        }

        return AssetInventoryDTO.builder()
                .id(po.getId())
                .inventoryCode(po.getInventoryCode())
                .inventoryName(po.getInventoryName())
                .scopeType(po.getScopeType())
                .scopeTypeDesc(getScopeTypeDesc(po.getScopeType()))
                .scopeValue(po.getScopeValue())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .status(po.getStatus())
                .statusDesc(po.getStatus() != null ? InventoryStatus.fromCode(po.getStatus()).getDescription() : null)
                .totalCount(po.getTotalCount())
                .checkedCount(po.getCheckedCount())
                .profitCount(po.getProfitCount())
                .lossCount(po.getLossCount())
                .progress(progress)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private AssetInventoryDTO toInventoryDTO(AssetInventory inventory) {
        int progress = 0;
        if (inventory.getTotalCount() != null && inventory.getTotalCount() > 0 && inventory.getCheckedCount() != null) {
            progress = (int) ((inventory.getCheckedCount() * 100.0) / inventory.getTotalCount());
        }

        return AssetInventoryDTO.builder()
                .id(inventory.getId())
                .inventoryCode(inventory.getInventoryCode())
                .inventoryName(inventory.getInventoryName())
                .scopeType(inventory.getScopeType())
                .scopeTypeDesc(getScopeTypeDesc(inventory.getScopeType()))
                .scopeValue(inventory.getScopeValue())
                .startDate(inventory.getStartDate())
                .endDate(inventory.getEndDate())
                .status(inventory.getStatus() != null ? inventory.getStatus().getCode() : null)
                .statusDesc(inventory.getStatus() != null ? inventory.getStatus().getDescription() : null)
                .totalCount(inventory.getTotalCount())
                .checkedCount(inventory.getCheckedCount())
                .profitCount(inventory.getProfitCount())
                .lossCount(inventory.getLossCount())
                .progress(progress)
                .createdBy(inventory.getCreatedBy())
                .createdAt(inventory.getCreatedAt())
                .build();
    }

    private AssetInventoryDTO.AssetInventoryDetailDTO toDetailDTO(AssetInventoryDetailPO po) {
        return AssetInventoryDTO.AssetInventoryDetailDTO.builder()
                .id(po.getId())
                .inventoryId(po.getInventoryId())
                .assetId(po.getAssetId())
                .assetCode(po.getAssetCode())
                .assetName(po.getAssetName())
                .locationName(po.getLocationName())
                .expectedQuantity(po.getExpectedQuantity())
                .actualQuantity(po.getActualQuantity())
                .difference(po.getDifference())
                .resultType(po.getResultType())
                .resultTypeDesc(getResultTypeDesc(po.getResultType()))
                .checkTime(po.getCheckTime())
                .checkerId(po.getCheckerId())
                .checkerName(po.getCheckerName())
                .remark(po.getRemark())
                .build();
    }

    private String getScopeTypeDesc(String scopeType) {
        if (scopeType == null || "all".equals(scopeType)) return "全部资产";
        if ("category".equals(scopeType)) return "按分类";
        if ("location".equals(scopeType)) return "按位置";
        return scopeType;
    }

    private String getResultTypeDesc(Integer resultType) {
        if (resultType == null) return "未盘点";
        switch (resultType) {
            case 1: return "正常";
            case 2: return "盘盈";
            case 3: return "盘亏";
            default: return "未知";
        }
    }

    /**
     * 盘点统计
     */
    @lombok.Data
    @lombok.Builder
    public static class InventoryStatistics {
        private int inProgressCount;
        private int completedCount;
    }
}
