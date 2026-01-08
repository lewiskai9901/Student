package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.BuildingRequest;
import com.school.management.entity.Building;

import java.util.List;

/**
 * 楼宇服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface BuildingService {

    /**
     * 分页查询楼宇
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param buildingType 楼宇类型
     * @param buildingNo 楼号
     * @param buildingName 楼宇名称
     * @param status 状态
     * @return 分页结果
     */
    IPage<Building> page(Integer pageNum, Integer pageSize, Integer buildingType,
                         String buildingNo, String buildingName, Integer status);

    /**
     * 根据ID获取楼宇
     *
     * @param id 楼宇ID
     * @return 楼宇信息
     */
    Building getById(Long id);

    /**
     * 创建楼宇
     *
     * @param request 请求对象
     * @return 楼宇信息
     */
    Building create(BuildingRequest request);

    /**
     * 更新楼宇
     *
     * @param id 楼宇ID
     * @param request 请求对象
     * @return 楼宇信息
     */
    Building update(Long id, BuildingRequest request);

    /**
     * 删除楼宇
     *
     * @param id 楼宇ID
     */
    void delete(Long id);

    /**
     * 获取所有启用的楼宇
     *
     * @param buildingType 楼宇类型(可选)
     * @return 楼宇列表
     */
    List<Building> getAllEnabled(Integer buildingType);

    /**
     * 检查楼号是否存在
     *
     * @param buildingNo 楼号
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsBuildingNo(String buildingNo, Long excludeId);
}
