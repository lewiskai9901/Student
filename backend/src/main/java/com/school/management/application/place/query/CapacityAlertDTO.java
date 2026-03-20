package com.school.management.application.place.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 容量告警DTO
 * 对标: AWS CloudWatch Alarm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAlertDTO {

    /**
     * 场所ID
     */
    private Long placeId;

    /**
     * 场所编码
     */
    private String placeCode;

    /**
     * 场所名称
     */
    private String placeName;

    /**
     * 场所类型编码
     */
    private String typeCode;

    /**
     * 场所类型名称
     */
    private String typeName;

    /**
     * 总容量
     */
    private Integer capacity;

    /**
     * 当前占用数
     */
    private Integer currentOccupancy;

    /**
     * 占用率（百分比）
     */
    private Double occupancyRate;

    /**
     * 告警级别
     * - WARNING: 80% <= occupancyRate < 95%
     * - CRITICAL: occupancyRate >= 95%
     * - FULL: occupancyRate = 100%
     */
    private AlertLevel alertLevel;

    /**
     * 所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 所属组织单元名称
     */
    private String orgUnitName;

    /**
     * 负责人ID
     */
    private Long responsibleUserId;

    /**
     * 负责人姓名
     */
    private String responsibleUserName;

    /**
     * 首次告警时间
     */
    private LocalDateTime firstAlertTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdatedAt;

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        WARNING,    // 警告（80-94%）
        CRITICAL,   // 严重（95-99%）
        FULL        // 已满（100%）
    }

    /**
     * 计算告警级别
     */
    public static AlertLevel calculateAlertLevel(double occupancyRate) {
        if (occupancyRate >= 100.0) {
            return AlertLevel.FULL;
        } else if (occupancyRate >= 95.0) {
            return AlertLevel.CRITICAL;
        } else if (occupancyRate >= 80.0) {
            return AlertLevel.WARNING;
        } else {
            return null; // 无告警
        }
    }
}
