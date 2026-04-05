package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ScheduleEntryPersistenceMapper extends BaseMapper<ScheduleEntryPO> {

    @Select("SELECT * FROM schedule_entries WHERE semester_id = #{sid} AND teacher_id = #{tid} AND weekday = #{day} AND deleted = 0 AND entry_status = 1")
    List<ScheduleEntryPO> findByTeacherAndWeekday(@Param("sid") Long semesterId, @Param("tid") Long teacherId, @Param("day") Integer weekday);

    @Select("SELECT * FROM schedule_entries WHERE semester_id = #{sid} AND classroom_id = #{cid} AND weekday = #{day} AND deleted = 0 AND entry_status = 1")
    List<ScheduleEntryPO> findByClassroomAndWeekday(@Param("sid") Long semesterId, @Param("cid") Long classroomId, @Param("day") Integer weekday);

    @Select("SELECT * FROM schedule_entries WHERE semester_id = #{sid} AND class_id = #{cid} AND weekday = #{day} AND deleted = 0 AND entry_status = 1")
    List<ScheduleEntryPO> findByClassAndWeekday(@Param("sid") Long semesterId, @Param("cid") Long classId, @Param("day") Integer weekday);

    @Delete("DELETE FROM schedule_entries WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
