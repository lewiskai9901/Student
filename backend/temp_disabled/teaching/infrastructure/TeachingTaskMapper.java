package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 教学任务Mapper
 */
@Mapper
public interface TeachingTaskMapper extends BaseMapper<TeachingTaskPO> {

    /**
     * 根据教师ID查询教学任务
     */
    @Select("SELECT t.* FROM teaching_tasks t " +
            "INNER JOIN teaching_task_teachers tt ON t.id = tt.task_id " +
            "WHERE tt.teacher_id = #{teacherId} " +
            "ORDER BY t.created_at DESC")
    List<TeachingTaskPO> findByTeacherId(@Param("teacherId") Long teacherId);
}
