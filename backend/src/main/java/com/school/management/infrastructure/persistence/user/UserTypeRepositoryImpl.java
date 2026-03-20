package com.school.management.infrastructure.persistence.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserTypeRepository;
import com.school.management.infrastructure.shared.TypeJsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户类型仓储实现（统一类型系统 Phase 2）
 */
@Slf4j
@Repository
public class UserTypeRepositoryImpl implements UserTypeRepository {

    private final UserTypeMapper userTypeMapper;
    private final ObjectMapper objectMapper;

    public UserTypeRepositoryImpl(UserTypeMapper userTypeMapper, ObjectMapper objectMapper) {
        this.userTypeMapper = userTypeMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserType save(UserType userType) {
        UserTypePO po = toPO(userType);
        if (po.getId() == null) {
            userTypeMapper.insert(po);
        } else {
            userTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UserType> findById(Long id) {
        return Optional.ofNullable(userTypeMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<UserType> findByTypeCode(String typeCode) {
        return Optional.ofNullable(userTypeMapper.findByTypeCode(typeCode)).map(this::toDomain);
    }

    @Override
    public List<UserType> findAll() {
        return userTypeMapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserType> findAllEnabled() {
        return userTypeMapper.findAllEnabled().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserType> findByParentTypeCode(String parentTypeCode) {
        return userTypeMapper.findByParentTypeCode(parentTypeCode).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserType> findTopLevelTypes() {
        return userTypeMapper.findTopLevelTypes().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserType> findByCategory(String category) {
        return userTypeMapper.findByCategory(category).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserType> findByFeature(String featureKey) {
        return userTypeMapper.findByFeature(featureKey).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return userTypeMapper.existsByTypeCode(typeCode);
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        return userTypeMapper.countUsersByTypeCode(typeCode) > 0;
    }

    @Override
    public void deleteById(Long id) {
        userTypeMapper.deleteById(id);
    }

    // ==================== PO ↔ Domain ====================

    private UserTypePO toPO(UserType entity) {
        UserTypePO po = new UserTypePO();
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
        po.setDefaultRoleCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultRoleCodes()));
        po.setDefaultOrgTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultOrgTypeCodes()));
        po.setDefaultPlaceTypeCodes(TypeJsonUtils.toJson(objectMapper, entity.getDefaultPlaceTypeCodes()));
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setSortOrder(entity.getSortOrder());
        return po;
    }

    private UserType toDomain(UserTypePO po) {
        return UserType.builder()
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
                .defaultRoleCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultRoleCodes()))
                .defaultOrgTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultOrgTypeCodes()))
                .defaultPlaceTypeCodes(TypeJsonUtils.fromJsonList(objectMapper, po.getDefaultPlaceTypeCodes()))
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
