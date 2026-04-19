package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 方案课程 Mapper
 */
@Mapper
public interface PlanCoursePersistenceMapper extends BaseMapper<PlanCoursePO> {

    @Select("SELECT pc.*, c.course_code, c.course_name " +
            "FROM curriculum_plan_courses pc " +
            "LEFT JOIN courses c ON pc.course_id = c.id AND c.deleted = 0 " +
            "WHERE pc.plan_id = #{planId} " +
            "ORDER BY pc.semester_number, pc.sort_order, pc.id")
    List<PlanCoursePO> findByPlanIdWithCourse(@Param("planId") Long planId);

    @Delete("DELETE FROM curriculum_plan_courses WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId") Long planId);
}
