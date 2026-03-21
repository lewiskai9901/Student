package com.school.management.domain.message.repository;

import com.school.management.domain.message.model.MsgSubscriptionRule;

import java.util.List;
import java.util.Optional;

/**
 * 消息订阅规则仓储接口
 */
public interface MsgSubscriptionRuleRepository {

    MsgSubscriptionRule save(MsgSubscriptionRule rule);

    Optional<MsgSubscriptionRule> findById(Long id);

    List<MsgSubscriptionRule> findAll();

    List<MsgSubscriptionRule> findEnabled();

    List<MsgSubscriptionRule> findByEventType(String eventCategory, String eventType);

    void deleteById(Long id);
}
