package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HolidayCalendarMapper extends BaseMapper<HolidayCalendarPO> {

    @Select("SELECT * FROM insp_holiday_calendars WHERE year = #{year} AND deleted = 0 ORDER BY is_default DESC")
    List<HolidayCalendarPO> findByYear(@Param("year") Integer year);

    @Select("SELECT * FROM insp_holiday_calendars WHERE is_default = 1 AND deleted = 0 LIMIT 1")
    HolidayCalendarPO findDefault();
}
