package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "course_evaluation", orgUnitField = "org_unit_id", creatorField = "created_by")
public interface CourseEvaluationMapper extends BaseMapper<CourseEvaluationPO> {
}
