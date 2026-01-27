package com.school.management.infrastructure.schedule;

import com.school.management.domain.schedule.model.ScheduleExecution;
import com.school.management.domain.schedule.model.SchedulePolicy;
import com.school.management.domain.schedule.repository.ScheduleExecutionRepository;
import com.school.management.domain.schedule.service.InspectorRotationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of InspectorRotationService.
 * Provides rotation algorithms for selecting inspectors from a policy's inspector pool.
 */
@Slf4j
@Service
public class InspectorRotationServiceImpl implements InspectorRotationService {

    private final ScheduleExecutionRepository executionRepository;

    public InspectorRotationServiceImpl(ScheduleExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Override
    public List<Long> selectInspectors(SchedulePolicy policy, LocalDate date) {
        List<Long> pool = policy.getInspectorPool();
        if (pool == null || pool.isEmpty()) {
            log.warn("Inspector pool is empty for policy: {}", policy.getPolicyCode());
            return Collections.emptyList();
        }

        switch (policy.getRotationAlgorithm()) {
            case ROUND_ROBIN:
                return selectRoundRobin(pool, date);
            case RANDOM:
                return selectRandom(pool, date);
            case LOAD_BALANCED:
                return selectLoadBalanced(pool, policy, date);
            default:
                log.warn("Unknown rotation algorithm: {}, falling back to ROUND_ROBIN", policy.getRotationAlgorithm());
                return selectRoundRobin(pool, date);
        }
    }

    /**
     * Round-robin selection based on the day of the year.
     * Uses date's day-of-year modulo pool size to pick the starting inspector.
     */
    private List<Long> selectRoundRobin(List<Long> pool, LocalDate date) {
        int index = date.getDayOfYear() % pool.size();
        Long selected = pool.get(index);
        log.debug("ROUND_ROBIN selected inspector {} (index={}, dayOfYear={}, poolSize={})",
                selected, index, date.getDayOfYear(), pool.size());
        return Collections.singletonList(selected);
    }

    /**
     * Random selection using the date as a seed for reproducibility.
     * The same date always produces the same selection.
     */
    private List<Long> selectRandom(List<Long> pool, LocalDate date) {
        long seed = date.toEpochDay();
        Random random = new Random(seed);
        List<Long> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        Long selected = shuffled.get(0);
        log.debug("RANDOM selected inspector {} (date={}, poolSize={})",
                selected, date, pool.size());
        return Collections.singletonList(selected);
    }

    /**
     * Load-balanced selection that picks the least-assigned inspector.
     * Queries recent executions (last 30 days) to count assignments per inspector,
     * then selects the inspector with the fewest assignments.
     */
    private List<Long> selectLoadBalanced(List<Long> pool, SchedulePolicy policy, LocalDate date) {
        LocalDate startDate = date.minusDays(30);
        List<ScheduleExecution> recentExecutions = executionRepository.findByDateRange(startDate, date);

        // Count assignments per inspector from recent executions
        Map<Long, Long> assignmentCounts = new HashMap<>();
        for (Long inspectorId : pool) {
            assignmentCounts.put(inspectorId, 0L);
        }

        for (ScheduleExecution execution : recentExecutions) {
            if (execution.getAssignedInspectors() != null) {
                for (Long inspectorId : execution.getAssignedInspectors()) {
                    if (assignmentCounts.containsKey(inspectorId)) {
                        assignmentCounts.merge(inspectorId, 1L, Long::sum);
                    }
                }
            }
        }

        // Find the inspector with the least assignments
        Long selected = assignmentCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(pool.get(0));

        log.debug("LOAD_BALANCED selected inspector {} (counts={}, date={})",
                selected, assignmentCounts, date);
        return Collections.singletonList(selected);
    }
}
