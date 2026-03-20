package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface BookingSeatAssignmentMapper extends BaseMapper<BookingSeatAssignmentPO> {

    @Select("SELECT * FROM booking_seat_assignments WHERE booking_id = #{bookingId} AND deleted = 0")
    List<BookingSeatAssignmentPO> findByBookingId(@Param("bookingId") Long bookingId);

    @Delete("DELETE FROM booking_seat_assignments WHERE booking_id = #{bookingId}")
    int physicalDeleteByBookingId(@Param("bookingId") Long bookingId);
}
