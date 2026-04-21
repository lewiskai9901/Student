package com.school.management.application.message.targetmode;

import com.school.management.infrastructure.extension.TargetModeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * BY_SUBJECT — 主体本人 (subject_type=user 时).
 *
 * 事件主体本身是 user 时, 把消息发给该 user. 其它 subject_type 返回空.
 */
@Slf4j
@Component
public class BySubjectTargetMode implements TargetModeResolver {

    @Override
    public String modeCode() {
        return "BY_SUBJECT";
    }

    @Override
    public String displayName() {
        return "主体本人";
    }

    @Override
    public boolean supportsPreview() { return false; }

    @Override
    public List<Long> resolve(Map<String, Object> config, Map<String, Object> event) {
        try {
            if (event == null || event.isEmpty()) return List.of();
            Object subjectIdObj = event.get("subjectId");
            Object subjectTypeObj = event.get("subjectType");
            if (subjectIdObj == null) return List.of();
            if (!(subjectTypeObj instanceof String st) || !"USER".equalsIgnoreCase(st)) {
                return List.of();
            }
            long id = ((Number) subjectIdObj).longValue();
            return List.of(id);
        } catch (Exception e) {
            log.warn("[BY_SUBJECT] 解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
