package com.school.management.infrastructure.persistence.space;

import com.school.management.domain.space.model.entity.UniversalSpaceType;
import com.school.management.domain.space.repository.UniversalSpaceTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用空间类型仓储实现
 */
@Repository
public class UniversalSpaceTypeRepositoryImpl implements UniversalSpaceTypeRepository {

    private final UniversalSpaceTypeMapper mapper;
    private final ObjectMapper objectMapper;

    public UniversalSpaceTypeRepositoryImpl(UniversalSpaceTypeMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UniversalSpaceType save(UniversalSpaceType spaceType) {
        UniversalSpaceTypePO po = toPO(spaceType);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UniversalSpaceType> findById(Long id) {
        UniversalSpaceTypePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<UniversalSpaceType> findByTypeCode(String typeCode) {
        UniversalSpaceTypePO po = mapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalSpaceType> findAll() {
        return mapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpaceType> findAllEnabled() {
        return mapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpaceType> findAllRootTypes() {
        return mapper.findAllRootTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalSpaceType> findAllowedChildTypes(String parentTypeCode) {
        Optional<UniversalSpaceType> parentOpt = findByTypeCode(parentTypeCode);
        if (parentOpt.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> allowedCodes = parentOpt.get().getAllowedChildTypes();
        if (allowedCodes == null || allowedCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return findByTypeCodes(allowedCodes);
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
    public List<UniversalSpaceType> findByTypeCodes(List<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return mapper.findByTypeCodes(typeCodes).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private UniversalSpaceTypePO toPO(UniversalSpaceType entity) {
        UniversalSpaceTypePO po = new UniversalSpaceTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setIcon(entity.getIcon());
        po.setDescription(entity.getDescription());
        po.setSortOrder(entity.getSortOrder());
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setIsRootType(entity.isRootType());
        po.setHasCapacity(entity.isHasCapacity());
        po.setBookable(entity.isBookable());
        po.setAssignable(entity.isAssignable());
        po.setOccupiable(entity.isOccupiable());
        po.setCapacityUnit(entity.getCapacityUnit());
        po.setDefaultCapacity(entity.getDefaultCapacity());
        po.setAttributeSchema(entity.getAttributeSchema());

        // 转换 allowedChildTypes 为 JSON
        if (entity.getAllowedChildTypes() != null && !entity.getAllowedChildTypes().isEmpty()) {
            try {
                po.setAllowedChildTypes(objectMapper.writeValueAsString(entity.getAllowedChildTypes()));
            } catch (JsonProcessingException e) {
                po.setAllowedChildTypes("[]");
            }
        } else {
            po.setAllowedChildTypes("[]");
        }

        return po;
    }

    private UniversalSpaceType toDomain(UniversalSpaceTypePO po) {
        List<String> allowedChildTypes = new ArrayList<>();
        if (po.getAllowedChildTypes() != null && !po.getAllowedChildTypes().isBlank()) {
            try {
                allowedChildTypes = objectMapper.readValue(po.getAllowedChildTypes(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                // 忽略解析错误
            }
        }

        return UniversalSpaceType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .icon(po.getIcon())
                .description(po.getDescription())
                .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .isRootType(Boolean.TRUE.equals(po.getIsRootType()))
                .allowedChildTypes(allowedChildTypes)
                .hasCapacity(Boolean.TRUE.equals(po.getHasCapacity()))
                .bookable(Boolean.TRUE.equals(po.getBookable()))
                .assignable(Boolean.TRUE.equals(po.getAssignable()))
                .occupiable(Boolean.TRUE.equals(po.getOccupiable()))
                .capacityUnit(po.getCapacityUnit())
                .defaultCapacity(po.getDefaultCapacity())
                .attributeSchema(po.getAttributeSchema())
                .build();
    }
}
