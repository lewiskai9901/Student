package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InspTaskMapper extends BaseMapper<InspTaskPO> {

    @Select("SELECT * FROM insp_tasks WHERE task_code = #{taskCode} AND deleted = 0")
    InspTaskPO findByTaskCode(@Param("taskCode") String taskCode);

    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND deleted = 0 ORDER BY task_date, time_slot_start")
    List<InspTaskPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_tasks WHERE inspector_id = #{inspectorId} AND deleted = 0 ORDER BY task_date DESC")
    List<InspTaskPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND task_date = #{taskDate} AND deleted = 0")
    List<InspTaskPO> findByProjectIdAndTaskDate(@Param("projectId") Long projectId, @Param("taskDate") LocalDate taskDate);

    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND task_date >= #{startDate} AND task_date <= #{endDate} AND deleted = 0 ORDER BY task_date, time_slot_start")
    List<InspTaskPO> findByProjectIdAndTaskDateBetween(@Param("projectId") Long projectId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM insp_tasks WHERE status = #{status} AND deleted = 0")
    List<InspTaskPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM insp_tasks WHERE deleted = 0 ORDER BY task_date DESC, time_slot_start")
    List<InspTaskPO> findAllTasks();

    @Select("SELECT * FROM insp_tasks WHERE status = 'PENDING' AND inspector_id IS NULL AND deleted = 0 ORDER BY task_date")
    List<InspTaskPO> findAvailableTasks();
}
