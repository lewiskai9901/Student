package com.school.management.infrastructure.event;

import com.school.management.domain.place.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 场所统计事件监听器
 * 监听领域事件并更新统计数据
 * 对标: AWS CloudWatch Metrics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceStatisticsEventListener {

    // TODO: 注入统计服务（如需实时统计）

    /**
     * 监听场所组织分配事件
     * 可用于统计组织变更频率
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaceOrgAssigned(PlaceOrgAssignedEvent event) {
        try {
            // TODO: 更新组织变更统计
            log.debug("统计：场所组织分配 placeId={}, newOrgId={}",
                    event.getPlaceId(), event.getNewOrgUnitId());
        } catch (Exception e) {
            log.error("更新组织变更统计失败", e);
        }
    }

    /**
     * 监听场所状态变更事件
     * 可用于统计状态分布
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaceStatusChanged(PlaceStatusChangedEvent event) {
        try {
            // TODO: 更新状态分布统计
            log.debug("统计：场所状态变更 placeId={}, newStatus={}",
                    event.getPlaceId(), event.getNewStatus());
        } catch (Exception e) {
            log.error("更新状态统计失败", e);
        }
    }

    /**
     * 监听场所容量更新事件
     * 可用于统计容量趋势
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaceCapacityUpdated(PlaceCapacityUpdatedEvent event) {
        try {
            // TODO: 更新容量趋势统计
            log.debug("统计：场所容量更新 placeId={}, occupancy={}->{}, rate={}%",
                    event.getPlaceId(), event.getOldOccupancy(), event.getNewOccupancy(),
                    event.getNewOccupancyRate());

            // 如果触发预警，可以发送通知
            if (event.isAlertTriggered()) {
                // TODO: 发送容量预警通知（邮件、钉钉、短信等）
                log.warn("容量预警：{} 占用率达到 {}%",
                        event.getPlaceName(), event.getNewOccupancyRate());
            }
        } catch (Exception e) {
            log.error("更新容量统计失败", e);
        }
    }
}
