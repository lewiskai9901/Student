package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.v7.execution.ScoringObservation;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 默认观察提取器 — 覆盖 90% 的检查项类型
 *
 * 支持: PASS_FAIL, SELECT, RADIO, MULTI_SELECT, CHECKBOX,
 *      NUMBER, SLIDER, RATING, CHECKLIST, CALCULATED,
 *      TEXT, TEXTAREA, RICH_TEXT, PHOTO, VIDEO 等
 *
 * 逻辑: score < 0 || isFlagged → 1 个观察（主体=检查对象）
 *       否则 → 0 个观察
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultObservationExtractor implements ObservationExtractor {

    @Override
    public boolean supports(String itemType) {
        return true; // 兜底，所有类型都支持
    }

    @Override
    public List<ScoringObservation> extract(SubmissionDetail detail, ObservationContext ctx) {
        BigDecimal score = detail.getScore();
        boolean isNegative = (score != null && score.compareTo(BigDecimal.ZERO) < 0)
                          || Boolean.TRUE.equals(detail.getIsFlagged());

        if (!isNegative) return Collections.emptyList();

        return List.of(ScoringObservation.builder()
                .subjectType(mapTargetType(ctx.getTargetType()))
                .subjectId(ctx.getTargetId())
                .subjectName(ctx.getTargetName())
                .orgUnitId(ctx.getOrgUnitId())
                .className(ctx.getClassName())
                .score(score != null ? score : BigDecimal.ZERO)
                .negative(true)
                .severity(ScoringObservation.calcSeverity(score, Boolean.TRUE.equals(detail.getIsFlagged())))
                .flagged(Boolean.TRUE.equals(detail.getIsFlagged()))
                .linkedEventType(ctx.getLinkedEventType(detail.getTemplateItemId()))
                .itemCode(detail.getItemCode())
                .itemName(detail.getItemName())
                .itemType(detail.getItemType())
                .sectionName(detail.getSectionName())
                .responseValue(detail.getResponseValue())
                .observedAt(ctx.getObservedAt())
                .submissionId(ctx.getSubmissionId())
                .detailId(detail.getId())
                .projectId(ctx.getProjectId())
                .taskId(ctx.getTaskId())
                .build());
    }

    /**
     * 将检查目标类型映射为标准主体类型
     */
    private String mapTargetType(String targetType) {
        if (targetType == null) return "ORG_UNIT";
        return switch (targetType.toUpperCase()) {
            case "STUDENT" -> "USER";
            case "CLASS", "DEPARTMENT" -> "ORG_UNIT";
            case "PLACE" -> "PLACE";
            default -> "ORG_UNIT";
        };
    }
}
