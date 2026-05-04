package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生提交记录:
 *   - 学生: SELF (只看自己提交的, creator=student_id)
 *   - 教师: 通过 teacher_id 直接过滤(应用层加 where), 这里走 org_unit (学生班级) 兼顾管理员
 *   - 管理员: DEPARTMENT_AND_BELOW
 */
@Mapper
@DataPermission(module = "evaluation_response", orgUnitField = "org_unit_id", creatorField = "student_id")
public interface EvaluationResponseMapper extends BaseMapper<EvaluationResponsePO> {
}
