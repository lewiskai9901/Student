package com.school.management.infrastructure.extension.event;

import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.Violation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Policy SPI 产生的非 BLOCK 违规事件 (WARN / INFO).
 *
 * PolicyRegistry.check()/enforce() 若结果含非 BLOCK 的 violations, 会发布此事件.
 * 由 {@link com.school.management.application.event.PolicyWarningToNotificationListener}
 * 监听并转为 POLICY_WARNING 触发点 fire, 进入消息派发管道.
 *
 * BLOCK 违规不走此路径 — enforce() 会抛 PolicyViolationException 中断调用方流程,
 * 消息来不及产出; 若希望 BLOCK 也通知, 应在调用方捕获异常后主动 triggerService.fire.
 */
@Getter
public class PolicyWarningEvent extends ApplicationEvent {

    private final PolicyContext<?> context;
    private final List<Violation> violations;

    public PolicyWarningEvent(Object source, PolicyContext<?> context, List<Violation> violations) {
        super(source);
        this.context = context;
        this.violations = violations;
    }
}
