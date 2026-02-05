package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.UniversalSpaceOccupant;

import java.util.List;
import java.util.Optional;

/**
 * 空间占用记录仓储接口
 */
public interface UniversalSpaceOccupantRepository {

    /**
     * 保存占用记录
     */
    UniversalSpaceOccupant save(UniversalSpaceOccupant occupant);

    /**
     * 根据ID查询
     */
    Optional<UniversalSpaceOccupant> findById(Long id);

    /**
     * 查询空间的当前活跃占用者
     */
    List<UniversalSpaceOccupant> findActiveBySpaceId(Long spaceId);

    /**
     * 查询空间的所有占用记录（包括历史）
     */
    List<UniversalSpaceOccupant> findAllBySpaceId(Long spaceId);

    /**
     * 查询占用者的当前活跃占用记录
     */
    Optional<UniversalSpaceOccupant> findActiveByOccupant(String occupantType, Long occupantId);

    /**
     * 查询占用者的所有占用历史
     */
    List<UniversalSpaceOccupant> findAllByOccupant(String occupantType, Long occupantId);

    /**
     * 检查位置是否被占用
     */
    boolean isPositionOccupied(Long spaceId, String positionNo);

    /**
     * 检查占用者是否有活跃占用
     */
    boolean hasActiveOccupancy(String occupantType, Long occupantId);

    /**
     * 统计空间的活跃占用数
     */
    int countActiveBySpaceId(Long spaceId);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);

    /**
     * 批量退出（更新状态）
     */
    void batchCheckOut(List<Long> ids);
}
