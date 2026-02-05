package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionEvidence;

import java.util.List;
import java.util.Optional;

/**
 * V6检查证据仓储接口
 */
public interface InspectionEvidenceRepository {

    InspectionEvidence save(InspectionEvidence evidence);

    void saveAll(List<InspectionEvidence> evidences);

    Optional<InspectionEvidence> findById(Long id);

    List<InspectionEvidence> findByDetailId(Long detailId);

    List<InspectionEvidence> findByTargetId(Long targetId);

    List<InspectionEvidence> findByDetailIds(List<Long> detailIds);

    void deleteById(Long id);

    void deleteByDetailId(Long detailId);

    void deleteByTargetId(Long targetId);

    int countByDetailId(Long detailId);

    int countByTargetId(Long targetId);
}
