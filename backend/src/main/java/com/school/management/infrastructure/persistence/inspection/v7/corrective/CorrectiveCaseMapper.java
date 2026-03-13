package com.school.management.infrastructure.persistence.inspection.v7.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CorrectiveCaseMapper extends BaseMapper<CorrectiveCasePO> {

    @Select("SELECT * FROM insp_corrective_cases WHERE case_code = #{caseCode} AND deleted = 0")
    CorrectiveCasePO findByCaseCode(@Param("caseCode") String caseCode);

    @Select("SELECT * FROM insp_corrective_cases WHERE project_id = #{projectId} AND deleted = 0 ORDER BY created_at DESC")
    List<CorrectiveCasePO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_corrective_cases WHERE submission_id = #{submissionId} AND deleted = 0")
    List<CorrectiveCasePO> findBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("SELECT * FROM insp_corrective_cases WHERE assignee_id = #{assigneeId} AND deleted = 0 ORDER BY deadline")
    List<CorrectiveCasePO> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    @Select("SELECT * FROM insp_corrective_cases WHERE status = #{status} AND deleted = 0")
    List<CorrectiveCasePO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM insp_corrective_cases WHERE priority = #{priority} AND deleted = 0")
    List<CorrectiveCasePO> findByPriority(@Param("priority") String priority);

    @Select("SELECT * FROM insp_corrective_cases WHERE deadline < #{now} AND status NOT IN ('CLOSED','VERIFIED','ESCALATED') AND deleted = 0")
    List<CorrectiveCasePO> findOverdue(@Param("now") LocalDateTime now);

    @Select("SELECT * FROM insp_corrective_cases WHERE task_id = #{taskId} AND deleted = 0")
    List<CorrectiveCasePO> findByTaskId(@Param("taskId") Long taskId);
}
