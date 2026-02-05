package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.repository.OrgTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 组织类型仓储实现
 */
@Repository
public class OrgTypeRepositoryImpl implements OrgTypeRepository {

    private final OrgTypeMapper orgTypeMapper;

    public OrgTypeRepositoryImpl(OrgTypeMapper orgTypeMapper) {
        this.orgTypeMapper = orgTypeMapper;
    }

    @Override
    public OrgType save(OrgType orgType) {
        OrgTypePO po = toPO(orgType);
        if (po.getId() == null) {
            orgTypeMapper.insert(po);
        } else {
            orgTypeMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<OrgType> findById(Long id) {
        OrgTypePO po = orgTypeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<OrgType> findByTypeCode(String typeCode) {
        OrgTypePO po = orgTypeMapper.findByTypeCode(typeCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<OrgType> findAll() {
        LambdaQueryWrapper<OrgTypePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(OrgTypePO::getLevelOrder)
               .orderByAsc(OrgTypePO::getSortOrder)
               .orderByAsc(OrgTypePO::getId);
        return orgTypeMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findAllEnabled() {
        return orgTypeMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findTopLevelTypes() {
        return orgTypeMapper.findTopLevelTypes().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findByParentTypeCode(String parentTypeCode) {
        return orgTypeMapper.findByParentTypeCode(parentTypeCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrgType> findByLevelOrder(Integer levelOrder) {
        return orgTypeMapper.findByLevelOrder(levelOrder).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTypeCode(String typeCode) {
        return orgTypeMapper.existsByTypeCode(typeCode) > 0;
    }

    @Override
    public boolean isTypeInUse(String typeCode) {
        return orgTypeMapper.countOrgUnitsByTypeCode(typeCode) > 0;
    }

    @Override
    public void deleteById(Long id) {
        orgTypeMapper.deleteById(id);
    }

    // ==================== 转换方法 ====================

    private OrgTypePO toPO(OrgType domain) {
        OrgTypePO po = new OrgTypePO();
        po.setId(domain.getId());
        po.setTypeCode(domain.getTypeCode());
        po.setTypeName(domain.getTypeName());
        po.setParentTypeCode(domain.getParentTypeCode());
        po.setLevelOrder(domain.getLevelOrder());
        po.setIcon(domain.getIcon());
        po.setColor(domain.getColor());
        po.setDescription(domain.getDescription());
        po.setCanHaveClasses(domain.isCanHaveClasses());
        po.setCanHaveStudents(domain.isCanHaveStudents());
        po.setCanBeInspected(domain.isCanBeInspected());
        po.setCanHaveLeader(domain.isCanHaveLeader());
        po.setIsSystem(domain.isSystem());
        po.setIsEnabled(domain.isEnabled());
        po.setSortOrder(domain.getSortOrder());
        return po;
    }

    private OrgType toDomain(OrgTypePO po) {
        return OrgType.builder()
                .id(po.getId())
                .typeCode(po.getTypeCode())
                .typeName(po.getTypeName())
                .parentTypeCode(po.getParentTypeCode())
                .levelOrder(po.getLevelOrder())
                .icon(po.getIcon())
                .color(po.getColor())
                .description(po.getDescription())
                .canHaveClasses(Boolean.TRUE.equals(po.getCanHaveClasses()))
                .canHaveStudents(Boolean.TRUE.equals(po.getCanHaveStudents()))
                .canBeInspected(Boolean.TRUE.equals(po.getCanBeInspected()))
                .canHaveLeader(Boolean.TRUE.equals(po.getCanHaveLeader()))
                .isSystem(Boolean.TRUE.equals(po.getIsSystem()))
                .isEnabled(Boolean.TRUE.equals(po.getIsEnabled()))
                .sortOrder(po.getSortOrder())
                .build();
    }
}
