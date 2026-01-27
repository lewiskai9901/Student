package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 调课记录Mapper
 */
@Mapper
public interface ScheduleAdjustmentMapper extends BaseMapper<ScheduleAdjustmentPO> {

    /**
     * 根据学期ID查询待审批的调课申请
     */
    @Select("SELECT sa.* FROM schedule_adjustments sa " +
            "INNER JOIN schedule_entries se ON sa.entry_id = se.id " +
            "INNER JOIN course_schedules cs ON se.schedule_id = cs.id " +
            "WHERE cs.semester_id = #{semesterId} AND sa.status = 0 " +
            "ORDER BY sa.created_at DESC")
    List<ScheduleAdjustmentPO> findPendingBySemesterId(@Param("semesterId") Long semesterId);
}
