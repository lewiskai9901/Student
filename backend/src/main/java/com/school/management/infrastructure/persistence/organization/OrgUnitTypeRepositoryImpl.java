package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.entity.OrgUnitTypeEntity;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 组织类型仓储实现
 */
@Repository
public class OrgUnitTypeRepositoryImpl implements OrgUnitTypeRepository {

    private final OrgUnitTypeMapper orgUnitTypeMapper;

    public OrgUnitTypeRepositoryImpl(OrgUnitTypeMapper orgUnitTypeMapper) {
        this.orgUnitTypeMapper = orgUnitTypeMapper;
    }

    @Override
    public OrgUnitTypeEntity save(OrgUnitTypeEntity orgUnitType) {
        OrgUnitTypePO po = toPO(orgUnitType);
        if (po.getId() == null) {
            orgUnitTypeMapper.insert(po);
        } else {
            orgUnitTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<OrgUnitTypeEntity> findById(Long id) {
        OrgUnitTypePO po = orgUnitTypeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<OrgUnitTypeEntity> findByTypeCode(String typeCode) {
        OrgUnitTypePO po = orgUnitTypeMapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<OrgUnitTypeEntity> findAll() {
        return orgUnitTypeMapper.selectList(null).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findAllEnabled() {
        return orgUnitTypeMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findByParentTypeCode(String parentTypeCode) {
        return orgUnitTypeMapper.findByParentTypeCode(parentTypeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findTopLevelTypes() {
        return orgUnitTypeMapper.findTopLevelTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findAcademicTypes() {
        return orgUnitTypeMapper.findAcademicTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findFunctionalTypes() {
        return orgUnitTypeMapper.findFunctionalTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnitTypeEntity> findInspectableTypes() {
        return orgUnitTypeMapper.findInspectableTypes().stream()
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
            // 表可能不存在，返回false
            return false;
        }
    }

    @Override
    public void deleteById(Long id) {
        orgUnitTypeMapper.deleteById(id);
    }

    // ==================== 转换方法 ====================

    private OrgUnitTypePO toPO(OrgUnitTypeEntity entity) {
        OrgUnitTypePO po = new OrgUnitTypePO();
        po.setId(entity.getId());
        po.setTypeCode(entity.getTypeCode());
        po.setTypeName(entity.getTypeName());
        po.setParentTypeCode(entity.getParentTypeCode());
        po.setLevelOrder(entity.getLevelOrder());
        po.setIcon(entity.getIcon());
        po.setColor(entity.getColor());
        po.setDescription(entity.getDescription());
        po.setIsAcademic(entity.isAcademic());
        po.setCanBeInspected(entity.isCanBeInspected());
        po.setCanHaveChildren(entity.isCanHaveChildren());
        po.setMaxDepth(entity.getMaxDepth());
        po.setIsSystem(entity.isSystem());
        po.setIsEnabled(entity.isEnabled());
        po.setSortOrder(entity.getSortOrder());
        return po;
    }

    private OrgUnitTypeEntity toDomain(OrgUnitTypePO po) {
        return OrgUnitTypeEntity.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .parentTypeCode(po.getParentTypeCode())
                .levelOrder(po.getLevelOrder())
                .icon(po.getIcon())
                .color(po.getColor())
                .description(po.getDescription())
                .isAcademic(Boolean.TRUE.equals(po.getIsAcademic()))
                .canBeInspected(Boolean.TRUE.equals(po.getCanBeInspected()))
                .canHaveChildren(Boolean.TRUE.equals(po.getCanHaveChildren()))
                .maxDepth(po.getMaxDepth())
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
