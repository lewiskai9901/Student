package com.school.management.domain.place.repository;

import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.model.valueobject.BuildingType;
import com.school.management.domain.place.model.valueobject.RoomType;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.model.valueobject.PlaceType;

import java.util.List;
import java.util.Optional;

/**
 * 场所仓储接口
 */
public interface PlaceRepository {

    /**
     * 保存场所
     */
    Place save(Place place);

    /**
     * 根据ID查找
     */
    Optional<Place> findById(Long id);

    /**
     * 根据编码查找
     */
    Optional<Place> findByCode(String placeCode);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String placeCode);

    /**
     * 检查校区内楼号是否存在（V10: 使用Integer）
     */
    boolean existsByBuildingNoInCampus(Integer buildingNo, Long campusId, Long excludeId);

    /**
     * 检查楼栋内房间号是否存在（V10: 使用Integer）
     */
    boolean existsByRoomNoInBuilding(Integer roomNo, Long buildingId, Long excludeId);

    /**
     * 删除场所（逻辑删除）
     */
    void delete(Long id);

    /**
     * 获取所有校区
     */
    List<Place> findAllCampuses();

    /**
     * 获取所有楼宇
     */
    List<Place> findAllBuildings(BuildingType buildingType, PlaceStatus status);

    /**
     * 获取子节点
     */
    List<Place> findChildren(Long parentId);

    /**
     * 获取楼宇下的所有楼层
     */
    List<Place> findFloorsByBuildingId(Long buildingId);

    /**
     * 获取楼宇下的所有房间
     */
    List<Place> findRoomsByBuildingId(Long buildingId, RoomType roomType, Integer floorNumber);

    /**
     * 获取楼层下的所有房间
     */
    List<Place> findRoomsByFloorId(Long floorId);

    /**
     * 按类型查询房间
     */
    List<Place> findRoomsByType(RoomType roomType, Long buildingId, PlaceStatus status);

    /**
     * 按组织单元查询
     */
    List<Place> findByOrgUnitId(Long orgUnitId);

    /**
     * 按路径前缀查询所有后代
     */
    List<Place> findByPathPrefix(String pathPrefix);

    /**
     * 分页查询
     */
    List<Place> findByConditions(PlaceType placeType, RoomType roomType, BuildingType buildingType,
                                  Long buildingId, Integer floorNumber, Long orgUnitId,
                                  PlaceStatus status, String keyword, int offset, int limit);

    /**
     * 统计数量
     */
    long countByConditions(PlaceType placeType, RoomType roomType, BuildingType buildingType,
                           Long buildingId, Integer floorNumber, Long orgUnitId,
                           PlaceStatus status, String keyword);

    /**
     * 更新占用数
     */
    void updateOccupancy(Long placeId, int occupancy);

    /**
     * 批量更新组织单元
     */
    void batchUpdateOrgUnit(List<Long> placeIds, Long orgUnitId);

    /**
     * 检查是否有子节点
     */
    boolean hasChildren(Long parentId);

    /**
     * 获取祖先链
     */
    List<Place> findAncestors(Long placeId);

    /**
     * 统计楼宇信息
     */
    PlaceBuildingStats getBuildingStats(Long buildingId);

    /**
     * 原子增加占用数（如果有空位）
     * 使用CAS操作,防止超卖
     *
     * @param placeId 场所ID
     * @param increment 增加数量
     * @return true表示操作成功,false表示容量不足
     */
    boolean incrementOccupancyIfAvailable(Long placeId, int increment);

    /**
     * 原子减少占用数
     * 防止负数
     *
     * @param placeId 场所ID
     * @param decrement 减少数量
     * @return true表示操作成功,false表示无法减少
     */
    boolean decrementOccupancy(Long placeId, int decrement);

    /**
     * 楼宇统计数据
     */
    interface PlaceBuildingStats {
        int getTotalFloors();
        int getTotalRooms();
        int getTotalCapacity();
        int getTotalOccupancy();
    }
}
