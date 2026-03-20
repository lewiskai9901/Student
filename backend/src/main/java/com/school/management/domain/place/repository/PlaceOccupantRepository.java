package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.PlaceOccupant;
import com.school.management.domain.place.model.valueobject.OccupantType;

import java.util.List;
import java.util.Optional;

/**
 * 场所占用仓储接口
 */
public interface PlaceOccupantRepository {

    /**
     * 保存占用记录
     */
    PlaceOccupant save(PlaceOccupant occupant);

    /**
     * 根据ID查找
     */
    Optional<PlaceOccupant> findById(Long id);

    /**
     * 获取场所的所有在住占用者
     */
    List<PlaceOccupant> findActiveByPlaceId(Long placeId);

    /**
     * 获取场所的所有占用记录（包括历史）
     */
    List<PlaceOccupant> findAllByPlaceId(Long placeId);

    /**
     * 根据占用者查找当前在住记录
     */
    Optional<PlaceOccupant> findActiveByOccupant(OccupantType occupantType, Long occupantId);

    /**
     * 根据位置查找当前占用者
     */
    Optional<PlaceOccupant> findActiveByPosition(Long placeId, Integer positionNo);

    /**
     * 检查位置是否已被占用
     */
    boolean isPositionOccupied(Long placeId, Integer positionNo);

    /**
     * 检查占用者是否已有在住记录
     */
    boolean hasActiveOccupancy(OccupantType occupantType, Long occupantId);

    /**
     * 统计场所当前在住人数
     */
    int countActiveByPlaceId(Long placeId);

    /**
     * 获取已占用的位置列表
     */
    List<Integer> findOccupiedPositions(Long placeId);

    /**
     * 批量退出（按场所）
     */
    void batchCheckOutByPlaceId(Long placeId);

    /**
     * 删除记录
     */
    void delete(Long id);
}
