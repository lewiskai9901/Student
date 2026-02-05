package com.school.management.infrastructure.persistence.inspection.v6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v6.DetailScope;
import com.school.management.domain.inspection.model.v6.InspectionDetail;
import com.school.management.domain.inspection.model.v6.ScoringMode;
import com.school.management.domain.inspection.repository.v6.InspectionDetailRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6检查明细仓储实现
 */
@Repository
public class InspectionDetailRepositoryImpl implements InspectionDetailRepository {

    private final InspectionDetailMapper mapper;
    private final ObjectMapper objectMapper;

    public InspectionDetailRepositoryImpl(InspectionDetailMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public InspectionDetail save(InspectionDetail detail) {
        InspectionDetailPO po = toPO(detail);
        if (detail.getId() == null) {
            mapper.insert(po);
            detail.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return detail;
    }

    @Override
    public void saveAll(List<InspectionDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        List<InspectionDetailPO> pos = details.stream()
                .map(this::toPO)
                .collect(Collectors.toList());
        mapper.batchInsert(pos);
    }

    @Override
    public Optional<InspectionDetail> findById(Long id) {
        InspectionDetailPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionDetail> findByTargetId(Long targetId) {
        return mapper.findByTargetId(targetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionDetail> findByTargetIdAndScoringMode(Long targetId, ScoringMode scoringMode) {
        return mapper.findByTargetIdAndScoringMode(targetId, scoringMode.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionDetail> findByTargetIdAndCategoryId(Long targetId, Long categoryId) {
        return mapper.findByTargetIdAndCategoryId(targetId, categoryId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionDetail> findByIndividual(String individualType, Long individualId) {
        return mapper.findByIndividual(individualType, individualId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByTargetId(Long targetId) {
        mapper.deleteByTargetId(targetId);
    }

    @Override
    public int countByTargetId(Long targetId) {
        return mapper.countByTargetId(targetId);
    }

    private InspectionDetailPO toPO(InspectionDetail domain) {
        InspectionDetailPO po = new InspectionDetailPO();
        po.setId(domain.getId());
        po.setTargetId(domain.getTargetId());
        po.setCategoryId(domain.getCategoryId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setCategoryName(domain.getCategoryName());
        po.setItemId(domain.getItemId());
        po.setItemCode(domain.getItemCode());
        po.setItemName(domain.getItemName());
        po.setScope(domain.getScope() != null ? domain.getScope().name() : null);
        po.setIndividualType(domain.getIndividualType());
        po.setIndividualId(domain.getIndividualId());
        po.setIndividualName(domain.getIndividualName());
        po.setScoringMode(domain.getScoringMode() != null ? domain.getScoringMode().name() : null);
        po.setScore(domain.getScore());
        po.setQuantity(domain.getQuantity());
        po.setTotalScore(domain.getTotalScore());
        po.setGradeCode(domain.getGradeCode());
        po.setGradeName(domain.getGradeName());
        po.setChecklistChecked(domain.getChecklistChecked());
        po.setRemark(domain.getRemark());
        po.setEvidenceIds(toJson(domain.getEvidenceIds()));
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private InspectionDetail toDomain(InspectionDetailPO po) {
        return InspectionDetail.builder()
                .id(po.getId())
                .targetId(po.getTargetId())
                .categoryId(po.getCategoryId())
                .categoryCode(po.getCategoryCode())
                .categoryName(po.getCategoryName())
                .itemId(po.getItemId())
                .itemCode(po.getItemCode())
                .itemName(po.getItemName())
                .scope(po.getScope() != null ? DetailScope.valueOf(po.getScope()) : null)
                .individualType(po.getIndividualType())
                .individualId(po.getIndividualId())
                .individualName(po.getIndividualName())
                .scoringMode(po.getScoringMode() != null ? ScoringMode.valueOf(po.getScoringMode()) : null)
                .score(po.getScore())
                .quantity(po.getQuantity())
                .totalScore(po.getTotalScore())
                .gradeCode(po.getGradeCode())
                .gradeName(po.getGradeName())
                .checklistChecked(po.getChecklistChecked())
                .remark(po.getRemark())
                .evidenceIds(fromJson(po.getEvidenceIds()))
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<Long> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
