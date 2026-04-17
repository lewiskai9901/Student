package com.school.management.domain.shared.repository;

import com.school.management.domain.shared.model.EntityTypeConfig;

import java.util.List;
import java.util.Optional;

/**
 * 统一类型配置仓储 — 按 entity_type + type_code 寻址。
 * entityType 取值: ORG_UNIT / PLACE / USER
 */
public interface EntityTypeConfigRepository {

    EntityTypeConfig save(EntityTypeConfig config);

    Optional<EntityTypeConfig> findById(Long id);

    Optional<EntityTypeConfig> findByTypeCode(String entityType, String typeCode);

    List<EntityTypeConfig> findAll(String entityType);

    List<EntityTypeConfig> findAllEnabled(String entityType);

    List<EntityTypeConfig> findByParentTypeCode(String entityType, String parentTypeCode);

    List<EntityTypeConfig> findTopLevelTypes(String entityType);

    List<EntityTypeConfig> findByCategory(String entityType, String category);

    List<EntityTypeConfig> findByFeature(String entityType, String featureKey);

    List<EntityTypeConfig> findByTypeCodes(String entityType, List<String> typeCodes);

    boolean existsByTypeCode(String entityType, String typeCode);

    void deleteById(Long id);
}
