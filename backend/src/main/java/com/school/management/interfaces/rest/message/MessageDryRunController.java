package com.school.management.interfaces.rest.message;

import com.school.management.application.message.MessageDispatcher;
import com.school.management.common.result.Result;
import com.school.management.domain.event.model.EntityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * S-5: 消息分发 dry-run — 给定 mock event 预览会派发给谁, 不真发.
 *
 * 用法:
 *   POST /message/dry-run
 *   { eventCategory: "GRADE", eventType: "GRADE_SUBMITTED",
 *     subjectType: "USER", subjectId: 100, payload: "{\"course\":\"高数\"}" }
 *
 * 返回所有命中规则 + 各自接收人 ID 列表.
 */
@RestController
@RequestMapping("/message/dry-run")
@RequiredArgsConstructor
public class MessageDryRunController {

    private final MessageDispatcher messageDispatcher;

    @PostMapping
    public Result<List<MessageDispatcher.DryRunRuleResult>> dryRun(@RequestBody DryRunRequest req) {
        if (req.eventType() == null || req.eventType().isBlank()) {
            throw new IllegalArgumentException("eventType 不能为空");
        }
        EntityEvent mockEvent = EntityEvent.create(
                req.subjectType() != null ? req.subjectType() : "USER",
                req.subjectId() != null ? req.subjectId() : 0L,
                req.subjectName(),
                req.eventCategory(),
                req.eventType(),
                req.eventLabel(),
                req.payload(),
                req.sourceModule(),
                req.sourceRefType(),
                req.sourceRefId(),
                null, // tags
                0L,   // createdBy (mock)
                "dry-run"
        );
        return Result.success(messageDispatcher.dryRun(mockEvent));
    }

    public record DryRunRequest(
            String eventCategory,
            String eventType,
            String eventLabel,
            String subjectType,
            Long subjectId,
            String subjectName,
            String payload,
            String sourceModule,
            String sourceRefType,
            Long sourceRefId,
            Map<String, Object> extra
    ) {}
}
