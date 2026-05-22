package com.school.management.infrastructure.persistence.inspection.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRulePO> {

    @DataPermission(module = "inspection_alert", orgUnitField = "org_unit_id", creatorField = "created_by")
    @Select("SELECT * FROM insp_alert_rules WHERE is_enabled = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<AlertRulePO> findEnabled();
}
