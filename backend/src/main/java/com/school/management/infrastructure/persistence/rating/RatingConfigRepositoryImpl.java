package com.school.management.infrastructure.persistence.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingConfig;
import com.school.management.domain.rating.model.RatingPeriodType;
import com.school.management.domain.rating.repository.RatingConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of RatingConfigRepository
 */
@Repository
public class RatingConfigRepositoryImpl implements RatingConfigRepository {

    private final RatingConfigPersistenceMapper configMapper;

    public RatingConfigRepositoryImpl(RatingConfigPersistenceMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public RatingConfig save(RatingConfig config) {
        RatingConfigPO po = toPO(config);

        if (config.getId() == null) {
            configMapper.insert(po);
            config.setId(po.getId());
        } else {
            configMapper.updateById(po);
        }

        return config;
    }

    @Override
    public Optional<RatingConfig> findById(Long id) {
        RatingConfigPO po = configMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public List<RatingConfig> findByCheckPlanId(Long checkPlanId) {
        return configMapper.findByCheckPlanId(checkPlanId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingConfig> findEnabledByCheckPlanAndPeriodType(Long checkPlanId, RatingPeriodType periodType) {
        String periodTypeStr = periodType != null ? periodType.name() : null;
        return configMapper.findEnabledByCheckPlanAndPeriodType(checkPlanId, periodTypeStr).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingConfig> findAllEnabled() {
        return configMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return configMapper.existsById(id);
    }

    // ==================== Mapping Methods ====================

    private RatingConfigPO toPO(RatingConfig domain) {
        RatingConfigPO po = new RatingConfigPO();
        po.setId(domain.getId());
        po.setCheckPlanId(domain.getCheckPlanId());
        po.setRatingName(domain.getRatingName());
        po.setPeriodType(domain.getPeriodType() != null ? domain.getPeriodType().name() : null);
        po.setDivisionMethod(domain.getDivisionMethod() != null ? domain.getDivisionMethod().name() : null);
        po.setDivisionValue(domain.getDivisionValue());
        po.setIcon(domain.getIcon());
        po.setColor(domain.getColor());
        po.setPriority(domain.getPriority());
        po.setDescription(domain.getDescription());
        po.setRequireApproval(domain.isRequireApproval());
        po.setAutoPublish(domain.isAutoPublish());
        po.setEnabled(domain.isEnabled());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setCreatedBy(domain.getCreatedBy());
        return po;
    }

    private RatingConfig toDomain(RatingConfigPO po) {
        return RatingConfig.builder()
                .id(po.getId())
                .checkPlanId(po.getCheckPlanId())
                .ratingName(po.getRatingName())
                .periodType(po.getPeriodType() != null ? RatingPeriodType.fromString(po.getPeriodType()) : null)
                .divisionMethod(po.getDivisionMethod() != null ? DivisionMethod.fromString(po.getDivisionMethod()) : null)
                .divisionValue(po.getDivisionValue())
                .icon(po.getIcon())
                .color(po.getColor())
                .priority(po.getPriority())
                .description(po.getDescription())
                .requireApproval(po.getRequireApproval() != null && po.getRequireApproval())
                .autoPublish(po.getAutoPublish() != null && po.getAutoPublish())
                .enabled(po.getEnabled() != null && po.getEnabled())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
