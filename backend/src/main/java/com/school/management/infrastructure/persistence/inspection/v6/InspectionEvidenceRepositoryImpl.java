package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.InspectionEvidence;
import com.school.management.domain.inspection.repository.v6.InspectionEvidenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6检查证据仓储实现
 */
@Repository
public class InspectionEvidenceRepositoryImpl implements InspectionEvidenceRepository {

    private final InspectionEvidenceMapper mapper;

    public InspectionEvidenceRepositoryImpl(InspectionEvidenceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionEvidence save(InspectionEvidence evidence) {
        InspectionEvidencePO po = toPO(evidence);
        if (evidence.getId() == null) {
            mapper.insert(po);
            evidence.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return evidence;
    }

    @Override
    public void saveAll(List<InspectionEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return;
        }
        List<InspectionEvidencePO> pos = evidences.stream()
                .map(this::toPO)
                .collect(Collectors.toList());
        mapper.batchInsert(pos);
    }

    @Override
    public Optional<InspectionEvidence> findById(Long id) {
        InspectionEvidencePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionEvidence> findByDetailId(Long detailId) {
        return mapper.findByDetailId(detailId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionEvidence> findByTargetId(Long targetId) {
        return mapper.findByTargetId(targetId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionEvidence> findByDetailIds(List<Long> detailIds) {
        if (detailIds == null || detailIds.isEmpty()) {
            return List.of();
        }
        return mapper.findByDetailIds(detailIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByDetailId(Long detailId) {
        mapper.deleteByDetailId(detailId);
    }

    @Override
    public void deleteByTargetId(Long targetId) {
        mapper.deleteByTargetId(targetId);
    }

    @Override
    public int countByDetailId(Long detailId) {
        return mapper.countByDetailId(detailId);
    }

    @Override
    public int countByTargetId(Long targetId) {
        return mapper.countByTargetId(targetId);
    }

    private InspectionEvidencePO toPO(InspectionEvidence domain) {
        InspectionEvidencePO po = new InspectionEvidencePO();
        po.setId(domain.getId());
        po.setDetailId(domain.getDetailId());
        po.setTargetId(domain.getTargetId());
        po.setFileName(domain.getFileName());
        po.setFilePath(domain.getFilePath());
        po.setFileUrl(domain.getFileUrl());
        po.setFileSize(domain.getFileSize());
        po.setFileType(domain.getFileType());
        po.setLatitude(domain.getLatitude());
        po.setLongitude(domain.getLongitude());
        po.setUploadBy(domain.getUploadBy());
        po.setUploadTime(domain.getUploadTime());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private InspectionEvidence toDomain(InspectionEvidencePO po) {
        return InspectionEvidence.builder()
                .id(po.getId())
                .detailId(po.getDetailId())
                .targetId(po.getTargetId())
                .fileName(po.getFileName())
                .filePath(po.getFilePath())
                .fileUrl(po.getFileUrl())
                .fileSize(po.getFileSize())
                .fileType(po.getFileType())
                .latitude(po.getLatitude())
                .longitude(po.getLongitude())
                .uploadBy(po.getUploadBy())
                .uploadTime(po.getUploadTime())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
