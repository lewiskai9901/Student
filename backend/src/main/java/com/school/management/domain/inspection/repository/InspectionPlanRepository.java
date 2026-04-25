package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.InspectionPlan;

import java.util.List;
import java.util.Optional;

public interface InspectionPlanRepository {

    InspectionPlan save(InspectionPlan plan);

    Optional<InspectionPlan> findById(Long id);

    List<InspectionPlan> findByProjectId(Long projectId);

    List<InspectionPlan> findEnabledByProjectId(Long projectId);

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);
}
