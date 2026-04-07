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
 * 违纪事件记录提取器
 *
 * VIOLATION_RECORD 类型: 一个 detail 的 responseValue 包含多条违规记录，
 * 每条记录有独立的 studentId、severity、score。
 * → 每条记录产生 1 个观察，主体=记录中的学生
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ViolationRecordObservationExtractor implements ObservationExtractor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String itemType) {
        return "VIOLATION_RECORD".equals(itemType);
    }

    @Override
    public List<ScoringObservation> extract(SubmissionDetail detail, ObservationContext ctx) {
        String responseValue = detail.getResponseValue();
        if (responseValue == null || responseValue.isBlank()) return Collections.emptyList();

        try {
            List<Map<String, Object>> records = objectMapper.readValue(
                    responseValue, new TypeReference<>() {});

            List<ScoringObservation> observations = new ArrayList<>();
            for (Map<String, Object> record : records) {
                Long studentId = toLong(record.get("studentId"));
                if (studentId == null) continue;

                String studentName = record.get("studentName") != null
                        ? record.get("studentName").toString() : "";
                String severity = record.get("severity") != null
                        ? record.get("severity").toString() : "MINOR";
                BigDecimal score = toBigDecimal(record.get("score"));
                String description = record.get("description") != null
                        ? record.get("description").toString() : "";

                observations.add(ScoringObservation.builder()
                        .subjectType("USER")
                        .subjectId(studentId)
                        .subjectName(studentName)
                        .orgUnitId(ctx.getOrgUnitId())
                        .className(ctx.getClassName())
                        .score(score != null ? score : BigDecimal.ZERO)
                        .negative(true) // 违规记录天生负面
                        .severity(ScoringObservation.mapViolationSeverity(severity))
                        .flagged(true)
                        .linkedEventType(ctx.getLinkedEventType(detail.getTemplateItemId()))
                        .itemCode(detail.getItemCode())
                        .itemName(detail.getItemName())
                        .itemType(detail.getItemType())
                        .sectionName(detail.getSectionName())
                        .responseValue(responseValue)
                        .description(description)
                        .observedAt(ctx.getObservedAt())
                        .submissionId(ctx.getSubmissionId())
                        .detailId(detail.getId())
                        .projectId(ctx.getProjectId())
                        .taskId(ctx.getTaskId())
                        .build());
            }
            return observations;
        } catch (Exception e) {
            log.warn("解析 VIOLATION_RECORD responseValue 失败, detailId={}: {}",
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
