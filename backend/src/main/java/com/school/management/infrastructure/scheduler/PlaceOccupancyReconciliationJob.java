package com.school.management.infrastructure.scheduler;

import com.school.management.infrastructure.persistence.place.UniversalPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 场所占用数定时对账任务
 * 每小时检查 current_occupancy 与 place_occupants 实际记录数是否一致，不一致则修正。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PlaceOccupancyReconciliationJob {

    private final UniversalPlaceMapper placeMapper;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void reconcile() {
        List<Map<String, Object>> mismatches = placeMapper.findOccupancyMismatches();
        if (mismatches.isEmpty()) {
            log.debug("场所占用数对账：全部一致");
            return;
        }

        log.warn("场所占用数对账：发现 {} 处偏差，开始修正", mismatches.size());
        int fixed = 0;
        for (Map<String, Object> row : mismatches) {
            Object idObj = row.get("id");
            Object storedObj = row.get("storedCount");
            Object actualObj = row.get("actualCount");
            if (idObj == null || storedObj == null || actualObj == null) {
                log.warn("对账查询返回了 null 字段，跳过: {}", row);
                continue;
            }
            Long placeId = ((Number) idObj).longValue();
            int stored = ((Number) storedObj).intValue();
            int actual = ((Number) actualObj).intValue();

            placeMapper.fixOccupancy(placeId, actual);
            log.info("修正场所 {} 占用数: {} → {}", placeId, stored, actual);
            fixed++;
        }
        log.warn("场所占用数对账完成：修正 {} 处", fixed);
    }
}
