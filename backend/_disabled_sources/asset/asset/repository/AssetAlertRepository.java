package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.entity.AssetAlert;
import com.school.management.domain.asset.model.valueobject.AlertType;

import java.util.List;
import java.util.Optional;

/**
 * 资产预警仓储接口
 */
public interface AssetAlertRepository {

    void save(AssetAlert alert);

    Optional<AssetAlert> findById(Long id);

    /**
     * 查找用户的未读预警
     */
    List<AssetAlert> findUnreadByUserId(Long userId);

    /**
     * 查找用户的未处理预警
     */
    List<AssetAlert> findUnhandledByUserId(Long userId);

    /**
     * 查找资产的预警
     */
    List<AssetAlert> findByAssetId(Long assetId);

    /**
     * 查找借用记录的预警
     */
    List<AssetAlert> findByBorrowId(Long borrowId);

    /**
     * 检查是否已存在同类型预警（避免重复）
     */
    boolean existsByAssetIdAndType(Long assetId, AlertType type);

    /**
     * 检查是否已存在借用相关预警
     */
    boolean existsByBorrowIdAndType(Long borrowId, AlertType type);

    /**
     * 统计未读数量
     */
    int countUnreadByUserId(Long userId);

    /**
     * 统计未处理数量
     */
    int countUnhandled();

    /**
     * 按类型统计未处理数量
     */
    int countUnhandledByType(AlertType type);

    /**
     * 分页查询
     */
    List<AssetAlert> findByCondition(
            AlertType type,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId,
            int offset,
            int limit
    );

    /**
     * 条件统计
     */
    int countByCondition(
            AlertType type,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId
    );
}
