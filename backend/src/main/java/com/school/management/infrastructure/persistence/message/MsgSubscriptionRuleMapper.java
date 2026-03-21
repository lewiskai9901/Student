package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息订阅规则 MyBatis Mapper
 */
@Mapper
public interface MsgSubscriptionRuleMapper extends BaseMapper<MsgSubscriptionRulePO> {

    @Select("SELECT * FROM msg_subscription_rules WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<MsgSubscriptionRulePO> findEnabled();

    @Select("<script>" +
            "SELECT * FROM msg_subscription_rules WHERE is_enabled = 1 AND deleted = 0" +
            " AND (event_category IS NULL OR event_category = #{eventCategory})" +
            " AND (event_type IS NULL OR event_type = #{eventType})" +
            " ORDER BY sort_order" +
            "</script>")
    List<MsgSubscriptionRulePO> findByEvent(@Param("eventCategory") String eventCategory,
                                             @Param("eventType") String eventType);

    @Select("SELECT * FROM msg_subscription_rules WHERE deleted = 0 ORDER BY sort_order")
    List<MsgSubscriptionRulePO> findAll();
}
