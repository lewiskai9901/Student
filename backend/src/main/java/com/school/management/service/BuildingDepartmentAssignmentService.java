package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.request.BuildingDepartmentAssignmentCreateRequest;
import com.school.management.dto.request.BuildingDepartmentAssignmentUpdateRequest;
import com.school.management.entity.BuildingDepartmentAssignment;

import java.util.List;

/**
 * 宿舍楼-院系分配服务接口
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
public interface BuildingDepartmentAssignmentService {

    /**
     * 分页查询分配列表
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param buildingId 宿舍楼ID
     * @param departmentId 院系ID
     * @param status 状态
     * @return 分页结果
     */
    IPage<BuildingDepartmentAssignment> page(Integer pageNum, Integer pageSize,
                                              Long buildingId, Long departmentId, Integer status);

    /**
     * 根据ID获取分配信息
     *
     * @param id 分配ID
     * @return 分配信息
     */
    BuildingDepartmentAssignment getById(Long id);

    /**
     * 创建分配
     *
     * @param request 创建请求
     * @return 创建的分配
     */
    BuildingDepartmentAssignment create(BuildingDepartmentAssignmentCreateRequest request);

    /**
     * 更新分配
     *
     * @param request 更新请求
     * @return 更新后的分配
     */
    BuildingDepartmentAssignment update(BuildingDepartmentAssignmentUpdateRequest request);

    /**
     * 删除分配
     *
     * @param id 分配ID
     */
    void delete(Long id);

    /**
     * 批量删除分配
     *
     * @param ids 分配ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 根据宿舍楼ID获取所有分配的院系
     *
     * @param buildingId 宿舍楼ID
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> getByBuildingId(Long buildingId);

    /**
     * 根据院系ID获取所有分配的宿舍楼
     *
     * @param departmentId 院系ID
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> getByDepartmentId(Long departmentId);

    /**
     * 根据宿舍楼ID和楼层获取分配的院系
     *
     * @param buildingId 宿舍楼ID
     * @param floor 楼层
     * @return 分配列表
     */
    List<BuildingDepartmentAssignment> getByBuildingAndFloor(Long buildingId, Integer floor);

    /**
     * 检查是否存在楼层冲突
     *
     * @param buildingId 宿舍楼ID
     * @param departmentId 院系ID
     * @param floorStart 起始楼层
     * @param floorEnd 结束楼层
     * @param excludeId 排除的ID
     * @return 是否存在冲突
     */
    boolean hasFloorConflict(Long buildingId, Long departmentId,
                            Integer floorStart, Integer floorEnd, Long excludeId);

    /**
     * 启用分配
     *
     * @param id 分配ID
     */
    void enable(Long id);

    /**
     * 禁用分配
     *
     * @param id 分配ID
     */
    void disable(Long id);
}
