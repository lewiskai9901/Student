package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookSubscriptionMapper extends BaseMapper<WebhookSubscriptionPO> {

    @Select("SELECT * FROM insp_webhook_subscriptions WHERE project_id = #{projectId} AND deleted = 0 ORDER BY created_at DESC")
    List<WebhookSubscriptionPO> findByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM insp_webhook_subscriptions WHERE event_types LIKE CONCAT('%', #{eventType}, '%') AND is_enabled = 1 AND deleted = 0")
    List<WebhookSubscriptionPO> findByEventType(@Param("eventType") String eventType);

    @Select("SELECT * FROM insp_webhook_subscriptions WHERE is_enabled = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<WebhookSubscriptionPO> findAllEnabled();
}
