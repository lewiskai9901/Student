package com.school.management.infrastructure.persistence.space;

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
public interface UniversalSpaceOccupantMapper extends BaseMapper<UniversalSpaceOccupantPO> {

    /**
     * 查询空间的当前活跃占用者
     */
    @Select("SELECT * FROM space_occupants WHERE space_id = #{spaceId} AND status = 1 AND deleted = 0 ORDER BY position_no")
    List<UniversalSpaceOccupantPO> findActiveBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询空间的所有占用记录（包括历史）
     */
    @Select("SELECT * FROM space_occupants WHERE space_id = #{spaceId} AND deleted = 0 ORDER BY check_in_time DESC")
    List<UniversalSpaceOccupantPO> findAllBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询占用者的当前活跃占用记录
     */
    @Select("SELECT * FROM space_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND status = 1 AND deleted = 0")
    UniversalSpaceOccupantPO findActiveByOccupant(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 查询占用者的所有占用历史
     */
    @Select("SELECT * FROM space_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND deleted = 0 ORDER BY check_in_time DESC")
    List<UniversalSpaceOccupantPO> findAllByOccupant(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 检查位置是否被占用
     */
    @Select("SELECT COUNT(*) > 0 FROM space_occupants WHERE space_id = #{spaceId} AND position_no = #{positionNo} AND status = 1 AND deleted = 0")
    boolean isPositionOccupied(@Param("spaceId") Long spaceId, @Param("positionNo") String positionNo);

    /**
     * 检查占用者是否有活跃占用
     */
    @Select("SELECT COUNT(*) > 0 FROM space_occupants WHERE occupant_type = #{occupantType} AND occupant_id = #{occupantId} AND status = 1 AND deleted = 0")
    boolean hasActiveOccupancy(@Param("occupantType") String occupantType, @Param("occupantId") Long occupantId);

    /**
     * 统计空间的活跃占用数
     */
    @Select("SELECT COUNT(*) FROM space_occupants WHERE space_id = #{spaceId} AND status = 1 AND deleted = 0")
    int countActiveBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 批量退出
     */
    @Update("<script>" +
            "UPDATE space_occupants SET status = 0, check_out_time = NOW(), updated_at = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchCheckOut(@Param("ids") List<Long> ids);
}
