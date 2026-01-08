package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.task.TaskSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务提交记录Mapper
 */
@Mapper
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

}
