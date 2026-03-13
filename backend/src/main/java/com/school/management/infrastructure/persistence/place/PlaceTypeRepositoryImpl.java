package com.school.management.infrastructure.persistence.space;

import com.school.management.domain.space.model.entity.SpaceType;
import com.school.management.domain.space.repository.SpaceTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 场所类型仓储实现（简化版）
 */
@Repository
public class SpaceTypeRepositoryImpl implements SpaceTypeRepository {

    private final SpaceTypeMapper spaceTypeMapper;

    public SpaceTypeRepositoryImpl(SpaceTypeMapper spaceTypeMapper) {
        this.spaceTypeMapper = spaceTypeMapper;
    }

    @Override
    public SpaceType save(SpaceType spaceType) {
        SpaceTypePO po = toPO(spaceType);
        if (po.getId() == null) {
            spaceTypeMapper.insert(po);
        } else {
            spaceTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<SpaceType> findById(Long id) {
        SpaceTypePO po = spaceTypeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<SpaceType> findByTypeCode(String typeCode) {
        SpaceTypePO po = spaceTypeMapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SpaceType> findAll() {
        return spaceTypeMapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceType> findAllEnabled() {
        return spaceTypeMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceType> findByParentTypeCode(String parentTypeCode) {
        return spaceTypeMapper.findByParentTypeCode(parentTypeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return spaceTypeMapper.existsByTypeCode(typeCode);
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        try {
            return spaceTypeMapper.countSpacesByTypeCode(typeCode) > 0;
        } catch (Exception e) {
            // spaces表可能不存在，返回false
            return false;
        }
    }

    @Override
    public void deleteById(Long id) {
        spaceTypeMapper.deleteById(id);
    }

    // ==================== 转换方法（简化版） ====================

    private SpaceTypePO toPO(SpaceType entity) {
        SpaceTypePO po = new SpaceTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setParentTypeCode(entity.getParentTypeCode());
        po.setLevelOrder(entity.getLevelOrder());
        po.setIcon(entity.getIcon());
        po.setDescription(entity.getDescription());
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setSortOrder(entity.getSortOrder());
        return po;
    }

    private SpaceType toDomain(SpaceTypePO po) {
        return SpaceType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .parentTypeCode(po.getParentTypeCode())
                .levelOrder(po.getLevelOrder())
                .icon(po.getIcon())
                .description(po.getDescription())
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
