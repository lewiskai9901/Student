package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.UniversalSpaceBooking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 空间预订记录仓储接口
 */
public interface UniversalSpaceBookingRepository {

    /**
     * 保存预订记录
     */
    UniversalSpaceBooking save(UniversalSpaceBooking booking);

    /**
     * 根据ID查询
     */
    Optional<UniversalSpaceBooking> findById(Long id);

    /**
     * 查询空间的所有预订
     */
    List<UniversalSpaceBooking> findBySpaceId(Long spaceId);

    /**
     * 查询空间在指定时间范围内的预订
     */
    List<UniversalSpaceBooking> findBySpaceIdAndTimeRange(Long spaceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询空间的活跃预订（待使用和使用中）
     */
    List<UniversalSpaceBooking> findActiveBySpaceId(Long spaceId);

    /**
     * 查询用户的预订
     */
    List<UniversalSpaceBooking> findByBookerId(Long bookerId);

    /**
     * 查询用户的活跃预订
     */
    List<UniversalSpaceBooking> findActiveByBookerId(Long bookerId);

    /**
     * 检查时间段是否有冲突
     */
    boolean hasConflict(Long spaceId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId);

    /**
     * 检查时间段是否有冲突（不排除任何预订）
     */
    default boolean hasConflict(Long spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        return hasConflict(spaceId, startTime, endTime, null);
    }

    /**
     * 根据ID删除
     */
    void deleteById(Long id);

    /**
     * 查询需要自动完成的预订（结束时间已过但状态还是待使用或使用中）
     */
    List<UniversalSpaceBooking> findExpiredBookings(LocalDateTime beforeTime);

    /**
     * 批量更新状态
     */
    void batchUpdateStatus(List<Long> ids, int status);

    /**
     * 分页查询
     */
    List<UniversalSpaceBooking> findPage(BookingQueryCriteria criteria, int page, int size);

    /**
     * 统计符合条件的数量
     */
    long count(BookingQueryCriteria criteria);

    /**
     * 查询条件
     */
    class BookingQueryCriteria {
        private Long spaceId;
        private Long bookerId;
        private Integer status;
        private LocalDateTime startTimeFrom;
        private LocalDateTime startTimeTo;

        // Getters and Setters
        public Long getSpaceId() { return spaceId; }
        public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
        public Long getBookerId() { return bookerId; }
        public void setBookerId(Long bookerId) { this.bookerId = bookerId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public LocalDateTime getStartTimeFrom() { return startTimeFrom; }
        public void setStartTimeFrom(LocalDateTime startTimeFrom) { this.startTimeFrom = startTimeFrom; }
        public LocalDateTime getStartTimeTo() { return startTimeTo; }
        public void setStartTimeTo(LocalDateTime startTimeTo) { this.startTimeTo = startTimeTo; }
    }
}
