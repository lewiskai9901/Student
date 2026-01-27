package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课表Mapper
 */
@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedulePO> {
}
