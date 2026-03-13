package com.school.management.domain.space.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间预订记录实体
 * 记录空间的预订信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSpaceBooking implements Entity<Long> {

    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 预订人ID
     */
    private Long bookerId;

    /**
     * 预订人名称（冗余字段）
     */
    private String bookerName;

    /**
     * 预订标题
     */
    private String title;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 参与人ID列表
     */
    private List<Long> attendeeIds;

    /**
     * 状态：0-已取消 1-待使用 2-使用中 3-已完成
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 备注
     */
    private String remark;

    // ==================== 状态常量 ====================

    public static final int STATUS_CANCELLED = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_IN_USE = 2;
    public static final int STATUS_COMPLETED = 3;

    // ==================== 业务方法 ====================

    /**
     * 是否待使用
     */
    public boolean isPending() {
        return status != null && status == STATUS_PENDING;
    }

    /**
     * 是否使用中
     */
    public boolean isInUse() {
        return status != null && status == STATUS_IN_USE;
    }

    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return status != null && status == STATUS_COMPLETED;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return status != null && status == STATUS_CANCELLED;
    }

    /**
     * 是否有效（未取消且未完成）
     */
    public boolean isActive() {
        return status != null && (status == STATUS_PENDING || status == STATUS_IN_USE);
    }

    /**
     * 取消预订
     */
    public void cancel() {
        if (status == STATUS_IN_USE) {
            throw new IllegalStateException("使用中的预订不能取消");
        }
        if (status == STATUS_COMPLETED) {
            throw new IllegalStateException("已完成的预订不能取消");
        }
        this.status = STATUS_CANCELLED;
    }

    /**
     * 开始使用
     */
    public void startUse() {
        if (status != STATUS_PENDING) {
            throw new IllegalStateException("只有待使用的预订才能开始使用");
        }
        this.status = STATUS_IN_USE;
    }

    /**
     * 完成使用
     */
    public void complete() {
        if (status != STATUS_IN_USE && status != STATUS_PENDING) {
            throw new IllegalStateException("只有待使用或使用中的预订才能完成");
        }
        this.status = STATUS_COMPLETED;
    }

    /**
     * 判断是否与另一个时间段冲突
     */
    public boolean conflictsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        if (!isActive()) {
            return false;
        }
        // 如果一个时段的结束在另一个时段开始之前，则不冲突
        return !(endTime.isBefore(otherStart) || endTime.equals(otherStart) ||
                 startTime.isAfter(otherEnd) || startTime.equals(otherEnd));
    }

    /**
     * 判断是否与另一个预订冲突
     */
    public boolean conflictsWith(UniversalSpaceBooking other) {
        if (other == null || !other.isActive()) {
            return false;
        }
        return conflictsWith(other.getStartTime(), other.getEndTime());
    }

    /**
     * 获取预订时长（分钟）
     */
    public long getDurationMinutes() {
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 获取预订时长（小时）
     */
    public double getDurationHours() {
        return getDurationMinutes() / 60.0;
    }

    /**
     * 判断当前时间是否在预订时段内
     */
    public boolean isCurrentlyInTimeSlot() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建预订
     */
    public static UniversalSpaceBooking create(Long spaceId, Long bookerId, String bookerName,
                                                String title, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        return UniversalSpaceBooking.builder()
                .spaceId(spaceId)
                .bookerId(bookerId)
                .bookerName(bookerName)
                .title(title)
                .startTime(startTime)
                .endTime(endTime)
                .status(STATUS_PENDING)
                .build();
    }
}
