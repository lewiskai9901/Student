package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.asset.command.*;
import com.school.management.application.asset.query.*;
import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.entity.AssetHistory;
import com.school.management.domain.asset.model.entity.AssetMaintenance;
import com.school.management.domain.asset.model.valueobject.*;
import com.school.management.domain.asset.repository.*;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.persistence.asset.AssetMapper;
import com.school.management.infrastructure.persistence.asset.AssetPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 固定资产应用服务
 * 管理设备、家具等固定资产的全生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetApplicationService {

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetHistoryRepository historyRepository;
    private final AssetMaintenanceRepository maintenanceRepository;
    private final AssetMapper assetMapper;

    /**
     * 创建资产
     */
    @Transactional
    public Long createAsset(CreateAssetCommand command) {
        // 获取分类信息
        var category = categoryRepository.findById(command.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        // 生成资产编号
        String assetCode = assetRepository.generateAssetCode(category.getCategoryCode());

        // 确定管理模式：优先使用命令中指定的，否则使用分类的默认模式
        ManagementMode managementMode;
        if (command.getManagementMode() != null) {
            managementMode = ManagementMode.fromCode(command.getManagementMode());
        } else {
            managementMode = category.getEffectiveManagementMode();
        }

        // 创建资产
        Asset asset = Asset.create(
                assetCode,
                command.getAssetName(),
                command.getCategoryId(),
                command.getUnit(),
                command.getQuantity(),
                managementMode
        );
        asset.setBrand(command.getBrand());
        asset.setModel(command.getModel());
        asset.setOriginalValue(command.getOriginalValue());
        asset.setNetValue(command.getNetValue() != null ? command.getNetValue() : command.getOriginalValue());
        asset.setPurchaseDate(command.getPurchaseDate());
        asset.setWarrantyDate(command.getWarrantyDate());
        asset.setSupplier(command.getSupplier());
        asset.setRemark(command.getRemark());
        asset.setCreatedBy(command.getOperatorId());

        // 设置位置信息
        if (command.getLocationType() != null && command.getLocationId() != null) {
            asset.setLocation(AssetLocation.of(
                    LocationType.fromCode(command.getLocationType()),
                    command.getLocationId(),
                    command.getLocationName()
            ));
            asset.setStatus(AssetStatus.IN_USE);
        }

        // 设置责任人
        asset.setResponsibleUserId(command.getResponsibleUserId());
        asset.setResponsibleUserName(command.getResponsibleUserName());

        // 保存资产
        assetRepository.save(asset);

        // 记录创建历史
        AssetHistory history = AssetHistory.createNew(
                asset.getId(),
                command.getOperatorId(),
                command.getOperatorName(),
                "新增资产: " + asset.getAssetName()
        );
        historyRepository.save(history);

        log.info("Created fixed asset: {} - {}", asset.getAssetCode(), asset.getAssetName());
        return asset.getId();
    }

    /**
     * 批量入库资产
     */
    @Transactional
    public BatchCreateResult batchCreateAssets(BatchCreateAssetCommand command) {
        // 1. 获取分类信息
        var category = categoryRepository.findById(command.getCategoryId())
                .orElseThrow(() -> new BusinessException("分类不存在"));

        int quantity = command.getQuantity();

        // 2. 批量生成资产编号
        List<String> assetCodes = assetRepository.generateAssetCodes(category.getCategoryCode(), quantity);

        // 3. 批量创建资产对象
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Asset asset = Asset.create(
                    assetCodes.get(i),
                    command.getAssetName(),
                    command.getCategoryId(),
                    command.getUnit(),
                    1  // 每件资产数量为1
            );
            asset.setBrand(command.getBrand());
            asset.setModel(command.getModel());
            asset.setOriginalValue(command.getOriginalValue());
            asset.setNetValue(command.getOriginalValue());
            asset.setPurchaseDate(command.getPurchaseDate());
            asset.setWarrantyDate(command.getWarrantyDate());
            asset.setSupplier(command.getSupplier());
            asset.setRemark(command.getRemark());
            asset.setCreatedBy(command.getOperatorId());

            // 设置位置信息
            if (command.getLocationType() != null && command.getLocationId() != null) {
                asset.setLocation(AssetLocation.of(
                        LocationType.fromCode(command.getLocationType()),
                        command.getLocationId(),
                        command.getLocationName()
                ));
                asset.setStatus(AssetStatus.IN_USE);
            }

            // 设置责任人
            asset.setResponsibleUserId(command.getResponsibleUserId());
            asset.setResponsibleUserName(command.getResponsibleUserName());

            assets.add(asset);
        }

        // 4. 批量保存
        List<Asset> savedAssets = assetRepository.batchSave(assets);

        // 5. 批量记录创建历史
        for (Asset asset : savedAssets) {
            AssetHistory history = AssetHistory.createNew(
                    asset.getId(),
                    command.getOperatorId(),
                    command.getOperatorName(),
                    "批量入库: " + asset.getAssetName()
            );
            historyRepository.save(history);
        }

        // 6. 计算总价值
        BigDecimal totalValue = BigDecimal.ZERO;
        if (command.getOriginalValue() != null) {
            totalValue = command.getOriginalValue().multiply(BigDecimal.valueOf(quantity));
        }

        // 7. 收集资产ID
        List<Long> assetIds = savedAssets.stream()
                .map(Asset::getId)
                .collect(Collectors.toList());

        log.info("Batch created {} assets: {} ~ {}",
                quantity, assetCodes.get(0), assetCodes.get(quantity - 1));

        return BatchCreateResult.builder()
                .totalCount(quantity)
                .successCount(quantity)
                .firstAssetCode(assetCodes.get(0))
                .lastAssetCode(assetCodes.get(quantity - 1))
                .assetIds(assetIds)
                .totalValue(totalValue)
                .build();
    }

    /**
     * 更新资产
     */
    @Transactional
    public void updateAsset(UpdateAssetCommand command) {
        Asset asset = assetRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        // 更新基本信息
        if (command.getAssetName() != null) {
            asset.setAssetName(command.getAssetName());
        }
        if (command.getCategoryId() != null) {
            asset.setCategoryId(command.getCategoryId());
        }
        if (command.getBrand() != null) {
            asset.setBrand(command.getBrand());
        }
        if (command.getModel() != null) {
            asset.setModel(command.getModel());
        }
        if (command.getUnit() != null) {
            asset.setUnit(command.getUnit());
        }
        if (command.getQuantity() != null) {
            asset.setQuantity(command.getQuantity());
        }
        if (command.getOriginalValue() != null) {
            asset.setOriginalValue(command.getOriginalValue());
        }
        if (command.getNetValue() != null) {
            asset.setNetValue(command.getNetValue());
        }
        if (command.getPurchaseDate() != null) {
            asset.setPurchaseDate(command.getPurchaseDate());
        }
        if (command.getWarrantyDate() != null) {
            asset.setWarrantyDate(command.getWarrantyDate());
        }
        if (command.getSupplier() != null) {
            asset.setSupplier(command.getSupplier());
        }
        if (command.getResponsibleUserId() != null) {
            asset.setResponsibleUserId(command.getResponsibleUserId());
            asset.setResponsibleUserName(command.getResponsibleUserName());
        }
        if (command.getRemark() != null) {
            asset.setRemark(command.getRemark());
        }

        assetRepository.save(asset);

        // 记录更新历史
        AssetHistory history = AssetHistory.createUpdate(
                asset.getId(),
                command.getOperatorId(),
                command.getOperatorName(),
                "更新资产信息"
        );
        historyRepository.save(history);

        log.info("Updated fixed asset: {}", asset.getAssetCode());
    }

    /**
     * 调拨资产
     */
    @Transactional
    public void transferAsset(TransferAssetCommand command) {
        Asset asset = assetRepository.findById(command.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        if (!asset.canTransfer()) {
            throw new BusinessException("当前资产状态不允许调拨");
        }

        // 记录原位置
        AssetLocation oldLocation = asset.getLocation();
        String oldLocationType = oldLocation != null && oldLocation.getLocationType() != null ?
                oldLocation.getLocationType().getCode() : null;
        Long oldLocationId = oldLocation != null ? oldLocation.getLocationId() : null;
        String oldLocationName = oldLocation != null ? oldLocation.getLocationName() : null;

        // 执行调拨
        asset.transfer(
                LocationType.fromCode(command.getLocationType()),
                command.getLocationId(),
                command.getLocationName(),
                command.getResponsibleUserId(),
                command.getResponsibleUserName()
        );

        assetRepository.save(asset);

        // 记录调拨历史
        AssetHistory history = AssetHistory.createTransfer(
                asset.getId(),
                command.getOperatorId(),
                command.getOperatorName(),
                oldLocationType,
                oldLocationId,
                oldLocationName,
                command.getLocationType(),
                command.getLocationId(),
                command.getLocationName(),
                command.getRemark()
        );
        historyRepository.save(history);

        log.info("Transferred fixed asset {} from {} to {}",
                asset.getAssetCode(), oldLocationName, command.getLocationName());
    }

    /**
     * 批量调拨资产
     */
    @Transactional
    public BatchTransferResult batchTransferAssets(BatchTransferAssetCommand command) {
        List<Long> assetIds = command.getAssetIds();
        List<Long> successIds = new ArrayList<>();
        List<BatchTransferResult.FailedAsset> failedAssets = new ArrayList<>();

        for (Long assetId : assetIds) {
            try {
                Asset asset = assetRepository.findById(assetId).orElse(null);
                if (asset == null) {
                    failedAssets.add(BatchTransferResult.FailedAsset.builder()
                            .assetId(assetId)
                            .reason("资产不存在")
                            .build());
                    continue;
                }

                if (!asset.canTransfer()) {
                    failedAssets.add(BatchTransferResult.FailedAsset.builder()
                            .assetId(assetId)
                            .assetCode(asset.getAssetCode())
                            .assetName(asset.getAssetName())
                            .reason("当前状态不允许调拨")
                            .build());
                    continue;
                }

                // 记录原位置
                AssetLocation oldLocation = asset.getLocation();
                String oldLocationType = oldLocation != null && oldLocation.getLocationType() != null ?
                        oldLocation.getLocationType().getCode() : null;
                Long oldLocationId = oldLocation != null ? oldLocation.getLocationId() : null;
                String oldLocationName = oldLocation != null ? oldLocation.getLocationName() : null;

                // 执行调拨
                asset.transfer(
                        LocationType.fromCode(command.getLocationType()),
                        command.getLocationId(),
                        command.getLocationName(),
                        command.getResponsibleUserId(),
                        command.getResponsibleUserName()
                );

                assetRepository.save(asset);

                // 记录调拨历史
                AssetHistory history = AssetHistory.createTransfer(
                        asset.getId(),
                        command.getOperatorId(),
                        command.getOperatorName(),
                        oldLocationType,
                        oldLocationId,
                        oldLocationName,
                        command.getLocationType(),
                        command.getLocationId(),
                        command.getLocationName(),
                        command.getRemark()
                );
                historyRepository.save(history);

                successIds.add(assetId);
            } catch (Exception e) {
                log.error("Failed to transfer asset {}: {}", assetId, e.getMessage());
                failedAssets.add(BatchTransferResult.FailedAsset.builder()
                        .assetId(assetId)
                        .reason("调拨失败: " + e.getMessage())
                        .build());
            }
        }

        log.info("Batch transferred {} assets to {}, success: {}, failed: {}",
                assetIds.size(), command.getLocationName(), successIds.size(), failedAssets.size());

        return BatchTransferResult.builder()
                .totalCount(assetIds.size())
                .successCount(successIds.size())
                .failedCount(failedAssets.size())
                .successAssetIds(successIds)
                .failedAssets(failedAssets)
                .targetLocationName(command.getLocationName())
                .build();
    }

    /**
     * 报废资产
     */
    @Transactional
    public void scrapAsset(ScrapAssetCommand command) {
        Asset asset = assetRepository.findById(command.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        if (!asset.canScrap()) {
            throw new BusinessException("当前资产已报废");
        }

        asset.scrap();
        assetRepository.save(asset);

        // 记录报废历史
        AssetHistory history = AssetHistory.createScrap(
                asset.getId(),
                command.getOperatorId(),
                command.getOperatorName(),
                command.getReason()
        );
        historyRepository.save(history);

        log.info("Scrapped fixed asset: {}", asset.getAssetCode());
    }

    /**
     * 创建维修记录
     */
    @Transactional
    public Long createMaintenance(CreateMaintenanceCommand command) {
        Asset asset = assetRepository.findById(command.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        // 检查是否有进行中的维修
        maintenanceRepository.findInProgressByAssetId(command.getAssetId())
                .ifPresent(m -> {
                    throw new BusinessException("该资产有未完成的维修记录");
                });

        // 创建维修记录
        AssetMaintenance maintenance;
        if (command.getMaintenanceType() == 1) {
            maintenance = AssetMaintenance.createRepair(command.getAssetId(), command.getFaultDesc());
        } else {
            maintenance = AssetMaintenance.createMaintenance(command.getAssetId(), command.getFaultDesc());
        }
        maintenance.setMaintainer(command.getMaintainer());
        maintenance.setCreatedBy(command.getOperatorId());

        maintenanceRepository.save(maintenance);

        // 如果是维修,更新资产状态
        if (command.getMaintenanceType() == 1) {
            asset.markAsRepairing();
            assetRepository.save(asset);

            // 记录历史
            AssetHistory history = AssetHistory.createRepair(
                    asset.getId(),
                    command.getOperatorId(),
                    command.getOperatorName(),
                    command.getFaultDesc()
            );
            historyRepository.save(history);
        }

        log.info("Created maintenance for fixed asset: {}", asset.getAssetCode());
        return maintenance.getId();
    }

    /**
     * 完成维修
     */
    @Transactional
    public void completeMaintenance(CompleteMaintenanceCommand command) {
        AssetMaintenance maintenance = maintenanceRepository.findById(command.getMaintenanceId())
                .orElseThrow(() -> new BusinessException("维修记录不存在"));

        if (!maintenance.isInProgress()) {
            throw new BusinessException("该维修记录已完成");
        }

        maintenance.complete(command.getResult(), command.getCost());
        if (command.getMaintainer() != null) {
            maintenance.setMaintainer(command.getMaintainer());
        }
        maintenanceRepository.save(maintenance);

        // 更新资产状态
        Asset asset = assetRepository.findById(maintenance.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));
        asset.completeRepair();
        assetRepository.save(asset);

        log.info("Completed maintenance {} for fixed asset", command.getMaintenanceId());
    }

    /**
     * 分页查询资产
     */
    public IPage<AssetDTO> queryAssets(AssetQueryCriteria criteria) {
        Page<AssetPO> page = new Page<>(criteria.getPageNum(), criteria.getPageSize());
        IPage<AssetPO> result = assetMapper.selectPageWithCategory(
                page,
                criteria.getCategoryId(),
                criteria.getStatus(),
                criteria.getLocationType(),
                criteria.getLocationId(),
                criteria.getKeyword()
        );

        return result.convert(this::toAssetDTO);
    }

    /**
     * 获取资产详情
     */
    public AssetDTO getAsset(Long id) {
        AssetPO po = assetMapper.selectByIdWithCategory(id);
        if (po == null) {
            throw new BusinessException("资产不存在");
        }
        return toAssetDTO(po);
    }

    /**
     * 根据位置查询资产
     */
    public List<AssetDTO> getAssetsByLocation(String locationType, Long locationId) {
        return assetMapper.selectByLocation(locationType, locationId).stream()
                .map(this::toAssetDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资产变更历史
     */
    public List<AssetHistoryDTO> getAssetHistory(Long assetId) {
        return historyRepository.findByAssetId(assetId).stream()
                .map(this::toAssetHistoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资产维修记录
     */
    public List<AssetMaintenanceDTO> getAssetMaintenanceRecords(Long assetId) {
        Asset asset = assetRepository.findById(assetId).orElse(null);
        String assetCode = asset != null ? asset.getAssetCode() : null;
        String assetName = asset != null ? asset.getAssetName() : null;

        return maintenanceRepository.findByAssetId(assetId).stream()
                .map(m -> toMaintenanceDTO(m, assetCode, assetName))
                .collect(Collectors.toList());
    }

    /**
     * 获取资产统计
     */
    public AssetStatisticsDTO getStatistics() {
        return AssetStatisticsDTO.builder()
                .totalCount(assetRepository.countAll())
                .inUseCount(assetRepository.countByStatus(AssetStatus.IN_USE))
                .idleCount(assetRepository.countByStatus(AssetStatus.IDLE))
                .repairingCount(assetRepository.countByStatus(AssetStatus.REPAIRING))
                .scrappedCount(assetRepository.countByStatus(AssetStatus.SCRAPPED))
                .build();
    }

    /**
     * 删除资产
     */
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资产不存在"));

        if (asset.getStatus() == AssetStatus.IN_USE) {
            throw new BusinessException("在用资产不能删除");
        }

        assetRepository.delete(id);
        log.info("Deleted fixed asset: {}", id);
    }

    // ============ 转换方法 ============

    private AssetDTO toAssetDTO(AssetPO po) {
        // 计算管理模式相关信息
        Integer managementMode = po.getManagementMode() != null ? po.getManagementMode() : ManagementMode.SINGLE_ITEM.getCode();
        String managementModeDesc = ManagementMode.fromCode(managementMode).getDescription();
        boolean isBatchMode = managementMode == ManagementMode.BATCH.getCode();

        return AssetDTO.builder()
                .id(po.getId())
                .assetCode(po.getAssetCode())
                .assetName(po.getAssetName())
                .categoryId(po.getCategoryId())
                .categoryName(po.getCategoryName())
                .categoryCode(po.getCategoryCode())
                .brand(po.getBrand())
                .model(po.getModel())
                .unit(po.getUnit())
                .quantity(po.getQuantity())
                .originalValue(po.getOriginalValue())
                .netValue(po.getNetValue())
                .purchaseDate(po.getPurchaseDate())
                .warrantyDate(po.getWarrantyDate())
                .supplier(po.getSupplier())
                .status(po.getStatus())
                .statusDesc(po.getStatus() != null ? AssetStatus.fromCode(po.getStatus()).getDescription() : null)
                .managementMode(managementMode)
                .managementModeDesc(managementModeDesc)
                .availableQuantity(isBatchMode ? po.getQuantity() : null)
                .locationType(po.getLocationType())
                .locationTypeDesc(po.getLocationType() != null ?
                        LocationType.fromCode(po.getLocationType()).getDescription() : null)
                .locationId(po.getLocationId())
                .locationName(po.getLocationName())
                .responsibleUserId(po.getResponsibleUserId())
                .responsibleUserName(po.getResponsibleUserName())
                .remark(po.getRemark())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .underWarranty(po.getWarrantyDate() != null &&
                        po.getWarrantyDate().isAfter(java.time.LocalDate.now()))
                .build();
    }

    private AssetHistoryDTO toAssetHistoryDTO(AssetHistory history) {
        return AssetHistoryDTO.builder()
                .id(history.getId())
                .assetId(history.getAssetId())
                .changeType(history.getChangeType() != null ? history.getChangeType().getCode() : null)
                .changeTypeDesc(history.getChangeType() != null ? history.getChangeType().getDescription() : null)
                .changeContent(history.getChangeContent())
                .oldLocationType(history.getOldLocationType())
                .oldLocationId(history.getOldLocationId())
                .oldLocationName(history.getOldLocationName())
                .newLocationType(history.getNewLocationType())
                .newLocationId(history.getNewLocationId())
                .newLocationName(history.getNewLocationName())
                .operatorId(history.getOperatorId())
                .operatorName(history.getOperatorName())
                .operateTime(history.getOperateTime())
                .remark(history.getRemark())
                .build();
    }

    private AssetMaintenanceDTO toMaintenanceDTO(AssetMaintenance maintenance, String assetCode, String assetName) {
        return AssetMaintenanceDTO.builder()
                .id(maintenance.getId())
                .assetId(maintenance.getAssetId())
                .assetCode(assetCode)
                .assetName(assetName)
                .maintenanceType(maintenance.getMaintenanceType() != null ?
                        maintenance.getMaintenanceType().getCode() : null)
                .maintenanceTypeDesc(maintenance.getMaintenanceType() != null ?
                        maintenance.getMaintenanceType().getDescription() : null)
                .faultDesc(maintenance.getFaultDesc())
                .startDate(maintenance.getStartDate())
                .endDate(maintenance.getEndDate())
                .cost(maintenance.getCost())
                .maintainer(maintenance.getMaintainer())
                .result(maintenance.getResult())
                .status(maintenance.getStatus() != null ? maintenance.getStatus().getCode() : null)
                .statusDesc(maintenance.getStatus() != null ? maintenance.getStatus().getDescription() : null)
                .createdBy(maintenance.getCreatedBy())
                .createdAt(maintenance.getCreatedAt())
                .build();
    }
}
