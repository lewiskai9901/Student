package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface InspectionPlanMapper extends BaseMapper<InspectionPlanPO> {

    @Select("SELECT * FROM insp_inspection_plans WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order ASC, created_at ASC")
    List<InspectionPlanPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_inspection_plans WHERE project_id = #{projectId} AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order ASC, created_at ASC")
    List<InspectionPlanPO> findEnabledByProjectId(@Param("projectId") Long projectId);

    @Update("UPDATE insp_inspection_plans SET deleted = 1 WHERE project_id = #{projectId} AND deleted = 0")
    int deleteByProjectId(@Param("projectId") Long projectId);
}
