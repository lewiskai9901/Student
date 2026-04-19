package com.school.management.infrastructure.extension.plugins.core.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.core.constants.InspectionTriggerPoints.*;

/**
 * 检查平台消息插件 — 检查是通用能力,触发点放 core.
 * 具体的检查模板由行业插件贡献(未来会加 InspectionTemplateContributionPlugin).
 */
@Component
public class InspectionMessagingPlugin implements MessagingDomainPlugin {

    @Override
    public String getDomainCode() { return "inspection"; }

    @Override
    public String getDomainName() { return "检查"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(INSP_ITEM_RESULT, "单项检查结果",
                Map.of("submissionId", "Long", "itemId", "Long", "score", "BigDecimal",
                       "passed", "Boolean", "targetId", "Long"),
                "单个检查项评分完成"),

            new TriggerPointDef(INSP_GRADE_RESULT, "等级评定结果",
                Map.of("submissionId", "Long", "gradeLevel", "String", "totalScore", "BigDecimal"),
                "按分数段评级完成"),

            new TriggerPointDef(INSP_RECORD_COMPLETE, "检查记录完成",
                Map.of("submissionId", "Long", "projectId", "Long", "taskId", "Long"),
                "整条检查提交完成,可触发汇总/整改")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("INSP_PASSED_EVT", "检查通过",
                "DISCIPLINE", "纪律", "POSITIVE", "check-shield", "#10b981",
                List.of("USER", "ORG_UNIT", "PLACE"),
                "检查项或整条检查通过"),

            new EventTypeDef("INSP_FAILED_EVT", "检查不通过",
                "DISCIPLINE", "纪律", "NEGATIVE", "alert-triangle", "#ef4444",
                List.of("USER", "ORG_UNIT", "PLACE"),
                "存在不合格项,可能触发整改"),

            // ─── 旧版兼容 (无 _EVT 后缀,早期迁移种子) ───
            new EventTypeDef("INSP_PASS", "检查合格",
                "INSPECTION", "检查", "NEUTRAL", "CheckSquare", "#22c55e",
                List.of("USER", "ORG_UNIT", "PLACE"), "检查通过(旧版)"),

            new EventTypeDef("INSP_FAIL", "检查不合格",
                "INSPECTION", "检查", "NEUTRAL", "XSquare", "#ef4444",
                List.of("USER", "ORG_UNIT", "PLACE"), "检查未通过(旧版)"),

            new EventTypeDef("INSP_RECTIFIED", "整改完成",
                "INSPECTION", "检查", "NEUTRAL", "CheckCircle2", "#3b82f6",
                List.of("USER", "ORG_UNIT", "PLACE"), "不合格项整改完成"),

            new EventTypeDef("DISCIPLINE_FAIL", "纪律不合格",
                "INSPECTION", "检查", "NEUTRAL", "ShieldX", "#ef4444",
                List.of("USER", "ORG_UNIT"), "纪律检查不合格")
        );
    }

    // 默认触发器留空 — 检查的评分结果是否触发事件由管理员规则决定 (避免过度打扰)
}
