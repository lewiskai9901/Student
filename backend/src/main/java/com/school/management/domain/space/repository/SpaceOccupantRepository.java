package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.SpaceOccupant;
import com.school.management.domain.space.model.valueobject.OccupantType;

import java.util.List;
import java.util.Optional;

/**
 * 场所占用仓储接口
 */
public interface SpaceOccupantRepository {

    /**
     * 保存占用记录
     */
    SpaceOccupant save(SpaceOccupant occupant);

    /**
     * 根据ID查找
     */
    Optional<SpaceOccupant> findById(Long id);

    /**
     * 获取场所的所有在住占用者
     */
    List<SpaceOccupant> findActiveBySpaceId(Long spaceId);

    /**
     * 获取场所的所有占用记录（包括历史）
     */
    List<SpaceOccupant> findAllBySpaceId(Long spaceId);

    /**
     * 根据占用者查找当前在住记录
     */
    Optional<SpaceOccupant> findActiveByOccupant(OccupantType occupantType, Long occupantId);

    /**
     * 根据位置查找当前占用者
     */
    Optional<SpaceOccupant> findActiveByPosition(Long spaceId, Integer positionNo);

    /**
     * 检查位置是否已被占用
     */
    boolean isPositionOccupied(Long spaceId, Integer positionNo);

    /**
     * 检查占用者是否已有在住记录
     */
    boolean hasActiveOccupancy(OccupantType occupantType, Long occupantId);

    /**
     * 统计场所当前在住人数
     */
    int countActiveBySpaceId(Long spaceId);

    /**
     * 获取已占用的位置列表
     */
    List<Integer> findOccupiedPositions(Long spaceId);

    /**
     * 批量退出（按场所）
     */
    void batchCheckOutBySpaceId(Long spaceId);

    /**
     * 删除记录
     */
    void delete(Long id);
}
