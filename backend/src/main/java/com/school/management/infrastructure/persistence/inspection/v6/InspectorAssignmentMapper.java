package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V6检查员分配Mapper
 */
@Mapper
public interface InspectorAssignmentMapper extends BaseMapper<TaskInspectorAssignmentPO> {

    @Select("SELECT * FROM task_inspector_assignments WHERE task_id = #{taskId}")
    List<TaskInspectorAssignmentPO> findByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM task_inspector_assignments WHERE inspector_id = #{inspectorId}")
    List<TaskInspectorAssignmentPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    @Select("SELECT * FROM task_inspector_assignments WHERE task_id = #{taskId} AND inspector_id = #{inspectorId}")
    TaskInspectorAssignmentPO findByTaskAndInspector(@Param("taskId") Long taskId, @Param("inspectorId") Long inspectorId);

    @Update("UPDATE task_inspector_assignments SET status = #{status}, accepted_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM task_inspector_assignments WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Insert("<script>" +
            "INSERT INTO task_inspector_assignments (task_id, inspector_id, inspector_name, scope_type, scope_ids, status, created_by, created_at) VALUES " +
            "<foreach collection='assignments' item='a' separator=','>" +
            "(#{a.taskId}, #{a.inspectorId}, #{a.inspectorName}, #{a.scopeType}, #{a.scopeIds}, #{a.status}, #{a.createdBy}, NOW())" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("assignments") List<TaskInspectorAssignmentPO> assignments);
}
