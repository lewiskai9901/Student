package com.school.management.infrastructure.extension.plugins.core.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 场所预订时间冲突阻断策略 (BLOCK).
 *
 * <p>语义:BEFORE_CREATE place_booking 阶段, 校验同一 place 在请求时间段
 * 内没有其他有效预订. 时间区间重叠 → 阻断.
 *
 * <p>payload 约定: Map 含
 * <ul>
 *   <li>placeId (Long)</li>
 *   <li>startTime (LocalDateTime)</li>
 *   <li>endTime (LocalDateTime)</li>
 *   <li>excludeBookingId (Long, 可选): 更新场景下排除自己</li>
 * </ul>
 */
@Slf4j
@Component
public class BookingTimeConflictPolicy implements Policy<Object> {

    private final JdbcTemplate jdbc;

    public BookingTimeConflictPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public String code() { return "BOOKING_TIME_CONFLICT"; }
    @Override public String name() { return "场所预订时间冲突"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "place".equals(ctx.entityType())
            && ("BEFORE_CREATE_BOOKING".equals(ctx.phase())
                || "BEFORE_UPDATE_BOOKING".equals(ctx.phase()));
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (!(ctx.payload() instanceof Map<?, ?> m)) return List.of();
        Object placeIdObj = m.get("placeId");
        Object startObj = m.get("startTime");
        Object endObj = m.get("endTime");
        if (!(placeIdObj instanceof Long placeId)
            || !(startObj instanceof LocalDateTime startTime)
            || !(endObj instanceof LocalDateTime endTime)) {
            return List.of();
        }
        if (!endTime.isAfter(startTime)) {
            return List.of(Violation.block(code(),
                "预订结束时间必须晚于开始时间"));
        }

        Long excludeId = m.get("excludeBookingId") instanceof Long lid ? lid : null;

        try {
            String sql = "SELECT COUNT(*) FROM place_bookings " +
                "WHERE deleted = 0 AND place_id = ? AND status IN ('PENDING','CONFIRMED','IN_USE') " +
                "  AND start_time < ? AND end_time > ?" +
                (excludeId != null ? " AND id != ?" : "");

            Integer conflictCount;
            if (excludeId != null) {
                conflictCount = jdbc.queryForObject(sql, Integer.class,
                    placeId, endTime, startTime, excludeId);
            } else {
                conflictCount = jdbc.queryForObject(sql, Integer.class,
                    placeId, endTime, startTime);
            }

            if (conflictCount != null && conflictCount > 0) {
                return List.of(Violation.block(code(),
                    String.format("场所 %d 在 %s 至 %s 时间段已有 %d 个有效预订, 冲突",
                        placeId, startTime, endTime, conflictCount)));
            }
        } catch (Exception e) {
            log.warn("[BookingTimeConflictPolicy] check failed: {}", e.getMessage());
        }
        return List.of();
    }
}
