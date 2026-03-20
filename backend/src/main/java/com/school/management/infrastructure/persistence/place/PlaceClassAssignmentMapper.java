package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场所-班级分配Mapper接口
 */
@Mapper
public interface PlaceClassAssignmentMapper extends BaseMapper<PlaceClassAssignmentPO> {

    /**
     * 根据场所ID查询（带关联信息）
     */
    List<PlaceClassAssignmentPO> selectByPlaceIdWithRelations(@Param("placeId") Long placeId);

    /**
     * 根据班级ID查询（带关联信息）
     */
    List<PlaceClassAssignmentPO> selectByClassIdWithRelations(@Param("classId") Long classId);

    /**
     * 根据组织单元ID查询（带关联信息）
     */
    List<PlaceClassAssignmentPO> selectByOrgUnitIdWithRelations(@Param("orgUnitId") Long orgUnitId);

    /**
     * 根据场所ID和班级ID查询
     */
    PlaceClassAssignmentPO selectByPlaceIdAndClassId(@Param("placeId") Long placeId, @Param("classId") Long classId);

    /**
     * 检查是否存在
     */
    int checkExists(@Param("placeId") Long placeId, @Param("classId") Long classId);

    /**
     * 统计场所的分配数量
     */
    int countByPlaceId(@Param("placeId") Long placeId);

    /**
     * 统计班级的分配数量
     */
    int countByClassId(@Param("classId") Long classId);

    /**
     * 删除场所的所有分配
     */
    void deleteByPlaceId(@Param("placeId") Long placeId);

    /**
     * 删除班级的所有分配
     */
    void deleteByClassId(@Param("classId") Long classId);

    /**
     * 批量插入
     */
    void batchInsert(@Param("list") List<PlaceClassAssignmentPO> list);
}
