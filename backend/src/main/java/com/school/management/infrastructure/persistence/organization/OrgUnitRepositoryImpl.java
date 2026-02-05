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
 * 组织单元仓储实现
 * 映射到 org_units 表
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
        OrgUnitPO po = orgUnitMapper.findByUnitCode(unitCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<OrgUnit> findByUnitType(OrgUnitType unitType) {
        return orgUnitMapper.findByUnitType(unitType.name()).stream()
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
        return orgUnitMapper.findByUnitCode(unitCode) != null;
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

    // ==================== 转换方法 ====================

    private OrgUnit toDomain(OrgUnitPO po) {
        // 解析 unitCategory
        UnitCategory category = UnitCategory.ACADEMIC; // 默认为教学单位
        if (po.getUnitCategory() != null) {
            category = UnitCategory.fromCode(po.getUnitCategory());
        }

        // 解析 unitType
        OrgUnitType unitType = parseUnitType(po.getUnitType(), po.getTreeLevel());

        return OrgUnit.builder()
            .id(po.getId())
            .unitCode(po.getUnitCode())
            .unitName(po.getUnitName())
            .unitType(unitType)
            .unitCategory(category)
            .parentId(po.getParentId())
            .treePath(po.getTreePath())
            .treeLevel(po.getTreeLevel() != null ? po.getTreeLevel() : 0)
            .leaderId(po.getLeaderId())
            .deputyLeaderIds(parseDeputyLeaderIds(po.getDeputyLeaderIds()))
            .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
            .enabled(po.getStatus() != null && po.getStatus() == 1)
            .createdBy(po.getCreatedBy())
            .build();
    }

    /**
     * 解析 unitType 字符串为枚举值
     */
    private OrgUnitType parseUnitType(String unitTypeStr, Integer treeLevel) {
        if (unitTypeStr != null && !unitTypeStr.isEmpty()) {
            try {
                return OrgUnitType.valueOf(unitTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 如果无法解析，根据层级推断
            }
        }
        // 根据层级推断类型
        if (treeLevel == null) return OrgUnitType.DEPARTMENT;
        return switch (treeLevel) {
            case 1 -> OrgUnitType.SCHOOL;
            case 2 -> OrgUnitType.COLLEGE;
            case 3 -> OrgUnitType.DEPARTMENT;
            case 4 -> OrgUnitType.TEACHING_GROUP;
            default -> OrgUnitType.DEPARTMENT;
        };
    }

    /**
     * 解析副职ID列表JSON
     */
    private List<Long> parseDeputyLeaderIds(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        // 简单JSON解析，假设格式为 [1,2,3]
        try {
            json = json.replace("[", "").replace("]", "").trim();
            if (json.isEmpty()) {
                return new ArrayList<>();
            }
            List<Long> ids = new ArrayList<>();
            for (String idStr : json.split(",")) {
                ids.add(Long.parseLong(idStr.trim()));
            }
            return ids;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private OrgUnitPO toPO(OrgUnit domain) {
        OrgUnitPO po = new OrgUnitPO();
        po.setId(domain.getId());
        po.setUnitCode(domain.getUnitCode());
        po.setUnitName(domain.getUnitName());
        po.setParentId(domain.getParentId());
        po.setTreeLevel(domain.getTreeLevel());
        po.setTreePath(domain.getTreePath());
        po.setLeaderId(domain.getLeaderId());
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.isEnabled() ? 1 : 0);

        // 保存 unitType
        if (domain.getUnitType() != null) {
            po.setUnitType(domain.getUnitType().name());
        }

        // 保存 unitCategory
        if (domain.getUnitCategory() != null) {
            po.setUnitCategory(domain.getUnitCategory().getCode());
        }

        // 保存副职ID列表
        if (domain.getDeputyLeaderIds() != null && !domain.getDeputyLeaderIds().isEmpty()) {
            po.setDeputyLeaderIds(domain.getDeputyLeaderIds().toString());
        }

        return po;
    }
}
