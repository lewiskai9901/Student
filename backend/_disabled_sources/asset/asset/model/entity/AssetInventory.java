package com.school.management.domain.asset.model.entity;

import com.school.management.domain.asset.model.valueobject.InventoryStatus;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产盘点实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInventory implements Entity<Long> {

    private String inventoryCode;
    private String inventoryName;
    private String scopeType;
    private String scopeValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private InventoryStatus status;
    private Integer totalCount;
    private Integer checkedCount;
    private Integer profitCount;
    private Integer lossCount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 盘点明细(非持久化字段)
    @Builder.Default
    private List<AssetInventoryDetail> details = new ArrayList<>();

    /**
     * 创建盘点任务
     */
    public static AssetInventory create(String inventoryCode, String inventoryName,
                                         String scopeType, String scopeValue,
                                         LocalDate startDate, LocalDate endDate,
                                         Integer totalCount, Long createdBy) {
        AssetInventory inventory = new AssetInventory();
        inventory.setInventoryCode(inventoryCode);
        inventory.setInventoryName(inventoryName);
        inventory.setScopeType(scopeType);
        inventory.setScopeValue(scopeValue);
        inventory.setStartDate(startDate);
        inventory.setEndDate(endDate);
        inventory.setStatus(InventoryStatus.IN_PROGRESS);
        inventory.setTotalCount(totalCount);
        inventory.setCheckedCount(0);
        inventory.setProfitCount(0);
        inventory.setLossCount(0);
        inventory.setCreatedBy(createdBy);
        inventory.setCreatedAt(LocalDateTime.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        return inventory;
    }

    /**
     * 完成盘点
     */
    public void complete(int checkedCount, int profitCount, int lossCount) {
        this.checkedCount = checkedCount;
        this.profitCount = profitCount;
        this.lossCount = lossCount;
        this.status = InventoryStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消盘点
     */
    public void cancel() {
        this.status = InventoryStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否可编辑
     */
    public boolean isEditable() {
        return status == InventoryStatus.IN_PROGRESS;
    }

    /**
     * 更新盘点进度
     */
    public void updateProgress(int checkedCount, int profitCount, int lossCount) {
        this.checkedCount = checkedCount;
        this.profitCount = profitCount;
        this.lossCount = lossCount;
        this.updatedAt = LocalDateTime.now();
    }
}
