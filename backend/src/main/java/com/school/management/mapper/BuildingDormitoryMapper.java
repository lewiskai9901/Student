package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.BuildingDormitory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 宿舍楼扩展Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface BuildingDormitoryMapper extends BaseMapper<BuildingDormitory> {

    /**
     * 分页查询宿舍楼列表（带楼宇信息）
     *
     * @param page 分页对象
     * @param buildingName 楼宇名称
     * @param dormitoryType 宿舍类型
     * @param orgUnitIds 组织单元ID列表（数据权限过滤）
     * @param managedBuildingIds 用户管理的楼宇ID列表（数据权限过滤）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT bd.id, bd.building_id, bd.dormitory_type, " +
            "  COALESCE((SELECT COUNT(*) FROM dormitories d WHERE d.building_id = bd.building_id AND d.deleted = 0), 0) as total_rooms, " +
            "  COALESCE((SELECT SUM(d.bed_capacity) FROM dormitories d WHERE d.building_id = bd.building_id AND d.deleted = 0), 0) as total_beds, " +
            "  COALESCE((SELECT SUM(d.occupied_beds) FROM dormitories d WHERE d.building_id = bd.building_id AND d.deleted = 0), 0) as occupied_beds, " +
            "  bd.occupied_rooms, bd.management_rules, bd.visiting_hours, bd.facilities, " +
            "  bd.created_at, bd.updated_at, bd.created_by, bd.updated_by, " +
            "  b.building_name, b.building_no, b.total_floors, b.status, b.location " +
            "FROM building_dormitories bd " +
            "INNER JOIN buildings b ON bd.building_id = b.id AND b.deleted = 0 AND b.building_type = 2 " +
            "WHERE 1=1 " +
            "<if test='buildingName != null and buildingName != \"\"'> " +
            "  AND b.building_name LIKE CONCAT('%', #{buildingName}, '%') " +
            "</if>" +
            "<if test='dormitoryType != null'> " +
            "  AND bd.dormitory_type = #{dormitoryType} " +
            "</if>" +
            "<if test='orgUnitIds != null and orgUnitIds.size() > 0'> " +
            "  AND EXISTS (" +
            "    SELECT 1 FROM building_departments bd2 " +
            "    WHERE bd2.building_id = bd.building_id " +
            "    AND bd2.org_unit_id IN " +
            "    <foreach collection='orgUnitIds' item='orgUnitId' open='(' separator=',' close=')'>" +
            "      #{orgUnitId}" +
            "    </foreach>" +
            "  ) " +
            "</if>" +
            "<if test='managedBuildingIds != null and managedBuildingIds.size() > 0'> " +
            "  OR bd.building_id IN " +
            "  <foreach collection='managedBuildingIds' item='buildingId' open='(' separator=',' close=')'>" +
            "    #{buildingId}" +
            "  </foreach>" +
            "</if>" +
            "ORDER BY b.building_no ASC, bd.created_at DESC" +
            "</script>")
    IPage<BuildingDormitory> selectDormitoryBuildingPage(
            Page<BuildingDormitory> page,
            @Param("buildingName") String buildingName,
            @Param("dormitoryType") Integer dormitoryType,
            @Param("orgUnitIds") java.util.List<Long> orgUnitIds,
            @Param("managedBuildingIds") java.util.List<Long> managedBuildingIds
    );

    /**
     * 根据楼宇ID查询宿舍楼信息
     *
     * @param buildingId 楼宇ID
     * @return 宿舍楼信息
     */
    @Select("SELECT bd.*, b.building_name, b.building_no " +
            "FROM building_dormitories bd " +
            "INNER JOIN buildings b ON bd.building_id = b.id " +
            "WHERE bd.building_id = #{buildingId}")
    BuildingDormitory selectByBuildingId(@Param("buildingId") Long buildingId);

    /**
     * 增加房间数量
     *
     * @param buildingId 楼宇ID
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET total_rooms = total_rooms + 1 WHERE building_id = #{buildingId}")
    int incrementTotalRooms(@Param("buildingId") Long buildingId);

    /**
     * 减少房间数量
     *
     * @param buildingId 楼宇ID
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET total_rooms = total_rooms - 1 WHERE building_id = #{buildingId} AND total_rooms > 0")
    int decrementTotalRooms(@Param("buildingId") Long buildingId);

    /**
     * 增加已入住房间数
     *
     * @param buildingId 楼宇ID
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET occupied_rooms = occupied_rooms + 1 WHERE building_id = #{buildingId}")
    int incrementOccupiedRooms(@Param("buildingId") Long buildingId);

    /**
     * 减少已入住房间数
     *
     * @param buildingId 楼宇ID
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET occupied_rooms = occupied_rooms - 1 WHERE building_id = #{buildingId} AND occupied_rooms > 0")
    int decrementOccupiedRooms(@Param("buildingId") Long buildingId);

    /**
     * 增加总床位数
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET total_beds = total_beds + #{bedCount} WHERE building_id = #{buildingId}")
    int incrementTotalBeds(@Param("buildingId") Long buildingId, @Param("bedCount") Integer bedCount);

    /**
     * 减少总床位数
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET total_beds = GREATEST(0, total_beds - #{bedCount}) WHERE building_id = #{buildingId}")
    int decrementTotalBeds(@Param("buildingId") Long buildingId, @Param("bedCount") Integer bedCount);

    /**
     * 增加已入住床位数
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET occupied_beds = occupied_beds + #{bedCount} WHERE building_id = #{buildingId}")
    int incrementOccupiedBeds(@Param("buildingId") Long buildingId, @Param("bedCount") Integer bedCount);

    /**
     * 减少已入住床位数
     *
     * @param buildingId 楼宇ID
     * @param bedCount 床位数
     * @return 影响行数
     */
    @Update("UPDATE building_dormitories SET occupied_beds = GREATEST(0, occupied_beds - #{bedCount}) WHERE building_id = #{buildingId}")
    int decrementOccupiedBeds(@Param("buildingId") Long buildingId, @Param("bedCount") Integer bedCount);
}
