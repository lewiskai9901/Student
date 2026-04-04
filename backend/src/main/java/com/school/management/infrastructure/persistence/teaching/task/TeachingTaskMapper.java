package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
@DataPermission(module = "teaching_task", orgUnitField = "org_unit_id", creatorField = "created_by")
public interface TeachingTaskMapper extends BaseMapper<TeachingTaskPO> {

    @Select("SELECT course_id, weekly_hours, total_hours " +
            "FROM curriculum_plan_courses WHERE plan_id = #{planId}")
    List<Map<String, Object>> selectPlanCourses(Long planId);
}
