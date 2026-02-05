package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionDetail;
import com.school.management.domain.inspection.model.v6.ScoringMode;

import java.util.List;
import java.util.Optional;

/**
 * V6检查明细仓储接口
 */
public interface InspectionDetailRepository {

    InspectionDetail save(InspectionDetail detail);

    void saveAll(List<InspectionDetail> details);

    Optional<InspectionDetail> findById(Long id);

    List<InspectionDetail> findByTargetId(Long targetId);

    List<InspectionDetail> findByTargetIdAndScoringMode(Long targetId, ScoringMode scoringMode);

    List<InspectionDetail> findByTargetIdAndCategoryId(Long targetId, Long categoryId);

    List<InspectionDetail> findByIndividual(String individualType, Long individualId);

    void deleteById(Long id);

    void deleteByTargetId(Long targetId);

    int countByTargetId(Long targetId);
}
