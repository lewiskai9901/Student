package com.school.management.infrastructure.persistence.asset;

import com.school.management.domain.asset.model.entity.AssetMaintenance;
import com.school.management.domain.asset.model.valueobject.MaintenanceStatus;
import com.school.management.domain.asset.model.valueobject.MaintenanceType;
import com.school.management.domain.asset.repository.AssetMaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产维修保养仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetMaintenanceRepositoryImpl implements AssetMaintenanceRepository {

    private final AssetMaintenanceMapper maintenanceMapper;

    @Override
    public AssetMaintenance save(AssetMaintenance maintenance) {
        AssetMaintenancePO po = toPO(maintenance);
        if (po.getId() == null) {
            maintenanceMapper.insert(po);
        } else {
            maintenanceMapper.updateById(po);
        }
        maintenance.setId(po.getId());
        return maintenance;
    }

    @Override
    public Optional<AssetMaintenance> findById(Long id) {
        AssetMaintenancePO po = maintenanceMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<AssetMaintenance> findByAssetId(Long assetId) {
        return maintenanceMapper.selectByAssetId(assetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetMaintenance> findByStatus(MaintenanceStatus status) {
        return maintenanceMapper.selectByStatus(status.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AssetMaintenance> findInProgressByAssetId(Long assetId) {
        AssetMaintenancePO po = maintenanceMapper.selectInProgressByAssetId(assetId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    // ============ 转换方法 ============

    private AssetMaintenance toDomain(AssetMaintenancePO po) {
        if (po == null) return null;

        AssetMaintenance maintenance = AssetMaintenance.builder()
                .assetId(po.getAssetId())
                .maintenanceType(po.getMaintenanceType() != null ?
                        MaintenanceType.fromCode(po.getMaintenanceType()) : null)
                .faultDesc(po.getFaultDesc())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .cost(po.getCost())
                .maintainer(po.getMaintainer())
                .result(po.getResult())
                .status(po.getStatus() != null ?
                        MaintenanceStatus.fromCode(po.getStatus()) : null)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
        maintenance.setId(po.getId());
        return maintenance;
    }

    private AssetMaintenancePO toPO(AssetMaintenance maintenance) {
        return AssetMaintenancePO.builder()
                .id(maintenance.getId())
                .assetId(maintenance.getAssetId())
                .maintenanceType(maintenance.getMaintenanceType() != null ?
                        maintenance.getMaintenanceType().getCode() : null)
                .faultDesc(maintenance.getFaultDesc())
                .startDate(maintenance.getStartDate())
                .endDate(maintenance.getEndDate())
                .cost(maintenance.getCost())
                .maintainer(maintenance.getMaintainer())
                .result(maintenance.getResult())
                .status(maintenance.getStatus() != null ?
                        maintenance.getStatus().getCode() : null)
                .createdBy(maintenance.getCreatedBy())
                .createdAt(maintenance.getCreatedAt())
                .updatedAt(maintenance.getUpdatedAt())
                .build();
    }
}
