package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 培养方案 Mapper
 */
@Mapper
public interface CurriculumPlanPersistenceMapper extends BaseMapper<CurriculumPlanPO> {

    @Select("SELECT MAX(version) FROM curriculum_plans WHERE plan_code = #{planCode} AND deleted = 0")
    Integer findMaxVersionByPlanCode(@Param("planCode") String planCode);
}
