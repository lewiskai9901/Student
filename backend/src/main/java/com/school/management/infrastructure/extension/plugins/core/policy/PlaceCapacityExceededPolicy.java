package com.school.management.infrastructure.extension.plugins.core.policy;

import com.school.management.infrastructure.extension.Policy;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 场所入住容量超限阻断策略 (BLOCK).
 *
 * <p>语义:BEFORE_CHECKIN 阶段, 校验场所当前入住人数 + 即将入住人数 <= capacity.
 * 超过 capacity → 阻断, 抛 PolicyViolationException.
 *
 * <p>与 MinOccupantsPolicy (WARN) 形成上下界: 入住人数应在 [min, capacity] 区间.
 *
 * <p>payload 约定: Map 含
 * <ul>
 *   <li>placeId (Long): 必须</li>
 *   <li>incomingCount (Integer): 即将入住人数, 默认 1</li>
 * </ul>
 */
@Slf4j
@Component
public class PlaceCapacityExceededPolicy implements Policy<Object> {

    private final JdbcTemplate jdbc;

    public PlaceCapacityExceededPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public String code() { return "PLACE_CAPACITY_EXCEEDED"; }
    @Override public String name() { return "场所容量超限"; }

    @Override
    public boolean supports(PolicyContext<?> ctx) {
        return "place".equals(ctx.entityType()) && "BEFORE_CHECKIN".equals(ctx.phase());
    }

    @Override
    public List<Violation> check(PolicyContext<Object> ctx) {
        if (!(ctx.payload() instanceof Map<?, ?> m)) {
            return List.of();
        }
        Object placeIdObj = m.get("placeId");
        if (!(placeIdObj instanceof Long placeId)) {
            return List.of();
        }
        int incoming = m.get("incomingCount") instanceof Number n ? n.intValue() : 1;

        try {
            // 一次查询取 capacity + current_occupancy
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT capacity, current_occupancy FROM places WHERE id = ? AND deleted = 0",
                placeId);
            Integer capacity = (Integer) row.get("capacity");
            Integer current = (Integer) row.get("current_occupancy");
            if (capacity == null) {
                // 未设 capacity 视为不约束
                return List.of();
            }
            int after = (current == null ? 0 : current) + incoming;
            if (after > capacity) {
                return List.of(Violation.block(code(),
                    String.format("场所 %d 入住将超容量: 当前 %d + 新增 %d = %d, 上限 %d",
                        placeId, current, incoming, after, capacity)));
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return List.of(Violation.block(code(),
                "场所 " + placeId + " 不存在或已删除, 无法入住"));
        } catch (Exception e) {
            log.warn("[PlaceCapacityExceededPolicy] check failed: {}", e.getMessage());
        }
        return List.of();
    }
}
