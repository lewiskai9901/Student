package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.EvidenceType;
import com.school.management.domain.inspection.model.v7.execution.InspEvidence;
import com.school.management.domain.inspection.repository.v7.InspEvidenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspEvidenceRepositoryImpl implements InspEvidenceRepository {

    private final InspEvidenceMapper mapper;

    public InspEvidenceRepositoryImpl(InspEvidenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspEvidence save(InspEvidence evidence) {
        InspEvidencePO po = toPO(evidence);
        if (evidence.getId() == null) {
            mapper.insert(po);
            evidence.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return evidence;
    }

    @Override
    public Optional<InspEvidence> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspEvidence> findBySubmissionId(Long submissionId) {
        return mapper.findBySubmissionId(submissionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspEvidence> findByDetailId(Long detailId) {
        return mapper.findByDetailId(detailId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteBySubmissionId(Long submissionId) {
        mapper.delete(new LambdaQueryWrapper<InspEvidencePO>().eq(InspEvidencePO::getSubmissionId, submissionId));
    }

    private InspEvidencePO toPO(InspEvidence d) {
        InspEvidencePO po = new InspEvidencePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setSubmissionId(d.getSubmissionId());
        po.setDetailId(d.getDetailId());
        po.setEvidenceType(d.getEvidenceType() != null ? d.getEvidenceType().name() : null);
        po.setFileName(d.getFileName());
        po.setFilePath(d.getFilePath());
        po.setFileUrl(d.getFileUrl());
        po.setFileSize(d.getFileSize());
        po.setMimeType(d.getMimeType());
        po.setThumbnailUrl(d.getThumbnailUrl());
        po.setLatitude(d.getLatitude());
        po.setLongitude(d.getLongitude());
        po.setCapturedAt(d.getCapturedAt());
        po.setMetadata(d.getMetadata());
        po.setAiAnalysis(d.getAiAnalysis());
        po.setAiConfidence(d.getAiConfidence());
        po.setCreatedAt(d.getCreatedAt());
        return po;
    }

    private InspEvidence toDomain(InspEvidencePO po) {
        return InspEvidence.reconstruct(InspEvidence.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .submissionId(po.getSubmissionId())
                .detailId(po.getDetailId())
                .evidenceType(po.getEvidenceType() != null ? EvidenceType.valueOf(po.getEvidenceType()) : null)
                .fileName(po.getFileName())
                .filePath(po.getFilePath())
                .fileUrl(po.getFileUrl())
                .fileSize(po.getFileSize())
                .mimeType(po.getMimeType())
                .thumbnailUrl(po.getThumbnailUrl())
                .latitude(po.getLatitude())
                .longitude(po.getLongitude())
                .capturedAt(po.getCapturedAt())
                .metadata(po.getMetadata())
                .aiAnalysis(po.getAiAnalysis())
                .aiConfidence(po.getAiConfidence())
                .createdAt(po.getCreatedAt()));
    }
}
