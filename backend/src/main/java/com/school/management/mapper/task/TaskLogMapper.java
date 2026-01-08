package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.task.TaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务操作日志Mapper
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {

}
