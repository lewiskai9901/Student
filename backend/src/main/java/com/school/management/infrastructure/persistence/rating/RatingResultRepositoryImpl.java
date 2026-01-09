package com.school.management.infrastructure.persistence.rating;

import com.school.management.domain.rating.model.RatingPeriodType;
import com.school.management.domain.rating.model.RatingResult;
import com.school.management.domain.rating.model.RatingResultStatus;
import com.school.management.domain.rating.repository.RatingResultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of RatingResultRepository
 */
@Repository
public class RatingResultRepositoryImpl implements RatingResultRepository {

    private final RatingResultPersistenceMapper resultMapper;

    public RatingResultRepositoryImpl(RatingResultPersistenceMapper resultMapper) {
        this.resultMapper = resultMapper;
    }

    @Override
    public RatingResult save(RatingResult result) {
        RatingResultPO po = toPO(result);

        if (result.getId() == null) {
            resultMapper.insert(po);
            result.setId(po.getId());
        } else {
            resultMapper.updateById(po);
        }

        return result;
    }

    @Override
    public List<RatingResult> saveAll(List<RatingResult> results) {
        return results.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RatingResult> findById(Long id) {
        RatingResultPO po = resultMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<RatingResult> findByRatingConfigId(Long ratingConfigId) {
        return resultMapper.findByRatingConfigId(ratingConfigId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResult> findByClassId(Long classId) {
        return resultMapper.findByClassId(classId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResult> findByStatus(RatingResultStatus status) {
        return resultMapper.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResult> findByConfigAndPeriod(Long ratingConfigId, LocalDate periodStart, LocalDate periodEnd) {
        return resultMapper.findByConfigAndPeriod(ratingConfigId, periodStart, periodEnd).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingResult> findPendingApproval() {
        return resultMapper.findPendingApproval().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(RatingResultStatus status) {
        return resultMapper.countByStatus(status.name());
    }

    @Override
    public void deleteById(Long id) {
        resultMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return resultMapper.existsById(id);
    }

    // ==================== Mapping Methods ====================

    private RatingResultPO toPO(RatingResult domain) {
        RatingResultPO po = new RatingResultPO();
        po.setId(domain.getId());
        po.setRatingConfigId(domain.getRatingConfigId());
        po.setCheckPlanId(domain.getCheckPlanId());
        po.setClassId(domain.getClassId());
        po.setClassName(domain.getClassName());
        po.setPeriodType(domain.getPeriodType() != null ? domain.getPeriodType().name() : null);
        po.setPeriodStart(domain.getPeriodStart());
        po.setPeriodEnd(domain.getPeriodEnd());
        po.setRanking(domain.getRanking());
        po.setFinalScore(domain.getFinalScore());
        po.setAwarded(domain.isAwarded());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setCalculatedAt(domain.getCalculatedAt());
        po.setApprovedBy(domain.getApprovedBy());
        po.setApprovedAt(domain.getApprovedAt());
        po.setApprovalComment(domain.getApprovalComment());
        po.setPublishedBy(domain.getPublishedBy());
        po.setPublishedAt(domain.getPublishedAt());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private RatingResult toDomain(RatingResultPO po) {
        return RatingResult.builder()
                .id(po.getId())
                .ratingConfigId(po.getRatingConfigId())
                .checkPlanId(po.getCheckPlanId())
                .classId(po.getClassId())
                .className(po.getClassName())
                .periodType(po.getPeriodType() != null ? RatingPeriodType.fromString(po.getPeriodType()) : null)
                .periodStart(po.getPeriodStart())
                .periodEnd(po.getPeriodEnd())
                .ranking(po.getRanking())
                .finalScore(po.getFinalScore())
                .awarded(po.getAwarded() != null && po.getAwarded())
                .status(po.getStatus() != null ? RatingResultStatus.valueOf(po.getStatus()) : null)
                .calculatedAt(po.getCalculatedAt())
                .approvedBy(po.getApprovedBy())
                .approvedAt(po.getApprovedAt())
                .approvalComment(po.getApprovalComment())
                .publishedBy(po.getPublishedBy())
                .publishedAt(po.getPublishedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
