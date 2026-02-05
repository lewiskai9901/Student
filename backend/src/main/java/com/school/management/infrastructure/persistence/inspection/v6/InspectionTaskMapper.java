package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * V6检查任务Mapper
 */
@Mapper
public interface InspectionTaskMapper extends BaseMapper<InspectionTaskPO> {

    @Select("SELECT * FROM inspection_tasks WHERE task_code = #{taskCode} AND deleted = 0")
    InspectionTaskPO findByTaskCode(@Param("taskCode") String taskCode);

    @Select("SELECT * FROM inspection_tasks WHERE project_id = #{projectId} AND deleted = 0 ORDER BY task_date DESC, time_slot")
    List<InspectionTaskPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM inspection_tasks WHERE project_id = #{projectId} AND task_date = #{taskDate} AND deleted = 0")
    List<InspectionTaskPO> findByProjectIdAndDate(@Param("projectId") Long projectId, @Param("taskDate") LocalDate taskDate);

    @Select("SELECT * FROM inspection_tasks WHERE inspector_id = #{inspectorId} AND deleted = 0 ORDER BY task_date DESC")
    List<InspectionTaskPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    @Select("SELECT * FROM inspection_tasks WHERE status = #{status} AND deleted = 0 ORDER BY task_date DESC")
    List<InspectionTaskPO> findByStatus(@Param("status") String status);

    @Select("SELECT t.*, p.project_name FROM inspection_tasks t " +
            "LEFT JOIN inspection_projects p ON t.project_id = p.id " +
            "WHERE t.task_date = #{date} AND t.status = 'PENDING' AND t.inspector_id IS NULL AND t.deleted = 0")
    List<InspectionTaskPO> findAvailableTasksForDate(@Param("date") LocalDate date);

    @Select("SELECT t.*, p.project_name FROM inspection_tasks t " +
            "LEFT JOIN inspection_projects p ON t.project_id = p.id " +
            "WHERE t.inspector_id = #{inspectorId} AND t.status IN ('PENDING', 'IN_PROGRESS') AND t.deleted = 0 " +
            "ORDER BY t.task_date")
    List<InspectionTaskPO> findMyTasks(@Param("inspectorId") Long inspectorId);

    @Select("<script>" +
            "SELECT t.*, p.project_name FROM inspection_tasks t " +
            "LEFT JOIN inspection_projects p ON t.project_id = p.id " +
            "WHERE t.deleted = 0 " +
            "<if test='projectId != null'>AND t.project_id = #{projectId}</if> " +
            "<if test='status != null'>AND t.status = #{status}</if> " +
            "<if test='startDate != null'>AND t.task_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND t.task_date &lt;= #{endDate}</if> " +
            "<if test='inspectorId != null'>AND t.inspector_id = #{inspectorId}</if> " +
            "ORDER BY t.task_date DESC, t.time_slot LIMIT #{offset}, #{limit}" +
            "</script>")
    List<InspectionTaskPO> findPagedWithConditions(@Param("offset") int offset,
                                                    @Param("limit") int limit,
                                                    @Param("projectId") Long projectId,
                                                    @Param("status") String status,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("inspectorId") Long inspectorId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM inspection_tasks t WHERE t.deleted = 0 " +
            "<if test='projectId != null'>AND t.project_id = #{projectId}</if> " +
            "<if test='status != null'>AND t.status = #{status}</if> " +
            "<if test='startDate != null'>AND t.task_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND t.task_date &lt;= #{endDate}</if> " +
            "<if test='inspectorId != null'>AND t.inspector_id = #{inspectorId}</if> " +
            "</script>")
    long countWithConditions(@Param("projectId") Long projectId,
                             @Param("status") String status,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("inspectorId") Long inspectorId);

    @Update("UPDATE inspection_tasks SET inspector_id = #{inspectorId}, inspector_name = #{inspectorName}, " +
            "claimed_at = NOW(), updated_at = NOW() WHERE id = #{id} AND inspector_id IS NULL")
    int claimTask(@Param("id") Long id, @Param("inspectorId") Long inspectorId, @Param("inspectorName") String inspectorName);

    @Update("UPDATE inspection_tasks SET total_targets = #{totalTargets}, updated_at = NOW() WHERE id = #{id}")
    void updateTotalTargets(@Param("id") Long id, @Param("totalTargets") Integer totalTargets);

    @Update("UPDATE inspection_tasks SET completed_targets = completed_targets + 1, updated_at = NOW() WHERE id = #{id}")
    void incrementCompletedTargets(@Param("id") Long id);

    @Update("UPDATE inspection_tasks SET skipped_targets = skipped_targets + 1, updated_at = NOW() WHERE id = #{id}")
    void incrementSkippedTargets(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM inspection_tasks WHERE project_id = #{projectId} AND task_date = #{taskDate} AND deleted = 0")
    int countByProjectAndDate(@Param("projectId") Long projectId, @Param("taskDate") LocalDate taskDate);
}
