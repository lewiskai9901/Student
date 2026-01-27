package com.school.management.infrastructure.persistence.corrective;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CorrectiveActionMapper extends BaseMapper<CorrectiveActionPO> {

    @Select("SELECT * FROM corrective_actions WHERE action_code = #{actionCode} AND deleted = 0")
    CorrectiveActionPO findByActionCode(@Param("actionCode") String actionCode);

    @Select("SELECT * FROM corrective_actions WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<CorrectiveActionPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM corrective_actions WHERE class_id = #{classId} AND deleted = 0 ORDER BY created_at DESC")
    List<CorrectiveActionPO> findByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM corrective_actions WHERE assignee_id = #{assigneeId} AND deleted = 0 ORDER BY created_at DESC")
    List<CorrectiveActionPO> findByAssigneeId(@Param("assigneeId") Long assigneeId);

    @Select("SELECT * FROM corrective_actions WHERE status IN ('IN_PROGRESS') AND deadline < NOW() AND deleted = 0 ORDER BY deadline ASC")
    List<CorrectiveActionPO> findOverdue();

    @Select("SELECT COUNT(*) FROM corrective_actions WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);

    @Select("SELECT * FROM corrective_actions WHERE source = #{source} AND source_id = #{sourceId} AND deleted = 0")
    List<CorrectiveActionPO> findBySourceAndSourceId(@Param("source") String source, @Param("sourceId") Long sourceId);
}
