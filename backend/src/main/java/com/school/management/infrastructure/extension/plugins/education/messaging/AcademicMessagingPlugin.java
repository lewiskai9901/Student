package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业学业学务事件 — 学业预警/考试优秀/考试不及格 + 成绩发布通知.
 *
 * GradeMessagingPlugin 声明的是 GRADE_*_EVT (带 _EVT 后缀的规范版);
 * 此插件处理与学业成绩相关的"结果"事件及通知类.
 */
@Component
public class AcademicMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "academic"; }
    @Override public String getDomainName() { return "学业"; }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("CREDIT_WARNING", "学业预警",
                "ACADEMIC", "学业", "NEGATIVE", "AlertCircle", "#f59e0b",
                List.of("USER"), "学分/绩点达到预警线"),
            new EventTypeDef("EXAM_EXCELLENT", "考试优秀",
                "ACADEMIC", "学业", "POSITIVE", "GraduationCap", "#22c55e",
                List.of("USER"), "单次考试成绩优秀"),
            new EventTypeDef("EXAM_FAIL", "考试不及格",
                "ACADEMIC", "学业", "NEGATIVE", "AlertTriangle", "#ef4444",
                List.of("USER"), "单次考试成绩不合格"),
            new EventTypeDef("GRADE_RELEASE", "成绩发布",
                "NOTIFICATION", "通知", "NEUTRAL", "FileText", "#3b82f6",
                List.of("USER", "ORG_UNIT"), "成绩对外发布通知(学生家长可见)")
        );
    }
}
