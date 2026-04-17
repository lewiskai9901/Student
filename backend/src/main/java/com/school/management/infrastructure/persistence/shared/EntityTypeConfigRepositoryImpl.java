package com.school.management.infrastructure.persistence.shared;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.shared.model.EntityTypeConfig;
import com.school.management.domain.shared.repository.EntityTypeConfigRepository;
import com.school.management.infrastructure.shared.TypeJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一类型配置仓储实现
 */
@Slf4j
@Repository
public class EntityTypeConfigRepositoryImpl implements EntityTypeConfigRepository {

    private final EntityTypeConfigMapper mapper;
    private final ObjectMapper objectMapper;

    public EntityTypeConfigRepositoryImpl(EntityTypeConfigMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public EntityTypeConfig save(EntityTypeConfig config) {
        EntityTypeConfigPO po = toPO(config);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<EntityTypeConfig> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<EntityTypeConfig> findByTypeCode(String entityType, String typeCode) {
        return Optional.ofNullable(mapper.findByTypeCode(entityType, typeCode)).map(this::toDomain);
    }

    @Override
    public List<EntityTypeConfig> findAll(String entityType) {
        return mapper.findAll(entityType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findAllEnabled(String entityType) {
        return mapper.findAllEnabled(entityType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findByParentTypeCode(String entityType, String parentTypeCode) {
        return mapper.findByParentTypeCode(entityType, parentTypeCode).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findTopLevelTypes(String entityType) {
        return mapper.findTopLevelTypes(entityType).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findByCategory(String entityType, String category) {
        return mapper.findByCategory(entityType, category).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findByFeature(String entityType, String featureKey) {
        return mapper.findByFeature(entityType, featureKey).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeConfig> findByTypeCodes(String entityType, List<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) return Collections.emptyList();
        return typeCodes.stream()
            .map(code -> mapper.findByTypeCode(entityType, code))
            .filter(Objects::nonNull)
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String entityType, String typeCode) {
        return mapper.existsByTypeCode(entityType, typeCode);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    // ==================== PO ↔ Domain ====================

    private EntityTypeConfigPO toPO(EntityTypeConfig e) {
        EntityTypeConfigPO po = new EntityTypeConfigPO();
        po.setId(e.getId());
        po.setTenantId(e.getTenantId() != null ? e.getTenantId() : 1L);
        po.setEntityType(e.getEntityType());
        po.setTypeCode(e.getTypeCode());
        po.setTypeName(e.getTypeName());
        po.setDescription(e.getDescription());
        po.setIcon(e.getIcon());
        po.setCategory(e.getCategory());
        po.setParentTypeCode(e.getParentTypeCode());
        po.setAllowedChildTypeCodes(TypeJsonUtils.toJson(objectMapper, e.getAllowedChildTypeCodes()));
        po.setMaxDepth(e.getMaxDepth());
        po.setMetadataSchema(e.getMetadataSchema());
        po.setFeatures(TypeJsonUtils.toJson(objectMapper, e.getFeatures()));
        po.setUiConfig(TypeJsonUtils.toJson(objectMapper, e.getUiConfig()));
        po.setDefaultRoleCodes(TypeJsonUtils.toJson(objectMapper, e.getDefaultRoleCodes()));
        po.setDefaultUserTypeCodes(TypeJsonUtils.toJson(objectMapper, e.getDefaultUserTypeCodes()));
        po.setDefaultOrgTypeCodes(TypeJsonUtils.toJson(objectMapper, e.getDefaultOrgTypeCodes()));
        po.setDefaultPlaceTypeCodes(TypeJsonUtils.toJson(objectMapper, e.getDefaultPlaceTypeCodes()));
        po.setIsPluginRegistered(e.isPluginRegistered());
        po.setPluginClass(e.getPluginClass());
        po.setIsSystem(e.isSystem());
        po.setIsEnabled(e.isEnabled());
        po.setSortOrder(e.getSortOrder());
        return po;
    }

    private EntityTypeConfig toDomain(EntityTypeConfigPO po) {
        return EntityTypeConfig.builder()
            .id(po.getId())
            .tenantId(po.getTenantId())
            .entityType(po.getEntityType())
            .typeCode(po.getTypeCode())
            .typeName(po.getTypeName())
            .description(po.getDescription())
            .icon(po.getIcon())
            .category(po.getCategory())
            .parentTypeCode(po.getParentTypeCode())
            .allowedChildTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getAllowedChildTypeCodes()))
            .maxDepth(po.getMaxDepth())
            .metadataSchema(po.getMetadataSchema())
            .features(TypeJsonUtils.fromJsonMap(objectMapper, po.getFeatures()))
            .uiConfig(TypeJsonUtils.fromJson(objectMapper, po.getUiConfig(),
                new TypeReference<Map<String, Object>>() {}))
            .defaultRoleCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultRoleCodes()))
            .defaultUserTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultUserTypeCodes()))
            .defaultOrgTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultOrgTypeCodes()))
            .defaultPlaceTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultPlaceTypeCodes()))
            .pluginRegistered(Boolean.TRUE.equals(po.getIsPluginRegistered()))
            .pluginClass(po.getPluginClass())
            .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
            .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
            .sortOrder(po.getSortOrder())
            .build();
    }
}
