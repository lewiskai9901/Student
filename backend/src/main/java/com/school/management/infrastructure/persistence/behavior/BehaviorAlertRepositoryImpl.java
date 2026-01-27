package com.school.management.infrastructure.persistence.behavior;

import com.school.management.domain.behavior.model.*;
import com.school.management.domain.behavior.repository.BehaviorAlertRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BehaviorAlertRepositoryImpl implements BehaviorAlertRepository {

    private final BehaviorAlertMapper mapper;

    public BehaviorAlertRepositoryImpl(BehaviorAlertMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BehaviorAlert save(BehaviorAlert aggregate) {
        BehaviorAlertPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            mapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<BehaviorAlert> findById(Long id) {
        BehaviorAlertPO po = mapper.selectById(id);
        if (po == null) return Optional.empty();
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(BehaviorAlert aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            mapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public List<BehaviorAlert> findByStudentId(Long studentId) {
        return mapper.findByStudentId(studentId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<BehaviorAlert> findUnhandledByClassId(Long classId) {
        return mapper.findUnhandledByClassId(classId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countUnhandledByClassId(Long classId) {
        return mapper.countUnhandledByClassId(classId);
    }

    @Override
    public List<BehaviorAlert> findByClassId(Long classId) {
        return mapper.findByClassId(classId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private BehaviorAlertPO toPO(BehaviorAlert domain) {
        BehaviorAlertPO po = new BehaviorAlertPO();
        po.setId(domain.getId());
        po.setStudentId(domain.getStudentId());
        po.setClassId(domain.getClassId());
        po.setAlertType(domain.getAlertType().name());
        po.setAlertLevel(domain.getAlertLevel());
        po.setTitle(domain.getTitle());
        po.setDescription(domain.getDescription());
        po.setTriggerData(domain.getTriggerData());
        po.setIsRead(domain.isRead());
        po.setIsHandled(domain.isHandled());
        po.setHandledBy(domain.getHandledBy());
        po.setHandledAt(domain.getHandledAt());
        po.setHandleNote(domain.getHandleNote());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }

    private BehaviorAlert toDomain(BehaviorAlertPO po) {
        return BehaviorAlert.reconstruct()
                .id(po.getId())
                .studentId(po.getStudentId())
                .classId(po.getClassId())
                .alertType(AlertType.valueOf(po.getAlertType()))
                .alertLevel(po.getAlertLevel())
                .title(po.getTitle())
                .description(po.getDescription())
                .triggerData(po.getTriggerData())
                .isRead(po.getIsRead() != null && po.getIsRead())
                .isHandled(po.getIsHandled() != null && po.getIsHandled())
                .handledBy(po.getHandledBy())
                .handledAt(po.getHandledAt())
                .handleNote(po.getHandleNote())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
