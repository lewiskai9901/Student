package com.school.management.domain.evaluation.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.evaluation.model.EvalCondition;
import com.school.management.domain.event.model.EntityEvent;
import com.school.management.domain.event.repository.EntityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 基于实体事件的条件评估器
 * sourceType = EVENT
 *
 * source_config: { "eventType": "INSP_VIOLATION" }
 * 指标: COUNT / SCORE_SUM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventEvaluator implements ConditionEvaluator {

    private final EntityEventRepository entityEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String sourceType) {
        return "EVENT".equals(sourceType);
    }

    @Override
    public ConditionResult evaluate(EvalCondition condition, Long targetId, String targetType,
                                    LocalDate cycleStart, LocalDate cycleEnd) {
        try {
            JsonNode config = objectMapper.readTree(condition.getSourceConfig());
            String eventType = config.has("eventType") ? config.get("eventType").asText() : null;

            if (eventType == null) {
                return ConditionResult.error("EVENT 条件缺少 eventType 配置");
            }

            // 查询目标的事件列表（按主体）
            List<EntityEvent> events = entityEventRepository.findBySubject(targetType, targetId, 1000);

            // 按 eventType 过滤，并按时间范围过滤
            List<EntityEvent> filtered = events.stream()
                    .filter(e -> eventType.equals(e.getEventType()))
                    .filter(e -> e.getOccurredAt() != null &&
                            !e.getOccurredAt().toLocalDate().isBefore(cycleStart) &&
                            !e.getOccurredAt().toLocalDate().isAfter(cycleEnd))
                    .toList();

            String metric = condition.getMetric();
            String operator = condition.getOperator();
            BigDecimal thresh = new BigDecimal(condition.getThreshold());

            return switch (metric) {
                case "COUNT" -> {
                    long count = filtered.size();
                    boolean passed = compare(BigDecimal.valueOf(count), operator, thresh);
                    yield new ConditionResult(passed, String.valueOf(count),
                            "事件[" + eventType + "] 次数 " + count + " " + operator + " " + condition.getThreshold());
                }
                case "SCORE_SUM" -> {
                    BigDecimal sum = filtered.stream()
                            .filter(e -> e.getPayload() != null)
                            .map(e -> extractScore(e.getPayload()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean passed = compare(sum, operator, thresh);
                    yield new ConditionResult(passed, sum.toPlainString(),
                            "事件[" + eventType + "] 分值合计 " + sum.toPlainString() + " " + operator + " " + condition.getThreshold());
                }
                default -> ConditionResult.error("不支持的事件指标: " + metric);
            };

        } catch (Exception e) {
            log.error("EventEvaluator error: {}", e.getMessage(), e);
            return ConditionResult.error("评估异常: " + e.getMessage());
        }
    }

    private BigDecimal extractScore(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("score")) {
                return new BigDecimal(node.get("score").asText("0"));
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    private boolean compare(BigDecimal actual, String operator, BigDecimal threshold) {
        int cmp = actual.compareTo(threshold);
        return switch (operator) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case "=" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            default -> false;
        };
    }
}
