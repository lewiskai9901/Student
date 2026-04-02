package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InspTaskMapper extends BaseMapper<InspTaskPO> {

    // 单条精确查询（按任务编码）：不加数据权限，通常用于内部精确获取
    @Select("SELECT * FROM insp_tasks WHERE task_code = #{taskCode} AND deleted = 0")
    InspTaskPO findByTaskCode(@Param("taskCode") String taskCode);

    /**
     * 按项目查询任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND deleted = 0 ORDER BY task_date, time_slot_start")
    List<InspTaskPO> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 按检查员查询任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE inspector_id = #{inspectorId} AND deleted = 0 ORDER BY task_date DESC")
    List<InspTaskPO> findByInspectorId(@Param("inspectorId") Long inspectorId);

    /**
     * 按项目和日期查询任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND task_date = #{taskDate} AND deleted = 0")
    List<InspTaskPO> findByProjectIdAndTaskDate(@Param("projectId") Long projectId, @Param("taskDate") LocalDate taskDate);

    /**
     * 按项目和日期范围查询任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE project_id = #{projectId} AND task_date >= #{startDate} AND task_date <= #{endDate} AND deleted = 0 ORDER BY task_date, time_slot_start")
    List<InspTaskPO> findByProjectIdAndTaskDateBetween(@Param("projectId") Long projectId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    /**
     * 按状态查询任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE status = #{status} AND deleted = 0")
    List<InspTaskPO> findByStatus(@Param("status") String status);

    /**
     * 列举所有任务 — 应用数据权限过滤（最重要：列表页入口）
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE deleted = 0 ORDER BY task_date DESC, time_slot_start")
    List<InspTaskPO> findAllTasks();

    /**
     * 查询可用任务列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_tasks WHERE status = 'PENDING' AND inspector_id IS NULL AND deleted = 0 ORDER BY task_date")
    List<InspTaskPO> findAvailableTasks();
}
