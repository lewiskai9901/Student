package com.school.management.infrastructure.persistence.inspection.scoring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.LatePolicy;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.RankDirection;
import com.school.management.domain.inspection.model.scoring.SubmissionDateField;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class IndicatorRepositoryImpl implements IndicatorRepository {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Long>> LIST_LONG = new TypeReference<>() {};
    private static final TypeReference<Map<String, BigDecimal>> MAP_STR_DEC = new TypeReference<>() {};

    private final IndicatorMapper mapper;

    public IndicatorRepositoryImpl(IndicatorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Indicator save(Indicator indicator) {
        IndicatorPO po = toPO(indicator);
        if (indicator.getId() == null) {
            mapper.insert(po);
            indicator.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return indicator;
    }

    @Override
    public Optional<Indicator> findById(Long id) {
        IndicatorPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Indicator> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Indicator> findByParentIndicatorId(Long parentId) {
        return mapper.findByParentIndicatorId(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.softDeleteByProjectId(projectId);
    }

    private IndicatorPO toPO(Indicator domain) {
        IndicatorPO po = new IndicatorPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setProjectId(domain.getProjectId());
        po.setParentIndicatorId(domain.getParentIndicatorId());
        po.setName(domain.getName());
        po.setIndicatorType(domain.getIndicatorType());
        List<Long> ids = domain.getSourceSectionIds();
        po.setSourceSectionId(domain.getSourceSectionId() != null
                ? domain.getSourceSectionId()
                : (ids != null && !ids.isEmpty() ? ids.get(0) : null));
        po.setSourceSectionIds(writeListJson(ids));
        po.setSourceAggregation(domain.getSourceAggregation());
        po.setCompositeAggregation(domain.getCompositeAggregation());
        po.setMissingPolicy(domain.getMissingPolicy() != null ? domain.getMissingPolicy().name() : null);
        po.setNormalization(domain.getNormalization());
        po.setNormalizationConfig(domain.getNormalizationConfig());
        po.setTriggerMode(domain.getTriggerMode() != null ? domain.getTriggerMode().name() : TriggerMode.TIME_WINDOW.name());
        po.setCountThreshold(domain.getCountThreshold());
        po.setWeightsBySection(writeWeightsJson(domain.getWeightsBySection()));
        po.setRankDirection(domain.getRankDirection() != null ? domain.getRankDirection().name() : null);
        po.setLatePolicy(domain.getLatePolicy() != null ? domain.getLatePolicy().name() : LatePolicy.REVISE_ORIGINAL.name());
        po.setSubmissionDateField(domain.getSubmissionDateField() != null
                ? domain.getSubmissionDateField().name() : SubmissionDateField.taskDate.name());
        po.setEvaluationPeriod(domain.getEvaluationPeriod());
        po.setGradeSchemeId(domain.getGradeSchemeId());
        po.setEvaluationMethod(domain.getEvaluationMethod());
        po.setGradeThresholds(domain.getGradeThresholds());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Indicator toDomain(IndicatorPO po) {
        return Indicator.reconstruct(Indicator.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .parentIndicatorId(po.getParentIndicatorId())
                .name(po.getName())
                .indicatorType(po.getIndicatorType())
                .sourceSectionId(po.getSourceSectionId())
                .sourceSectionIds(readListJson(po.getSourceSectionIds(), po.getSourceSectionId()))
                .sourceAggregation(po.getSourceAggregation())
                .compositeAggregation(po.getCompositeAggregation())
                .missingPolicy(MissingPolicy.fromString(po.getMissingPolicy()))
                .normalization(po.getNormalization())
                .normalizationConfig(po.getNormalizationConfig())
                .triggerMode(po.getTriggerMode() != null
                        ? safeEnum(TriggerMode.class, po.getTriggerMode(), TriggerMode.TIME_WINDOW)
                        : TriggerMode.TIME_WINDOW)
                .countThreshold(po.getCountThreshold())
                .weightsBySection(readWeightsJson(po.getWeightsBySection()))
                .rankDirection(po.getRankDirection() != null
                        ? safeEnum(RankDirection.class, po.getRankDirection(), null) : null)
                .latePolicy(po.getLatePolicy() != null
                        ? safeEnum(LatePolicy.class, po.getLatePolicy(), LatePolicy.REVISE_ORIGINAL)
                        : LatePolicy.REVISE_ORIGINAL)
                .submissionDateField(po.getSubmissionDateField() != null
                        ? safeEnum(SubmissionDateField.class, po.getSubmissionDateField(), SubmissionDateField.taskDate)
                        : SubmissionDateField.taskDate)
                .evaluationPeriod(po.getEvaluationPeriod())
                .gradeSchemeId(po.getGradeSchemeId())
                .evaluationMethod(po.getEvaluationMethod())
                .gradeThresholds(po.getGradeThresholds())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }

    // ── JSON helpers ────────────────────────────────────────

    private static <E extends Enum<E>> E safeEnum(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static String writeListJson(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Long> readListJson(String json, Long fallbackSingle) {
        if (json == null || json.isBlank()) {
            return fallbackSingle == null ? Collections.emptyList() : List.of(fallbackSingle);
        }
        try {
            List<Long> parsed = JSON.readValue(json, LIST_LONG);
            if (parsed == null || parsed.isEmpty()) {
                return fallbackSingle == null ? Collections.emptyList() : List.of(fallbackSingle);
            }
            return parsed;
        } catch (Exception e) {
            return fallbackSingle == null ? Collections.emptyList() : List.of(fallbackSingle);
        }
    }

    private static String writeWeightsJson(Map<Long, BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) return null;
        try {
            Map<String, BigDecimal> serializable = new LinkedHashMap<>();
            for (Map.Entry<Long, BigDecimal> e : weights.entrySet()) {
                if (e.getKey() == null) continue;
                serializable.put(String.valueOf(e.getKey()), e.getValue());
            }
            return JSON.writeValueAsString(serializable);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<Long, BigDecimal> readWeightsJson(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            Map<String, BigDecimal> raw = JSON.readValue(json, MAP_STR_DEC);
            Map<Long, BigDecimal> out = new LinkedHashMap<>();
            for (Map.Entry<String, BigDecimal> e : raw.entrySet()) {
                try {
                    out.put(Long.parseLong(e.getKey()), e.getValue());
                } catch (NumberFormatException ignore) { /* skip */ }
            }
            return out;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    // unused holder to silence unused imports if compile complains
    @SuppressWarnings("unused")
    private static List<Long> _emptyListLong() { return new ArrayList<>(); }
}
