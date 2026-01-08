package com.school.management.infrastructure.persistence.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务 Mapper 接口 (DDD 版本)
 */
@Mapper
public interface TaskDomainMapper extends BaseMapper<TaskPO> {

    @Select("SELECT * FROM tasks WHERE task_code = #{taskCode} AND deleted = 0")
    TaskPO selectByTaskCode(@Param("taskCode") String taskCode);

    @Select("SELECT * FROM tasks WHERE assigner_id = #{assignerId} AND deleted = 0 ORDER BY created_at DESC")
    List<TaskPO> selectByAssignerId(@Param("assignerId") Long assignerId);

    @Select("SELECT * FROM tasks WHERE department_id = #{departmentId} AND deleted = 0 ORDER BY created_at DESC")
    List<TaskPO> selectByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT * FROM tasks WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<TaskPO> selectByStatus(@Param("status") Integer status);

    @Select("SELECT * FROM tasks WHERE due_date < #{dueDate} AND status NOT IN (3, 4) AND deleted = 0")
    List<TaskPO> selectOverdue(@Param("dueDate") LocalDateTime dueDate);

    @Select("SELECT COUNT(*) FROM tasks WHERE task_code = #{taskCode} AND deleted = 0")
    long countByTaskCode(@Param("taskCode") String taskCode);

    @Select("SELECT COUNT(*) FROM tasks WHERE department_id = #{departmentId} AND deleted = 0")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT COUNT(*) FROM tasks WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") Integer status);
}
