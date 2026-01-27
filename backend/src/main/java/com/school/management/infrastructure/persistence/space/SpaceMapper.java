package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 场所 Mapper
 */
@Mapper
public interface SpaceMapper extends BaseMapper<SpacePO> {

    /**
     * 根据ID查询（带关联信息）
     */
    SpacePO selectByIdWithRelations(@Param("id") Long id);

    /**
     * 根据编码查询
     */
    SpacePO selectByCode(@Param("spaceCode") String spaceCode);

    /**
     * 查询子节点
     */
    List<SpacePO> selectChildren(@Param("parentId") Long parentId);

    /**
     * 查询所有校区
     */
    List<SpacePO> selectAllCampuses();

    /**
     * 查询所有楼宇
     */
    List<SpacePO> selectAllBuildings(@Param("buildingType") String buildingType,
                                      @Param("status") Integer status);

    /**
     * 查询楼宇的楼层
     */
    List<SpacePO> selectFloorsByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 查询楼宇的房间
     */
    List<SpacePO> selectRoomsByBuildingId(@Param("buildingId") Long buildingId,
                                           @Param("roomType") String roomType,
                                           @Param("floorNumber") Integer floorNumber);

    /**
     * 按条件查询
     */
    List<SpacePO> selectByConditions(@Param("spaceType") String spaceType,
                                      @Param("roomType") String roomType,
                                      @Param("buildingType") String buildingType,
                                      @Param("buildingId") Long buildingId,
                                      @Param("floorNumber") Integer floorNumber,
                                      @Param("orgUnitId") Long orgUnitId,
                                      @Param("status") Integer status,
                                      @Param("keyword") String keyword,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计数量
     */
    long countByConditions(@Param("spaceType") String spaceType,
                           @Param("roomType") String roomType,
                           @Param("buildingType") String buildingType,
                           @Param("buildingId") Long buildingId,
                           @Param("floorNumber") Integer floorNumber,
                           @Param("orgUnitId") Long orgUnitId,
                           @Param("status") Integer status,
                           @Param("keyword") String keyword);

    /**
     * 按路径前缀查询
     */
    List<SpacePO> selectByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 按组织单元查询
     */
    List<SpacePO> selectByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

    /**
     * 更新占用数
     */
    void updateOccupancy(@Param("id") Long id, @Param("occupancy") int occupancy);

    /**
     * 批量更新组织单元
     */
    void batchUpdateOrgUnit(@Param("ids") List<Long> ids, @Param("orgUnitId") Long orgUnitId);

    /**
     * 检查是否有子节点
     */
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 获取祖先链
     */
    List<SpacePO> selectAncestors(@Param("path") String path);

    /**
     * 获取楼宇统计
     */
    Map<String, Object> selectBuildingStats(@Param("buildingId") Long buildingId);

    /**
     * 统计校区内的楼号数量
     */
    int countByBuildingNoInCampus(@Param("buildingNo") String buildingNo,
                                   @Param("campusId") Long campusId,
                                   @Param("excludeId") Long excludeId);

    /**
     * 统计楼栋内的房间号数量
     */
    int countByRoomNoInBuilding(@Param("roomNo") String roomNo,
                                 @Param("buildingId") Long buildingId,
                                 @Param("excludeId") Long excludeId);
}
