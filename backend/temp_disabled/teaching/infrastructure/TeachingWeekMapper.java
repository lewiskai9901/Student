package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教学周Mapper
 */
@Mapper
public interface TeachingWeekMapper extends BaseMapper<TeachingWeekPO> {

    /**
     * 删除学期的所有教学周
     */
    @Delete("DELETE FROM teaching_weeks WHERE semester_id = #{semesterId}")
    void deleteBySemesterId(@Param("semesterId") Long semesterId);
}
