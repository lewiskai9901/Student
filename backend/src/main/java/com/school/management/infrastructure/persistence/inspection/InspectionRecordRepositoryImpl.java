package com.school.management.infrastructure.persistence.inspection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.InspectionRecordRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of InspectionRecordRepository.
 */
@Repository
public class InspectionRecordRepositoryImpl implements InspectionRecordRepository {

    private final InspectionRecordMapper recordMapper;
    private final ClassScoreMapper classScoreMapper;
    private final DeductionDetailMapper detailMapper;
    private final ObjectMapper objectMapper;

    public InspectionRecordRepositoryImpl(
            InspectionRecordMapper recordMapper,
            ClassScoreMapper classScoreMapper,
            DeductionDetailMapper detailMapper,
            ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.classScoreMapper = classScoreMapper;
        this.detailMapper = detailMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public InspectionRecord save(InspectionRecord aggregate) {
        InspectionRecordPO po = toPO(aggregate);

        if (aggregate.getId() == null) {
            recordMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            recordMapper.updateById(po);
        }

        // Save class scores
        for (ClassScore classScore : aggregate.getClassScores()) {
            ClassScorePO scorePO = toClassScorePO(classScore, aggregate.getId());
            if (classScore.getId() == null) {
                classScoreMapper.insert(scorePO);
                classScore.setId(scorePO.getId());
            } else {
                classScoreMapper.updateById(scorePO);
            }

            // Save deduction details
            for (DeductionDetail detail : classScore.getDeductionDetails()) {
                DeductionDetailPO detailPO = toDetailPO(detail, classScore.getId());
                if (detail.getId() == null) {
                    detailMapper.insert(detailPO);
                    detail.setId(detailPO.getId());
                } else {
                    detailMapper.updateById(detailPO);
                }
            }
        }

        return aggregate;
    }

    @Override
    public Optional<InspectionRecord> findById(Long id) {
        InspectionRecordPO po = recordMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(InspectionRecord aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            recordMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        recordMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return recordMapper.selectById(id) != null;
    }

    @Override
    public Optional<InspectionRecord> findByRecordCode(String recordCode) {
        InspectionRecordPO po = recordMapper.findByRecordCode(recordCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<InspectionRecord> findByStatus(RecordStatus status) {
        return recordMapper.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionRecord> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return recordMapper.findByDateRange(startDate, endDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionRecord> findByTemplateId(Long templateId) {
        return recordMapper.findByTemplateId(templateId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds records by round ID. (Internal use)
     */
    public List<InspectionRecord> findByRoundId(Long roundId) {
        return recordMapper.findByRoundId(roundId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionRecord> findByInspectorId(Long inspectorId) {
        return recordMapper.findByInspectorId(inspectorId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionRecord> findByInspectionDate(LocalDate date) {
        return recordMapper.findByInspectionDate(date).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionRecord> findPendingReview() {
        return recordMapper.findByStatus(RecordStatus.SUBMITTED.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByRecordCode(String recordCode) {
        return recordMapper.findByRecordCode(recordCode) != null;
    }

    @Override
    public Optional<InspectionRecord> findLatestByDateAndPeriod(LocalDate date, String period) {
        List<InspectionRecordPO> records = recordMapper.findByInspectionDate(date);
        return records.stream()
            .filter(r -> period.equals(r.getInspectionPeriod()))
            .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .map(this::toDomain);
    }

    @Override
    public long countByStatus(RecordStatus status) {
        return recordMapper.countByStatus(status.name());
    }

    // ==================== Mapping Methods ====================

    private InspectionRecordPO toPO(InspectionRecord domain) {
        InspectionRecordPO po = new InspectionRecordPO();
        po.setId(domain.getId());
        po.setRecordCode(domain.getRecordCode());
        po.setTemplateId(domain.getTemplateId());
        po.setTemplateVersion(domain.getTemplateVersion());
        po.setRoundId(domain.getRoundId());
        po.setInspectionDate(domain.getInspectionDate());
        po.setInspectionPeriod(domain.getInspectionPeriod());
        po.setStatus(domain.getStatus().name());
        po.setInspectorId(domain.getInspectorId());
        po.setInspectorName(domain.getInspectorName());
        po.setInspectedAt(domain.getInspectedAt());
        po.setReviewerId(domain.getReviewerId());
        po.setReviewedAt(domain.getReviewedAt());
        po.setPublishedAt(domain.getPublishedAt());
        po.setRemarks(domain.getRemarks());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        return po;
    }

    private ClassScorePO toClassScorePO(ClassScore domain, Long recordId) {
        ClassScorePO po = new ClassScorePO();
        po.setId(domain.getId());
        po.setRecordId(recordId);
        po.setClassId(domain.getClassId());
        po.setClassName(domain.getClassName());
        po.setBaseScore(domain.getBaseScore().intValue());
        po.setTotalDeduction(domain.getTotalDeduction());
        po.setFinalScore(domain.getFinalScore());
        po.setRanking(domain.getRanking() != null ? domain.getRanking().toString() : null);
        return po;
    }

    private DeductionDetailPO toDetailPO(DeductionDetail domain, Long classScoreId) {
        DeductionDetailPO po = new DeductionDetailPO();
        po.setId(domain.getId());
        po.setClassScoreId(classScoreId);
        po.setDeductionItemId(domain.getDeductionItemId());
        po.setItemName(domain.getItemName());
        po.setCount(domain.getCount());
        po.setDeductionAmount(domain.getDeductionAmount());
        po.setRemark(domain.getRemark());

        // Serialize evidence URLs to JSON
        try {
            po.setEvidenceUrls(objectMapper.writeValueAsString(domain.getEvidenceUrls()));
        } catch (Exception e) {
            po.setEvidenceUrls("[]");
        }

        return po;
    }

    private InspectionRecord toDomain(InspectionRecordPO po) {
        InspectionRecord record = InspectionRecord.builder()
            .id(po.getId())
            .recordCode(po.getRecordCode())
            .templateId(po.getTemplateId())
            .templateVersion(po.getTemplateVersion())
            .roundId(po.getRoundId())
            .inspectionDate(po.getInspectionDate())
            .inspectionPeriod(po.getInspectionPeriod())
            .inspectorId(po.getInspectorId())
            .inspectorName(po.getInspectorName())
            .remarks(po.getRemarks())
            .createdBy(po.getCreatedBy())
            .build();

        // Load class scores
        List<ClassScorePO> scorePOs = classScoreMapper.findByRecordId(po.getId());
        for (ClassScorePO scorePO : scorePOs) {
            ClassScore classScore = toClassScoreDomain(scorePO);
            // Add to record's classScores list through reflection or a special method
            // For now, we'll use the builder approach
        }

        return record;
    }

    private ClassScore toClassScoreDomain(ClassScorePO po) {
        ClassScore.Builder builder = ClassScore.builder()
            .id(po.getId())
            .recordId(po.getRecordId())
            .classId(po.getClassId())
            .className(po.getClassName())
            .baseScore(new BigDecimal(po.getBaseScore()));

        ClassScore classScore = builder.build();

        // Load deduction details
        List<DeductionDetailPO> detailPOs = detailMapper.findByClassScoreId(po.getId());
        for (DeductionDetailPO detailPO : detailPOs) {
            List<String> evidenceUrls = parseEvidenceUrls(detailPO.getEvidenceUrls());
            classScore.addDeduction(
                detailPO.getDeductionItemId(),
                detailPO.getItemName(),
                detailPO.getCount(),
                detailPO.getDeductionAmount(),
                detailPO.getRemark(),
                evidenceUrls
            );
        }

        return classScore;
    }

    private List<String> parseEvidenceUrls(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
