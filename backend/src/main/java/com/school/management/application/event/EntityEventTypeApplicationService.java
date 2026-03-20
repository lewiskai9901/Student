package com.school.management.application.event;

import com.school.management.domain.event.model.EntityEventType;
import com.school.management.domain.event.repository.EntityEventTypeRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事件类型应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityEventTypeApplicationService {

    private final EntityEventTypeRepository typeRepository;

    public List<EntityEventType> listTypes() {
        return typeRepository.findAll();
    }

    public List<EntityEventType> listByCategory(String categoryCode) {
        return typeRepository.findByCategory(categoryCode);
    }

    public List<EntityEventType> listEnabled() {
        return typeRepository.findEnabled();
    }

    @Transactional
    public EntityEventType create(EntityEventType type) {
        typeRepository.findByTypeCode(type.getTypeCode()).ifPresent(existing -> {
            throw new BusinessException("事件类型编码已存在: " + type.getTypeCode());
        });
        EntityEventType saved = typeRepository.save(type);
        log.info("Created entity event type: {}", saved.getTypeCode());
        return saved;
    }

    @Transactional
    public EntityEventType update(Long id, EntityEventType updated) {
        EntityEventType existing = typeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("事件类型不存在"));
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException("系统预置类型不允许修改");
        }
        EntityEventType toSave = EntityEventType.builder()
                .id(id)
                .tenantId(existing.getTenantId())
                .categoryCode(updated.getCategoryCode() != null ? updated.getCategoryCode() : existing.getCategoryCode())
                .categoryName(updated.getCategoryName() != null ? updated.getCategoryName() : existing.getCategoryName())
                .typeCode(existing.getTypeCode()) // typeCode 不允许修改
                .typeName(updated.getTypeName() != null ? updated.getTypeName() : existing.getTypeName())
                .hasScore(updated.getHasScore() != null ? updated.getHasScore() : existing.getHasScore())
                .hasSeverity(updated.getHasSeverity() != null ? updated.getHasSeverity() : existing.getHasSeverity())
                .severityLevels(updated.getSeverityLevels() != null ? updated.getSeverityLevels() : existing.getSeverityLevels())
                .icon(updated.getIcon() != null ? updated.getIcon() : existing.getIcon())
                .color(updated.getColor() != null ? updated.getColor() : existing.getColor())
                .applicableSubjects(updated.getApplicableSubjects() != null ? updated.getApplicableSubjects() : existing.getApplicableSubjects())
                .isSystem(existing.getIsSystem())
                .isEnabled(updated.getIsEnabled() != null ? updated.getIsEnabled() : existing.getIsEnabled())
                .sortOrder(updated.getSortOrder() != null ? updated.getSortOrder() : existing.getSortOrder())
                .build();
        return typeRepository.save(toSave);
    }

    @Transactional
    public void delete(Long id) {
        EntityEventType existing = typeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("事件类型不存在"));
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new BusinessException("系统预置类型不允许删除");
        }
        typeRepository.delete(id);
        log.info("Deleted entity event type: {}", id);
    }
}
