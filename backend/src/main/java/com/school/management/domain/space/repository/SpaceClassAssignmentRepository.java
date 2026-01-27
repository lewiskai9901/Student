package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.entity.SpaceClassAssignment;

import java.util.List;
import java.util.Optional;

/**
 * 场所-班级分配仓储接口
 */
public interface SpaceClassAssignmentRepository {

    /**
     * 保存分配记录
     */
    SpaceClassAssignment save(SpaceClassAssignment assignment);

    /**
     * 根据ID查询
     */
    Optional<SpaceClassAssignment> findById(Long id);

    /**
     * 根据场所ID查询所有分配
     */
    List<SpaceClassAssignment> findBySpaceId(Long spaceId);

    /**
     * 根据班级ID查询所有分配
     */
    List<SpaceClassAssignment> findByClassId(Long classId);

    /**
     * 根据组织单元ID查询所有分配
     */
    List<SpaceClassAssignment> findByOrgUnitId(Long orgUnitId);

    /**
     * 根据场所ID和班级ID查询
     */
    Optional<SpaceClassAssignment> findBySpaceIdAndClassId(Long spaceId, Long classId);

    /**
     * 删除分配记录
     */
    void delete(Long id);

    /**
     * 删除场所的所有分配
     */
    void deleteBySpaceId(Long spaceId);

    /**
     * 删除班级的所有分配
     */
    void deleteByClassId(Long classId);

    /**
     * 检查场所是否已分配给班级
     */
    boolean existsBySpaceIdAndClassId(Long spaceId, Long classId);

    /**
     * 统计场所的分配数量
     */
    int countBySpaceId(Long spaceId);

    /**
     * 统计班级的分配数量
     */
    int countByClassId(Long classId);

    /**
     * 批量保存
     */
    void batchSave(List<SpaceClassAssignment> assignments);
}
