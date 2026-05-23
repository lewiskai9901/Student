package com.school.management.infrastructure.persistence.inspection.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;
import com.school.management.domain.inspection.repository.IndicatorResultRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class IndicatorResultRepositoryImpl implements IndicatorResultRepository {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Long>> LIST_LONG = new TypeReference<>() {};

    private final IndicatorResultMapper mapper;

    public IndicatorResultRepositoryImpl(IndicatorResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public IndicatorResult save(IndicatorResult result) {
        IndicatorResultPO po = toPO(result);
        if (result.getId() == null) {
            mapper.insert(po);
            result.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return result;
    }

    @Override
    public Optional<IndicatorResult> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<IndicatorResult> findByIndicatorId(Long indicatorId) {
        return mapper.findByIndicatorId(indicatorId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<IndicatorResult> findByIndicatorIdAndStatus(Long indicatorId, ResultStatus status) {
        return mapper.findByIndicatorIdAndStatus(indicatorId, status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<IndicatorResult> findCurrentPublished(Long indicatorId, Long targetId, String periodKey) {
        return Optional.ofNullable(mapper.findCurrentPublished(indicatorId, targetId, periodKey))
                .map(this::toDomain);
    }

    @Override
    public List<IndicatorResult> findRevisionHistory(Long indicatorId, Long targetId, String periodKey) {
        return mapper.findRevisionHistory(indicatorId, targetId, periodKey).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private IndicatorResultPO toPO(IndicatorResult d) {
        IndicatorResultPO po = new IndicatorResultPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setIndicatorId(d.getIndicatorId());
        po.setTargetId(d.getTargetId());
        po.setTargetName(d.getTargetName());
        po.setPeriodKey(d.getPeriodKey());
        po.setValue(d.getValue());
        po.setRankPosition(d.getRankPosition());
        po.setGrade(d.getGrade());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : ResultStatus.DRAFT.name());
        po.setComputedAt(d.getComputedAt());
        po.setPublishedAt(d.getPublishedAt());
        po.setRevisionOf(d.getRevisionOf());
        po.setSourceSubmissionIds(writeListJson(d.getSourceSubmissionIds()));
        po.setSourceSectionIds(writeListJson(d.getSourceSectionIds()));
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private IndicatorResult toDomain(IndicatorResultPO po) {
        return IndicatorResult.reconstruct(IndicatorResult.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .orgUnitId(po.getOrgUnitId())
                .indicatorId(po.getIndicatorId())
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .periodKey(po.getPeriodKey())
                .value(po.getValue())
                .rankPosition(po.getRankPosition())
                .grade(po.getGrade())
                .status(po.getStatus() != null ? ResultStatus.valueOf(po.getStatus()) : ResultStatus.DRAFT)
                .computedAt(po.getComputedAt())
                .publishedAt(po.getPublishedAt())
                .revisionOf(po.getRevisionOf())
                .sourceSubmissionIds(readListJson(po.getSourceSubmissionIds()))
                .sourceSectionIds(readListJson(po.getSourceSectionIds()))
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }

    private static String writeListJson(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Long> readListJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return JSON.readValue(json, LIST_LONG);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
