package com.school.management.infrastructure.persistence.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.event.model.EntityEventType;
import com.school.management.domain.event.repository.EntityEventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 事件类型仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EntityEventTypeRepositoryImpl implements EntityEventTypeRepository {

    private final EntityEventTypeMapper mapper;

    @Override
    public EntityEventType save(EntityEventType type) {
        EntityEventTypePO po = toPO(type);
        if (type.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());
            mapper.insert(po);
            return EntityEventType.builder()
                    .id(po.getId())
                    .tenantId(type.getTenantId())
                    .categoryCode(type.getCategoryCode())
                    .categoryName(type.getCategoryName())
                    .typeCode(type.getTypeCode())
                    .typeName(type.getTypeName())
                    .hasScore(type.getHasScore())
                    .hasSeverity(type.getHasSeverity())
                    .severityLevels(type.getSeverityLevels())
                    .icon(type.getIcon())
                    .color(type.getColor())
                    .applicableSubjects(type.getApplicableSubjects())
                    .isSystem(type.getIsSystem())
                    .isEnabled(type.getIsEnabled())
                    .sortOrder(type.getSortOrder())
                    .createdAt(po.getCreatedAt())
                    .updatedAt(po.getUpdatedAt())
                    .build();
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(po);
            return type;
        }
    }

    @Override
    public Optional<EntityEventType> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<EntityEventType> findByTypeCode(String typeCode) {
        return mapper.selectList(new LambdaQueryWrapper<EntityEventTypePO>()
                        .eq(EntityEventTypePO::getTypeCode, typeCode))
                .stream().findFirst().map(this::toDomain);
    }

    @Override
    public List<EntityEventType> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<EntityEventTypePO>()
                        .orderByAsc(EntityEventTypePO::getCategoryCode, EntityEventTypePO::getSortOrder))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityEventType> findByCategory(String categoryCode) {
        return mapper.selectByCategory(categoryCode).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityEventType> findEnabled() {
        return mapper.selectEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private EntityEventType toDomain(EntityEventTypePO po) {
        return EntityEventType.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .categoryCode(po.getCategoryCode())
                .categoryName(po.getCategoryName())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .hasScore(po.getHasScore() != null && po.getHasScore() == 1)
                .hasSeverity(po.getHasSeverity() != null && po.getHasSeverity() == 1)
                .severityLevels(po.getSeverityLevels())
                .icon(po.getIcon())
                .color(po.getColor())
                .applicableSubjects(po.getApplicableSubjects())
                .isSystem(po.getIsSystem() != null && po.getIsSystem() == 1)
                .isEnabled(po.getIsEnabled() == null || po.getIsEnabled() == 1)
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private EntityEventTypePO toPO(EntityEventType type) {
        EntityEventTypePO po = new EntityEventTypePO();
        po.setId(type.getId());
        po.setTenantId(type.getTenantId() != null ? type.getTenantId() : 0L);
        po.setCategoryCode(type.getCategoryCode());
        po.setCategoryName(type.getCategoryName());
        po.setTypeCode(type.getTypeCode());
        po.setTypeName(type.getTypeName());
        po.setHasScore(Boolean.TRUE.equals(type.getHasScore()) ? 1 : 0);
        po.setHasSeverity(Boolean.TRUE.equals(type.getHasSeverity()) ? 1 : 0);
        po.setSeverityLevels(type.getSeverityLevels());
        po.setIcon(type.getIcon());
        po.setColor(type.getColor());
        po.setApplicableSubjects(type.getApplicableSubjects());
        po.setIsSystem(Boolean.TRUE.equals(type.getIsSystem()) ? 1 : 0);
        po.setIsEnabled(Boolean.TRUE.equals(type.getIsEnabled()) ? 1 : 0);
        po.setSortOrder(type.getSortOrder() != null ? type.getSortOrder() : 0);
        return po;
    }
}
