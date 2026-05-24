package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspectionPlan;
import com.school.management.domain.inspection.model.execution.RecurrenceRule;
import com.school.management.domain.inspection.model.execution.TimeSlot;
import com.school.management.domain.inspection.repository.InspectionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 调度组冲突检测 (V20260524_6, 2026-05-24).
 *
 * <p>检测同一项目内**两个 plan 同时覆盖同分区 + 同日期 + 同时段重叠**的情况, 输出 warnings.
 * 不硬拒绝 (业务上可能确实需要两个 plan 走同分区做交叉验证), 仅在 response 中返回告警让用户确认.
 *
 * <p>算法简化:
 * <ul>
 *   <li>按 sectionId 取交集 — 0 个交集直接跳过</li>
 *   <li>探测未来 30 天内每天双方 shouldRunToday — 任一天重合则记冲突日</li>
 *   <li>时段精确重叠用 TimeSlot 比较 (考虑跨日)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionPlanConflictDetector {

    private final InspectionPlanRepository planRepository;
    private static final int LOOKAHEAD_DAYS = 30;
    private static final int MAX_REPORTED_DATES_PER_CONFLICT = 5;

    /**
     * 检测一个待保存 plan 与项目内其他 active plan 的冲突.
     * 若 plan 已存在 (update 路径), 自动排除自身.
     *
     * @return 冲突报告列表; 空列表表示无冲突
     */
    public List<ConflictReport> detect(InspectionPlan candidate) {
        if (candidate == null || candidate.getProjectId() == null) return List.of();

        List<InspectionPlan> peers = planRepository.findEnabledByProjectId(candidate.getProjectId());
        List<ConflictReport> reports = new ArrayList<>();

        for (InspectionPlan peer : peers) {
            if (candidate.getId() != null && candidate.getId().equals(peer.getId())) continue;

            // 1. section 交集
            Set<Long> overlapSections = new HashSet<>(candidate.getSectionIdList());
            overlapSections.retainAll(peer.getSectionIdList());
            if (overlapSections.isEmpty()) continue;

            // 2. 30 天内冲突日
            List<LocalDate> conflictDays = findConflictDays(candidate, peer);
            if (conflictDays.isEmpty()) continue;

            // 3. 时段重叠 (任一对时段在跨日意义上重合)
            boolean timeSlotsOverlap = hasOverlappingTimeSlots(candidate, peer);

            reports.add(new ConflictReport(
                    peer.getId(), peer.getPlanName(),
                    overlapSections,
                    conflictDays.stream().limit(MAX_REPORTED_DATES_PER_CONFLICT).toList(),
                    conflictDays.size(),
                    timeSlotsOverlap
            ));
        }
        return reports;
    }

    private List<LocalDate> findConflictDays(InspectionPlan a, InspectionPlan b) {
        List<LocalDate> hits = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < LOOKAHEAD_DAYS; i++) {
            LocalDate d = today.plusDays(i);
            if (shouldRunOn(a, d) && shouldRunOn(b, d)) hits.add(d);
        }
        return hits;
    }

    /** 同样的逻辑 InspectionPlanScheduler.shouldRunToday — 这里独立实现避免循环依赖. */
    private boolean shouldRunOn(InspectionPlan plan, LocalDate date) {
        RecurrenceRule rrule = plan.getParsedRrule();
        if (rrule != null) {
            LocalDate anchor = plan.getCreatedAt() != null ? plan.getCreatedAt().toLocalDate() : date;
            return rrule.matches(date, anchor);
        }
        String cycleType = plan.getCycleType();
        if ("DAILY".equals(cycleType)) return true;
        if ("WEEKLY".equals(cycleType)) {
            int dow = date.getDayOfWeek().getValue();
            List<Integer> days = parseDays(plan.getScheduleDays());
            return days.isEmpty() ? dow == 1 : days.contains(dow);
        }
        if ("MONTHLY".equals(cycleType)) {
            int dom = date.getDayOfMonth();
            List<Integer> days = parseDays(plan.getScheduleDays());
            return days.isEmpty() ? dom == 1 : days.contains(dom);
        }
        return false;
    }

    private List<Integer> parseDays(String json) {
        List<Integer> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(json);
        while (m.find()) try { out.add(Integer.parseInt(m.group())); } catch (NumberFormatException ignored) {}
        return out;
    }

    private boolean hasOverlappingTimeSlots(InspectionPlan a, InspectionPlan b) {
        List<TimeSlot> as = a.getParsedTimeSlots();
        List<TimeSlot> bs = b.getParsedTimeSlots();
        // 任一为空 = "全天" 视为永远重叠
        if (as.isEmpty() || bs.isEmpty()) return true;
        LocalDate today = LocalDate.now();
        for (TimeSlot x : as) {
            for (TimeSlot y : bs) {
                if (intervalOverlap(x.toStartDateTime(today), x.toEndDateTime(today),
                                    y.toStartDateTime(today), y.toEndDateTime(today))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean intervalOverlap(java.time.LocalDateTime aStart, java.time.LocalDateTime aEnd,
                                    java.time.LocalDateTime bStart, java.time.LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /** 冲突报告 — 一个候选 plan 与一个 peer plan 的冲突详情. */
    public record ConflictReport(
            Long peerPlanId,
            String peerPlanName,
            Set<Long> overlappingSectionIds,
            List<LocalDate> firstConflictDates,
            int totalConflictDaysIn30,
            boolean timeSlotsOverlap
    ) {
        public String summary() {
            return String.format(
                "与「%s」(plan#%d) 在 %d 个分区上 %d 天内冲突%s",
                peerPlanName, peerPlanId,
                overlappingSectionIds.size(), totalConflictDaysIn30,
                timeSlotsOverlap ? " (时段重叠)" : " (时段错开但同日)"
            );
        }
    }
}
