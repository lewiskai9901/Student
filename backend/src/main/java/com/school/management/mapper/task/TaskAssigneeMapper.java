package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.task.TaskAssignee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务执行人Mapper
 */
@Mapper
public interface TaskAssigneeMapper extends BaseMapper<TaskAssignee> {

    /**
     * 批量插入执行人记录
     *
     * @param assignees 执行人列表
     * @return 插入记录数
     */
    int batchInsert(@Param("assignees") List<TaskAssignee> assignees);

}
