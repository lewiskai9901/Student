package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "grade_batch")
public interface GradeBatchMapper extends BaseMapper<GradeBatchPO> {
}
