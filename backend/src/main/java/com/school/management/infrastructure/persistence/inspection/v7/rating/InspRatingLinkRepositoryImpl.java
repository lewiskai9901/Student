package com.school.management.infrastructure.persistence.inspection.v7.rating;

import com.school.management.domain.inspection.model.v7.rating.InspRatingLink;
import com.school.management.domain.inspection.repository.v7.InspRatingLinkRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspRatingLinkRepositoryImpl implements InspRatingLinkRepository {

    private final InspRatingLinkMapper mapper;

    public InspRatingLinkRepositoryImpl(InspRatingLinkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspRatingLink save(InspRatingLink link) {
        InspRatingLinkPO po = toPO(link);
        if (link.getId() == null) {
            mapper.insert(po);
            link.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return link;
    }

    @Override
    public Optional<InspRatingLink> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspRatingLink> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspRatingLink> findByProjectIdAndPeriodType(Long projectId, String periodType) {
        return mapper.findByProjectIdAndPeriodType(projectId, periodType).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspRatingLink> findByRatingConfigId(Long ratingConfigId) {
        return mapper.findByRatingConfigId(ratingConfigId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private InspRatingLinkPO toPO(InspRatingLink d) {
        InspRatingLinkPO po = new InspRatingLinkPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setRatingConfigId(d.getRatingConfigId());
        po.setPeriodType(d.getPeriodType());
        po.setAutoCalculate(d.isAutoCalculate());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspRatingLink toDomain(InspRatingLinkPO po) {
        return InspRatingLink.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .ratingConfigId(po.getRatingConfigId())
                .periodType(po.getPeriodType())
                .autoCalculate(po.getAutoCalculate() != null && po.getAutoCalculate())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
