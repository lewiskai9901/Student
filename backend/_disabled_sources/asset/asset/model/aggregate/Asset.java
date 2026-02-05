package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.model.valueobject.AssetLocation;
import com.school.management.domain.asset.model.valueobject.AssetStatus;
import com.school.management.domain.asset.model.valueobject.LocationType;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.shared.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产聚合根
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset extends AggregateRoot<Long> {

    private String assetCode;
    private String assetName;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String model;
    private String unit;
    private Integer quantity;
    private BigDecimal originalValue;
    private BigDecimal netValue;
    private LocalDate purchaseDate;
    private LocalDate warrantyDate;
    private String supplier;
    private AssetStatus status;
    private ManagementMode managementMode;

    // 位置信息
    private AssetLocation location;

    // 责任人
    private Long responsibleUserId;
    private String responsibleUserName;

    // 折旧相关
    private Integer categoryType;
    private Integer depreciationMethod;
    private BigDecimal residualValue;
    private BigDecimal accumulatedDepreciation;
    private Integer usefulLife; // 使用年限（年）
    private Integer stockWarningThreshold; // 库存预警阈值

    // 其他
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建新资产
     */
    public static Asset create(String assetCode, String assetName, Long categoryId, String unit,
                               Integer quantity, ManagementMode managementMode) {
        Asset asset = new Asset();
        asset.setAssetCode(assetCode);
        asset.setAssetName(assetName);
        asset.setCategoryId(categoryId);
        asset.setUnit(unit);
        asset.setManagementMode(managementMode != null ? managementMode : ManagementMode.SINGLE_ITEM);
        // 单品管理模式下数量固定为1
        if (asset.getManagementMode() == ManagementMode.SINGLE_ITEM) {
            asset.setQuantity(1);
        } else {
            asset.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
        }
        asset.setStatus(AssetStatus.IDLE);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        return asset;
    }

    /**
     * 创建新资产（兼容旧方法）
     */
    public static Asset create(String assetCode, String assetName, Long categoryId, String unit, Integer quantity) {
        return create(assetCode, assetName, categoryId, unit, quantity, ManagementMode.SINGLE_ITEM);
    }

    /**
     * 调拨资产到新位置
     */
    public void transfer(LocationType locationType, Long locationId, String locationName,
                        Long responsibleUserId, String responsibleUserName) {
        this.location = AssetLocation.of(locationType, locationId, locationName);
        this.responsibleUserId = responsibleUserId;
        this.responsibleUserName = responsibleUserName;
        this.status = AssetStatus.IN_USE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为维修中
     */
    public void markAsRepairing() {
        this.status = AssetStatus.REPAIRING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为闲置
     */
    public void markAsIdle() {
        this.status = AssetStatus.IDLE;
        this.location = AssetLocation.empty();
        this.responsibleUserId = null;
        this.responsibleUserName = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 报废资产
     */
    public void scrap() {
        this.status = AssetStatus.SCRAPPED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成维修
     */
    public void completeRepair() {
        if (this.location != null && !this.location.isEmpty()) {
            this.status = AssetStatus.IN_USE;
        } else {
            this.status = AssetStatus.IDLE;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否在保修期内
     */
    public boolean isUnderWarranty() {
        return warrantyDate != null && warrantyDate.isAfter(LocalDate.now());
    }

    /**
     * 检查是否可以调拨
     */
    public boolean canTransfer() {
        return status != AssetStatus.SCRAPPED && status != AssetStatus.REPAIRING;
    }

    /**
     * 检查是否可以报废
     */
    public boolean canScrap() {
        return status != AssetStatus.SCRAPPED;
    }

    /**
     * 是否为批量管理模式
     */
    public boolean isBatchManagement() {
        return managementMode == ManagementMode.BATCH;
    }

    /**
     * 是否为单品管理模式
     */
    public boolean isSingleItemManagement() {
        return managementMode == null || managementMode == ManagementMode.SINGLE_ITEM;
    }

    /**
     * 获取可用库存数量
     */
    public int getAvailableQuantity() {
        return quantity != null ? quantity : 0;
    }

    /**
     * 检查是否有足够的库存
     */
    public boolean hasEnoughStock(int requiredQuantity) {
        if (isSingleItemManagement()) {
            return requiredQuantity == 1 && status == AssetStatus.IDLE;
        }
        return getAvailableQuantity() >= requiredQuantity;
    }

    /**
     * 扣减库存（用于批量资产的领用）
     *
     * @param deductAmount 扣减数量
     * @throws IllegalStateException 如果不是批量管理模式或库存不足
     */
    public void deductQuantity(int deductAmount) {
        if (deductAmount <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于0");
        }
        if (isSingleItemManagement()) {
            throw new IllegalStateException("单品管理模式不支持库存扣减操作");
        }
        if (getAvailableQuantity() < deductAmount) {
            throw new IllegalStateException(
                String.format("库存不足，当前库存: %d, 需要: %d", getAvailableQuantity(), deductAmount)
            );
        }
        this.quantity = this.quantity - deductAmount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 恢复库存（用于批量资产的归还）
     *
     * @param restoreAmount 恢复数量
     * @throws IllegalStateException 如果不是批量管理模式
     */
    public void restoreQuantity(int restoreAmount) {
        if (restoreAmount <= 0) {
            throw new IllegalArgumentException("恢复数量必须大于0");
        }
        if (isSingleItemManagement()) {
            throw new IllegalStateException("单品管理模式不支持库存恢复操作");
        }
        this.quantity = (this.quantity != null ? this.quantity : 0) + restoreAmount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否可以被借用/领用
     */
    public boolean canBorrow() {
        if (status == AssetStatus.SCRAPPED || status == AssetStatus.REPAIRING) {
            return false;
        }
        if (isSingleItemManagement()) {
            return status == AssetStatus.IDLE;
        }
        return getAvailableQuantity() > 0;
    }

    /**
     * 检查指定数量是否可以被借用/领用
     */
    public boolean canBorrow(int quantity) {
        if (status == AssetStatus.SCRAPPED || status == AssetStatus.REPAIRING) {
            return false;
        }
        if (isSingleItemManagement()) {
            return quantity == 1 && status == AssetStatus.IDLE;
        }
        return hasEnoughStock(quantity);
    }
}
