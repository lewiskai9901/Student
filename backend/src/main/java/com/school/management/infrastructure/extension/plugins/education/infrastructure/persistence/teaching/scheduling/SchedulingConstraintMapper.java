package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "scheduling_constraint", orgUnitField = "org_unit_id", creatorField = "created_by")
public interface SchedulingConstraintMapper extends BaseMapper<SchedulingConstraintPO> {
}
