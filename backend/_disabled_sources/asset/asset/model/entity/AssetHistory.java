package com.school.management.domain.asset.model.entity;

import com.school.management.domain.asset.model.valueobject.ChangeType;
import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 资产变更历史记录实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHistory implements Entity<Long> {

    private Long assetId;
    private ChangeType changeType;
    private String changeContent;
    private String oldLocationType;
    private Long oldLocationId;
    private String oldLocationName;
    private String newLocationType;
    private Long newLocationId;
    private String newLocationName;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
    private String remark;

    /**
     * 创建新增记录
     */
    public static AssetHistory createNew(Long assetId, Long operatorId, String operatorName, String content) {
        return AssetHistory.builder()
                .assetId(assetId)
                .changeType(ChangeType.CREATE)
                .changeContent(content)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建更新记录
     */
    public static AssetHistory createUpdate(Long assetId, Long operatorId, String operatorName, String content) {
        return AssetHistory.builder()
                .assetId(assetId)
                .changeType(ChangeType.UPDATE)
                .changeContent(content)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建调拨记录
     */
    public static AssetHistory createTransfer(Long assetId, Long operatorId, String operatorName,
                                               String oldLocationType, Long oldLocationId, String oldLocationName,
                                               String newLocationType, Long newLocationId, String newLocationName,
                                               String remark) {
        return AssetHistory.builder()
                .assetId(assetId)
                .changeType(ChangeType.TRANSFER)
                .oldLocationType(oldLocationType)
                .oldLocationId(oldLocationId)
                .oldLocationName(oldLocationName)
                .newLocationType(newLocationType)
                .newLocationId(newLocationId)
                .newLocationName(newLocationName)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operateTime(LocalDateTime.now())
                .remark(remark)
                .build();
    }

    /**
     * 创建维修记录
     */
    public static AssetHistory createRepair(Long assetId, Long operatorId, String operatorName, String remark) {
        return AssetHistory.builder()
                .assetId(assetId)
                .changeType(ChangeType.REPAIR)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operateTime(LocalDateTime.now())
                .remark(remark)
                .build();
    }

    /**
     * 创建报废记录
     */
    public static AssetHistory createScrap(Long assetId, Long operatorId, String operatorName, String remark) {
        return AssetHistory.builder()
                .assetId(assetId)
                .changeType(ChangeType.SCRAP)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .operateTime(LocalDateTime.now())
                .remark(remark)
                .build();
    }
}
