package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.BuildingDormitoryDTO;
import com.school.management.entity.BuildingDormitory;

import java.util.List;

/**
 * 宿舍楼管理服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface BuildingDormitoryService {

    /**
     * 分页查询宿舍楼列表
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param buildingName 楼宇名称
     * @param dormitoryType 宿舍类型
     * @return 分页结果
     */
    IPage<BuildingDormitory> page(Integer pageNum, Integer pageSize, String buildingName, Integer dormitoryType);

    /**
     * 根据ID获取宿舍楼信息
     *
     * @param id 宿舍楼ID
     * @return 宿舍楼信息
     */
    BuildingDormitory getById(Long id);

    /**
     * 根据楼宇ID获取宿舍楼信息
     *
     * @param buildingId 楼宇ID
     * @return 宿舍楼信息
     */
    BuildingDormitory getByBuildingId(Long buildingId);

    /**
     * 更新宿舍楼信息
     *
     * @param id 宿舍楼ID
     * @param dto DTO对象
     * @return 更新后的宿舍楼信息
     */
    BuildingDormitory update(Long id, BuildingDormitoryDTO dto);

    /**
     * 分配管理员
     *
     * @param buildingId 楼宇ID
     * @param managerIds 管理员ID列表
     */
    void assignManagers(Long buildingId, List<Long> managerIds);

    /**
     * 移除管理员
     *
     * @param buildingId 楼宇ID
     * @param userId 用户ID
     */
    void removeManager(Long buildingId, Long userId);

    /**
     * 验证用户是否有宿舍管理权限
     *
     * @param userId 用户ID
     * @return 是否有权限
     */
    boolean hasDormitoryPermission(Long userId);

    /**
     * 验证用户是否为指定楼宇的管理员
     *
     * @param buildingId 楼宇ID
     * @param userId 用户ID
     * @return 是否为管理员
     */
    boolean isManager(Long buildingId, Long userId);

    /**
     * 验证是否可以修改宿舍楼类型
     *
     * @param buildingId 楼宇ID
     * @param newDormitoryType 新的宿舍类型
     */
    void validateDormitoryTypeChange(Long buildingId, Integer newDormitoryType);

    /**
     * 增加房间数量
     *
     * @param buildingId 楼宇ID
     */
    void incrementRoomCount(Long buildingId);

    /**
     * 减少房间数量
     *
     * @param buildingId 楼宇ID
     */
    void decrementRoomCount(Long buildingId);

    /**
     * 增加床位数量
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     */
    void incrementBedCount(Long buildingId, Integer bedCount);

    /**
     * 减少床位数量
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     */
    void decrementBedCount(Long buildingId, Integer bedCount);
}
