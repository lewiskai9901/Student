package com.school.management.application.place;

import com.school.management.application.place.query.CapacityAlertDTO;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.place.service.PlaceCapacityService;
import com.school.management.infrastructure.cache.PlaceCapacityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 场所容量告警应用服务
 * 对标: AWS CloudWatch Alarms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCapacityAlertService {

    private final UniversalPlaceRepository placeRepository;
    private final PlaceCapacityService capacityService;
    private final PlaceCapacityCacheService cacheService;

    /**
     * 获取高占用率场所告警列表
     *
     * @param typeCode 场所类型编码（可选）
     * @return 告警列表
     */
    public List<CapacityAlertDTO> getHighOccupancyAlerts(String typeCode) {
        // 先尝试从缓存获取
        List<Long> highOccupancyPlaceIds = cacheService.getHighOccupancyPlaceIds(typeCode);

        // 根据ID批量查询场所详情
        return highOccupancyPlaceIds.stream()
                .map(placeId -> placeRepository.findById(placeId).orElse(null))
                .filter(place -> place != null && capacityService.isCapacityAlert(place))
                .map(this::toAlertDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定场所的告警信息
     *
     * @param placeId 场所ID
     * @return 告警DTO，如果无告警返回null
     */
    public CapacityAlertDTO getPlaceAlert(Long placeId) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));

        if (!capacityService.isCapacityAlert(place)) {
            return null; // 无告警
        }

        return toAlertDTO(place);
    }

    /**
     * 检查是否需要触发容量告警
     *
     * @param placeId 场所ID
     * @return true表示需要告警
     */
    public boolean shouldAlert(Long placeId) {
        UniversalPlace place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("场所不存在: " + placeId));
        return capacityService.isCapacityAlert(place);
    }

    /**
     * 获取所有类型的高占用率统计
     *
     * @return 按类型分组的告警统计
     */
    public List<TypeAlertSummary> getAlertSummaryByType() {
        // 获取所有高占用率场所
        List<UniversalPlace> highOccupancyPlaces = capacityService.findHighOccupancyPlaces(null);

        // 按类型分组统计
        return highOccupancyPlaces.stream()
                .collect(Collectors.groupingBy(UniversalPlace::getTypeCode))
                .entrySet().stream()
                .map(entry -> {
                    String typeCode = entry.getKey();
                    List<UniversalPlace> places = entry.getValue();

                    long warningCount = places.stream()
                            .filter(p -> {
                                double rate = p.getOccupancyRate() * 100;
                                return rate >= 80 && rate < 95;
                            })
                            .count();

                    long criticalCount = places.stream()
                            .filter(p -> {
                                double rate = p.getOccupancyRate() * 100;
                                return rate >= 95 && rate < 100;
                            })
                            .count();

                    long fullCount = places.stream()
                            .filter(p -> p.getOccupancyRate() >= 1.0)
                            .count();

                    return new TypeAlertSummary(
                            typeCode,
                            places.size(),
                            warningCount,
                            criticalCount,
                            fullCount
                    );
                })
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 转换为告警DTO
     */
    private CapacityAlertDTO toAlertDTO(UniversalPlace place) {
        double occupancyRate = place.getOccupancyRate() * 100;

        return CapacityAlertDTO.builder()
                .placeId(place.getId())
                .placeCode(place.getPlaceCode())
                .placeName(place.getPlaceName())
                .typeCode(place.getTypeCode())
                .typeName(null) // TODO: 需要关联查询 PlaceType
                .capacity(place.getCapacity())
                .currentOccupancy(place.getCurrentOccupancy())
                .occupancyRate(occupancyRate)
                .alertLevel(CapacityAlertDTO.calculateAlertLevel(occupancyRate))
                .orgUnitId(place.getOrgUnitId())
                .responsibleUserId(place.getResponsibleUserId())
                .lastUpdatedAt(null) // TODO: 添加 updatedAt 字段
                .build();
    }

    // ========== 内部DTO ==========

    /**
     * 类型告警汇总
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TypeAlertSummary {
        /**
         * 类型编码
         */
        private String typeCode;

        /**
         * 总告警数
         */
        private long totalAlerts;

        /**
         * 警告级别数量
         */
        private long warningCount;

        /**
         * 严重级别数量
         */
        private long criticalCount;

        /**
         * 已满数量
         */
        private long fullCount;
    }
}
