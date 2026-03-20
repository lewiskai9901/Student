package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.school.management.domain.inspection.model.v7.execution.ViolationRecord;
import com.school.management.domain.inspection.repository.v7.ViolationRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ViolationRecordRepositoryImpl implements ViolationRecordRepository {

    private final ViolationRecordMapper mapper;

    public ViolationRecordRepositoryImpl(ViolationRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ViolationRecord save(ViolationRecord record) {
        ViolationRecordPO po = toPO(record);
        if (record.getId() == null) {
            mapper.insert(po);
            record.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return record;
    }

    @Override
    public Optional<ViolationRecord> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<ViolationRecord> findBySubmissionId(Long submissionId) {
        return mapper.findBySubmissionId(submissionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ViolationRecord> findBySubmissionDetailId(Long submissionDetailId) {
        return mapper.findBySubmissionDetailId(submissionDetailId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ViolationRecord> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private ViolationRecordPO toPO(ViolationRecord d) {
        ViolationRecordPO po = new ViolationRecordPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setSubmissionId(d.getSubmissionId());
        po.setSubmissionDetailId(d.getSubmissionDetailId());
        po.setSectionId(d.getSectionId());
        po.setItemId(d.getItemId());
        po.setUserId(d.getUserId());
        po.setUserName(d.getUserName());
        po.setClassInfo(d.getClassInfo());
        po.setOccurredAt(d.getOccurredAt());
        po.setSeverity(d.getSeverity());
        po.setDescription(d.getDescription());
        po.setEvidenceUrls(d.getEvidenceUrls());
        po.setScore(d.getScore());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ViolationRecord toDomain(ViolationRecordPO po) {
        return ViolationRecord.reconstruct(ViolationRecord.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .submissionId(po.getSubmissionId())
                .submissionDetailId(po.getSubmissionDetailId())
                .sectionId(po.getSectionId())
                .itemId(po.getItemId())
                .userId(po.getUserId())
                .userName(po.getUserName())
                .classInfo(po.getClassInfo())
                .occurredAt(po.getOccurredAt())
                .severity(po.getSeverity())
                .description(po.getDescription())
                .evidenceUrls(po.getEvidenceUrls())
                .score(po.getScore())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
