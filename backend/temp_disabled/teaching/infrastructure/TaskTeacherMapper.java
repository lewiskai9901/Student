package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教学任务教师Mapper
 */
@Mapper
public interface TaskTeacherMapper extends BaseMapper<TaskTeacherPO> {

    /**
     * 删除任务的所有教师
     */
    @Delete("DELETE FROM teaching_task_teachers WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
