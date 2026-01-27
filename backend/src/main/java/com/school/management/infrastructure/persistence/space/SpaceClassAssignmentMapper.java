package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场所-班级分配Mapper接口
 */
@Mapper
public interface SpaceClassAssignmentMapper extends BaseMapper<SpaceClassAssignmentPO> {

    /**
     * 根据场所ID查询（带关联信息）
     */
    List<SpaceClassAssignmentPO> selectBySpaceIdWithRelations(@Param("spaceId") Long spaceId);

    /**
     * 根据班级ID查询（带关联信息）
     */
    List<SpaceClassAssignmentPO> selectByClassIdWithRelations(@Param("classId") Long classId);

    /**
     * 根据组织单元ID查询（带关联信息）
     */
    List<SpaceClassAssignmentPO> selectByOrgUnitIdWithRelations(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据场所ID和班级ID查询
     */
    SpaceClassAssignmentPO selectBySpaceIdAndClassId(@Param("spaceId") Long spaceId, @Param("classId") Long classId);

    /**
     * 检查是否存在
     */
    int checkExists(@Param("spaceId") Long spaceId, @Param("classId") Long classId);

    /**
     * 统计场所的分配数量
     */
    int countBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 统计班级的分配数量
     */
    int countByClassId(@Param("classId") Long classId);

    /**
     * 删除场所的所有分配
     */
    void deleteBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 删除班级的所有分配
     */
    void deleteByClassId(@Param("classId") Long classId);

    /**
     * 批量插入
     */
    void batchInsert(@Param("list") List<SpaceClassAssignmentPO> list);
}
