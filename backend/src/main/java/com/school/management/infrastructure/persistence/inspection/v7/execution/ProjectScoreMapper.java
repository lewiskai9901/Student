package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProjectScoreMapper extends BaseMapper<ProjectScorePO> {

    @Select("SELECT * FROM insp_project_scores WHERE project_id = #{projectId} AND cycle_date = #{cycleDate} AND deleted = 0")
    ProjectScorePO findByProjectIdAndCycleDate(@Param("projectId") Long projectId,
                                                @Param("cycleDate") LocalDate cycleDate);

    @Select("SELECT * FROM insp_project_scores WHERE project_id = #{projectId} AND deleted = 0 ORDER BY cycle_date DESC")
    List<ProjectScorePO> findByProjectId(@Param("projectId") Long projectId);
}
