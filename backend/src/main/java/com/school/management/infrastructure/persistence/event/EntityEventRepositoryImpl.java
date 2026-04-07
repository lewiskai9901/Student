package com.school.management.infrastructure.persistence.event;

import com.school.management.domain.event.model.EntityEvent;
import com.school.management.domain.event.model.EntityEventRelation;
import com.school.management.domain.event.repository.EntityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EntityEventRepositoryImpl implements EntityEventRepository {

    private final EntityEventMapper eventMapper;
    private final EntityEventRelationMapper relationMapper;

    @Override
    public EntityEvent save(EntityEvent event) {
        EntityEventPO po = toPO(event);
        if (event.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            eventMapper.insert(po);
            // 保存关联主体
            if (event.getRelations() != null) {
                for (EntityEventRelation relation : event.getRelations()) {
                    EntityEventRelationPO relPO = toRelationPO(relation);
                    relPO.setEventId(po.getId());
                    relationMapper.insert(relPO);
                }
            }
            return EntityEvent.builder()
                    .id(po.getId())
                    .tenantId(event.getTenantId())
                    .subjectType(event.getSubjectType())
                    .subjectId(event.getSubjectId())
                    .subjectName(event.getSubjectName())
                    .eventCategory(event.getEventCategory())
                    .eventType(event.getEventType())
                    .eventLabel(event.getEventLabel())
                    .payload(event.getPayload())
                    .sourceModule(event.getSourceModule())
                    .sourceRefType(event.getSourceRefType())
                    .sourceRefId(event.getSourceRefId())
                    .tags(event.getTags())
                    .createdBy(event.getCreatedBy())
                    .createdByName(event.getCreatedByName())
                    .occurredAt(event.getOccurredAt())
                    .createdAt(po.getCreatedAt())
                    .relations(event.getRelations())
                    .build();
        } else {
            eventMapper.updateById(po);
            return event;
        }
    }

    @Override
    public List<EntityEvent> findBySubject(String subjectType, Long subjectId, int limit) {
        return eventMapper.selectBySubject(subjectType, subjectId, limit)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityEvent> findByRelated(String relatedType, Long relatedId, int limit) {
        return eventMapper.selectByRelated(relatedType, relatedId, limit)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> countBySubjectGroupByType(String subjectType, Long subjectId) {
        return eventMapper.countGroupByType(subjectType, subjectId);
    }

    @Override
    public String findCategoryByTypeCode(String typeCode) {
        return eventMapper.selectCategoryByTypeCode(typeCode);
    }

    private EntityEvent toDomain(EntityEventPO po) {
        return EntityEvent.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .subjectType(po.getSubjectType())
                .subjectId(po.getSubjectId())
                .subjectName(po.getSubjectName())
                .eventCategory(po.getEventCategory())
                .eventType(po.getEventType())
                .eventLabel(po.getEventLabel())
                .payload(po.getPayload())
                .sourceModule(po.getSourceModule())
                .sourceRefType(po.getSourceRefType())
                .sourceRefId(po.getSourceRefId())
                .tags(po.getTags())
                .createdBy(po.getCreatedBy())
                .createdByName(po.getCreatedByName())
                .occurredAt(po.getOccurredAt())
                .createdAt(po.getCreatedAt())
                .build();
    }

    private EntityEventPO toPO(EntityEvent event) {
        EntityEventPO po = new EntityEventPO();
        po.setId(event.getId());
        po.setTenantId(event.getTenantId() != null ? event.getTenantId() : 0L);
        po.setSubjectType(event.getSubjectType());
        po.setSubjectId(event.getSubjectId());
        po.setSubjectName(event.getSubjectName());
        po.setEventCategory(event.getEventCategory());
        po.setEventType(event.getEventType());
        po.setEventLabel(event.getEventLabel());
        po.setPayload(event.getPayload());
        po.setSourceModule(event.getSourceModule());
        po.setSourceRefType(event.getSourceRefType());
        po.setSourceRefId(event.getSourceRefId());
        po.setTags(event.getTags());
        po.setCreatedBy(event.getCreatedBy());
        po.setCreatedByName(event.getCreatedByName());
        po.setOccurredAt(event.getOccurredAt());
        return po;
    }

    private EntityEventRelationPO toRelationPO(EntityEventRelation relation) {
        EntityEventRelationPO po = new EntityEventRelationPO();
        po.setId(relation.getId());
        po.setEventId(relation.getEventId());
        po.setRelatedType(relation.getRelatedType());
        po.setRelatedId(relation.getRelatedId());
        po.setRelatedName(relation.getRelatedName());
        po.setRelation(relation.getRelation());
        return po;
    }
}
