package com.school.management.infrastructure.persistence.behavior;

import com.school.management.domain.behavior.model.*;
import com.school.management.domain.behavior.repository.BehaviorRecordRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BehaviorRecordRepositoryImpl implements BehaviorRecordRepository {

    private final BehaviorRecordMapper mapper;

    public BehaviorRecordRepositoryImpl(BehaviorRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BehaviorRecord save(BehaviorRecord aggregate) {
        BehaviorRecordPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            mapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<BehaviorRecord> findById(Long id) {
        BehaviorRecordPO po = mapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(BehaviorRecord aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            mapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public List<BehaviorRecord> findByStudentId(Long studentId) {
        return mapper.findByStudentId(studentId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BehaviorRecord> findByClassId(Long classId) {
        return mapper.findByClassId(classId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BehaviorRecord> findByClassIdAndDateRange(Long classId, LocalDateTime start, LocalDateTime end) {
        return mapper.findByClassIdAndDateRange(classId, start, end).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByStudentIdAndType(Long studentId, BehaviorType type) {
        return mapper.countByStudentIdAndType(studentId, type.name());
    }

    @Override
    public List<BehaviorRecord> findByStudentIdAndDateRange(Long studentId, LocalDateTime start, LocalDateTime end) {
        return mapper.findByStudentIdAndDateRange(studentId, start, end).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private BehaviorRecordPO toPO(BehaviorRecord domain) {
        BehaviorRecordPO po = new BehaviorRecordPO();
        po.setId(domain.getId());
        po.setStudentId(domain.getStudentId());
        po.setClassId(domain.getClassId());
        po.setBehaviorType(domain.getBehaviorType().name());
        po.setSource(domain.getSource().name());
        po.setSourceId(domain.getSourceId());
        po.setCategory(domain.getCategory().name());
        po.setTitle(domain.getTitle());
        po.setDetail(domain.getDetail());
        po.setDeductionAmount(domain.getDeductionAmount());
        po.setStatus(domain.getStatus().name());
        po.setRecordedBy(domain.getRecordedBy());
        po.setRecordedAt(domain.getRecordedAt());
        po.setNotifiedAt(domain.getNotifiedAt());
        po.setAcknowledgedAt(domain.getAcknowledgedAt());
        po.setResolvedAt(domain.getResolvedAt());
        po.setResolutionNote(domain.getResolutionNote());
        return po;
    }

    private BehaviorRecord toDomain(BehaviorRecordPO po) {
        return BehaviorRecord.reconstruct()
                .id(po.getId())
                .studentId(po.getStudentId())
                .classId(po.getClassId())
                .behaviorType(BehaviorType.valueOf(po.getBehaviorType()))
                .source(BehaviorSource.valueOf(po.getSource()))
                .sourceId(po.getSourceId())
                .category(BehaviorCategory.valueOf(po.getCategory()))
                .title(po.getTitle())
                .detail(po.getDetail())
                .deductionAmount(po.getDeductionAmount() != null ? po.getDeductionAmount() : BigDecimal.ZERO)
                .status(BehaviorStatus.valueOf(po.getStatus()))
                .recordedBy(po.getRecordedBy())
                .recordedAt(po.getRecordedAt())
                .notifiedAt(po.getNotifiedAt())
                .acknowledgedAt(po.getAcknowledgedAt())
                .resolvedAt(po.getResolvedAt())
                .resolutionNote(po.getResolutionNote())
                .build();
    }
}
