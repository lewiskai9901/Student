package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRulePO> {

    @Select("SELECT * FROM insp_alert_rules WHERE is_enabled = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<AlertRulePO> findEnabled();
}
