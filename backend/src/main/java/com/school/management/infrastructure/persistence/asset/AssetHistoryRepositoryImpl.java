package com.school.management.infrastructure.persistence.asset;

import com.school.management.domain.asset.model.entity.AssetHistory;
import com.school.management.domain.asset.model.valueobject.ChangeType;
import com.school.management.domain.asset.repository.AssetHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产变更记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class AssetHistoryRepositoryImpl implements AssetHistoryRepository {

    private final AssetHistoryMapper historyMapper;

    @Override
    public AssetHistory save(AssetHistory history) {
        AssetHistoryPO po = toPO(history);
        historyMapper.insert(po);
        history.setId(po.getId());
        return history;
    }

    @Override
    public List<AssetHistory> findByAssetId(Long assetId) {
        return historyMapper.selectByAssetId(assetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetHistory> findByAssetIdAndChangeType(Long assetId, ChangeType changeType) {
        return historyMapper.selectByAssetIdAndChangeType(assetId, changeType.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssetHistory> findRecentByAssetId(Long assetId, int limit) {
        return historyMapper.selectRecentByAssetId(assetId, limit).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ============ 转换方法 ============

    private AssetHistory toDomain(AssetHistoryPO po) {
        if (po == null) return null;

        AssetHistory history = AssetHistory.builder()
                .assetId(po.getAssetId())
                .changeType(po.getChangeType() != null ? ChangeType.fromCode(po.getChangeType()) : null)
                .changeContent(po.getChangeContent())
                .oldLocationType(po.getOldLocationType())
                .oldLocationId(po.getOldLocationId())
                .oldLocationName(po.getOldLocationName())
                .newLocationType(po.getNewLocationType())
                .newLocationId(po.getNewLocationId())
                .newLocationName(po.getNewLocationName())
                .operatorId(po.getOperatorId())
                .operatorName(po.getOperatorName())
                .operateTime(po.getOperateTime())
                .remark(po.getRemark())
                .build();
        history.setId(po.getId());
        return history;
    }

    private AssetHistoryPO toPO(AssetHistory history) {
        return AssetHistoryPO.builder()
                .id(history.getId())
                .assetId(history.getAssetId())
                .changeType(history.getChangeType() != null ? history.getChangeType().getCode() : null)
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
}
