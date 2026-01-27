package com.school.management.infrastructure.persistence.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * MyBatis mapper for schedule executions.
 */
@Mapper
public interface ScheduleExecutionMapper extends BaseMapper<ScheduleExecutionPO> {

    @Select("SELECT * FROM schedule_executions WHERE policy_id = #{policyId} AND execution_date = #{date} AND deleted = 0")
    List<ScheduleExecutionPO> findByPolicyIdAndDate(@Param("policyId") Long policyId, @Param("date") LocalDate date);

    @Select("SELECT * FROM schedule_executions WHERE execution_date = #{date} AND deleted = 0 ORDER BY created_at DESC")
    List<ScheduleExecutionPO> findByDate(@Param("date") LocalDate date);

    @Select("SELECT * FROM schedule_executions WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<ScheduleExecutionPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM schedule_executions WHERE execution_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY execution_date DESC")
    List<ScheduleExecutionPO> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
