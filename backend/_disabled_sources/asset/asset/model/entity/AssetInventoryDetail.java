package com.school.management.domain.asset.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资产盘点明细实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInventoryDetail implements Entity<Long> {

    public static final int RESULT_NORMAL = 1;
    public static final int RESULT_PROFIT = 2;
    public static final int RESULT_LOSS = 3;

    private Long inventoryId;
    private Long assetId;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private Integer resultType;
    private LocalDateTime checkTime;
    private Long checkerId;
    private String checkerName;
    private String remark;

    // 关联资产信息(非持久化字段)
    private String assetCode;
    private String assetName;
    private String locationName;

    /**
     * 创建盘点明细
     */
    public static AssetInventoryDetail create(Long inventoryId, Long assetId, Integer expectedQuantity) {
        AssetInventoryDetail detail = new AssetInventoryDetail();
        detail.setInventoryId(inventoryId);
        detail.setAssetId(assetId);
        detail.setExpectedQuantity(expectedQuantity);
        return detail;
    }

    /**
     * 执行盘点
     */
    public void check(Integer actualQuantity, Long checkerId, String checkerName, String remark) {
        this.actualQuantity = actualQuantity;
        this.difference = actualQuantity - expectedQuantity;
        this.checkTime = LocalDateTime.now();
        this.checkerId = checkerId;
        this.checkerName = checkerName;
        this.remark = remark;

        // 设置结果类型
        if (difference == 0) {
            this.resultType = RESULT_NORMAL;
        } else if (difference > 0) {
            this.resultType = RESULT_PROFIT;
        } else {
            this.resultType = RESULT_LOSS;
        }
    }

    /**
     * 检查是否已盘点
     */
    public boolean isChecked() {
        return actualQuantity != null;
    }

    /**
     * 获取结果描述
     */
    public String getResultDesc() {
        if (resultType == null) return "未盘点";
        return switch (resultType) {
            case RESULT_NORMAL -> "正常";
            case RESULT_PROFIT -> "盘盈";
            case RESULT_LOSS -> "盘亏";
            default -> "未知";
        };
    }
}
