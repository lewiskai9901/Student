package com.school.management.domain.space.repository;

import com.school.management.domain.space.model.aggregate.Space;
import com.school.management.domain.space.model.valueobject.BuildingType;
import com.school.management.domain.space.model.valueobject.RoomType;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import com.school.management.domain.space.model.valueobject.SpaceType;

import java.util.List;
import java.util.Optional;

/**
 * 场所仓储接口
 */
public interface SpaceRepository {

    /**
     * 保存场所
     */
    Space save(Space space);

    /**
     * 根据ID查找
     */
    Optional<Space> findById(Long id);

    /**
     * 根据编码查找
     */
    Optional<Space> findByCode(String spaceCode);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String spaceCode);

    /**
     * 检查校区内楼号是否存在
     */
    boolean existsByBuildingNoInCampus(String buildingNo, Long campusId, Long excludeId);

    /**
     * 检查楼栋内房间号是否存在
     */
    boolean existsByRoomNoInBuilding(String roomNo, Long buildingId, Long excludeId);

    /**
     * 删除场所（逻辑删除）
     */
    void delete(Long id);

    /**
     * 获取所有校区
     */
    List<Space> findAllCampuses();

    /**
     * 获取所有楼宇
     */
    List<Space> findAllBuildings(BuildingType buildingType, SpaceStatus status);

    /**
     * 获取子节点
     */
    List<Space> findChildren(Long parentId);

    /**
     * 获取楼宇下的所有楼层
     */
    List<Space> findFloorsByBuildingId(Long buildingId);

    /**
     * 获取楼宇下的所有房间
     */
    List<Space> findRoomsByBuildingId(Long buildingId, RoomType roomType, Integer floorNumber);

    /**
     * 获取楼层下的所有房间
     */
    List<Space> findRoomsByFloorId(Long floorId);

    /**
     * 按类型查询房间
     */
    List<Space> findRoomsByType(RoomType roomType, Long buildingId, SpaceStatus status);

    /**
     * 按组织单元查询
     */
    List<Space> findByOrgUnitId(Long orgUnitId);

    /**
     * 按路径前缀查询所有后代
     */
    List<Space> findByPathPrefix(String pathPrefix);

    /**
     * 分页查询
     */
    List<Space> findByConditions(SpaceType spaceType, RoomType roomType, BuildingType buildingType,
                                  Long buildingId, Integer floorNumber, Long orgUnitId,
                                  SpaceStatus status, String keyword, int offset, int limit);

    /**
     * 统计数量
     */
    long countByConditions(SpaceType spaceType, RoomType roomType, BuildingType buildingType,
                           Long buildingId, Integer floorNumber, Long orgUnitId,
                           SpaceStatus status, String keyword);

    /**
     * 更新占用数
     */
    void updateOccupancy(Long spaceId, int occupancy);

    /**
     * 批量更新组织单元
     */
    void batchUpdateOrgUnit(List<Long> spaceIds, Long orgUnitId);

    /**
     * 检查是否有子节点
     */
    boolean hasChildren(Long parentId);

    /**
     * 获取祖先链
     */
    List<Space> findAncestors(Long spaceId);

    /**
     * 统计楼宇信息
     */
    SpaceBuildingStats getBuildingStats(Long buildingId);

    /**
     * 楼宇统计数据
     */
    interface SpaceBuildingStats {
        int getTotalFloors();
        int getTotalRooms();
        int getTotalCapacity();
        int getTotalOccupancy();
    }
}
