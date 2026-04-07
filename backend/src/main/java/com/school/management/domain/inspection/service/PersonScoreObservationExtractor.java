package com.school.management.domain.inspection.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.ScoringObservation;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 人次扣分提取器
 *
 * PERSON_SCORE 类型: 一个 detail 的 responseValue 包含多人打分，
 * 每个人有独立的 score。
 * → 每个负分人员产生 1 个观察，主体=该人员
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PersonScoreObservationExtractor implements ObservationExtractor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String itemType) {
        return "PERSON_SCORE".equals(itemType);
    }

    @Override
    public List<ScoringObservation> extract(SubmissionDetail detail, ObservationContext ctx) {
        String responseValue = detail.getResponseValue();
        if (responseValue == null || responseValue.isBlank()) return Collections.emptyList();

        try {
            List<Map<String, Object>> persons = objectMapper.readValue(
                    responseValue, new TypeReference<>() {});

            List<ScoringObservation> observations = new ArrayList<>();
            for (Map<String, Object> person : persons) {
                BigDecimal personScore = toBigDecimal(person.get("score"));
                if (personScore == null || personScore.compareTo(BigDecimal.ZERO) >= 0) continue;

                Long studentId = toLong(person.get("studentId"));
                if (studentId == null) continue;

                String studentName = person.get("studentName") != null
                        ? person.get("studentName").toString() : "";

                observations.add(ScoringObservation.builder()
                        .subjectType("USER")
                        .subjectId(studentId)
                        .subjectName(studentName)
                        .orgUnitId(ctx.getOrgUnitId())
                        .className(ctx.getClassName())
                        .score(personScore)
                        .negative(true)
                        .severity(ScoringObservation.calcSeverity(personScore, false))
                        .flagged(true)
                        .linkedEventType(ctx.getLinkedEventType(detail.getTemplateItemId()))
                        .itemCode(detail.getItemCode())
                        .itemName(detail.getItemName())
                        .itemType(detail.getItemType())
                        .sectionName(detail.getSectionName())
                        .observedAt(ctx.getObservedAt())
                        .submissionId(ctx.getSubmissionId())
                        .detailId(detail.getId())
                        .projectId(ctx.getProjectId())
                        .taskId(ctx.getTaskId())
                        .build());
            }
            return observations;
        } catch (Exception e) {
            log.warn("解析 PERSON_SCORE responseValue 失败, detailId={}: {}",
                    detail.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
