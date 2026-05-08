package com.school.management.interfaces.rest.demo;

import com.school.management.application.message.MessageDispatcher;
import com.school.management.common.result.Result;
import com.school.management.domain.event.model.EntityEvent;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 演示用临时端点 — 直接 fire mock event 验证消息推送链路.
 * 上线前应删除或加白名单保护.
 */
@Slf4j
@RestController
@RequestMapping("/admin/demo/event")
@RequiredArgsConstructor
@CasbinAccess(resource = "system:message", action = "manage")
public class EventFireDemoController {

    private final MessageDispatcher messageDispatcher;

    @PostMapping("/fire")
    public Result<Map<String, Object>> fire(@RequestBody FireRequest req) {
        EntityEvent event = EntityEvent.create(
                req.subjectType() != null ? req.subjectType() : "USER",
                req.subjectId(),
                req.subjectName(),
                req.eventCategory(),
                req.eventType(),
                req.eventLabel(),
                req.payload(),
                req.sourceModule() != null ? req.sourceModule() : "demo",
                "DEMO", 0L,
                null,
                req.createdBy() != null ? req.createdBy() : 1L,
                req.createdByName() != null ? req.createdByName() : "demo");
        // 直接调 dispatcher (绕过事务事件机制)
        messageDispatcher.dispatch(event);
        return Result.success(Map.of(
                "fired", true,
                "subject", req.subjectName(),
                "event_type", req.eventType()
        ));
    }

    public record FireRequest(
            String subjectType,
            Long subjectId,
            String subjectName,
            String eventCategory,
            String eventType,
            String eventLabel,
            String payload,
            String sourceModule,
            Long createdBy,
            String createdByName
    ) {}
}
