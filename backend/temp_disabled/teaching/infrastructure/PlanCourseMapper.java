package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 培养方案课程Mapper
 */
@Mapper
public interface PlanCourseMapper extends BaseMapper<PlanCoursePO> {

    /**
     * 删除方案的所有课程
     */
    @Delete("DELETE FROM curriculum_plan_courses WHERE plan_id = #{planId}")
    void deleteByPlanId(@Param("planId") Long planId);
}
