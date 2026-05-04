package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DataPermission(module = "schedule_conflict_record", orgUnitField = "org_unit_id", creatorField = "created_by")
public interface ScheduleConflictRecordMapper extends BaseMapper<ScheduleConflictRecordPO> {
}
