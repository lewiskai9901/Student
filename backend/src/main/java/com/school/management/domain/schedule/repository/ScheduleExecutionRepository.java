package com.school.management.domain.schedule.repository;

import com.school.management.domain.schedule.model.ScheduleExecution;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleExecutionRepository {
    ScheduleExecution save(ScheduleExecution execution);
    Optional<ScheduleExecution> findById(Long id);
    List<ScheduleExecution> findByPolicyIdAndDate(Long policyId, LocalDate date);
    List<ScheduleExecution> findByDate(LocalDate date);
    List<ScheduleExecution> findByStatus(String status);
    List<ScheduleExecution> findByDateRange(LocalDate startDate, LocalDate endDate);
}
