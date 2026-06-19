package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "class_course_assignment")
public interface ClassCourseAssignmentMapper extends BaseMapper<ClassCourseAssignmentPO> {
}
