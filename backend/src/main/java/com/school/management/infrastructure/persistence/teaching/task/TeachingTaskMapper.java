package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeachingTaskMapper extends BaseMapper<TeachingTaskPO> {

    @Select("SELECT course_id, weekly_hours, total_hours " +
            "FROM curriculum_plan_courses WHERE plan_id = #{planId}")
    List<Map<String, Object>> selectPlanCourses(Long planId);
}
