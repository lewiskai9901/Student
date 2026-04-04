package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.Indicator;

import java.util.List;
import java.util.Optional;

public interface IndicatorRepository {

    Indicator save(Indicator indicator);

    Optional<Indicator> findById(Long id);

    List<Indicator> findByProjectId(Long projectId);

    List<Indicator> findByParentIndicatorId(Long parentId);

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);
}
