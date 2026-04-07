package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.entity.PlaceClassAssignment;

import java.util.List;
import java.util.Optional;

/**
 * 场所-班级分配仓储接口
 */
public interface PlaceClassAssignmentRepository {

    /**
     * 保存分配记录
     */
    PlaceClassAssignment save(PlaceClassAssignment assignment);

    /**
     * 根据ID查询
     */
    Optional<PlaceClassAssignment> findById(Long id);

    /**
     * 根据场所ID查询所有分配
     */
    List<PlaceClassAssignment> findByPlaceId(Long placeId);

    /**
     * 根据班级ID查询所有分配
     */
    List<PlaceClassAssignment> findByClassId(Long orgUnitId);

    /**
     * 根据组织单元ID查询所有分配
     */
    List<PlaceClassAssignment> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据场所ID和班级ID查询
     */
    Optional<PlaceClassAssignment> findByPlaceIdAndClassId(Long placeId, Long orgUnitId);

    /**
     * 删除分配记录
     */
    void delete(Long id);

    /**
     * 删除场所的所有分配
     */
    void deleteByPlaceId(Long placeId);

    /**
     * 删除班级的所有分配
     */
    void deleteByClassId(Long orgUnitId);

    /**
     * 检查场所是否已分配给班级
     */
    boolean existsByPlaceIdAndClassId(Long placeId, Long orgUnitId);

    /**
     * 统计场所的分配数量
     */
    int countByPlaceId(Long placeId);

    /**
     * 统计班级的分配数量
     */
    int countByClassId(Long orgUnitId);

    /**
     * 批量保存
     */
    void batchSave(List<PlaceClassAssignment> assignments);
}
