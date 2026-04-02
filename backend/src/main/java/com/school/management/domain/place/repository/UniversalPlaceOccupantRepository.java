package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;

import java.util.List;
import java.util.Optional;

/**
 * 空间占用记录仓储接口
 */
public interface UniversalPlaceOccupantRepository {

    /**
     * 保存占用记录
     */
    UniversalPlaceOccupant save(UniversalPlaceOccupant occupant);

    /**
     * 根据ID查询
     */
    Optional<UniversalPlaceOccupant> findById(Long id);

    /**
     * 查询空间的当前活跃占用者
     */
    List<UniversalPlaceOccupant> findActiveByPlaceId(Long placeId);

    /**
     * 查询空间的所有占用记录（包括历史）
     */
    List<UniversalPlaceOccupant> findAllByPlaceId(Long placeId);

    /**
     * 查询占用者的当前活跃占用记录
     */
    Optional<UniversalPlaceOccupant> findActiveByOccupant(String occupantType, Long occupantId);

    /**
     * 查询占用者的所有占用历史
     */
    List<UniversalPlaceOccupant> findAllByOccupant(String occupantType, Long occupantId);

    /**
     * 检查位置是否被占用
     */
    boolean isPositionOccupied(Long placeId, String positionNo);

    /**
     * 检查占用者是否有活跃占用
     */
    boolean hasActiveOccupancy(String occupantType, Long occupantId);

    /**
     * 统计空间的活跃占用数
     */
    int countActiveByPlaceId(Long placeId);

    /**
     * 根据ID删除
     */
    void deleteById(Long id);

    /**
     * 批量退出（更新状态）
     */
    void batchCheckOut(List<Long> ids);

    /**
     * 查询某用户的所有在住记录（跨场所）
     */
    List<UniversalPlaceOccupant> findActiveByOccupantId(Long occupantId);

    /**
     * 查询指定场所列表中的所有活跃占用记录（可按 occupantType 过滤）
     */
    List<UniversalPlaceOccupant> findActiveByPlaceIds(List<Long> placeIds, String occupantType);
}
