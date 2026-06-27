package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.teachingclass;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "teaching_class")
public interface TeachingClassMapper extends BaseMapper<TeachingClassPO> {
}
