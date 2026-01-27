package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课表条目Mapper
 */
@Mapper
public interface ScheduleEntryMapper extends BaseMapper<ScheduleEntryPO> {

    /**
     * 删除课表的所有条目
     */
    @Delete("DELETE FROM schedule_entries WHERE schedule_id = #{scheduleId}")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * 根据班级查询课表条目
     */
    @Select("SELECT se.* FROM schedule_entries se " +
            "INNER JOIN teaching_tasks tt ON se.task_id = tt.id " +
            "INNER JOIN course_schedules cs ON se.schedule_id = cs.id " +
            "WHERE cs.semester_id = #{semesterId} AND tt.class_id = #{classId} " +
            "ORDER BY se.weekday, se.slot")
    List<ScheduleEntryPO> findByClassId(@Param("semesterId") Long semesterId, @Param("classId") Long classId);

    /**
     * 根据教师查询课表条目
     */
    @Select("SELECT DISTINCT se.* FROM schedule_entries se " +
            "INNER JOIN teaching_tasks tt ON se.task_id = tt.id " +
            "INNER JOIN teaching_task_teachers ttt ON tt.id = ttt.task_id " +
            "INNER JOIN course_schedules cs ON se.schedule_id = cs.id " +
            "WHERE cs.semester_id = #{semesterId} AND ttt.teacher_id = #{teacherId} " +
            "ORDER BY se.weekday, se.slot")
    List<ScheduleEntryPO> findByTeacherId(@Param("semesterId") Long semesterId, @Param("teacherId") Long teacherId);

    /**
     * 根据教室查询课表条目
     */
    @Select("SELECT se.* FROM schedule_entries se " +
            "INNER JOIN course_schedules cs ON se.schedule_id = cs.id " +
            "WHERE cs.semester_id = #{semesterId} AND se.classroom_id = #{classroomId} " +
            "ORDER BY se.weekday, se.slot")
    List<ScheduleEntryPO> findByClassroomId(@Param("semesterId") Long semesterId, @Param("classroomId") Long classroomId);
}
