package com.school.management.infrastructure.persistence.inspection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.ClassInspectionRecordRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of ClassInspectionRecordRepository.
 * Handles aggregate reconstruction including child entities (deductions + checklist responses).
 */
@Repository
public class ClassInspectionRecordRepositoryImpl implements ClassInspectionRecordRepository {

    private final ClassInspectionRecordMapper recordMapper;
    private final InspectionDeductionMapper deductionMapper;
    private final ChecklistResponseMapper checklistMapper;
    private final ObjectMapper objectMapper;

    public ClassInspectionRecordRepositoryImpl(
            ClassInspectionRecordMapper recordMapper,
            InspectionDeductionMapper deductionMapper,
            ChecklistResponseMapper checklistMapper,
            ObjectMapper objectMapper) {
        this.recordMapper = recordMapper;
        this.deductionMapper = deductionMapper;
        this.checklistMapper = checklistMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClassInspectionRecord save(ClassInspectionRecord aggregate) {
        ClassInspectionRecordPO po = toPO(aggregate);

        if (aggregate.getId() == null) {
            recordMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            recordMapper.updateById(po);
        }

        // Save deductions
        for (InspectionDeduction deduction : aggregate.getDeductions()) {
            InspectionDeductionPO dPO = toDeductionPO(deduction, aggregate.getId(), aggregate.getSessionId());
            if (deduction.getId() == null) {
                deductionMapper.insert(dPO);
                deduction.setId(dPO.getId());
            } else {
                deductionMapper.updateById(dPO);
            }
        }

        // Save checklist responses
        for (ChecklistResponse response : aggregate.getChecklistResponses()) {
            ChecklistResponsePO cPO = toChecklistPO(response, aggregate.getId(), aggregate.getSessionId());
            if (response.getId() == null) {
                checklistMapper.insert(cPO);
                response.setId(cPO.getId());
            } else {
                checklistMapper.updateById(cPO);
            }
        }

        return aggregate;
    }

    @Override
    public Optional<ClassInspectionRecord> findById(Long id) {
        ClassInspectionRecordPO po = recordMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(ClassInspectionRecord aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            recordMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public List<ClassInspectionRecord> findBySessionId(Long sessionId) {
        return recordMapper.findBySessionId(sessionId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClassInspectionRecord> findBySessionIdAndClassId(Long sessionId, Long classId) {
        ClassInspectionRecordPO po = recordMapper.findBySessionIdAndClassId(sessionId, classId);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public int countBySessionId(Long sessionId) {
        return recordMapper.countBySessionId(sessionId);
    }

    // ==================== Mapping Methods ====================

    private ClassInspectionRecordPO toPO(ClassInspectionRecord domain) {
        ClassInspectionRecordPO po = new ClassInspectionRecordPO();
        po.setId(domain.getId());
        po.setSessionId(domain.getSessionId());
        po.setClassId(domain.getClassId());
        po.setClassName(domain.getClassName());
        po.setOrgUnitId(domain.getOrgUnitId());
        po.setOrgUnitName(domain.getOrgUnitName());
        po.setBaseScore(domain.getBaseScore());
        po.setTotalDeduction(domain.getTotalDeduction());
        po.setBonusScore(domain.getBonusScore());
        po.setFinalScore(domain.getFinalScore());
        po.setStatus(domain.getStatus().name());
        po.setRemarks(domain.getRemarks());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ClassInspectionRecord toDomain(ClassInspectionRecordPO po) {
        ClassInspectionRecord record = ClassInspectionRecord.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .classId(po.getClassId())
            .className(po.getClassName())
            .orgUnitId(po.getOrgUnitId())
            .orgUnitName(po.getOrgUnitName())
            .baseScore(po.getBaseScore())
            .totalDeduction(po.getTotalDeduction())
            .bonusScore(po.getBonusScore())
            .finalScore(po.getFinalScore())
            .status(ClassRecordStatus.valueOf(po.getStatus()))
            .remarks(po.getRemarks())
            .createdAt(po.getCreatedAt())
            .build();

        // Load child entities
        List<InspectionDeductionPO> deductionPOs = deductionMapper.findByClassRecordId(po.getId());
        List<InspectionDeduction> deductions = deductionPOs.stream()
            .map(this::toDeductionDomain)
            .collect(Collectors.toList());
        record.loadDeductions(deductions);

        List<ChecklistResponsePO> checklistPOs = checklistMapper.findByClassRecordId(po.getId());
        List<ChecklistResponse> responses = checklistPOs.stream()
            .map(this::toChecklistDomain)
            .collect(Collectors.toList());
        record.loadChecklistResponses(responses);

        return record;
    }

    private InspectionDeductionPO toDeductionPO(InspectionDeduction domain, Long classRecordId, Long sessionId) {
        InspectionDeductionPO po = new InspectionDeductionPO();
        po.setId(domain.getId());
        po.setSessionId(sessionId);
        po.setClassRecordId(classRecordId);
        po.setDeductionItemId(domain.getDeductionItemId());
        po.setItemName(domain.getItemName());
        po.setCategoryName(domain.getCategoryName());
        po.setSpaceType(domain.getSpaceType() != null ? domain.getSpaceType().name() : null);
        po.setSpaceId(domain.getSpaceId());
        po.setSpaceName(domain.getSpaceName());
        po.setStudentIds(toJson(domain.getStudentIds()));
        po.setStudentNames(toJson(domain.getStudentNames()));
        po.setPersonCount(domain.getPersonCount());
        po.setDeductionAmount(domain.getDeductionAmount());
        po.setInputSource(domain.getInputSource() != null ? domain.getInputSource().name() : null);
        po.setRemark(domain.getRemark());
        po.setEvidenceUrls(toJson(domain.getEvidenceUrls()));
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private InspectionDeduction toDeductionDomain(InspectionDeductionPO po) {
        return InspectionDeduction.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .classRecordId(po.getClassRecordId())
            .deductionItemId(po.getDeductionItemId())
            .itemName(po.getItemName())
            .categoryName(po.getCategoryName())
            .spaceType(po.getSpaceType() != null ? SpaceType.valueOf(po.getSpaceType()) : SpaceType.NONE)
            .spaceId(po.getSpaceId())
            .spaceName(po.getSpaceName())
            .studentIds(parseJsonList(po.getStudentIds(), Long.class))
            .studentNames(parseJsonList(po.getStudentNames(), String.class))
            .personCount(po.getPersonCount())
            .deductionAmount(po.getDeductionAmount())
            .inputSource(po.getInputSource() != null ? InputSource.valueOf(po.getInputSource()) : null)
            .remark(po.getRemark())
            .evidenceUrls(parseJsonList(po.getEvidenceUrls(), String.class))
            .createdAt(po.getCreatedAt())
            .build();
    }

    private ChecklistResponsePO toChecklistPO(ChecklistResponse domain, Long classRecordId, Long sessionId) {
        ChecklistResponsePO po = new ChecklistResponsePO();
        po.setId(domain.getId());
        po.setSessionId(sessionId);
        po.setClassRecordId(classRecordId);
        po.setChecklistItemId(domain.getChecklistItemId());
        po.setItemName(domain.getItemName());
        po.setCategoryName(domain.getCategoryName());
        po.setResult(domain.getResult().name());
        po.setAutoDeduction(domain.getAutoDeduction());
        po.setInspectorNote(domain.getInspectorNote());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private ChecklistResponse toChecklistDomain(ChecklistResponsePO po) {
        return ChecklistResponse.builder()
            .id(po.getId())
            .sessionId(po.getSessionId())
            .classRecordId(po.getClassRecordId())
            .checklistItemId(po.getChecklistItemId())
            .itemName(po.getItemName())
            .categoryName(po.getCategoryName())
            .result(ChecklistResult.valueOf(po.getResult()))
            .autoDeduction(po.getAutoDeduction())
            .inspectorNote(po.getInspectorNote())
            .createdAt(po.getCreatedAt())
            .build();
    }

    // ==================== JSON Helpers ====================

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parseJsonList(String json, Class<T> elementType) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            if (elementType == Long.class) {
                return objectMapper.readValue(json, new TypeReference<List<T>>() {});
            } else {
                return objectMapper.readValue(json, new TypeReference<List<T>>() {});
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
