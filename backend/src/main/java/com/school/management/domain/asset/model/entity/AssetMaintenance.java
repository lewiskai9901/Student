package com.school.management.domain.asset.model.entity;

import com.school.management.domain.asset.model.valueobject.MaintenanceStatus;
import com.school.management.domain.asset.model.valueobject.MaintenanceType;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产维修保养记录实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMaintenance extends Entity<Long> {

    private Long assetId;
    private MaintenanceType maintenanceType;
    private String faultDesc;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal cost;
    private String maintainer;
    private String result;
    private MaintenanceStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建维修记录
     */
    public static AssetMaintenance createRepair(Long assetId, String faultDesc) {
        AssetMaintenance maintenance = new AssetMaintenance();
        maintenance.setAssetId(assetId);
        maintenance.setMaintenanceType(MaintenanceType.REPAIR);
        maintenance.setFaultDesc(faultDesc);
        maintenance.setStartDate(LocalDate.now());
        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setCreatedAt(LocalDateTime.now());
        maintenance.setUpdatedAt(LocalDateTime.now());
        return maintenance;
    }

    /**
     * 创建保养记录
     */
    public static AssetMaintenance createMaintenance(Long assetId, String description) {
        AssetMaintenance maintenance = new AssetMaintenance();
        maintenance.setAssetId(assetId);
        maintenance.setMaintenanceType(MaintenanceType.MAINTENANCE);
        maintenance.setFaultDesc(description);
        maintenance.setStartDate(LocalDate.now());
        maintenance.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenance.setCreatedAt(LocalDateTime.now());
        maintenance.setUpdatedAt(LocalDateTime.now());
        return maintenance;
    }

    /**
     * 完成维修/保养
     */
    public void complete(String result, BigDecimal cost) {
        this.result = result;
        this.cost = cost;
        this.endDate = LocalDate.now();
        this.status = MaintenanceStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否进行中
     */
    public boolean isInProgress() {
        return status == MaintenanceStatus.IN_PROGRESS;
    }
}
