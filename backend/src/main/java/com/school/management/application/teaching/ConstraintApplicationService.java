package com.school.management.application.teaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.domain.teaching.repository.SchedulingConstraintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ConstraintApplicationService {
    private final SchedulingConstraintRepository constraintRepo;
    private final ObjectMapper objectMapper;

    public List<SchedulingConstraint> list(Long semesterId, Integer level, Long targetId) {
        if (level != null && targetId != null) {
            return constraintRepo.findBySemesterIdAndLevelAndTargetId(
                semesterId, ConstraintLevel.fromCode(level), targetId);
        }
        if (level != null) {
            return constraintRepo.findBySemesterIdAndLevel(semesterId, ConstraintLevel.fromCode(level));
        }
        return constraintRepo.findBySemesterId(semesterId);
    }

    public SchedulingConstraint create(Map<String, Object> data, Long userId) {
        String paramsJson = convertParamsToJson(data.get("params"));

        SchedulingConstraint c = SchedulingConstraint.create(
            toLong(data.get("semesterId")),
            (String) data.get("constraintName"),
            ConstraintLevel.fromCode(toInt(data.get("constraintLevel"))),
            toLong(data.get("targetId")),
            (String) data.get("targetName"),
            ConstraintType.valueOf((String) data.get("constraintType")),
            toBool(data.getOrDefault("isHard", true)),
            toInt(data.getOrDefault("priority", 50)),
            paramsJson,
            (String) data.get("effectiveWeeks"),
            userId
        );
        return constraintRepo.save(c);
    }

    public SchedulingConstraint update(Long id, Map<String, Object> data) {
        SchedulingConstraint c = constraintRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("约束不存在: " + id));
        String paramsJson = convertParamsToJson(data.get("params"));
        c.update(
            (String) data.get("constraintName"),
            toBool(data.get("isHard")),
            toInt(data.get("priority")),
            paramsJson,
            (String) data.get("effectiveWeeks")
        );
        return constraintRepo.save(c);
    }

    public void delete(Long id) {
        constraintRepo.deleteById(id);
    }

    public void enable(Long id) {
        SchedulingConstraint c = constraintRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("约束不存在: " + id));
        c.enable();
        constraintRepo.save(c);
    }

    public void disable(Long id) {
        SchedulingConstraint c = constraintRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("约束不存在: " + id));
        c.disable();
        constraintRepo.save(c);
    }

    /**
     * Compute effective time matrix for a target (7 days x 10 periods).
     * Aggregates global constraints + target-specific constraints.
     * Returns a 2D array of slot statuses.
     */
    public List<List<Map<String, Object>>> getTimeMatrix(Long semesterId, Integer level, Long targetId) {
        // Get all applicable constraints
        List<SchedulingConstraint> globalConstraints = constraintRepo.findBySemesterIdAndLevel(
            semesterId, ConstraintLevel.GLOBAL);
        List<SchedulingConstraint> targetConstraints = (level != null && targetId != null)
            ? constraintRepo.findBySemesterIdAndLevelAndTargetId(
                semesterId, ConstraintLevel.fromCode(level), targetId)
            : Collections.emptyList();

        // Build matrix: 7 days x 10 periods
        List<List<Map<String, Object>>> matrix = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            List<Map<String, Object>> row = new ArrayList<>();
            for (int period = 1; period <= 10; period++) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("day", day);
                slot.put("period", period);

                // Check each constraint
                List<String> reasons = new ArrayList<>();
                String status = "available";

                for (SchedulingConstraint c : globalConstraints) {
                    if (!Boolean.TRUE.equals(c.getEnabled())) continue;
                    String slotStatus = evaluateConstraintForSlot(c, day, period);
                    if (slotStatus != null) {
                        status = mergeStatus(status, slotStatus);
                        reasons.add(c.getConstraintName());
                    }
                }
                for (SchedulingConstraint c : targetConstraints) {
                    if (!Boolean.TRUE.equals(c.getEnabled())) continue;
                    String slotStatus = evaluateConstraintForSlot(c, day, period);
                    if (slotStatus != null) {
                        status = mergeStatus(status, slotStatus);
                        reasons.add(c.getConstraintName());
                    }
                }

                slot.put("status", status);
                slot.put("reasons", reasons);
                row.add(slot);
            }
            matrix.add(row);
        }
        return matrix;
    }

    private String mergeStatus(String current, String incoming) {
        if ("forbidden".equals(incoming)) return "forbidden";
        if ("preferred".equals(incoming) && !"forbidden".equals(current)) return "preferred";
        if ("avoided".equals(incoming) && "available".equals(current)) return "avoided";
        return current;
    }

    @SuppressWarnings("unchecked")
    private String evaluateConstraintForSlot(SchedulingConstraint c, int day, int period) {
        try {
            Map<String, Object> params = objectMapper.readValue(c.getParams(), Map.class);

            switch (c.getConstraintType()) {
                case TIME_FORBIDDEN: {
                    List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
                    List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
                    if ((days.isEmpty() || days.contains(day)) && (periods.isEmpty() || periods.contains(period))) {
                        return "forbidden";
                    }
                    break;
                }
                case TIME_PREFERRED: {
                    List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
                    List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
                    if ((days.isEmpty() || days.contains(day)) && (periods.isEmpty() || periods.contains(period))) {
                        return "preferred";
                    }
                    break;
                }
                case TIME_AVOIDED: {
                    List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
                    List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
                    if ((days.isEmpty() || days.contains(day)) && (periods.isEmpty() || periods.contains(period))) {
                        return "avoided";
                    }
                    break;
                }
                case TIME_FIXED: {
                    Integer fixedDay = (Integer) params.get("day");
                    List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
                    if (fixedDay != null && fixedDay == day && periods.contains(period)) {
                        return "preferred"; // Fixed time shows as preferred
                    }
                    break;
                }
                default:
                    // Other constraint types don't affect individual time slots
                    break;
            }
        } catch (Exception e) {
            // Invalid JSON params, skip
        }
        return null;
    }

    private String convertParamsToJson(Object params) {
        if (params instanceof String) {
            return (String) params;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private Boolean toBool(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
