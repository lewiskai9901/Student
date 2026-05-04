package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.preference;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "teacher_preference", orgUnitField = "", creatorField = "teacher_id")
public interface TeacherPreferenceMapper extends BaseMapper<TeacherPreferencePO> {
}
