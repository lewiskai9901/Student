package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 空间占用记录 Mapper
 */
@Mapper
public interface UniversalPlaceOccupantMapper extends BaseMapper<UniversalPlaceOccupantPO> {

    /**
     * 查询空间的当前活跃占用者
     */
    @Select("SELECT * FROM place_occupants WHERE place_id = #{placeId} AND status = 1 AND deleted = 0 ORDER BY position_no")
    List<UniversalPlaceOccupantPO> findActiveByPlaceId(@Param("placeId") Long placeId);

    /**
     * 查询空间的所有占用记录（包括历史）
     */
    @Select("SELECT * FROM place_occupants WHERE place_id = #{placeId} AND deleted = 0 ORDER BY check_in_time DESC")
    List<UniversalPlaceOccupantPO> findAllByPlaceId(@Param("placeId") Long placeId);

    /**
     * 查询占用者的当前活跃占用记录
     */
    @Select("SELECT * FROM place_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND status = 1 AND deleted = 0")
    UniversalPlaceOccupantPO findActiveByOccupant(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 查询占用者的所有占用历史
     */
    @Select("SELECT * FROM place_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND deleted = 0 ORDER BY check_in_time DESC")
    List<UniversalPlaceOccupantPO> findAllByOccupant(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 检查位置是否被占用
     */
    @Select("SELECT COUNT(*) > 0 FROM place_occupants WHERE place_id = #{placeId} AND position_no = #{positionNo} AND status = 1 AND deleted = 0")
    boolean isPositionOccupied(@Param("placeId") Long placeId, @Param("positionNo") String positionNo);

    /**
     * 检查占用者是否有活跃占用
     */
    @Select("SELECT COUNT(*) > 0 FROM place_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND status = 1 AND deleted = 0")
    boolean hasActiveOccupancy(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 统计空间的活跃占用数
     */
    @Select("SELECT COUNT(*) FROM place_occupants WHERE place_id = #{placeId} AND status = 1 AND deleted = 0")
    int countActiveByPlaceId(@Param("placeId") Long placeId);

    /**
     * 批量退出
     */
    @Update("<script>" +
            "UPDATE place_occupants SET status = 0, check_out_time = NOW(), updated_at = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchCheckOut(@Param("ids") List<Long> ids);

    /**
     * 查询某用户的所有在住记录（跨场所）
     */
    @Select("SELECT * FROM place_occupants WHERE occupant_id = #{occupantId} AND status = 1 AND deleted = 0")
    List<UniversalPlaceOccupantPO> findActiveByOccupantId(@Param("occupantId") Long occupantId);

    /**
     * 清空在住记录中的组织名称（组织删除时调用）
     */
    @Update("UPDATE place_occupants SET org_unit_name = NULL, updated_at = NOW() " +
            "WHERE org_unit_name = #{orgUnitName} AND status = 1 AND deleted = 0")
    int clearOrgUnitName(@Param("orgUnitName") String orgUnitName);
}
