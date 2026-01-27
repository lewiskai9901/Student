package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.model.UnitCategory;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of OrgUnitRepository.
 * Maps to the existing 'departments' table.
 */
@Repository
public class OrgUnitRepositoryImpl implements OrgUnitRepository {

    private final OrgUnitMapper orgUnitMapper;

    public OrgUnitRepositoryImpl(OrgUnitMapper orgUnitMapper) {
        this.orgUnitMapper = orgUnitMapper;
    }

    @Override
    public Optional<OrgUnit> findById(Long id) {
        OrgUnitPO po = orgUnitMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public OrgUnit save(OrgUnit aggregate) {
        OrgUnitPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            orgUnitMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            orgUnitMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public void delete(OrgUnit aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            orgUnitMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        orgUnitMapper.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return orgUnitMapper.selectById(id) != null;
    }

    @Override
    public Optional<OrgUnit> findByUnitCode(String unitCode) {
        OrgUnitPO po = orgUnitMapper.findByDeptCode(unitCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<OrgUnit> findByUnitType(OrgUnitType unitType) {
        // Departments table doesn't have unit_type, return all enabled
        return orgUnitMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findByParentId(Long parentId) {
        return orgUnitMapper.findByParentId(parentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findRoots() {
        return orgUnitMapper.findRoots().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findDescendants(String treePath) {
        return orgUnitMapper.selectDescendants(treePath).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findAncestors(Long orgUnitId) {
        Optional<OrgUnit> orgUnit = findById(orgUnitId);
        if (orgUnit.isEmpty() || orgUnit.get().getTreePath() == null) {
            return new ArrayList<>();
        }

        String treePath = orgUnit.get().getTreePath();
        String[] ids = treePath.split("/");
        List<OrgUnit> ancestors = new ArrayList<>();
        for (String idStr : ids) {
            if (!idStr.isEmpty()) {
                try {
                    Long id = Long.parseLong(idStr);
                    if (!id.equals(orgUnitId)) {
                        findById(id).ifPresent(ancestors::add);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
        }
        return ancestors;
    }

    @Override
    public boolean existsByUnitCode(String unitCode) {
        return orgUnitMapper.findByDeptCode(unitCode) != null;
    }

    @Override
    public List<OrgUnit> findByLeaderId(Long leaderId) {
        return orgUnitMapper.findAllEnabled().stream()
            .filter(po -> leaderId.equals(po.getLeaderId()))
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public long countByParentId(Long parentId) {
        return orgUnitMapper.countByParentId(parentId);
    }

    @Override
    public List<OrgUnit> findAll() {
        return orgUnitMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findByUnitCategory(UnitCategory unitCategory) {
        return orgUnitMapper.findByUnitCategory(unitCategory.getCode()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // Conversion methods
    private OrgUnit toDomain(OrgUnitPO po) {
        // 解析unitCategory
        UnitCategory category = UnitCategory.ACADEMIC; // 默认为教学单位
        if (po.getUnitCategory() != null) {
            category = UnitCategory.fromCode(po.getUnitCategory());
        }

        // 解析unitType
        OrgUnitType unitType = parseUnitType(po.getUnitType(), po.getDeptLevel());

        return OrgUnit.builder()
            .id(po.getId())
            .unitCode(po.getDeptCode())
            .unitName(po.getDeptName())
            .unitType(unitType)
            .unitCategory(category)
            .parentId(po.getParentId())
            .treePath(po.getDeptPath())
            .treeLevel(po.getDeptLevel() != null ? po.getDeptLevel() : 0)
            .leaderId(po.getLeaderId())
            .deputyLeaderIds(new ArrayList<>()) // Not in departments table
            .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
            .enabled(po.getStatus() != null && po.getStatus() == 1)
            .createdBy(null) // Not tracked in departments
            .build();
    }

    /**
     * 解析unitType字符串为枚举值
     */
    private OrgUnitType parseUnitType(String unitTypeStr, Integer deptLevel) {
        if (unitTypeStr != null && !unitTypeStr.isEmpty()) {
            try {
                return OrgUnitType.valueOf(unitTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果无法解析，根据层级推断
            }
        }
        // 根据层级推断类型
        if (deptLevel == null) return OrgUnitType.DEPARTMENT;
        return switch (deptLevel) {
            case 1 -> OrgUnitType.SCHOOL;
            case 2 -> OrgUnitType.COLLEGE;
            case 3 -> OrgUnitType.DEPARTMENT;
            case 4 -> OrgUnitType.TEACHING_GROUP;
            default -> OrgUnitType.DEPARTMENT;
        };
    }

    private OrgUnitPO toPO(OrgUnit domain) {
        OrgUnitPO po = new OrgUnitPO();
        po.setId(domain.getId());
        po.setDeptCode(domain.getUnitCode());
        po.setDeptName(domain.getUnitName());
        po.setDeptDesc(null); // Description not in domain
        po.setParentId(domain.getParentId());
        po.setDeptLevel(domain.getTreeLevel());
        po.setDeptPath(domain.getTreePath());
        po.setLeaderId(domain.getLeaderId());
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.isEnabled() ? 1 : 0);
        // 保存unitType
        if (domain.getUnitType() != null) {
            po.setUnitType(domain.getUnitType().name());
        }
        // 保存unitCategory
        if (domain.getUnitCategory() != null) {
            po.setUnitCategory(domain.getUnitCategory().getCode());
        }
        return po;
    }
}
