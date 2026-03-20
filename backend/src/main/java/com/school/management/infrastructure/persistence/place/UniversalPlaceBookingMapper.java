package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间预订记录 Mapper
 */
@Mapper
public interface UniversalPlaceBookingMapper extends BaseMapper<UniversalPlaceBookingPO> {

    /**
     * 查询空间的所有预订
     */
    @Select("SELECT * FROM place_bookings WHERE place_id = #{placeId} AND deleted = 0 ORDER BY start_time DESC")
    List<UniversalPlaceBookingPO> findByPlaceId(@Param("placeId") Long placeId);

    /**
     * 查询空间在指定时间范围内的预订
     */
    @Select("SELECT * FROM place_bookings WHERE place_id = #{placeId} " +
            "AND ((start_time >= #{startTime} AND start_time < #{endTime}) " +
            "OR (end_time > #{startTime} AND end_time <= #{endTime}) " +
            "OR (start_time <= #{startTime} AND end_time >= #{endTime})) " +
            "AND deleted = 0 ORDER BY start_time")
    List<UniversalPlaceBookingPO> findByPlaceIdAndTimeRange(@Param("placeId") Long placeId,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询空间的活跃预订
     */
    @Select("SELECT * FROM place_bookings WHERE place_id = #{placeId} AND status IN (1, 2) AND deleted = 0 ORDER BY start_time")
    List<UniversalPlaceBookingPO> findActiveByPlaceId(@Param("placeId") Long placeId);

    /**
     * 查询用户的预订
     */
    @Select("SELECT * FROM place_bookings WHERE booker_id = #{bookerId} AND deleted = 0 ORDER BY start_time DESC")
    List<UniversalPlaceBookingPO> findByBookerId(@Param("bookerId") Long bookerId);

    /**
     * 查询用户的活跃预订
     */
    @Select("SELECT * FROM place_bookings WHERE booker_id = #{bookerId} AND status IN (1, 2) AND deleted = 0 ORDER BY start_time")
    List<UniversalPlaceBookingPO> findActiveByBookerId(@Param("bookerId") Long bookerId);

    /**
     * 检查时间段是否有冲突
     */
    @Select("<script>" +
            "SELECT COUNT(*) > 0 FROM place_bookings WHERE place_id = #{placeId} " +
            "AND status IN (1, 2) " +
            "AND NOT (end_time &lt;= #{startTime} OR start_time &gt;= #{endTime}) " +
            "<if test='excludeBookingId != null'>" +
            "AND id != #{excludeBookingId} " +
            "</if>" +
            "AND deleted = 0" +
            "</script>")
    boolean hasConflict(@Param("placeId") Long placeId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("excludeBookingId") Long excludeBookingId);

    /**
     * 查询需要自动完成的预订
     */
    @Select("SELECT * FROM place_bookings WHERE end_time < #{beforeTime} AND status IN (1, 2) AND deleted = 0")
    List<UniversalPlaceBookingPO> findExpiredBookings(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 批量更新状态
     */
    @Update("<script>" +
            "UPDATE place_bookings SET status = #{status}, updated_at = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") int status);
}
