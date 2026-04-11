package com.school.management.infrastructure.persistence.organization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.organization.model.OrgUnit;
import lombok.extern.slf4j.Slf4j;
import com.school.management.domain.organization.model.valueobject.OrgUnitStatus;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织单元仓储实现
 * 映射到 org_units 表
 */
@Slf4j
@Repository
public class OrgUnitRepositoryImpl implements OrgUnitRepository {

    private final OrgUnitMapper orgUnitMapper;
    private final ObjectMapper objectMapper;

    public OrgUnitRepositoryImpl(OrgUnitMapper orgUnitMapper, ObjectMapper objectMapper) {
        this.orgUnitMapper = orgUnitMapper;
        this.objectMapper = objectMapper;
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
    public List<OrgUnit> findByUnitType(String unitType) {
        return orgUnitMapper.findByUnitType(unitType).stream()
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
        List<Long> ancestorIds = Arrays.stream(treePath.split("/"))
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .filter(id -> !id.equals(orgUnitId))
                .collect(Collectors.toList());

        if (ancestorIds.isEmpty()) {
            return new ArrayList<>();
        }

        return findByIds(ancestorIds);
    }

    @Override
    public boolean existsByUnitCode(String unitCode) {
        return orgUnitMapper.findByUnitCode(unitCode) != null;
    }

    @Override
    public long countByParentId(Long parentId) {
        return orgUnitMapper.countByParentId(parentId);
    }

    @Override
    public List<OrgUnit> findAll() {
        return orgUnitMapper.findAllIncludeDisabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<OrgUnitPO> poList = orgUnitMapper.selectBatchIds(ids);
        return poList.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OrgUnit> findByParentIds(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orgUnitMapper.findByParentIds(parentIds).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Set<Long> findParentIdsWithChildren(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(orgUnitMapper.findParentIdsWithChildren(parentIds));
    }

    // ==================== 转换方法 ====================

    private OrgUnit toDomain(OrgUnitPO po) {
        String unitType = po.getUnitType();
        if (unitType == null || unitType.isEmpty()) {
            throw new IllegalStateException(
                "org_units 记录 id=" + po.getId() + " 的 unit_type 为空，数据不一致");
        }

        return OrgUnit.builder()
            .id(po.getId())
            .unitCode(po.getUnitCode())
            .unitName(po.getUnitName())
            .unitType(unitType)
            .parentId(po.getParentId())
            .treePath(po.getTreePath())
            .treeLevel(po.getTreeLevel() != null ? po.getTreeLevel() : 0)
            .sortOrder(po.getSortOrder() != null ? po.getSortOrder() : 0)
            .status(parseStatus(po.getStatus()))
            .headcount(po.getHeadcount())
            .attributes(parseAttributes(po.getAttributes()))
            .mergedIntoId(po.getMergedIntoId())
            .splitFromId(po.getSplitFromId())
            .dissolvedAt(po.getDissolvedAt())
            .dissolvedReason(po.getDissolvedReason())
            .version(po.getVersion() != null ? po.getVersion().longValue() : 0L)
            .createdBy(po.getCreatedBy())
            .updatedBy(po.getUpdatedBy())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private Map<String, Object> parseAttributes(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (Exception e) {
            return null;
        }
    }

    private OrgUnitStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) return OrgUnitStatus.ACTIVE;
        try {
            return OrgUnitStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return OrgUnitStatus.ACTIVE;
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
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE");
        po.setHeadcount(domain.getHeadcount());
        po.setAttributes(serializeAttributes(domain.getAttributes()));
        po.setMergedIntoId(domain.getMergedIntoId());
        po.setSplitFromId(domain.getSplitFromId());
        po.setDissolvedAt(domain.getDissolvedAt());
        po.setDissolvedReason(domain.getDissolvedReason());
        po.setVersion(domain.getVersion() != null ? domain.getVersion().intValue() : 0);
        po.setCreatedBy(domain.getCreatedBy());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getUnitType() != null) {
            po.setUnitType(domain.getUnitType());
        }

        return po;
    }
}
