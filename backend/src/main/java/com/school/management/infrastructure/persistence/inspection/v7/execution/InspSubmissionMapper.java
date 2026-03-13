package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InspSubmissionMapper extends BaseMapper<InspSubmissionPO> {

    @Select("SELECT * FROM insp_submissions WHERE task_id = #{taskId} AND deleted = 0 ORDER BY id")
    List<InspSubmissionPO> findByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT * FROM insp_submissions WHERE target_id = #{targetId} AND deleted = 0 ORDER BY created_at DESC")
    List<InspSubmissionPO> findByTargetId(@Param("targetId") Long targetId);

    @Select("SELECT * FROM insp_submissions WHERE task_id = #{taskId} AND updated_at > #{since} AND deleted = 0 ORDER BY id")
    List<InspSubmissionPO> findModifiedAfter(@Param("taskId") Long taskId, @Param("since") LocalDateTime since);
}
