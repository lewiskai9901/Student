package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TeachingWeekMapper extends BaseMapper<TeachingWeekPO> {

    @Select("SELECT * FROM academic_weeks WHERE semester_id = #{semesterId} ORDER BY week_number")
    List<TeachingWeekPO> findBySemesterId(@Param("semesterId") Long semesterId);

    @Delete("DELETE FROM academic_weeks WHERE semester_id = #{semesterId}")
    void deleteBySemesterId(@Param("semesterId") Long semesterId);
}
