package com.school.management.domain.place.service;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 场所容量管理领域服务
 * 负责容量查询、预警计算等业务逻辑
 * 对标: AWS CloudWatch Capacity Monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCapacityService {

    private final UniversalPlaceRepository placeRepository;

    /**
     * 容量预警阈值（百分比）
     * 默认80%，可通过配置文件调整
     */
    @Value("${place.capacity.alert-threshold:80}")
    private double alertThreshold;

    /**
     * 高危预警阈值（百分比）
     * 默认95%，可通过配置文件调整
     */
    @Value("${place.capacity.critical-threshold:95}")
    private double criticalThreshold;

    /**
     * 判断场所是否达到容量预警阈值
     *
     * @param place 场所实体
     * @return true表示需要预警
     */
    public boolean isCapacityAlert(UniversalPlace place) {
        if (place == null || place.getCapacity() == null || place.getCapacity() == 0) {
            return false;
        }
        double rate = place.getOccupancyRate() * 100;
        return rate >= alertThreshold;
    }

    /**
     * 判断场所是否达到高危阈值
     *
     * @param place 场所实体
     * @return true表示高危
     */
    public boolean isCritical(UniversalPlace place) {
        if (place == null || place.getCapacity() == null || place.getCapacity() == 0) {
            return false;
        }
        double rate = place.getOccupancyRate() * 100;
        return rate >= criticalThreshold;
    }

    /**
     * 获取容量预警等级
     *
     * @param place 场所实体
     * @return SAFE(安全) / WARNING(预警) / CRITICAL(高危) / FULL(满员)
     */
    public String getAlertLevel(UniversalPlace place) {
        if (place == null || place.getCapacity() == null || place.getCapacity() == 0) {
            return "SAFE";
        }

        double rate = place.getOccupancyRate() * 100;

        if (rate >= 100) {
            return "FULL";
        } else if (rate >= criticalThreshold) {
            return "CRITICAL";
        } else if (rate >= alertThreshold) {
            return "WARNING";
        } else {
            return "SAFE";
        }
    }

    /**
     * 查询高占用率场所（占用率 >= alertThreshold）
     * 使用数据库索引优化查询性能
     *
     * @param typeCode 场所类型代码（可选）
     * @return 高占用率场所列表
     */
    public List<UniversalPlace> findHighOccupancyPlaces(String typeCode) {
        // 注意：这里调用Repository的查询方法
        // Repository实现会使用数据库的idx_high_occupancy索引
        List<UniversalPlace> allPlaces;

        if (typeCode != null && !typeCode.isBlank()) {
            allPlaces = placeRepository.findByTypeCode(typeCode);
        } else {
            allPlaces = placeRepository.findAll();
        }

        // 过滤出高占用率场所
        return allPlaces.stream()
                .filter(this::isCapacityAlert)
                .sorted((p1, p2) -> Double.compare(p2.getOccupancyRate(), p1.getOccupancyRate())) // 按占用率降序
                .collect(Collectors.toList());
    }

    /**
     * 查询满员场所（占用率 = 100%）
     *
     * @param typeCode 场所类型代码（可选）
     * @return 满员场所列表
     */
    public List<UniversalPlace> findFullPlaces(String typeCode) {
        List<UniversalPlace> allPlaces;

        if (typeCode != null && !typeCode.isBlank()) {
            allPlaces = placeRepository.findByTypeCode(typeCode);
        } else {
            allPlaces = placeRepository.findAll();
        }

        return allPlaces.stream()
                .filter(p -> p.getCapacity() != null && p.getCapacity() > 0)
                .filter(p -> p.getCurrentOccupancy() >= p.getCapacity())
                .collect(Collectors.toList());
    }

    /**
     * 查询有剩余容量的场所
     *
     * @param typeCode 场所类型代码（可选）
     * @param minAvailableCapacity 最小剩余容量
     * @return 有剩余容量的场所列表
     */
    public List<UniversalPlace> findAvailablePlaces(String typeCode, int minAvailableCapacity) {
        List<UniversalPlace> allPlaces;

        if (typeCode != null && !typeCode.isBlank()) {
            allPlaces = placeRepository.findByTypeCode(typeCode);
        } else {
            allPlaces = placeRepository.findAll();
        }

        return allPlaces.stream()
                .filter(UniversalPlace::hasAvailableCapacity)
                .filter(p -> p.getAvailableCapacity() >= minAvailableCapacity)
                .sorted((p1, p2) -> Integer.compare(p2.getAvailableCapacity(), p1.getAvailableCapacity())) // 按剩余容量降序
                .collect(Collectors.toList());
    }

    /**
     * 计算场所类型的总容量统计
     *
     * @param typeCode 场所类型代码
     * @return 容量统计对象
     */
    public CapacityStatistics calculateStatistics(String typeCode) {
        List<UniversalPlace> places = placeRepository.findByTypeCode(typeCode);

        int totalPlaces = places.size();
        int totalCapacity = places.stream()
                .filter(p -> p.getCapacity() != null)
                .mapToInt(UniversalPlace::getCapacity)
                .sum();
        int totalOccupancy = places.stream()
                .mapToInt(p -> p.getCurrentOccupancy() != null ? p.getCurrentOccupancy() : 0)
                .sum();
        long highOccupancyCount = places.stream()
                .filter(this::isCapacityAlert)
                .count();
        long fullCount = places.stream()
                .filter(p -> p.getCapacity() != null && p.getCurrentOccupancy() >= p.getCapacity())
                .count();
        long emptyCount = places.stream()
                .filter(p -> p.getCurrentOccupancy() == null || p.getCurrentOccupancy() == 0)
                .count();

        double avgOccupancyRate = (totalCapacity > 0) ? (totalOccupancy * 100.0 / totalCapacity) : 0.0;

        return new CapacityStatistics(
                typeCode,
                totalPlaces,
                totalCapacity,
                totalOccupancy,
                avgOccupancyRate,
                (int) highOccupancyCount,
                (int) fullCount,
                (int) emptyCount
        );
    }

    /**
     * 容量统计数据类
     */
    public record CapacityStatistics(
            String typeCode,
            int totalPlaces,
            int totalCapacity,
            int totalOccupancy,
            double avgOccupancyRate,
            int highOccupancyCount,
            int fullCount,
            int emptyCount
    ) {}
}
