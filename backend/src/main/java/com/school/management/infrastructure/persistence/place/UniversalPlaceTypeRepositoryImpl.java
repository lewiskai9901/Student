package com.school.management.infrastructure.persistence.place;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.place.model.entity.UniversalPlaceType;
import com.school.management.domain.place.repository.UniversalPlaceTypeRepository;
import com.school.management.infrastructure.shared.TypeJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用场所类型仓储实现（统一类型系统 Phase 3）
 */
@Slf4j
@Repository
public class UniversalPlaceTypeRepositoryImpl implements UniversalPlaceTypeRepository {

    private final UniversalPlaceTypeMapper mapper;
    private final ObjectMapper objectMapper;

    public UniversalPlaceTypeRepositoryImpl(UniversalPlaceTypeMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UniversalPlaceType save(UniversalPlaceType placeType) {
        UniversalPlaceTypePO po = toPO(placeType);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UniversalPlaceType> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<UniversalPlaceType> findByTypeCode(String typeCode) {
        return Optional.ofNullable(mapper.findByTypeCode(typeCode)).map(this::toDomain);
    }

    @Override
    public List<UniversalPlaceType> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findAllEnabled() {
        return mapper.findAllEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findAllRootTypes() {
        return mapper.findAllRootTypes().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findByParentTypeCode(String parentTypeCode) {
        return mapper.findByParentTypeCode(parentTypeCode).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findByCategory(String category) {
        return mapper.findByCategory(category).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return mapper.existsByTypeCode(typeCode);
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        return mapper.isTypeInUse(typeCode);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public List<UniversalPlaceType> findByTypeCodes(List<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) return new ArrayList<>();
        return mapper.findByTypeCodes(typeCodes).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findAllBaseCategories() {
        return mapper.findAllBaseCategories().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findConcreteRootTypes() {
        return mapper.findConcreteRootTypes().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceType> findAllConcreteTypes() {
        return mapper.findAllConcreteTypes().stream().map(this::toDomain).collect(Collectors.toList());
    }

    // ==================== PO ↔ Domain ====================

    private UniversalPlaceTypePO toPO(UniversalPlaceType entity) {
        UniversalPlaceTypePO po = new UniversalPlaceTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setDescription(entity.getDescription());
        po.setSortOrder(entity.getSortOrder());
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setCategory(entity.getCategory());
        po.setParentTypeCode(entity.getParentTypeCode());
        po.setFeatures(TypeJsonUtils.toJson(objectMapper, entity.getFeatures()));
        po.setMetadataSchema(entity.getMetadataSchema());
        po.setAllowedChildTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getAllowedChildTypeCodes()));
        po.setMaxDepth(entity.getMaxDepth());
        po.setDefaultUserTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultUserTypeCodes()));
        po.setDefaultOrgTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultOrgTypeCodes()));
        po.setIsRootType(entity.isRootType());
        po.setCapacityUnit(entity.getCapacityUnit());
        po.setDefaultCapacity(entity.getDefaultCapacity());
        return po;
    }

    private UniversalPlaceType toDomain(UniversalPlaceTypePO po) {
        return UniversalPlaceType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .description(po.getDescription())
                .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .category(po.getCategory())
                .parentTypeCode(po.getParentTypeCode())
                .features(TypeJsonUtils.fromJsonMap(objectMapper, po.getFeatures()))
                .metadataSchema(po.getMetadataSchema())
                .allowedChildTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getAllowedChildTypeCodes()))
                .maxDepth(po.getMaxDepth())
                .defaultUserTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultUserTypeCodes()))
                .defaultOrgTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultOrgTypeCodes()))
                .isRootType(Boolean.TRUE.equals(po.getIsRootType()))
                .capacityUnit(po.getCapacityUnit())
                .defaultCapacity(po.getDefaultCapacity())
                .build();
    }
}
