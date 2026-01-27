package com.school.management.infrastructure.persistence.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.InspectionSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



/**
 * MyBatis Plus implementation of InspectionSessionRepository.
 */
@Repository
public class InspectionSessionRepositoryImpl implements InspectionSessionRepository {

    private final InspectionSessionMapper sessionMapper;

    public InspectionSessionRepositoryImpl(InspectionSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public InspectionSession save(InspectionSession aggregate) {
        InspectionSessionPO po = toPO(aggregate);

        if (aggregate.getId() == null) {
            sessionMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            sessionMapper.updateById(po);
        }

        return aggregate;
    }

    @Override
    public Optional<InspectionSession> findById(Long id) {
        InspectionSessionPO po = sessionMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(InspectionSession aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            sessionMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public Optional<InspectionSession> findBySessionCode(String sessionCode) {
        InspectionSessionPO po = sessionMapper.findBySessionCode(sessionCode);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<InspectionSession> findByStatus(SessionStatus status) {
        return sessionMapper.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionSession> findByInspectionDate(LocalDate date) {
        return sessionMapper.findByInspectionDate(date).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionSession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return sessionMapper.findByDateRange(startDate, endDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionSession> findByInspectorId(Long inspectorId) {
        return sessionMapper.findByInspectorId(inspectorId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySessionCode(String sessionCode) {
        return sessionMapper.findBySessionCode(sessionCode) != null;
    }

    @Override
    public List<InspectionSession> findByInspectionLevel(InspectionLevel level) {
        return sessionMapper.findByInspectionLevel(level.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<InspectionSession> findPublishedByDateRange(LocalDate startDate, LocalDate endDate) {
        return sessionMapper.findPublishedByDateRange(startDate, endDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private InspectionSessionPO toPO(InspectionSession domain) {
        InspectionSessionPO po = new InspectionSessionPO();
        po.setId(domain.getId());
        po.setSessionCode(domain.getSessionCode());
        po.setTemplateId(domain.getTemplateId());
        po.setTemplateVersion(domain.getTemplateVersion());
        po.setInspectionDate(domain.getInspectionDate());
        po.setInspectionPeriod(domain.getInspectionPeriod());
        po.setInputMode(domain.getInputMode().name());
        po.setScoringMode(domain.getScoringMode().name());
        po.setBaseScore(domain.getBaseScore());
        po.setInspectionLevel(domain.getInspectionLevel() != null ? domain.getInspectionLevel().name() : "CLASS");
        po.setStatus(domain.getStatus().name());
        po.setInspectorId(domain.getInspectorId());
        po.setInspectorName(domain.getInspectorName());
        po.setSubmittedAt(domain.getSubmittedAt());
        po.setPublishedAt(domain.getPublishedAt());
        po.setRemarks(domain.getRemarks());
        po.setCreatedAt(domain.getCreatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        return po;
    }

    private InspectionSession toDomain(InspectionSessionPO po) {
        return InspectionSession.builder()
            .id(po.getId())
            .sessionCode(po.getSessionCode())
            .templateId(po.getTemplateId())
            .templateVersion(po.getTemplateVersion())
            .inspectionDate(po.getInspectionDate())
            .inspectionPeriod(po.getInspectionPeriod())
            .inputMode(InputMode.valueOf(po.getInputMode()))
            .scoringMode(ScoringMode.valueOf(po.getScoringMode()))
            .baseScore(po.getBaseScore())
            .inspectionLevel(po.getInspectionLevel() != null ? InspectionLevel.valueOf(po.getInspectionLevel()) : InspectionLevel.CLASS)
            .status(SessionStatus.valueOf(po.getStatus()))
            .inspectorId(po.getInspectorId())
            .inspectorName(po.getInspectorName())
            .submittedAt(po.getSubmittedAt())
            .publishedAt(po.getPublishedAt())
            .remarks(po.getRemarks())
            .createdAt(po.getCreatedAt())
            .createdBy(po.getCreatedBy())
            .build();
    }
}
