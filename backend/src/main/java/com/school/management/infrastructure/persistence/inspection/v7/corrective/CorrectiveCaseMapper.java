package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CorrectiveCaseMapper extends BaseMapper<CorrectiveCasePO> {

    // 单条精确查询（按案例编码）：不加数据权限，通常用于内部精确获取
    @Select("SELECT * FROM insp_corrective_cases WHERE case_code = #{caseCode} AND deleted = 0")
    CorrectiveCasePO findByCaseCode(@Param("caseCode") String caseCode);

    /**
     * 按项目查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE project_id = #{projectId} AND deleted = 0 ORDER BY created_at DESC")
    List<CorrectiveCasePO> findByProjectId(@Param("projectId") Long projectId);

    /**
     * 按提交记录查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE submission_id = #{submissionId} AND deleted = 0")
    List<CorrectiveCasePO> findBySubmissionId(@Param("submissionId") Long submissionId);

    /**
     * 按负责人查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE assignee_id = #{assigneeId} AND deleted = 0 ORDER BY deadline")
    List<CorrectiveCasePO> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    /**
     * 按状态查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE status = #{status} AND deleted = 0")
    List<CorrectiveCasePO> findByStatus(@Param("status") String status);

    /**
     * 按优先级查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE priority = #{priority} AND deleted = 0")
    List<CorrectiveCasePO> findByPriority(@Param("priority") String priority);

    /**
     * 查询逾期整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE deadline < #{now} AND status NOT IN ('CLOSED','VERIFIED','ESCALATED') AND deleted = 0")
    List<CorrectiveCasePO> findOverdue(@Param("now") LocalDateTime now);

    /**
     * 按任务查询整改案例列表 — 应用数据权限过滤
     */
    @DataPermission(module = "inspection_record", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_corrective_cases WHERE task_id = #{taskId} AND deleted = 0")
    List<CorrectiveCasePO> findByTaskId(@Param("taskId") Long taskId);
}
