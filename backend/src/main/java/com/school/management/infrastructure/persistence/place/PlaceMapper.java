package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 场所 Mapper
 */
@Mapper
public interface PlaceMapper extends BaseMapper<PlacePO> {

    /**
     * 根据ID查询（带关联信息）
     */
    PlacePO selectByIdWithRelations(@Param("id") Long id);

    /**
     * 根据编码查询
     */
    PlacePO selectByCode(@Param("placeCode") String placeCode);

    /**
     * 查询子节点
     */
    List<PlacePO> selectChildren(@Param("parentId") Long parentId);

    /**
     * 查询所有校区
     */
    List<PlacePO> selectAllCampuses();

    /**
     * 查询所有楼宇
     */
    List<PlacePO> selectAllBuildings(@Param("buildingType") String buildingType,
                                      @Param("status") Integer status);

    /**
     * 查询楼宇的楼层
     */
    List<PlacePO> selectFloorsByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 查询楼宇的房间
     */
    List<PlacePO> selectRoomsByBuildingId(@Param("buildingId") Long buildingId,
                                           @Param("roomType") String roomType,
                                           @Param("floorNumber") Integer floorNumber);

    /**
     * 按条件查询
     */
    List<PlacePO> selectByConditions(@Param("placeType") String placeType,
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
    long countByConditions(@Param("placeType") String placeType,
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
    List<PlacePO> selectByPathPrefix(@Param("pathPrefix") String pathPrefix);

    /**
     * 按组织单元查询
     */
    List<PlacePO> selectByOrgUnitId(@Param("orgUnitId") Long orgUnitId);

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
    List<PlacePO> selectAncestors(@Param("path") String path);

    /**
     * 获取楼宇统计
     */
    Map<String, Object> selectBuildingStats(@Param("buildingId") Long buildingId);

    /**
     * 统计校区内的楼号数量（V10: 使用Integer）
     */
    int countByBuildingNoInCampus(@Param("buildingNo") Integer buildingNo,
                                   @Param("campusId") Long campusId,
                                   @Param("excludeId") Long excludeId);

    /**
     * 统计楼栋内的房间号数量（V10: 使用Integer）
     */
    int countByRoomNoInBuilding(@Param("roomNo") Integer roomNo,
                                 @Param("buildingId") Long buildingId,
                                 @Param("excludeId") Long excludeId);

    /**
     * 原子增加占用数（如果有空位）
     * 使用CAS操作,防止超卖
     *
     * @param placeId 场所ID
     * @param increment 增加数量
     * @return 受影响行数（1=成功,0=容量不足）
     */
    int incrementOccupancyIfAvailable(@Param("placeId") Long placeId,
                                       @Param("increment") int increment);

    /**
     * 原子减少占用数
     * 防止负数
     *
     * @param placeId 场所ID
     * @param decrement 减少数量
     * @return 受影响行数（1=成功,0=无法减少）
     */
    int decrementOccupancy(@Param("placeId") Long placeId,
                           @Param("decrement") int decrement);
}
