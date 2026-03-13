package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskSectionAssignmentMapper extends BaseMapper<TaskSectionAssignmentPO> {

    @Select("SELECT * FROM insp_task_section_assignments WHERE task_id = #{taskId} AND deleted = 0 ORDER BY created_at")
    List<TaskSectionAssignmentPO> findByTaskId(@Param("taskId") Long taskId);
}
