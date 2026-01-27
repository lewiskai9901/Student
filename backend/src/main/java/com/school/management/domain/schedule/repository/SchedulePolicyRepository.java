package com.school.management.domain.schedule.repository;

import com.school.management.domain.schedule.model.SchedulePolicy;

import java.util.List;
import java.util.Optional;

public interface SchedulePolicyRepository {
    SchedulePolicy save(SchedulePolicy policy);
    Optional<SchedulePolicy> findById(Long id);
    Optional<SchedulePolicy> findByPolicyCode(String policyCode);
    List<SchedulePolicy> findEnabled();
    List<SchedulePolicy> findByTemplateId(Long templateId);
    List<SchedulePolicy> findAll();
    void delete(SchedulePolicy policy);
}
