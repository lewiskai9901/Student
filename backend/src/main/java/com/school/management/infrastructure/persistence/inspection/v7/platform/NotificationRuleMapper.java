package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationRuleMapper extends BaseMapper<NotificationRulePO> {

    @Select("SELECT * FROM insp_notification_rules WHERE project_id = #{projectId} AND deleted = 0 ORDER BY priority DESC")
    List<NotificationRulePO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_notification_rules WHERE event_type = #{eventType} AND is_enabled = 1 AND deleted = 0 ORDER BY priority DESC")
    List<NotificationRulePO> findByEventType(@Param("eventType") String eventType);

    @Select("SELECT * FROM insp_notification_rules WHERE is_enabled = 1 AND deleted = 0 ORDER BY priority DESC")
    List<NotificationRulePO> findAllEnabled();
}
