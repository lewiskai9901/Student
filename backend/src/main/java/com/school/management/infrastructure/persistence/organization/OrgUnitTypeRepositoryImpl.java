package com.school.management.infrastructure.persistence.organization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.model.valueobject.PositionTemplate;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.infrastructure.shared.TypeJsonUtils;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织类型仓储实现
 */
@Repository
public class OrgUnitTypeRepositoryImpl implements OrgUnitTypeRepository {

    private final OrgUnitTypeMapper orgUnitTypeMapper;
    private final ObjectMapper objectMapper;

    public OrgUnitTypeRepositoryImpl(OrgUnitTypeMapper orgUnitTypeMapper, ObjectMapper objectMapper) {
        this.orgUnitTypeMapper = orgUnitTypeMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public OrgType save(OrgType orgType) {
        OrgUnitTypePO po = toPO(orgType);
        if (po.getId() == null) {
            orgUnitTypeMapper.insert(po);
        } else {
            orgUnitTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<OrgType> findById(Long id) {
        OrgUnitTypePO po = orgUnitTypeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<OrgType> findByTypeCode(String typeCode) {
        OrgUnitTypePO po = orgUnitTypeMapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<OrgType> findAll() {
        return orgUnitTypeMapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findAllEnabled() {
        return orgUnitTypeMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findByParentTypeCode(String parentTypeCode) {
        return orgUnitTypeMapper.findByParentTypeCode(parentTypeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findTopLevelTypes() {
        return orgUnitTypeMapper.findTopLevelTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findByCategory(String category) {
        return orgUnitTypeMapper.findByCategory(category).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findByFeature(String featureKey) {
        return orgUnitTypeMapper.findByFeature(featureKey).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return orgUnitTypeMapper.existsByTypeCode(typeCode);
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        try {
            return orgUnitTypeMapper.countOrgUnitsByTypeCode(typeCode) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteById(Long id) {
        orgUnitTypeMapper.softDeleteById(id);
    }

    // ==================== 转换方法 ====================

    private OrgUnitTypePO toPO(OrgType entity) {
        OrgUnitTypePO po = new OrgUnitTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setCategory(entity.getCategory());
        po.setParentTypeCode(entity.getParentTypeCode());
        po.setIcon(entity.getIcon());
        po.setDescription(entity.getDescription());
        po.setFeatures(TypeJsonUtils.toJson(objectMapper, entity.getFeatures()));
        po.setMetadataSchema(entity.getMetadataSchema());
        po.setAllowedChildTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getAllowedChildTypeCodes()));
        po.setMaxDepth(entity.getMaxDepth());
        po.setDefaultUserTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultUserTypeCodes()));
        po.setDefaultPlaceTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultPlaceTypeCodes()));
        po.setDefaultPositions(TypeJsonUtils.toJson(objectMapper, entity.getDefaultPositions()));
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setSortOrder(entity.getSortOrder());
        return po;
    }

    private OrgType toDomain(OrgUnitTypePO po) {
        return OrgType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .category(po.getCategory())
                .parentTypeCode(po.getParentTypeCode())
                .icon(po.getIcon())
                .description(po.getDescription())
                .features(TypeJsonUtils.fromJsonMap(objectMapper, po.getFeatures()))
                .metadataSchema(po.getMetadataSchema())
                .allowedChildTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getAllowedChildTypeCodes()))
                .maxDepth(po.getMaxDepth())
                .defaultUserTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultUserTypeCodes()))
                .defaultPlaceTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultPlaceTypeCodes()))
                .defaultPositions(TypeJsonUtils.fromJson(objectMapper, po.getDefaultPositions(), new TypeReference<List<PositionTemplate>>(){}))
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
