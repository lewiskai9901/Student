package com.school.management.application.asset;

import com.school.management.application.asset.query.AssetAlertDTO;
import com.school.management.domain.asset.model.entity.AssetAlert;
import com.school.management.domain.asset.model.valueobject.AlertLevel;
import com.school.management.domain.asset.model.valueobject.AlertType;
import com.school.management.domain.asset.repository.AssetAlertRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产预警应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAlertApplicationService {

    private final AssetAlertRepository alertRepository;

    /**
     * 创建预警
     */
    @Transactional
    public Long createAlert(
            AlertType alertType,
            Long assetId,
            String assetCode,
            String assetName,
            String alertContent,
            AlertLevel alertLevel
    ) {
        // 检查是否已存在同类型预警
        if (alertRepository.existsByAssetIdAndType(assetId, alertType)) {
            log.debug("Alert already exists for asset {} type {}", assetId, alertType);
            return null;
        }

        AssetAlert alert = AssetAlert.create(
                alertType,
                assetId,
                assetCode,
                assetName,
                alertContent,
                alertLevel
        );

        alertRepository.save(alert);
        log.info("Created alert: {} for asset {}", alertType, assetId);
        return alert.getId();
    }

    /**
     * 创建借用相关预警
     */
    @Transactional
    public Long createBorrowAlert(
            AlertType alertType,
            Long assetId,
            String assetCode,
            String assetName,
            Long borrowId,
            String alertContent,
            AlertLevel alertLevel,
            Long notifyUserId,
            String notifyUserName
    ) {
        // 检查是否已存在同类型预警
        if (alertRepository.existsByBorrowIdAndType(borrowId, alertType)) {
            log.debug("Alert already exists for borrow {} type {}", borrowId, alertType);
            return null;
        }

        AssetAlert alert = AssetAlert.createBorrowAlert(
                alertType,
                assetId,
                assetCode,
                assetName,
                borrowId,
                alertContent,
                alertLevel,
                notifyUserId,
                notifyUserName
        );

        alertRepository.save(alert);
        log.info("Created borrow alert: {} for borrow {}", alertType, borrowId);
        return alert.getId();
    }

    /**
     * 标记为已读
     */
    @Transactional
    public void markAsRead(Long alertId) {
        AssetAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("预警不存在"));
        alert.markAsRead();
        alertRepository.save(alert);
    }

    /**
     * 批量标记为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<AssetAlert> alerts = alertRepository.findUnreadByUserId(userId);
        for (AssetAlert alert : alerts) {
            alert.markAsRead();
            alertRepository.save(alert);
        }
    }

    /**
     * 处理预警
     */
    @Transactional
    public void handleAlert(Long alertId, Long handlerId, String handlerName, String remark) {
        AssetAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("预警不存在"));
        alert.handle(handlerId, handlerName, remark);
        alertRepository.save(alert);
        log.info("Alert {} handled by {}", alertId, handlerName);
    }

    /**
     * 获取预警详情
     */
    public AssetAlertDTO getAlert(Long id) {
        return alertRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("预警不存在"));
    }

    /**
     * 获取用户未读预警
     */
    public List<AssetAlertDTO> getUnreadAlerts(Long userId) {
        return alertRepository.findUnreadByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户未处理预警
     */
    public List<AssetAlertDTO> getUnhandledAlerts(Long userId) {
        return alertRepository.findUnhandledByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询预警
     */
    public List<AssetAlertDTO> queryAlerts(
            Integer alertType,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId,
            int pageNum,
            int pageSize
    ) {
        AlertType type = alertType != null ? AlertType.fromCode(alertType) : null;
        int offset = (pageNum - 1) * pageSize;
        return alertRepository.findByCondition(type, isRead, isHandled, notifyUserId, offset, pageSize)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 统计数量
     */
    public int countAlerts(
            Integer alertType,
            Boolean isRead,
            Boolean isHandled,
            Long notifyUserId
    ) {
        AlertType type = alertType != null ? AlertType.fromCode(alertType) : null;
        return alertRepository.countByCondition(type, isRead, isHandled, notifyUserId);
    }

    /**
     * 获取预警统计
     */
    public Map<String, Integer> getAlertStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("unhandledCount", alertRepository.countUnhandled());
        stats.put("overdueCount", alertRepository.countUnhandledByType(AlertType.OVERDUE));
        stats.put("nearOverdueCount", alertRepository.countUnhandledByType(AlertType.NEAR_OVERDUE));
        stats.put("warrantyExpireCount", alertRepository.countUnhandledByType(AlertType.WARRANTY_EXPIRE));
        stats.put("lowStockCount", alertRepository.countUnhandledByType(AlertType.LOW_STOCK));
        return stats;
    }

    /**
     * 获取用户未读数量
     */
    public int countUnread(Long userId) {
        return alertRepository.countUnreadByUserId(userId);
    }

    // ============ 转换方法 ============

    private AssetAlertDTO toDTO(AssetAlert alert) {
        return AssetAlertDTO.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType().getCode())
                .alertTypeDesc(alert.getAlertType().getDescription())
                .assetId(alert.getAssetId())
                .assetCode(alert.getAssetCode())
                .assetName(alert.getAssetName())
                .borrowId(alert.getBorrowId())
                .alertContent(alert.getAlertContent())
                .alertLevel(alert.getAlertLevel().getCode())
                .alertLevelDesc(alert.getAlertLevel().getDescription())
                .isRead(alert.isRead())
                .isHandled(alert.isHandled())
                .handleRemark(alert.getHandleRemark())
                .handleTime(alert.getHandleTime())
                .handlerId(alert.getHandlerId())
                .handlerName(alert.getHandlerName())
                .notifyUserId(alert.getNotifyUserId())
                .notifyUserName(alert.getNotifyUserName())
                .alertTime(alert.getAlertTime())
                .expireTime(alert.getExpireTime())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
