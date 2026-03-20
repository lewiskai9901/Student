package com.school.management.domain.event.repository;

import com.school.management.domain.event.model.EntityEventType;

import java.util.List;
import java.util.Optional;

/**
 * 事件类型仓储接口
 */
public interface EntityEventTypeRepository {

    EntityEventType save(EntityEventType type);

    Optional<EntityEventType> findById(Long id);

    Optional<EntityEventType> findByTypeCode(String typeCode);

    List<EntityEventType> findAll();

    List<EntityEventType> findByCategory(String categoryCode);

    List<EntityEventType> findEnabled();

    void delete(Long id);
}
