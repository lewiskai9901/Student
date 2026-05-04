package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "class_course_assignment", orgUnitField = "org_unit_id", creatorField = "created_by")
public interface ClassCourseAssignmentMapper extends BaseMapper<ClassCourseAssignmentPO> {
}
