package com.school.management.interfaces.rest.message;

import com.school.management.application.message.preference.UserMsgPreferenceService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * S-3: 用户消息偏好 REST API.
 *
 * 当前用户在自己的偏好范围内 GET / PUT / DELETE.
 */
@RestController
@RequestMapping("/message/preferences")
@RequiredArgsConstructor
public class UserMsgPreferenceController {

    private final UserMsgPreferenceService preferenceService;

    /** 列出当前用户所有偏好 */
    @GetMapping
    public Result<List<Map<String, Object>>> myPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(preferenceService.listForUser(0L, userId));
    }

    /** Upsert 偏好 (按 eventTypeCode 唯一, null = 全局默认) */
    @PutMapping
    public Result<Void> upsert(@RequestBody UpsertRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        preferenceService.upsert(0L, userId, req.eventTypeCode(),
                req.channels(), req.quietHoursStart(), req.quietHoursEnd(),
                req.enabled() != null ? req.enabled() : true);
        return Result.success();
    }

    /** 删除一条偏好 (回退到全局/系统默认) */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        preferenceService.delete(0L, userId, id);
        return Result.success();
    }

    public record UpsertRequest(
            String eventTypeCode,
            List<String> channels,
            String quietHoursStart,
            String quietHoursEnd,
            Boolean enabled
    ) {}
}
