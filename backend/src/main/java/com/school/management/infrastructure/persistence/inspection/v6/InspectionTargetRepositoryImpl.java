package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.InspectionTarget;
import com.school.management.domain.inspection.model.v6.TargetStatus;
import com.school.management.domain.inspection.model.v6.TargetType;
import com.school.management.domain.inspection.repository.v6.InspectionTargetRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6检查目标仓储实现
 */
@Repository
public class InspectionTargetRepositoryImpl implements InspectionTargetRepository {

    private final InspectionTargetMapper mapper;

    public InspectionTargetRepositoryImpl(InspectionTargetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionTarget save(InspectionTarget target) {
        InspectionTargetPO po = toPO(target);
        if (target.getId() == null) {
            mapper.insert(po);
            target.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return target;
    }

    @Override
    public void saveAll(List<InspectionTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        List<InspectionTargetPO> pos = targets.stream()
                .map(this::toPO)
                .collect(Collectors.toList());
        mapper.batchInsert(pos);
    }

    @Override
    public Optional<InspectionTarget> findById(Long id) {
        InspectionTargetPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionTarget> findByTaskId(Long taskId) {
        return mapper.findByTaskId(taskId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTarget> findByTaskIdAndStatus(Long taskId, TargetStatus status) {
        return mapper.findByTaskIdAndStatus(taskId, status.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InspectionTarget> findByTaskAndTarget(Long taskId, TargetType targetType, Long targetId) {
        InspectionTargetPO po = mapper.findByTaskAndTarget(taskId, targetType.name(), targetId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionTarget> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionTarget> findByClassId(Long classId) {
        return mapper.findByClassId(classId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean lockTarget(Long id, Long lockedBy) {
        return mapper.lockTarget(id, lockedBy) > 0;
    }

    @Override
    public boolean unlockTarget(Long id) {
        return mapper.unlockTarget(id) > 0;
    }

    @Override
    public void completeTarget(Long id) {
        mapper.completeTarget(id);
    }

    @Override
    public void skipTarget(Long id, String skipReason) {
        mapper.skipTarget(id, skipReason);
    }

    @Override
    public void addDeduction(Long id, BigDecimal deduction) {
        mapper.addDeduction(id, deduction);
    }

    @Override
    public void addBonus(Long id, BigDecimal bonus) {
        mapper.addBonus(id, bonus);
    }

    @Override
    public int countByTaskId(Long taskId) {
        return mapper.countByTaskId(taskId);
    }

    @Override
    public int countCompletedByTaskId(Long taskId) {
        return mapper.countCompletedByTaskId(taskId);
    }

    @Override
    public int countSkippedByTaskId(Long taskId) {
        return mapper.countSkippedByTaskId(taskId);
    }

    private InspectionTargetPO toPO(InspectionTarget domain) {
        InspectionTargetPO po = new InspectionTargetPO();
        po.setId(domain.getId());
        po.setTaskId(domain.getTaskId());
        po.setTargetType(domain.getTargetType() != null ? domain.getTargetType().name() : null);
        po.setTargetId(domain.getTargetId());
        po.setTargetName(domain.getTargetName());
        po.setTargetCode(domain.getTargetCode());
        po.setOrgUnitId(domain.getOrgUnitId());
        po.setOrgUnitName(domain.getOrgUnitName());
        po.setClassId(domain.getClassId());
        po.setClassName(domain.getClassName());
        po.setWeightRatio(domain.getWeightRatio());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setLockedBy(domain.getLockedBy());
        po.setLockedAt(domain.getLockedAt());
        po.setCompletedAt(domain.getCompletedAt());
        po.setBaseScore(domain.getBaseScore());
        po.setFinalScore(domain.getFinalScore());
        po.setDeductionTotal(domain.getDeductionTotal());
        po.setBonusTotal(domain.getBonusTotal());
        po.setSnapshot(domain.getSnapshot());
        po.setSkipReason(domain.getSkipReason());
        po.setRemarks(domain.getRemarks());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private InspectionTarget toDomain(InspectionTargetPO po) {
        return InspectionTarget.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .targetType(TargetType.fromCode(po.getTargetType()))
                .targetId(po.getTargetId())
                .targetName(po.getTargetName())
                .targetCode(po.getTargetCode())
                .orgUnitId(po.getOrgUnitId())
                .orgUnitName(po.getOrgUnitName())
                .classId(po.getClassId())
                .className(po.getClassName())
                .weightRatio(po.getWeightRatio())
                .status(TargetStatus.fromCode(po.getStatus()))
                .lockedBy(po.getLockedBy())
                .lockedAt(po.getLockedAt())
                .completedAt(po.getCompletedAt())
                .baseScore(po.getBaseScore())
                .finalScore(po.getFinalScore())
                .deductionTotal(po.getDeductionTotal())
                .bonusTotal(po.getBonusTotal())
                .snapshot(po.getSnapshot())
                .skipReason(po.getSkipReason())
                .remarks(po.getRemarks())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
