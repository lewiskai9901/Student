package com.school.management.application.event;

import com.school.management.domain.event.model.EntityEvent;
import com.school.management.domain.event.model.EntityEventRelation;
import com.school.management.domain.event.repository.EntityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体事件应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityEventApplicationService {

    private final EntityEventRepository eventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 创建事件
     * 自动将 sourceRef 添加为关联主体（如果提供）
     */
    @Transactional
    public EntityEvent createEvent(String subjectType, Long subjectId, String subjectName,
                                   String eventType, String eventLabel,
                                   String payload, String sourceModule,
                                   String sourceRefType, Long sourceRefId,
                                   String tags, Long createdBy, String createdByName) {
        // 从 eventType 推导 eventCategory（取下划线前的第一段）
        String eventCategory = deriveCategory(eventType);

        EntityEvent event = EntityEvent.create(
                subjectType, subjectId, subjectName,
                eventCategory, eventType, eventLabel,
                payload, sourceModule, sourceRefType, sourceRefId,
                tags, createdBy, createdByName
        );

        // 自动添加 sourceRef 关联主体
        if (sourceRefType != null && sourceRefId != null) {
            List<EntityEventRelation> relations = new ArrayList<>();
            relations.add(EntityEventRelation.builder()
                    .relatedType(sourceRefType)
                    .relatedId(sourceRefId)
                    .relation("SOURCE")
                    .build());
            event = EntityEvent.builder()
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
                    .relations(relations)
                    .build();
        }

        EntityEvent saved = eventRepository.save(event);
        log.info("Created entity event: {} for {}/{}", eventType, subjectType, subjectId);

        // 发布 Spring Application Event → EntityEventDispatchListener → MessageDispatcher
        applicationEventPublisher.publishEvent(new EntityEventCreatedNotification(saved));

        return saved;
    }

    /**
     * 按主体查询时间线
     */
    public List<EntityEvent> getSubjectTimeline(String subjectType, Long subjectId, int limit) {
        int safeLimit = Math.min(limit, 200);
        return eventRepository.findBySubject(subjectType, subjectId, safeLimit);
    }

    /**
     * 按关联主体查询时间线
     */
    public List<EntityEvent> getRelatedTimeline(String relatedType, Long relatedId, int limit) {
        int safeLimit = Math.min(limit, 200);
        return eventRepository.findByRelated(relatedType, relatedId, safeLimit);
    }

    /**
     * 获取主体事件统计
     */
    public List<Map<String, Object>> getSubjectStats(String subjectType, Long subjectId) {
        return eventRepository.countBySubjectGroupByType(subjectType, subjectId);
    }

    /**
     * 从 entity_event_types 表查询 eventType 对应的 category_code
     * 查不到时降级为取下划线前缀
     */
    private String deriveCategory(String eventType) {
        if (eventType == null) return "UNKNOWN";
        try {
            return eventRepository.findCategoryByTypeCode(eventType);
        } catch (Exception e) {
            // 降级：取第一个下划线前的部分
            int idx = eventType.indexOf('_');
            return idx > 0 ? eventType.substring(0, idx) : eventType;
        }
    }
}
