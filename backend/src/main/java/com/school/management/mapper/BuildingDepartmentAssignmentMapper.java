package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.BuildingDepartmentAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宿舍楼-院系分配Mapper接口
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
@Mapper
public interface BuildingDepartmentAssignmentMapper extends BaseMapper<BuildingDepartmentAssignment> {

    /**
     * 分页查询分配列表（带关联信息）
     *
     * @param page 分页对象
     * @param buildingId 宿舍楼ID
     * @param departmentId 院系ID
     * @param status 状态
     * @return 分配列表
     */
    IPage<BuildingDepartmentAssignment> selectAssignmentPageWithDetails(
            Page<BuildingDepartmentAssignment> page,
            @Param("buildingId") Long buildingId,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status
    );

    /**
     * 根据宿舍楼ID查询所有分配的院系
     *
     * @param buildingId 宿舍楼ID
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 根据院系ID查询所有分配的宿舍楼
     *
     * @param departmentId 院系ID
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 检查楼层范围是否有冲突
     *
     * @param buildingId 宿舍楼ID
     * @param departmentId 院系ID
     * @param floorStart 起始楼层
     * @param floorEnd 结束楼层
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 冲突数量
     */
    int checkFloorConflict(
            @Param("buildingId") Long buildingId,
            @Param("departmentId") Long departmentId,
            @Param("floorStart") Integer floorStart,
            @Param("floorEnd") Integer floorEnd,
            @Param("excludeId") Long excludeId
    );

    /**
     * 根据宿舍楼ID和楼层查询分配的院系
     *
     * @param buildingId 宿舍楼ID
     * @param floor 楼层
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> selectByBuildingAndFloor(
            @Param("buildingId") Long buildingId,
            @Param("floor") Integer floor
    );
}
